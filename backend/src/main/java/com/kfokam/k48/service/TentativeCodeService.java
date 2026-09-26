package com.kfokam.k48.service;

import com.kfokam.k48.domain.TentativeCode;
import com.kfokam.k48.error.BusinessException;
import com.kfokam.k48.repository.TentativeCodeRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * EF4 / RG7 / H5 : cinq codes inconnus consécutifs bloquent l'étudiant deux minutes.
 *
 * Chaque méthode s'exécute dans sa propre transaction (REQUIRES_NEW) : un échec est enregistré
 * juste avant de renvoyer 400 CODE_INCONNU, et la transaction de l'appelant est alors annulée —
 * sans transaction séparée, le compteur serait annulé avec elle et le blocage n'arriverait jamais.
 */
@Service
public class TentativeCodeService {

    static final int ECHECS_AVANT_BLOCAGE = 5;
    static final Duration DUREE_BLOCAGE = Duration.ofMinutes(2);

    private final TentativeCodeRepository tentativeCodeRepository;
    private final Clock clock;

    public TentativeCodeService(TentativeCodeRepository tentativeCodeRepository, Clock clock) {
        this.tentativeCodeRepository = tentativeCodeRepository;
        this.clock = clock;
    }

    /** Refuse avec 429 pendant le blocage ; remet le compteur à zéro une fois le blocage terminé. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void verifierNonBloque(Long etudiantId) {
        tentativeCodeRepository.findById(etudiantId).ifPresent(tentative -> {
            Instant fin = tentative.getBloqueJusquA();
            if (fin == null) {
                return;
            }
            Instant maintenant = clock.instant();
            if (maintenant.isBefore(fin)) {
                long secondes = Math.max(1, Duration.between(maintenant, fin).toSeconds());
                throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "TROP_DE_TENTATIVES",
                        "Trop de codes erronés. Réessayez dans " + secondes + " s.");
            }
            tentative.reinitialiser();
            tentativeCodeRepository.save(tentative);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enregistrerEchec(Long etudiantId) {
        TentativeCode tentative = tentativeCodeRepository.findById(etudiantId).orElseGet(() -> new TentativeCode(etudiantId));
        tentative.enregistrerEchec();
        if (tentative.getEchecsConsecutifs() >= ECHECS_AVANT_BLOCAGE) {
            tentative.bloquerJusqua(clock.instant().plus(DUREE_BLOCAGE));
        }
        tentativeCodeRepository.save(tentative);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reinitialiser(Long etudiantId) {
        tentativeCodeRepository.findById(etudiantId).ifPresent(tentative -> {
            tentative.reinitialiser();
            tentativeCodeRepository.save(tentative);
        });
    }
}
