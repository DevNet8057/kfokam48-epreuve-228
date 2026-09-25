package com.kfokam.k48.repository;

import com.kfokam.k48.domain.Presence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PresenceRepository extends JpaRepository<Presence, Long> {

    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);

    @Query("select p.etudiant.id from Presence p where p.session.id = :sessionId")
    List<Long> trouverIdsEtudiantsPresents(@Param("sessionId") Long sessionId);
}
