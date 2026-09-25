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
import com.kfokam.k48.dto.LigneTableauResponse;
import com.kfokam.k48.error.BusinessException;
import com.kfokam.k48.repository.EtudiantRepository;
import com.kfokam.k48.repository.ExerciceRepository;
import com.kfokam.k48.repository.PresenceRepository;
import com.kfokam.k48.repository.PromotionRepository;
import com.kfokam.k48.repository.RelectureRepository;
import com.kfokam.k48.repository.SessionCoursRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * K48-10 : RG18 (moyenne, arrondie, null sans note), une ligne par étudiant même sans présence.
 */
@ExtendWith(MockitoExtension.class)
class TableauServiceTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private EtudiantRepository etudiantRepository;

    @Mock
    private SessionCoursRepository sessionCoursRepository;

    @Mock
    private PresenceRepository presenceRepository;

    @Mock
    private ExerciceRepository exerciceRepository;

    @Mock
    private RelectureRepository relectureRepository;

    private TableauService tableauService;

    @BeforeEach
    void demarrer() {
        tableauService = new TableauService(
                promotionRepository, etudiantRepository, sessionCoursRepository, presenceRepository, exerciceRepository, relectureRepository);
    }

    private void forcerId(Object entite, long id) throws Exception {
        Field champ = entite.getClass().getDeclaredField("id");
        champ.setAccessible(true);
        champ.set(entite, id);
    }

    private Etudiant creerEtudiant(long id, String nom, Promotion promotion) throws Exception {
        var constructeur = Etudiant.class.getDeclaredConstructor();
        constructeur.setAccessible(true);
        Etudiant etudiant = constructeur.newInstance();
        forcerId(etudiant, id);
        Field champNom = Etudiant.class.getDeclaredField("nom");
        champNom.setAccessible(true);
        champNom.set(etudiant, nom);
        Field champPromotion = Etudiant.class.getDeclaredField("promotion");
        champPromotion.setAccessible(true);
        champPromotion.set(etudiant, promotion);
        return etudiant;
    }

    @Test
    void refuse_une_promotion_inconnue_avec_404() {
        when(promotionRepository.existsById(99L)).thenReturn(false);

        BusinessException exception = catchThrowableOfType(
                () -> tableauService.obtenirTableau(99L),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("PROMOTION_INCONNUE");
    }

    @Test
    void inclut_une_ligne_pour_un_etudiant_sans_aucune_presence_avec_moyenne_nulle() throws Exception {
        Promotion promotion = new Promotion("Promo");
        forcerId(promotion, 1L);
        Etudiant sansActivite = creerEtudiant(1L, "Zoé", promotion);

        when(promotionRepository.existsById(1L)).thenReturn(true);
        when(etudiantRepository.findByPromotionIdOrderByNomAsc(1L)).thenReturn(List.of(sansActivite));
        when(sessionCoursRepository.findByPromotionIdOrderByOuvertureAtAsc(1L)).thenReturn(List.of());
        when(presenceRepository.findBySessionIdIn(List.of())).thenReturn(List.of());
        when(exerciceRepository.findByAuteurIdIn(List.of(1L))).thenReturn(List.of());
        when(relectureRepository.findByRelecteurIdInAndRendueAtIsNull(List.of(1L))).thenReturn(List.of());
        when(relectureRepository.findByExercice_AuteurIdIn(List.of(1L))).thenReturn(List.of());

        List<LigneTableauResponse> tableau = tableauService.obtenirTableau(1L);

        assertThat(tableau).hasSize(1);
        LigneTableauResponse ligne = tableau.get(0);
        assertThat(ligne.nom()).isEqualTo("Zoé");
        assertThat(ligne.presences()).isZero();
        assertThat(ligne.moyenne()).isNull();
        assertThat(ligne.exercicesEnAttente()).isZero();
        assertThat(ligne.relecturesEnAttente()).isZero();
    }

    @Test
    void calcule_la_moyenne_arrondie_a_deux_decimales() throws Exception {
        Promotion promotion = new Promotion("Promo");
        forcerId(promotion, 1L);
        Etudiant auteur = creerEtudiant(1L, "Amina", promotion);

        SessionCours session = new SessionCours("Cours", promotion, "AAAAAA", Instant.now(), Instant.now().plusSeconds(900));
        forcerId(session, 10L);
        Exercice exercice = new Exercice(session, auteur, "https://exemple.test", StatutExercice.RELU, Instant.now());
        forcerId(exercice, 100L);

        Relecture relectureA = new Relecture(exercice, creerEtudiant(2L, "Boris", promotion));
        relectureA.setNote(15);
        Relecture relectureB = new Relecture(exercice, creerEtudiant(3L, "Chloé", promotion));
        relectureB.setNote(14);

        when(promotionRepository.existsById(1L)).thenReturn(true);
        when(etudiantRepository.findByPromotionIdOrderByNomAsc(1L)).thenReturn(List.of(auteur));
        when(sessionCoursRepository.findByPromotionIdOrderByOuvertureAtAsc(1L)).thenReturn(List.of());
        when(presenceRepository.findBySessionIdIn(List.of())).thenReturn(List.of());
        when(exerciceRepository.findByAuteurIdIn(List.of(1L))).thenReturn(List.of(exercice));
        when(relectureRepository.findByRelecteurIdInAndRendueAtIsNull(List.of(1L))).thenReturn(List.of());
        when(relectureRepository.findByExercice_AuteurIdIn(List.of(1L))).thenReturn(List.of(relectureA, relectureB));

        List<LigneTableauResponse> tableau = tableauService.obtenirTableau(1L);

        // (15 + 14) / 2 = 14.5
        assertThat(tableau.get(0).moyenne()).isEqualTo(14.5);
        assertThat(tableau.get(0).exercicesEnAttente()).isZero();
    }
}
