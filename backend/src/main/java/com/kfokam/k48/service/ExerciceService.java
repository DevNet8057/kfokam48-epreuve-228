package com.kfokam.k48.service;

import com.kfokam.k48.domain.Etudiant;
import com.kfokam.k48.domain.Exercice;
import com.kfokam.k48.domain.Relecture;
import com.kfokam.k48.domain.SessionCours;
import com.kfokam.k48.domain.StatutExercice;
import com.kfokam.k48.dto.DeposerExerciceRequete;
import com.kfokam.k48.dto.ExerciceResponse;
import com.kfokam.k48.error.BusinessException;
import com.kfokam.k48.repository.EtudiantRepository;
import com.kfokam.k48.repository.ExerciceRepository;
import com.kfokam.k48.repository.PresenceRepository;
import com.kfokam.k48.repository.RelectureRepository;
import com.kfokam.k48.repository.SessionCoursRepository;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final PresenceRepository presenceRepository;
    private final RelectureRepository relectureRepository;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public ExerciceService(
            SessionCoursRepository sessionCoursRepository,
            EtudiantRepository etudiantRepository,
            ExerciceRepository exerciceRepository,
            PresenceRepository presenceRepository,
            RelectureRepository relectureRepository,
            Clock clock) {
        this.sessionCoursRepository = sessionCoursRepository;
        this.etudiantRepository = etudiantRepository;
        this.exerciceRepository = exerciceRepository;
        this.presenceRepository = presenceRepository;
        this.relectureRepository = relectureRepository;
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

        List<Long> relecteurIds = tirerRelecteurs(session.getId(), auteur.getId());
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
     * RG12 v2 (C2, étape 3) : jusqu'à DEUX relecteurs distincts, parmi les présents autres que
     * l'auteur, les moins chargés en priorité, tirage au hasard en cas d'égalité. Un seul candidat
     * disponible -> un seul relecteur tiré (le second sera retenté à la prochaine présence, RG13).
     */
    private static final int NOMBRE_RELECTEURS = 2;

    private List<Long> tirerRelecteurs(Long sessionId, Long auteurId) {
        List<Long> candidats = new ArrayList<>(presenceRepository.trouverIdsEtudiantsPresents(sessionId).stream()
                .distinct()
                .filter(id -> !id.equals(auteurId))
                .toList());

        List<Long> retenus = new ArrayList<>();
        for (int i = 0; i < NOMBRE_RELECTEURS && !candidats.isEmpty(); i++) {
            long chargeMinimale = candidats.stream()
                    .mapToLong(relectureRepository::countByRelecteurId)
                    .min()
                    .orElse(0);

            List<Long> moinsCharges = candidats.stream()
                    .filter(id -> relectureRepository.countByRelecteurId(id) == chargeMinimale)
                    .sorted(Comparator.naturalOrder())
                    .toList();

            Long tire = moinsCharges.get(random.nextInt(moinsCharges.size()));
            retenus.add(tire);
            candidats.remove(tire);
        }
        return retenus;
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
