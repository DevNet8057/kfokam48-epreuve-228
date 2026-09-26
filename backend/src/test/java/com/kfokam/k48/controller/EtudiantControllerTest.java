package com.kfokam.k48.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfokam.k48.domain.Etudiant;
import com.kfokam.k48.domain.Exercice;
import com.kfokam.k48.domain.Promotion;
import com.kfokam.k48.domain.Relecture;
import com.kfokam.k48.domain.SessionCours;
import com.kfokam.k48.domain.StatutExercice;
import com.kfokam.k48.repository.EtudiantRepository;
import com.kfokam.k48.repository.ExerciceRepository;
import com.kfokam.k48.repository.PromotionRepository;
import com.kfokam.k48.repository.RelectureRepository;
import com.kfokam.k48.repository.SessionCoursRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * K48-14 : GET /api/etudiants/{id}/exercices. RG17 : la réponse ne contient aucune donnée
 * permettant d'identifier les relecteurs. RG18 v2 : une seule note rendue sur deux = provisoire.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EtudiantControllerTest {

    @Autowired
    private MockMvc mockMvc;

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

    @Test
    void montre_la_note_provisoire_et_le_commentaire_sans_jamais_le_relecteur() throws Exception {
        Instant maintenant = Instant.now();
        Promotion promotion = promotionRepository.findById(1L).orElseThrow();
        SessionCours session = sessionCoursRepository.save(new SessionCours("Cours relu", promotion, "FFFFFF",
                maintenant.minus(1, ChronoUnit.MINUTES), maintenant.plus(14, ChronoUnit.MINUTES)));
        Etudiant auteur = etudiantRepository.findById(1L).orElseThrow();
        Exercice exercice = exerciceRepository.save(
                new Exercice(session, auteur, "https://exemple.test/ex", StatutExercice.EN_COURS_RELECTURE, maintenant));

        Relecture rendue = new Relecture(exercice, etudiantRepository.findById(2L).orElseThrow());
        rendue.setNote(14);
        rendue.setCommentaire("Clair et bien structuré");
        rendue.setOuverteAt(maintenant);
        rendue.setRendueAt(maintenant);
        relectureRepository.save(rendue);
        relectureRepository.save(new Relecture(exercice, etudiantRepository.findById(3L).orElseThrow()));

        String corps = mockMvc.perform(get("/api/etudiants/1/exercices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].note", is(14.0)))
                .andExpect(jsonPath("$[0].provisoire", is(true)))
                .andExpect(jsonPath("$[0].commentaires[0]", is("Clair et bien structuré")))
                .andReturn().getResponse().getContentAsString();

        // RG17 : ni champ relecteur, ni nom des deux relecteurs (Boris, Chloé) dans la réponse.
        assertThat(corps).doesNotContainIgnoringCase("relecteur");
        assertThat(corps).doesNotContain("Boris").doesNotContain("Chlo");
    }

    @Test
    void refuse_un_etudiant_inconnu_avec_404() throws Exception {
        mockMvc.perform(get("/api/etudiants/999/exercices"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("ETUDIANT_INCONNU")));
    }
}
