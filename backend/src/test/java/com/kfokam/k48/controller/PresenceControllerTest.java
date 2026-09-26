package com.kfokam.k48.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfokam.k48.domain.Promotion;
import com.kfokam.k48.domain.SessionCours;
import com.kfokam.k48.repository.PromotionRepository;
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
 * B6 : test d'intégration MockMvc sur POST /api/presences (EF3) — 201, 409, 410.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PresenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private SessionCoursRepository sessionCoursRepository;

    private SessionCours creerSession(String code, Instant ouvertureAt, Instant expirationAt) {
        Promotion promotion = promotionRepository.findById(1L).orElseThrow();
        return sessionCoursRepository.save(new SessionCours("Cours de test", promotion, code, ouvertureAt, expirationAt));
    }

    @Test
    void marque_la_presence_et_renvoie_201() throws Exception {
        Instant maintenant = Instant.now();
        SessionCours session = creerSession("AAAAAA", maintenant.minus(1, ChronoUnit.MINUTES), maintenant.plus(14, ChronoUnit.MINUTES));

        mockMvc.perform(post("/api/presences")
                        .contentType("application/json")
                        .content("{\"code\":\"AAAAAA\",\"etudiantId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId", is(session.getId().intValue())))
                .andExpect(jsonPath("$.etudiantId", is(1)))
                .andExpect(jsonPath("$.source", is("ETUDIANT")));
    }

    @Test
    void refuse_une_deuxieme_presence_avec_409() throws Exception {
        Instant maintenant = Instant.now();
        creerSession("BBBBBB", maintenant.minus(1, ChronoUnit.MINUTES), maintenant.plus(14, ChronoUnit.MINUTES));

        mockMvc.perform(post("/api/presences")
                        .contentType("application/json")
                        .content("{\"code\":\"BBBBBB\",\"etudiantId\":2}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/presences")
                        .contentType("application/json")
                        .content("{\"code\":\"BBBBBB\",\"etudiantId\":2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DEJA_PRESENT")));
    }

    @Test
    void refuse_un_code_expire_avec_410() throws Exception {
        Instant maintenant = Instant.now();
        creerSession("CCCCCC", maintenant.minus(20, ChronoUnit.MINUTES), maintenant.minus(5, ChronoUnit.MINUTES));

        mockMvc.perform(post("/api/presences")
                        .contentType("application/json")
                        .content("{\"code\":\"CCCCCC\",\"etudiantId\":3}"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code", is("CODE_EXPIRE")));
    }

    @Test
    void refuse_un_code_inconnu_avec_400() throws Exception {
        mockMvc.perform(post("/api/presences")
                        .contentType("application/json")
                        .content("{\"code\":\"ZZZZZZ\",\"etudiantId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("CODE_INCONNU")));
    }

    @Test
    void refuse_le_code_d_une_session_cloturee_avant_15_minutes_avec_410() throws Exception {
        Instant maintenant = Instant.now();
        SessionCours session = creerSession("DDDDDD", maintenant.minus(1, ChronoUnit.MINUTES), maintenant.plus(14, ChronoUnit.MINUTES));
        session.cloturer(maintenant);
        sessionCoursRepository.save(session);

        mockMvc.perform(post("/api/presences")
                        .contentType("application/json")
                        .content("{\"code\":\"DDDDDD\",\"etudiantId\":2}"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code", is("CODE_EXPIRE")));
    }

    @Test
    void cloture_une_session_puis_refuse_une_seconde_cloture_avec_409() throws Exception {
        Instant maintenant = Instant.now();
        SessionCours session = creerSession("EEEEEE", maintenant, maintenant.plus(15, ChronoUnit.MINUTES));

        mockMvc.perform(post("/api/sessions/" + session.getId() + "/cloture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clotureAt").exists());

        mockMvc.perform(post("/api/sessions/" + session.getId() + "/cloture"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("SESSION_CLOTUREE")));
    }
}
