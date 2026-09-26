package com.kfokam.k48.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfokam.k48.domain.Etudiant;
import com.kfokam.k48.domain.Exercice;
import com.kfokam.k48.domain.Promotion;
import com.kfokam.k48.domain.Relecture;
import com.kfokam.k48.domain.SessionCours;
import com.kfokam.k48.domain.StatutExercice;
import com.kfokam.k48.repository.EtudiantRepository;
import com.kfokam.k48.repository.ExerciceRepository;
import com.kfokam.k48.repository.PresenceRepository;
import com.kfokam.k48.repository.RelectureRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RG13 : les relecteurs manquants sont retentés à chaque nouvelle présence ; jamais l'auteur (RG2),
 * jamais deux fois le même relecteur, rien après la clôture (RG20).
 */
@ExtendWith(MockitoExtension.class)
class AttributionServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-09-25T09:00:00Z");

    @Mock
    private PresenceRepository presenceRepository;

    @Mock
    private RelectureRepository relectureRepository;

    @Mock
    private ExerciceRepository exerciceRepository;

    @Mock
    private EtudiantRepository etudiantRepository;

    private AttributionService attributionService;
    private Promotion promotion;
    private SessionCours session;

    @BeforeEach
    void demarrer() throws Exception {
        attributionService = new AttributionService(presenceRepository, relectureRepository, exerciceRepository, etudiantRepository);
        promotion = new Promotion("Promo");
        forcerId(promotion, 1L);
        session = new SessionCours("Cours", promotion, "AAAAAA", MAINTENANT, MAINTENANT.plusSeconds(900));
        forcerId(session, 10L);
    }

    private void forcerId(Object entite, long id) throws Exception {
        Field champ = entite.getClass().getDeclaredField("id");
        champ.setAccessible(true);
        champ.set(entite, id);
    }

    private Etudiant etudiant(long id) throws Exception {
        var constructeur = Etudiant.class.getDeclaredConstructor();
        constructeur.setAccessible(true);
        Etudiant etudiant = constructeur.newInstance();
        forcerId(etudiant, id);
        return etudiant;
    }

    private Exercice exercice(Etudiant auteur, StatutExercice statut) throws Exception {
        Exercice exercice = new Exercice(session, auteur, "https://exemple.test", statut, MAINTENANT);
        forcerId(exercice, 100L);
        return exercice;
    }

    @Test
    void un_exercice_sans_relecteur_en_recoit_un_quand_un_pair_arrive_mais_jamais_l_auteur() throws Exception {
        Exercice exercice = exercice(etudiant(1L), StatutExercice.SANS_RELECTEUR);
        when(exerciceRepository.findBySessionId(10L)).thenReturn(List.of(exercice));
        when(relectureRepository.findByExercice_Id(100L)).thenReturn(List.of());
        // L'auteur (1) et le nouvel arrivant (2) sont présents.
        when(presenceRepository.trouverIdsEtudiantsPresents(10L)).thenReturn(List.of(1L, 2L));
        when(etudiantRepository.getReferenceById(2L)).thenReturn(etudiant(2L));

        attributionService.retenterAttribution(session);

        verify(relectureRepository, times(1)).save(any(Relecture.class));
        verify(etudiantRepository, never()).getReferenceById(1L);
        assertThat(exercice.getStatut()).isEqualTo(StatutExercice.EN_ATTENTE_RELECTURE);
    }

    @Test
    void le_second_relecteur_manquant_est_complete_sans_reprendre_le_premier() throws Exception {
        Exercice exercice = exercice(etudiant(1L), StatutExercice.EN_ATTENTE_RELECTURE);
        Relecture premiere = new Relecture(exercice, etudiant(2L));
        when(exerciceRepository.findBySessionId(10L)).thenReturn(List.of(exercice));
        when(relectureRepository.findByExercice_Id(100L)).thenReturn(List.of(premiere));
        when(presenceRepository.trouverIdsEtudiantsPresents(10L)).thenReturn(List.of(1L, 2L, 3L));
        when(etudiantRepository.getReferenceById(3L)).thenReturn(etudiant(3L));

        attributionService.retenterAttribution(session);

        verify(relectureRepository, times(1)).save(any(Relecture.class));
        verify(etudiantRepository, never()).getReferenceById(2L);
    }

    @Test
    void rien_n_est_attribue_apres_la_cloture() throws Exception {
        Field champCloture = SessionCours.class.getDeclaredField("clotureAt");
        champCloture.setAccessible(true);
        champCloture.set(session, MAINTENANT);

        attributionService.retenterAttribution(session);

        verify(exerciceRepository, never()).findBySessionId(any());
        verify(relectureRepository, never()).save(any());
    }
}
