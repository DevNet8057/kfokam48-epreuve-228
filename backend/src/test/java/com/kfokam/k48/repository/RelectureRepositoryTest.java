package com.kfokam.k48.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfokam.k48.domain.Etudiant;
import com.kfokam.k48.domain.Exercice;
import com.kfokam.k48.domain.Promotion;
import com.kfokam.k48.domain.Relecture;
import com.kfokam.k48.domain.SessionCours;
import com.kfokam.k48.domain.StatutExercice;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * K48-29 : le schéma réel (migrations Flyway V1 à V4 sur H2) accepte deux relecteurs distincts pour
 * un exercice (C2, RG12 v2) et refuse deux fois le même relecteur.
 */
@SpringBootTest
@Transactional
class RelectureRepositoryTest {

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private EtudiantRepository etudiantRepository;

    @Autowired
    private SessionCoursRepository sessionCoursRepository;

    @Autowired
    private ExerciceRepository exerciceRepository;

    @Autowired
    private RelectureRepository relectureRepository;

    private Exercice exercice;

    @BeforeEach
    void deposerUnExercice() {
        Instant maintenant = Instant.now();
        Promotion promotion = promotionRepository.findById(1L).orElseThrow();
        SessionCours session = sessionCoursRepository.save(
                new SessionCours("Cours", promotion, "GGGGGG", maintenant, maintenant.plusSeconds(900)));
        exercice = exerciceRepository.save(new Exercice(session, etudiantRepository.findById(1L).orElseThrow(),
                "https://exemple.test", StatutExercice.EN_ATTENTE_RELECTURE, maintenant));
    }

    private Etudiant etudiant(long id) {
        return etudiantRepository.findById(id).orElseThrow();
    }

    @Test
    void accepte_deux_relecteurs_distincts_pour_un_meme_exercice() {
        relectureRepository.saveAndFlush(new Relecture(exercice, etudiant(2L)));
        relectureRepository.saveAndFlush(new Relecture(exercice, etudiant(3L)));

        assertThat(relectureRepository.findByExercice_Id(exercice.getId())).hasSize(2);
    }

    @Test
    void refuse_deux_fois_le_meme_relecteur_pour_un_meme_exercice() {
        relectureRepository.saveAndFlush(new Relecture(exercice, etudiant(2L)));

        assertThatThrownBy(() -> relectureRepository.saveAndFlush(new Relecture(exercice, etudiant(2L))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
