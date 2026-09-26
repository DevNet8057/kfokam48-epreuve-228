package com.kfokam.k48.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
 * K48-7 / C2 (deux relecteurs, étape 3) : RG2 (jamais l'auteur), RG12 v2 (deux distincts, les
 * moins chargés), RG13 (un seul candidat -> un seul relecteur ; aucun -> SANS_RELECTEUR).
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
        AttributionService attributionService =
                new AttributionService(presenceRepository, relectureRepository, exerciceRepository, etudiantRepository);
        exerciceService = new ExerciceService(
                sessionCoursRepository, etudiantRepository, exerciceRepository, relectureRepository, attributionService, horlogeFixe);
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

    private void preparerDepot(Promotion promotion, Etudiant auteur, SessionCours session, List<Long> presents) throws Exception {
        forcerId(promotion, 1L);
        forcerId(session, 10L);
        when(etudiantRepository.findById(1L)).thenReturn(Optional.of(auteur));
        when(sessionCoursRepository.findById(10L)).thenReturn(Optional.of(session));
        when(exerciceRepository.existsBySessionIdAndAuteurId(10L, 1L)).thenReturn(false);
        when(presenceRepository.trouverIdsEtudiantsPresents(10L)).thenReturn(presents);
        when(exerciceRepository.save(any(Exercice.class))).thenAnswer(invocation -> {
            Exercice exercice = invocation.getArgument(0);
            forcerId(exercice, 100L);
            return exercice;
        });
    }

    @Test
    void ne_tire_jamais_l_auteur_meme_seul_present() throws Exception {
        Promotion promotion = new Promotion("Promo");
        Etudiant auteur = creerEtudiant(1L, promotion);
        SessionCours session = new SessionCours("Cours", promotion, "AAAAAA", MAINTENANT, MAINTENANT.plusSeconds(900));
        preparerDepot(promotion, auteur, session, List.of(1L));

        ExerciceResponse reponse = exerciceService.deposerExercice(new DeposerExerciceRequete(10L, 1L, "https://exemple.test/mon-exercice"));

        assertThat(reponse.statut()).isEqualTo(StatutExercice.SANS_RELECTEUR.name());
        verify(relectureRepository, never()).save(any());
    }

    @Test
    void tire_un_seul_relecteur_si_un_seul_candidat_disponible() throws Exception {
        Promotion promotion = new Promotion("Promo");
        Etudiant auteur = creerEtudiant(1L, promotion);
        SessionCours session = new SessionCours("Cours", promotion, "AAAAAA", MAINTENANT, MAINTENANT.plusSeconds(900));
        preparerDepot(promotion, auteur, session, List.of(1L, 2L));
        when(etudiantRepository.getReferenceById(2L)).thenReturn(creerEtudiant(2L, promotion));

        ExerciceResponse reponse = exerciceService.deposerExercice(new DeposerExerciceRequete(10L, 1L, "https://exemple.test/mon-exercice"));

        assertThat(reponse.statut()).isEqualTo(StatutExercice.EN_ATTENTE_RELECTURE.name());
        verify(relectureRepository, times(1)).save(any());
    }

    @Test
    void tire_deux_relecteurs_distincts_si_deux_candidats_disponibles() throws Exception {
        Promotion promotion = new Promotion("Promo");
        Etudiant auteur = creerEtudiant(1L, promotion);
        SessionCours session = new SessionCours("Cours", promotion, "AAAAAA", MAINTENANT, MAINTENANT.plusSeconds(900));
        preparerDepot(promotion, auteur, session, List.of(1L, 2L, 3L));
        when(etudiantRepository.getReferenceById(2L)).thenReturn(creerEtudiant(2L, promotion));
        when(etudiantRepository.getReferenceById(3L)).thenReturn(creerEtudiant(3L, promotion));

        ExerciceResponse reponse = exerciceService.deposerExercice(new DeposerExerciceRequete(10L, 1L, "https://exemple.test/mon-exercice"));

        assertThat(reponse.statut()).isEqualTo(StatutExercice.EN_ATTENTE_RELECTURE.name());
        verify(relectureRepository, times(2)).save(any());
    }

    @Test
    void ne_tire_pas_plus_de_deux_relecteurs_meme_avec_trois_candidats_disponibles() throws Exception {
        Promotion promotion = new Promotion("Promo");
        Etudiant auteur = creerEtudiant(1L, promotion);
        SessionCours session = new SessionCours("Cours", promotion, "AAAAAA", MAINTENANT, MAINTENANT.plusSeconds(900));
        // Auteur (1) + trois candidats (2, 3, 4) ; 2 est le plus charge, 3 et 4 les moins charges.
        preparerDepot(promotion, auteur, session, List.of(1L, 2L, 3L, 4L));
        when(relectureRepository.countByRelecteurId(2L)).thenReturn(5L);
        when(relectureRepository.countByRelecteurId(3L)).thenReturn(0L);
        when(relectureRepository.countByRelecteurId(4L)).thenReturn(0L);
        when(etudiantRepository.getReferenceById(3L)).thenReturn(creerEtudiant(3L, promotion));
        when(etudiantRepository.getReferenceById(4L)).thenReturn(creerEtudiant(4L, promotion));

        ExerciceResponse reponse = exerciceService.deposerExercice(new DeposerExerciceRequete(10L, 1L, "https://exemple.test/mon-exercice"));

        assertThat(reponse.statut()).isEqualTo(StatutExercice.EN_ATTENTE_RELECTURE.name());
        verify(relectureRepository, times(2)).save(any());
        verify(etudiantRepository, never()).getReferenceById(2L);
    }

    @Test
    void refuse_un_lien_non_http() {
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
