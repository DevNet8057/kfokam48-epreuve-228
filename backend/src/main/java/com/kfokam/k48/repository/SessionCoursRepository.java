package com.kfokam.k48.repository;

import com.kfokam.k48.domain.SessionCours;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionCoursRepository extends JpaRepository<SessionCours, Long> {

    List<SessionCours> findByPromotionIdOrderByOuvertureAtAsc(Long promotionId);

    @Query("""
            select case when count(s) > 0 then true else false end
            from SessionCours s
            where s.code = :code and s.clotureAt is null and s.expirationAt > :maintenant
            """)
    boolean existeCodeActif(@Param("code") String code, @Param("maintenant") Instant maintenant);

    /**
     * RG6/H4 : un code n'est reconnu que dans la promotion de l'étudiant qui le saisit ;
     * sinon (mauvaise promotion ou code inexistant), l'appelant reçoit CODE_INCONNU.
     * La plus récente en cas d'homonymie de code entre deux périodes.
     */
    Optional<SessionCours> findFirstByCodeAndPromotionIdOrderByOuvertureAtDesc(String code, Long promotionId);
}
