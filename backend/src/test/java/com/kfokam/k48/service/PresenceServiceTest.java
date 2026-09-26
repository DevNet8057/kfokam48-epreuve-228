package com.kfokam.k48.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfokam.k48.domain.Etudiant;
import com.kfokam.k48.domain.Promotion;
import com.kfokam.k48.domain.SessionCours;
import com.kfokam.k48.dto.MarquerPresenceRequete;
import com.kfokam.k48.error.BusinessException;
import com.kfokam.k48.repository.EtudiantRepository;
import com.kfokam.k48.repository.PresenceRepository;
import com.kfokam.k48.repository.SessionCoursRepository;
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
 * K48-11 : RG7 / H5 — le blocage est vérifié avant le code, et seul un code inconnu compte comme échec.
 */
@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-09-25T09:00:00Z");

    @Mock
    private EtudiantRepository etudiantRepository;

    @Mock
    private SessionCoursRepository sessionCoursRepository;

    @Mock
    private PresenceRepository presenceRepository;

    @Mock
    private TentativeCodeService tentativeCodeService;

    private PresenceService presenceService;
    private Etudiant etudiant;
    private Promotion promotion;

    @BeforeEach
    void demarrer() throws Exception {
        presenceService = new PresenceService(etudiantRepository, sessionCoursRepository, presenceRepository,
                tentativeCodeService, Clock.fixed(MAINTENANT, ZoneOffset.UTC));
        promotion = new Promotion("Promo");
        forcerId(promotion, 1L);
        var constructeur = Etudiant.class.getDeclaredConstructor();
        constructeur.setAccessible(true);
        etudiant = constructeur.newInstance();
        forcerId(etudiant, 7L);
        Field champPromotion = Etudiant.class.getDeclaredField("promotion");
        champPromotion.setAccessible(true);
        champPromotion.set(etudiant, promotion);
        when(etudiantRepository.findById(7L)).thenReturn(Optional.of(etudiant));
    }

    private void forcerId(Object entite, long id) throws Exception {
        Field champ = entite.getClass().getDeclaredField("id");
        champ.setAccessible(true);
        champ.set(entite, id);
    }

    @Test
    void un_code_inconnu_compte_comme_un_echec() {
        when(sessionCoursRepository.findFirstByCodeAndPromotionIdOrderByOuvertureAtDesc(anyString(), anyLong()))
                .thenReturn(Optional.empty());

        BusinessException exception = catchThrowableOfType(
                () -> presenceService.marquerPresence(new MarquerPresenceRequete("ZZZZZZ", 7L)), BusinessException.class);

        assertThat(exception.code()).isEqualTo("CODE_INCONNU");
        verify(tentativeCodeService).enregistrerEchec(7L);
    }

    @Test
    void un_code_expire_ne_compte_pas_comme_un_echec() throws Exception {
        SessionCours session = new SessionCours("Cours", promotion, "AAAAAA",
                MAINTENANT.minusSeconds(1800), MAINTENANT.minusSeconds(900));
        forcerId(session, 10L);
        when(sessionCoursRepository.findFirstByCodeAndPromotionIdOrderByOuvertureAtDesc("AAAAAA", 1L))
                .thenReturn(Optional.of(session));

        BusinessException exception = catchThrowableOfType(
                () -> presenceService.marquerPresence(new MarquerPresenceRequete("AAAAAA", 7L)), BusinessException.class);

        assertThat(exception.code()).isEqualTo("CODE_EXPIRE");
        verify(tentativeCodeService, never()).enregistrerEchec(anyLong());
    }

    @Test
    void un_etudiant_bloque_recoit_429_meme_avec_le_bon_code() {
        doThrow(new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "TROP_DE_TENTATIVES", "Bloqué."))
                .when(tentativeCodeService).verifierNonBloque(7L);

        BusinessException exception = catchThrowableOfType(
                () -> presenceService.marquerPresence(new MarquerPresenceRequete("AAAAAA", 7L)), BusinessException.class);

        assertThat(exception.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        verify(sessionCoursRepository, never()).findFirstByCodeAndPromotionIdOrderByOuvertureAtDesc(anyString(), anyLong());
    }
}
