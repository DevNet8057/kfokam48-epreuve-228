package com.kfokam.k48.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfokam.k48.domain.Promotion;
import com.kfokam.k48.domain.SessionCours;
import com.kfokam.k48.dto.CreateSessionRequest;
import com.kfokam.k48.dto.SessionResponse;
import com.kfokam.k48.error.BusinessException;
import com.kfokam.k48.repository.PromotionRepository;
import com.kfokam.k48.repository.SessionCoursRepository;
import java.time.Clock;
import java.time.Duration;
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
 * EF1 / RG1 / RG4 / H14 : ouverture d'une session de cours.
 */
@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-09-25T09:00:00Z");

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private SessionCoursRepository sessionCoursRepository;

    private SessionService sessionService;

    @BeforeEach
    void demarrerAvecUneHorlogeFixe() {
        Clock horlogeFixe = Clock.fixed(MAINTENANT, ZoneOffset.UTC);
        sessionService = new SessionService(promotionRepository, sessionCoursRepository, horlogeFixe);
    }

    @Test
    void ouvre_une_session_avec_expiration_a_15_minutes_de_l_ouverture() {
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(new Promotion("Promotion Démonstration")));
        when(sessionCoursRepository.existeCodeActif(anyString(), any())).thenReturn(false);
        when(sessionCoursRepository.save(any(SessionCours.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SessionResponse reponse = sessionService.ouvrirSession(new CreateSessionRequest("Cours Java", 1L));

        assertThat(reponse.ouvertureAt()).isEqualTo(MAINTENANT);
        assertThat(reponse.expirationAt()).isEqualTo(MAINTENANT.plus(Duration.ofMinutes(15)));
    }

    @Test
    void genere_un_code_de_6_caracteres_sans_caracteres_ambigus() {
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(new Promotion("Promotion Démonstration")));
        when(sessionCoursRepository.existeCodeActif(anyString(), any())).thenReturn(false);
        when(sessionCoursRepository.save(any(SessionCours.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SessionResponse reponse = sessionService.ouvrirSession(new CreateSessionRequest("Cours Java", 1L));

        assertThat(reponse.code()).hasSize(6);
        assertThat(reponse.code()).matches("[A-Z2-9]{6}");
        assertThat(reponse.code()).doesNotContainPattern("[0O1I]");
    }

    @Test
    void regenere_le_code_si_un_code_actif_identique_existe_deja() {
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(new Promotion("Promotion Démonstration")));
        when(sessionCoursRepository.existeCodeActif(anyString(), any())).thenReturn(true, false);
        when(sessionCoursRepository.save(any(SessionCours.class))).thenAnswer(invocation -> invocation.getArgument(0));

        sessionService.ouvrirSession(new CreateSessionRequest("Cours Java", 1L));

        verify(sessionCoursRepository, times(2)).existeCodeActif(anyString(), any());
    }

    @Test
    void refuse_une_promotion_inconnue_avec_le_code_400_dedie() {
        when(promotionRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException exception = catchThrowableOfType(
                () -> sessionService.ouvrirSession(new CreateSessionRequest("Cours Java", 99L)),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("PROMOTION_INCONNUE");
        assertThat(exception.status()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
