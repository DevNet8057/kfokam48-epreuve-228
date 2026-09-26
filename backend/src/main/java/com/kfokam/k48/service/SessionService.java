package com.kfokam.k48.service;

import com.kfokam.k48.domain.Promotion;
import com.kfokam.k48.domain.SessionCours;
import com.kfokam.k48.dto.CreateSessionRequest;
import com.kfokam.k48.dto.SessionResponse;
import com.kfokam.k48.dto.SessionResumeResponse;
import java.util.List;
import com.kfokam.k48.error.BusinessException;
import com.kfokam.k48.repository.PromotionRepository;
import com.kfokam.k48.repository.SessionCoursRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * EF1 : le formateur ouvre une session et obtient un code de présence.
 * Règles : RG1 (expiration à 15 min), RG4 (forme et unicité du code), H14 (promotion inconnue référencée dans le corps).
 */
@Service
public class SessionService {

    static final Duration DUREE_VALIDITE_CODE = Duration.ofMinutes(15);

    // Majuscules et chiffres, sans caractères ambigus (0, O, 1, I) — RG4.
    private static final String ALPHABET_CODE = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int LONGUEUR_CODE = 6;
    private static final int TENTATIVES_MAX_GENERATION_CODE = 20;

    private final PromotionRepository promotionRepository;
    private final SessionCoursRepository sessionCoursRepository;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public SessionService(PromotionRepository promotionRepository, SessionCoursRepository sessionCoursRepository, Clock clock) {
        this.promotionRepository = promotionRepository;
        this.sessionCoursRepository = sessionCoursRepository;
        this.clock = clock;
    }

    @Transactional
    public SessionResponse ouvrirSession(CreateSessionRequest requete) {
        Promotion promotion = promotionRepository.findById(requete.promotionId())
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "PROMOTION_INCONNUE", "La promotion n'existe pas."));

        Instant ouvertureAt = clock.instant();
        Instant expirationAt = ouvertureAt.plus(DUREE_VALIDITE_CODE);
        String code = genererCodeUnique();

        SessionCours session = new SessionCours(requete.titre(), promotion, code, ouvertureAt, expirationAt);
        return SessionResponse.from(sessionCoursRepository.save(session));
    }

    /** Sessions d'une promotion, la plus récente d'abord (écran formateur : présence manuelle, clôture). */
    @Transactional(readOnly = true)
    public List<SessionResumeResponse> listerSessions(Long promotionId) {
        if (!promotionRepository.existsById(promotionId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "PROMOTION_INCONNUE", "Cette promotion n'existe pas.");
        }
        return sessionCoursRepository.findByPromotionIdOrderByOuvertureAtDesc(promotionId).stream()
                .map(SessionResumeResponse::from)
                .toList();
    }

    private String genererCodeUnique() {
        for (int tentative = 0; tentative < TENTATIVES_MAX_GENERATION_CODE; tentative++) {
            String code = genererCode();
            if (!sessionCoursRepository.existeCodeActif(code, clock.instant())) {
                return code;
            }
        }
        throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "ERREUR_INTERNE", "Impossible de générer un code de session unique.");
    }

    private String genererCode() {
        StringBuilder code = new StringBuilder(LONGUEUR_CODE);
        for (int i = 0; i < LONGUEUR_CODE; i++) {
            code.append(ALPHABET_CODE.charAt(random.nextInt(ALPHABET_CODE.length())));
        }
        return code.toString();
    }
}
