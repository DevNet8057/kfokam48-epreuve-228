package com.kfokam.k48.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.when;

import com.kfokam.k48.dto.PromotionResponse;
import com.kfokam.k48.error.BusinessException;
import com.kfokam.k48.repository.EtudiantRepository;
import com.kfokam.k48.repository.PromotionRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * K48-25 : identification par promotion/nom.
 */
@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private EtudiantRepository etudiantRepository;

    private PromotionService promotionService;

    @BeforeEach
    void demarrer() {
        promotionService = new PromotionService(promotionRepository, etudiantRepository);
    }

    @Test
    void liste_les_promotions() {
        when(promotionRepository.findAll()).thenReturn(List.of(new com.kfokam.k48.domain.Promotion("Promotion A")));

        List<PromotionResponse> reponse = promotionService.listerPromotions();

        assertThat(reponse).hasSize(1);
        assertThat(reponse.get(0).nom()).isEqualTo("Promotion A");
    }

    @Test
    void refuse_une_promotion_inconnue_avec_404() {
        when(promotionRepository.existsById(99L)).thenReturn(false);

        BusinessException exception = catchThrowableOfType(
                () -> promotionService.listerEtudiantsDeLaPromotion(99L),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("PROMOTION_INCONNUE");
        assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
