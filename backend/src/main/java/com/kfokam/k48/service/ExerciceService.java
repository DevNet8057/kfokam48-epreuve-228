package com.kfokam.k48.service;

import com.kfokam.k48.domain.Etudiant;
import com.kfokam.k48.domain.Exercice;
import com.kfokam.k48.domain.Relecture;
import com.kfokam.k48.domain.SessionCours;
import com.kfokam.k48.domain.StatutExercice;
import com.kfokam.k48.dto.DeposerExerciceRequete;
import com.kfokam.k48.dto.ExerciceResponse;
import com.kfokam.k48.dto.MonExerciceResponse;
import java.util.Objects;
import com.kfokam.k48.dto.RemplacerLienRequete;
import com.kfokam.k48.error.BusinessException;
import com.kfokam.k48.repository.EtudiantRepository;
import com.kfokam.k48.repository.ExerciceRepository;
import com.kfokam.k48.repository.RelectureRepository;
import com.kfokam.k48.repository.SessionCoursRepository;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * EF6/EF7 : dépôt de l'exercice et tirage du relecteur. Règles RG2 (jamais l'auteur),
 * RG9 (un seul dépôt), RG10 (dépôt possible même absent), RG11 (lien absolu http/https),
 * RG12 (le moins chargé parmi les présents), RG13 (SANS_RELECTEUR si personne), RG20/RG22.
 */
@Service
public class ExerciceService {

    private static final Set<String> SCHEMES_AUTORISES = Set.of("http", "https");

    private final SessionCoursRepository sessionCoursRepository;
    private final EtudiantRepository etudiantRepository;
    private final ExerciceRepository exerciceRepository;
    private final RelectureRepository relectureRepository;
    private final AttributionService attributionService;
    private final Clock clock;

    public ExerciceService(
            SessionCoursRepository sessionCoursRepository,
            EtudiantRepository etudiantRepository,
            ExerciceRepository exerciceRepository,
            RelectureRepository relectureRepository,
            AttributionService attributionService,
            Clock clock) {
        this.sessionCoursRepository = sessionCoursRepository;
        this.etudiantRepository = etudiantRepository;
        this.exerciceRepository = exerciceRepository;
        this.relectureRepository = relectureRepository;
        this.attributionService = attributionService;
        this.clock = clock;
    }

    @Transactional
    public ExerciceResponse deposerExercice(DeposerExerciceRequete requete) {
        validerLien(requete.lien());

        Etudiant auteur = etudiantRepository.findById(requete.etudiantId())
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "ETUDIANT_INCONNU", "Cet étudiant n'existe pas."));

        SessionCours session = sessionCoursRepository.findById(requete.sessionId())
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "SESSION_INCONNUE", "Cette session n'existe pas."));

        if (!session.getPromotion().getId().equals(auteur.getPromotion().getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ETUDIANT_HORS_PROMOTION",
                    "Cette session n'appartient pas à votre promotion.");
        }

        if (session.getClotureAt() != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "SESSION_CLOTUREE", "Cette session est clôturée.");
        }

        if (exerciceRepository.existsBySessionIdAndAuteurId(session.getId(), auteur.getId())) {
            throw new BusinessException(HttpStatus.CONFLICT, "EXERCICE_DEJA_DEPOSE",
                    "Vous avez déjà déposé un exercice pour cette session.");
        }

        Exercice exercice = new Exercice(session, auteur, requete.lien(), StatutExercice.SANS_RELECTEUR, clock.instant());

        List<Long> relecteurIds = attributionService.tirerRelecteurs(
                session.getId(), Set.of(auteur.getId()), AttributionService.NOMBRE_RELECTEURS);
        if (!relecteurIds.isEmpty()) {
            exercice.setStatut(StatutExercice.EN_ATTENTE_RELECTURE);
        }

        exercice = exerciceRepository.save(exercice);

        for (Long relecteurId : relecteurIds) {
            Etudiant relecteur = etudiantRepository.getReferenceById(relecteurId);
            relectureRepository.save(new Relecture(exercice, relecteur));
        }

        return ExerciceResponse.from(exercice);
    }

    /**
     * EF11 : exercices de l'étudiant avec la note reçue. RG17 : jamais l'identité des relecteurs.
     * RG18 v2 : moyenne des notes rendues ; provisoire si un relecteur assigné n'a pas encore rendu.
     */
    @Transactional(readOnly = true)
    public List<MonExerciceResponse> listerExercicesDeLEtudiant(Long etudiantId) {
        if (!etudiantRepository.existsById(etudiantId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "ETUDIANT_INCONNU", "Cet étudiant n'existe pas.");
        }
        return exerciceRepository.findByAuteurIdOrderByDeposeAtDesc(etudiantId).stream()
                .map(this::versMonExercice)
                .toList();
    }

    private MonExerciceResponse versMonExercice(Exercice exercice) {
        List<Relecture> relectures = relectureRepository.findByExercice_Id(exercice.getId());
        List<Relecture> rendues = relectures.stream().filter(r -> r.getRendueAt() != null).toList();
        List<Integer> notes = rendues.stream().map(Relecture::getNote).filter(Objects::nonNull).toList();
        Double note = notes.isEmpty()
                ? null
                : Math.round(notes.stream().mapToInt(Integer::intValue).average().orElse(0) * 100.0) / 100.0;
        boolean provisoire = !notes.isEmpty() && rendues.size() < relectures.size();
        List<String> commentaires = rendues.stream().map(Relecture::getCommentaire).filter(Objects::nonNull).toList();
        return new MonExerciceResponse(
                exercice.getId(),
                exercice.getSession().getId(),
                exercice.getSession().getTitre(),
                exercice.getLien(),
                exercice.getStatut().name(),
                note,
                provisoire,
                commentaires);
    }

    /**
     * EF12 : remplacer le lien tant qu'aucune relecture n'a commencé (RG16) et que la session
     * n'est pas clôturée (RG20). RG2/RG15-esprit : seul l'auteur, identifié par X-Etudiant-Id.
     */
    @Transactional
    public ExerciceResponse remplacerLien(Long exerciceId, Long etudiantIdAppelant, RemplacerLienRequete requete) {
        validerLien(requete.lien());

        Exercice exercice = exerciceRepository.findById(exerciceId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "EXERCICE_INCONNU", "Cet exercice n'existe pas."));

        if (!exercice.getAuteur().getId().equals(etudiantIdAppelant)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "PAS_AUTEUR", "Vous n'êtes pas l'auteur de cet exercice.");
        }

        if (exercice.getSession().getClotureAt() != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "REMPLACEMENT_IMPOSSIBLE", "Cette session est clôturée.");
        }

        boolean relectureCommencee = relectureRepository.findByExercice_Id(exercice.getId()).stream()
                .anyMatch(relecture -> relecture.getOuverteAt() != null);
        if (relectureCommencee) {
            throw new BusinessException(HttpStatus.CONFLICT, "REMPLACEMENT_IMPOSSIBLE",
                    "Un relecteur a déjà commencé à relire cet exercice.");
        }

        exercice.setLien(requete.lien());
        return ExerciceResponse.from(exercice);
    }

    private void validerLien(String lien) {
        try {
            URI uri = new URI(lien);
            if (!uri.isAbsolute() || !SCHEMES_AUTORISES.contains(uri.getScheme().toLowerCase())) {
                throw lienInvalide();
            }
        } catch (URISyntaxException | NullPointerException erreur) {
            throw lienInvalide();
        }
    }

    private BusinessException lienInvalide() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "LIEN_INVALIDE", "Le lien doit être une URL absolue http(s).");
    }
}
