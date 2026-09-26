package com.kfokam.k48.service;

import com.kfokam.k48.domain.Exercice;
import com.kfokam.k48.domain.Relecture;
import com.kfokam.k48.domain.SessionCours;
import com.kfokam.k48.domain.StatutExercice;
import com.kfokam.k48.repository.EtudiantRepository;
import com.kfokam.k48.repository.ExerciceRepository;
import com.kfokam.k48.repository.PresenceRepository;
import com.kfokam.k48.repository.RelectureRepository;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Attribution des relecteurs. RG12 v2 (C2) : jusqu'à deux relecteurs distincts, parmi les présents
 * de la session, jamais l'auteur (RG2), les moins chargés en priorité, au hasard en cas d'égalité.
 * RG13 : les relecteurs manquants sont retentés à chaque nouvelle présence.
 */
@Service
public class AttributionService {

    static final int NOMBRE_RELECTEURS = 2;

    private final PresenceRepository presenceRepository;
    private final RelectureRepository relectureRepository;
    private final ExerciceRepository exerciceRepository;
    private final EtudiantRepository etudiantRepository;
    private final SecureRandom random = new SecureRandom();

    public AttributionService(
            PresenceRepository presenceRepository,
            RelectureRepository relectureRepository,
            ExerciceRepository exerciceRepository,
            EtudiantRepository etudiantRepository) {
        this.presenceRepository = presenceRepository;
        this.relectureRepository = relectureRepository;
        this.exerciceRepository = exerciceRepository;
        this.etudiantRepository = etudiantRepository;
    }

    /** Tire au plus {@code nombre} relecteurs parmi les présents de la session, hors {@code exclus}. */
    public List<Long> tirerRelecteurs(Long sessionId, Set<Long> exclus, int nombre) {
        List<Long> candidats = new ArrayList<>(presenceRepository.trouverIdsEtudiantsPresents(sessionId).stream()
                .distinct()
                .filter(id -> !exclus.contains(id))
                .toList());

        List<Long> retenus = new ArrayList<>();
        for (int i = 0; i < nombre && !candidats.isEmpty(); i++) {
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

    /**
     * RG13 : après une nouvelle présence, complète les exercices non encore relus de la session qui
     * ont moins de deux relecteurs. Rien après la clôture (RG20). Un exercice déjà RELU par son seul
     * relecteur n'en reçoit pas de second (H15).
     */
    @Transactional
    public void retenterAttribution(SessionCours session) {
        if (session.getClotureAt() != null) {
            return;
        }
        for (Exercice exercice : exerciceRepository.findBySessionId(session.getId())) {
            if (exercice.getStatut() == StatutExercice.RELU) {
                continue;
            }
            List<Relecture> existantes = relectureRepository.findByExercice_Id(exercice.getId());
            int manquants = NOMBRE_RELECTEURS - existantes.size();
            if (manquants <= 0) {
                continue;
            }
            Set<Long> exclus = new HashSet<>();
            exclus.add(exercice.getAuteur().getId());
            existantes.forEach(relecture -> exclus.add(relecture.getRelecteur().getId()));

            List<Long> tires = tirerRelecteurs(session.getId(), exclus, manquants);
            for (Long relecteurId : tires) {
                relectureRepository.save(new Relecture(exercice, etudiantRepository.getReferenceById(relecteurId)));
            }
            if (!tires.isEmpty() && exercice.getStatut() == StatutExercice.SANS_RELECTEUR) {
                exercice.setStatut(StatutExercice.EN_ATTENTE_RELECTURE);
            }
        }
    }
}
