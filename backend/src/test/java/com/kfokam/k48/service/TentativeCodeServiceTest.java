package com.kfokam.k48.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.kfokam.k48.domain.TentativeCode;
import com.kfokam.k48.error.BusinessException;
import com.kfokam.k48.repository.TentativeCodeRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * K48-11 / RG7 : cinq codes inconnus consécutifs → blocage de deux minutes, vérifié avec une
 * horloge que le test fait avancer (ENF5).
 */
@ExtendWith(MockitoExtension.class)
class TentativeCodeServiceTest {

    private static final Instant DEBUT = Instant.parse("2026-09-25T09:00:00Z");

    /** Horloge dont le test fixe l'instant. */
    private static final class HorlogeReglable extends Clock {
        private Instant instant = DEBUT;

        void avancer(Duration duree) {
            instant = instant.plus(duree);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    @Mock
    private TentativeCodeRepository tentativeCodeRepository;

    private final HorlogeReglable horloge = new HorlogeReglable();
    private TentativeCodeService tentativeCodeService;
    private TentativeCode stockee;

    @BeforeEach
    void demarrer() {
        tentativeCodeService = new TentativeCodeService(tentativeCodeRepository, horloge);
        // Dépôt en mémoire : une seule ligne pour l'étudiant 1.
        when(tentativeCodeRepository.findById(1L)).thenAnswer(invocation -> Optional.ofNullable(stockee));
        when(tentativeCodeRepository.save(any(TentativeCode.class))).thenAnswer(invocation -> {
            stockee = invocation.getArgument(0);
            return stockee;
        });
    }

    private void echouer(int fois) {
        for (int i = 0; i < fois; i++) {
            tentativeCodeService.enregistrerEchec(1L);
        }
    }

    @Test
    void quatre_echecs_ne_bloquent_pas() {
        echouer(4);

        assertThatCode(() -> tentativeCodeService.verifierNonBloque(1L)).doesNotThrowAnyException();
    }

    @Test
    void cinq_echecs_bloquent_deux_minutes_puis_liberent_et_remettent_le_compteur_a_zero() {
        echouer(5);

        horloge.avancer(Duration.ofSeconds(119));
        BusinessException exception = catchThrowableOfType(
                () -> tentativeCodeService.verifierNonBloque(1L), BusinessException.class);
        assertThat(exception.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exception.code()).isEqualTo("TROP_DE_TENTATIVES");
        assertThat(exception.getMessage()).contains("dans 1 s");

        horloge.avancer(Duration.ofSeconds(2));
        assertThatCode(() -> tentativeCodeService.verifierNonBloque(1L)).doesNotThrowAnyException();
        assertThat(stockee.getEchecsConsecutifs()).isZero();
        assertThat(stockee.getBloqueJusquA()).isNull();
    }

    @Test
    void une_presence_reussie_remet_le_compteur_a_zero() {
        echouer(4);

        tentativeCodeService.reinitialiser(1L);
        echouer(4);

        assertThatCode(() -> tentativeCodeService.verifierNonBloque(1L)).doesNotThrowAnyException();
    }
}
