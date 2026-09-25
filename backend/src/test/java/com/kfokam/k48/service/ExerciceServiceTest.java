package com.kfokam.k48.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfokam.k48.domain.Etudiant;
import com.kfokam.k48.domain.Exercice;
import com.kfokam.k48.domain.Promotion;
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
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * K48-7 : RG2 (jamais l'auteur), RG12 (le moins chargé), RG13 (SANS_RELECTEUR si personne).
 */
@ExtendWith(MockitoExtension.class)
class ExerciceServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-09-25T09:00:00Z");

    @Mock
    private SessionCoursRepository sessionCoursRepository;

    @Mock
    private EtudiantRepository etudiantRepository;

    @Mock
    private ExerciceRepository exerciceRepository;

    @Mock
    private PresenceRepository presenceRepository;

    @Mock
    private RelectureRepository relectureRepository;

    private ExerciceService exerciceService;

    @BeforeEach
    void demarrer() {
        Clock horlogeFixe = Clock.fixed(MAINTENANT, ZoneOffset.UTC);
        exerciceService = new ExerciceService(
                sessionCoursRepository, etudiantRepository, exerciceRepository, presenceRepository, relectureRepository, horlogeFixe);
    }

    private Etudiant creerEtudiant(long id, Promotion promotion) throws Exception {
        var constructeur = Etudiant.class.getDeclaredConstructor();
        constructeur.setAccessible(true);
        Etudiant etudiant = constructeur.newInstance();
        forcerId(etudiant, id);
        Field champPromotion = Etudiant.class.getDeclaredField("promotion");
        champPromotion.setAccessible(true);
        champPromotion.set(etudiant, promotion);
        return etudiant;
    }

    private void forcerId(Object entite, long id) throws Exception {
        Field champId = entite.getClass().getDeclaredField("id");
        champId.setAccessible(true);
        champId.set(entite, id);
    }

    @Test
    void ne_tire_jamais_l_auteur_meme_seul_present() throws Exception {
        Promotion promotion = new Promotion("Promo");
        forcerId(promotion, 1L);
        Etudiant auteur = creerEtudiant(1L, promotion);
        SessionCours session = new SessionCours("Cours", promotion, "AAAAAA", MAINTENANT, MAINTENANT.plusSeconds(900));
        forcerId(session, 10L);

        when(etudiantRepository.findById(1L)).thenReturn(Optional.of(auteur));
        when(sessionCoursRepository.findById(10L)).thenReturn(Optional.of(session));
        when(exerciceRepository.existsBySessionIdAndAuteurId(10L, 1L)).thenReturn(false);
        // Seul l'auteur est présent à sa propre session.
        when(presenceRepository.trouverIdsEtudiantsPresents(10L)).thenReturn(List.of(1L));
        when(exerciceRepository.save(any(Exercice.class))).thenAnswer(invocation -> {
            Exercice exercice = invocation.getArgument(0);
            forcerId(exercice, 100L);
            return exercice;
        });

        ExerciceResponse reponse = exerciceService.deposerExercice(new DeposerExerciceRequete(10L, 1L, "https://exemple.test/mon-exercice"));

        assertThat(reponse.statut()).isEqualTo(StatutExercice.SANS_RELECTEUR.name());
        verify(relectureRepository, never()).save(any());
    }

    @Test
    void tire_le_relecteur_le_moins_charge_parmi_les_presents() throws Exception {
        Promotion promotion = new Promotion("Promo");
        forcerId(promotion, 1L);
        Etudiant auteur = creerEtudiant(1L, promotion);
        SessionCours session = new SessionCours("Cours", promotion, "AAAAAA", MAINTENANT, MAINTENANT.plusSeconds(900));
        forcerId(session, 10L);

        when(etudiantRepository.findById(1L)).thenReturn(Optional.of(auteur));
        when(sessionCoursRepository.findById(10L)).thenReturn(Optional.of(session));
        when(exerciceRepository.existsBySessionIdAndAuteurId(10L, 1L)).thenReturn(false);
        // Auteur (1) + deux candidats (2 et 3) ; 2 est déjà plus chargé, 3 doit être choisi.
        when(presenceRepository.trouverIdsEtudiantsPresents(10L)).thenReturn(List.of(1L, 2L, 3L));
        when(relectureRepository.countByRelecteurId(2L)).thenReturn(3L);
        when(relectureRepository.countByRelecteurId(3L)).thenReturn(0L);
        when(etudiantRepository.getReferenceById(3L)).thenReturn(creerEtudiant(3L, promotion));
        when(exerciceRepository.save(any(Exercice.class))).thenAnswer(invocation -> {
            Exercice exercice = invocation.getArgument(0);
            forcerId(exercice, 100L);
            return exercice;
        });

        ExerciceResponse reponse = exerciceService.deposerExercice(new DeposerExerciceRequete(10L, 1L, "https://exemple.test/mon-exercice"));

        assertThat(reponse.statut()).isEqualTo(StatutExercice.EN_ATTENTE_RELECTURE.name());
        verify(relectureRepository).save(any());
        verify(etudiantRepository, never()).getReferenceById(2L);
    }

    @Test
    void refuse_un_lien_non_http() throws Exception {
        BusinessException exception = catchThrowableOfType(
                () -> exerciceService.deposerExercice(new DeposerExerciceRequete(10L, 1L, "ftp://exemple.test/x")),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("LIEN_INVALIDE");
    }

    @Test
    void refuse_le_depot_sur_une_session_cloturee() throws Exception {
        Promotion promotion = new Promotion("Promo");
        forcerId(promotion, 1L);
        Etudiant auteur = creerEtudiant(1L, promotion);
        SessionCours session = new SessionCours("Cours", promotion, "AAAAAA", MAINTENANT.minusSeconds(2000), MAINTENANT.minusSeconds(1100));
        forcerId(session, 10L);
        Field champCloture = SessionCours.class.getDeclaredField("clotureAt");
        champCloture.setAccessible(true);
        champCloture.set(session, MAINTENANT.minusSeconds(500));

        when(etudiantRepository.findById(1L)).thenReturn(Optional.of(auteur));
        when(sessionCoursRepository.findById(10L)).thenReturn(Optional.of(session));

        BusinessException exception = catchThrowableOfType(
                () -> exerciceService.deposerExercice(new DeposerExerciceRequete(10L, 1L, "https://exemple.test/x")),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("SESSION_CLOTUREE");
        verify(exerciceRepository, never()).save(any());
    }
}
