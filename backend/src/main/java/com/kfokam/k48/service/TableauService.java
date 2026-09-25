package com.kfokam.k48.service;

import com.kfokam.k48.domain.Etudiant;
import com.kfokam.k48.domain.Exercice;
import com.kfokam.k48.domain.Presence;
import com.kfokam.k48.domain.Relecture;
import com.kfokam.k48.domain.SessionCours;
import com.kfokam.k48.domain.StatutExercice;
import com.kfokam.k48.dto.DetailPresenceResponse;
import com.kfokam.k48.dto.LigneTableauResponse;
import com.kfokam.k48.error.BusinessException;
import com.kfokam.k48.repository.EtudiantRepository;
import com.kfokam.k48.repository.ExerciceRepository;
import com.kfokam.k48.repository.PresenceRepository;
import com.kfokam.k48.repository.PromotionRepository;
import com.kfokam.k48.repository.RelectureRepository;
import com.kfokam.k48.repository.SessionCoursRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * EF10 : tableau de présence et de notes du formateur. RG18 (moyenne calculée ici, jamais côté
 * frontend), RG19 (relectures en attente), H11 (detailPresences), H12 (exercicesEnAttente).
 * Un aller par table (ENF3), aucune requête par étudiant.
 */
@Service
public class TableauService {

    private final PromotionRepository promotionRepository;
    private final EtudiantRepository etudiantRepository;
    private final SessionCoursRepository sessionCoursRepository;
    private final PresenceRepository presenceRepository;
    private final ExerciceRepository exerciceRepository;
    private final RelectureRepository relectureRepository;

    public TableauService(
            PromotionRepository promotionRepository,
            EtudiantRepository etudiantRepository,
            SessionCoursRepository sessionCoursRepository,
            PresenceRepository presenceRepository,
            ExerciceRepository exerciceRepository,
            RelectureRepository relectureRepository) {
        this.promotionRepository = promotionRepository;
        this.etudiantRepository = etudiantRepository;
        this.sessionCoursRepository = sessionCoursRepository;
        this.presenceRepository = presenceRepository;
        this.exerciceRepository = exerciceRepository;
        this.relectureRepository = relectureRepository;
    }

    @Transactional(readOnly = true)
    public List<LigneTableauResponse> obtenirTableau(Long promotionId) {
        if (!promotionRepository.existsById(promotionId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "PROMOTION_INCONNUE", "Cette promotion n'existe pas.");
        }

        List<Etudiant> etudiants = etudiantRepository.findByPromotionIdOrderByNomAsc(promotionId);
        List<Long> etudiantIds = etudiants.stream().map(Etudiant::getId).toList();

        List<SessionCours> sessions = sessionCoursRepository.findByPromotionIdOrderByOuvertureAtAsc(promotionId);
        List<Long> sessionIds = sessions.stream().map(SessionCours::getId).toList();

        // etudiantId -> sessionId -> presence
        Map<Long, Map<Long, Presence>> presencesParEtudiantEtSession = new HashMap<>();
        for (Presence presence : presenceRepository.findBySessionIdIn(sessionIds)) {
            presencesParEtudiantEtSession
                    .computeIfAbsent(presence.getEtudiant().getId(), id -> new HashMap<>())
                    .put(presence.getSession().getId(), presence);
        }

        Map<Long, List<Exercice>> exercicesParAuteur = new HashMap<>();
        for (Exercice exercice : exerciceRepository.findByAuteurIdIn(etudiantIds)) {
            exercicesParAuteur.computeIfAbsent(exercice.getAuteur().getId(), id -> new ArrayList<>()).add(exercice);
        }

        Map<Long, Long> relecturesEnAttenteParRelecteur = new HashMap<>();
        for (Relecture relecture : relectureRepository.findByRelecteurIdInAndRendueAtIsNull(etudiantIds)) {
            relecturesEnAttenteParRelecteur.merge(relecture.getRelecteur().getId(), 1L, Long::sum);
        }

        Map<Long, List<Integer>> notesParAuteur = new HashMap<>();
        for (Relecture relecture : relectureRepository.findByExercice_AuteurIdIn(etudiantIds)) {
            if (relecture.getNote() != null) {
                notesParAuteur.computeIfAbsent(relecture.getExercice().getAuteur().getId(), id -> new ArrayList<>())
                        .add(relecture.getNote());
            }
        }

        List<LigneTableauResponse> lignes = new ArrayList<>();
        for (Etudiant etudiant : etudiants) {
            Map<Long, Presence> presencesDeCetEtudiant = presencesParEtudiantEtSession.getOrDefault(etudiant.getId(), Map.of());

            List<DetailPresenceResponse> detailPresences = sessions.stream()
                    .map(session -> {
                        Presence presence = presencesDeCetEtudiant.get(session.getId());
                        return new DetailPresenceResponse(
                                session.getId(),
                                session.getTitre(),
                                presence != null,
                                presence != null ? presence.getSource().name() : null);
                    })
                    .toList();

            List<Exercice> exercicesDeCetEtudiant = exercicesParAuteur.getOrDefault(etudiant.getId(), List.of());
            long exercicesEnAttente = exercicesDeCetEtudiant.stream()
                    .filter(exercice -> exercice.getStatut() != StatutExercice.RELU)
                    .count();

            List<Integer> notes = notesParAuteur.get(etudiant.getId());
            Double moyenne = (notes == null || notes.isEmpty()) ? null : arrondirADeuxDecimales(moyenneDe(notes));

            lignes.add(new LigneTableauResponse(
                    etudiant.getId(),
                    etudiant.getNom(),
                    presencesDeCetEtudiant.size(),
                    exercicesDeCetEtudiant.size(),
                    moyenne,
                    relecturesEnAttenteParRelecteur.getOrDefault(etudiant.getId(), 0L).intValue(),
                    detailPresences,
                    (int) exercicesEnAttente));
        }

        return lignes;
    }

    private double moyenneDe(List<Integer> notes) {
        return notes.stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    private double arrondirADeuxDecimales(double valeur) {
        return Math.round(valeur * 100.0) / 100.0;
    }
}
