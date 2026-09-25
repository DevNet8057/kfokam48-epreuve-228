package com.kfokam.k48.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.when;

import com.kfokam.k48.domain.Etudiant;
import com.kfokam.k48.domain.Exercice;
import com.kfokam.k48.domain.Promotion;
import com.kfokam.k48.domain.Relecture;
import com.kfokam.k48.domain.SessionCours;
import com.kfokam.k48.domain.StatutExercice;
import com.kfokam.k48.dto.RelectureDetailResponse;
import com.kfokam.k48.error.BusinessException;
import com.kfokam.k48.repository.EtudiantRepository;
import com.kfokam.k48.repository.RelectureRepository;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * K48-8 : RG15 (seul le relecteur désigné), RG16/H9 (ouverte_at à la première ouverture).
 */
@ExtendWith(MockitoExtension.class)
class RelectureServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-09-25T09:00:00Z");

    @Mock
    private RelectureRepository relectureRepository;

    @Mock
    private EtudiantRepository etudiantRepository;

    private RelectureService relectureService;

    @BeforeEach
    void demarrer() {
        relectureService = new RelectureService(relectureRepository, etudiantRepository, Clock.fixed(MAINTENANT, ZoneOffset.UTC));
    }

    private void forcerId(Object entite, long id) throws Exception {
        Field champ = entite.getClass().getDeclaredField("id");
        champ.setAccessible(true);
        champ.set(entite, id);
    }

    private Etudiant creerEtudiant(long id) throws Exception {
        var constructeur = Etudiant.class.getDeclaredConstructor();
        constructeur.setAccessible(true);
        Etudiant etudiant = constructeur.newInstance();
        forcerId(etudiant, id);
        return etudiant;
    }

    private Relecture creerRelecture(Etudiant relecteur, StatutExercice statutExercice) throws Exception {
        Promotion promotion = new Promotion("Promo");
        SessionCours session = new SessionCours("Cours", promotion, "AAAAAA", MAINTENANT, MAINTENANT.plusSeconds(900));
        Exercice exercice = new Exercice(session, creerEtudiant(99L), "https://exemple.test", statutExercice, MAINTENANT);
        forcerId(exercice, 5L);
        Relecture relecture = new Relecture(exercice, relecteur);
        forcerId(relecture, 1L);
        return relecture;
    }

    @Test
    void refuse_un_relecteur_non_assigne_avec_403() throws Exception {
        Relecture relecture = creerRelecture(creerEtudiant(2L), StatutExercice.EN_ATTENTE_RELECTURE);
        when(relectureRepository.findById(1L)).thenReturn(Optional.of(relecture));

        BusinessException exception = catchThrowableOfType(
                () -> relectureService.obtenirRelecture(1L, 3L),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("RELECTEUR_NON_ASSIGNE");
        assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void enregistre_ouverte_at_a_la_premiere_ouverture_et_passe_en_cours() throws Exception {
        Relecture relecture = creerRelecture(creerEtudiant(2L), StatutExercice.EN_ATTENTE_RELECTURE);
        when(relectureRepository.findById(1L)).thenReturn(Optional.of(relecture));

        RelectureDetailResponse reponse = relectureService.obtenirRelecture(1L, 2L);

        assertThat(reponse.ouverteAt()).isEqualTo(MAINTENANT);
        assertThat(relecture.getExercice().getStatut()).isEqualTo(StatutExercice.EN_COURS_RELECTURE);
    }

    @Test
    void ne_ecrase_pas_ouverte_at_a_la_deuxieme_ouverture() throws Exception {
        Relecture relecture = creerRelecture(creerEtudiant(2L), StatutExercice.EN_ATTENTE_RELECTURE);
        relecture.setOuverteAt(MAINTENANT.minusSeconds(60));
        when(relectureRepository.findById(1L)).thenReturn(Optional.of(relecture));

        RelectureDetailResponse reponse = relectureService.obtenirRelecture(1L, 2L);

        assertThat(reponse.ouverteAt()).isEqualTo(MAINTENANT.minusSeconds(60));
    }

    @Test
    void refuse_une_relecture_inconnue_avec_404() {
        when(relectureRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException exception = catchThrowableOfType(
                () -> relectureService.obtenirRelecture(99L, 2L),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("RELECTURE_INCONNUE");
    }
}
