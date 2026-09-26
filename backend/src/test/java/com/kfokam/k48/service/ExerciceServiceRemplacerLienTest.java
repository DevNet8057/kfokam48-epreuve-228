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
import com.kfokam.k48.dto.RemplacerLienRequete;
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

/** K48-15 : EF12 / RG16 (remplaçable tant qu'aucune relecture n'a commencé), RG20. */
@ExtendWith(MockitoExtension.class)
class ExerciceServiceRemplacerLienTest {

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
    private SessionCours session;
    private Etudiant auteur;

    @BeforeEach
    void demarrer() throws Exception {
        AttributionService attributionService =
                new AttributionService(presenceRepository, relectureRepository, exerciceRepository, etudiantRepository);
        exerciceService = new ExerciceService(sessionCoursRepository, etudiantRepository, exerciceRepository,
                relectureRepository, attributionService, Clock.fixed(MAINTENANT, ZoneOffset.UTC));
        Promotion promotion = new Promotion("Promo");
        session = new SessionCours("Cours", promotion, "AAAAAA", MAINTENANT, MAINTENANT.plusSeconds(900));
        forcerId(session, 10L);
        auteur = etudiant(1L);
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

    private Exercice exercice(StatutExercice statut) throws Exception {
        Exercice exercice = new Exercice(session, auteur, "https://exemple.test/ancien", statut, MAINTENANT);
        forcerId(exercice, 100L);
        return exercice;
    }

    @Test
    void remplace_le_lien_tant_qu_aucune_relecture_n_a_commence() throws Exception {
        Exercice exercice = exercice(StatutExercice.EN_ATTENTE_RELECTURE);
        when(exerciceRepository.findById(100L)).thenReturn(Optional.of(exercice));
        when(relectureRepository.findByExercice_Id(100L)).thenReturn(
                List.of(new Relecture(exercice, etudiant(2L))));

        exerciceService.remplacerLien(100L, 1L, new RemplacerLienRequete("https://exemple.test/nouveau"));

        assertThat(exercice.getLien()).isEqualTo("https://exemple.test/nouveau");
    }

    @Test
    void refuse_le_remplacement_si_une_relecture_a_deja_ete_ouverte() throws Exception {
        Exercice exercice = exercice(StatutExercice.EN_COURS_RELECTURE);
        Relecture relecture = new Relecture(exercice, etudiant(2L));
        relecture.setOuverteAt(MAINTENANT);
        when(exerciceRepository.findById(100L)).thenReturn(Optional.of(exercice));
        when(relectureRepository.findByExercice_Id(100L)).thenReturn(List.of(relecture));

        BusinessException exception = catchThrowableOfType(
                () -> exerciceService.remplacerLien(100L, 1L, new RemplacerLienRequete("https://exemple.test/nouveau")),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("REMPLACEMENT_IMPOSSIBLE");
        assertThat(exercice.getLien()).isEqualTo("https://exemple.test/ancien");
    }

    @Test
    void refuse_le_remplacement_par_un_autre_etudiant_que_l_auteur() throws Exception {
        Exercice exercice = exercice(StatutExercice.SANS_RELECTEUR);
        when(exerciceRepository.findById(100L)).thenReturn(Optional.of(exercice));

        BusinessException exception = catchThrowableOfType(
                () -> exerciceService.remplacerLien(100L, 99L, new RemplacerLienRequete("https://exemple.test/nouveau")),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("PAS_AUTEUR");
    }

    @Test
    void refuse_le_remplacement_apres_la_cloture() throws Exception {
        Field champCloture = SessionCours.class.getDeclaredField("clotureAt");
        champCloture.setAccessible(true);
        champCloture.set(session, MAINTENANT);
        Exercice exercice = exercice(StatutExercice.SANS_RELECTEUR);
        when(exerciceRepository.findById(100L)).thenReturn(Optional.of(exercice));

        BusinessException exception = catchThrowableOfType(
                () -> exerciceService.remplacerLien(100L, 1L, new RemplacerLienRequete("https://exemple.test/nouveau")),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("REMPLACEMENT_IMPOSSIBLE");
    }

    @Test
    void refuse_un_exercice_inconnu_avec_404() {
        when(exerciceRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException exception = catchThrowableOfType(
                () -> exerciceService.remplacerLien(999L, 1L, new RemplacerLienRequete("https://exemple.test/nouveau")),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("EXERCICE_INCONNU");
    }
}
