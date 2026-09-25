package com.kfokam.k48.repository;

import com.kfokam.k48.domain.SessionCours;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionCoursRepository extends JpaRepository<SessionCours, Long> {

    @Query("""
            select case when count(s) > 0 then true else false end
            from SessionCours s
            where s.code = :code and s.clotureAt is null and s.expirationAt > :maintenant
            """)
    boolean existeCodeActif(@Param("code") String code, @Param("maintenant") Instant maintenant);
}
