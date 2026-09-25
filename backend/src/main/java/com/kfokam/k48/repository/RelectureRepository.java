package com.kfokam.k48.repository;

import com.kfokam.k48.domain.Relecture;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RelectureRepository extends JpaRepository<Relecture, Long> {

    long countByRelecteurId(Long relecteurId);

    /** EF8 : relectures assignées et non rendues (RG19). */
    List<Relecture> findByRelecteurIdAndRendueAtIsNull(Long relecteurId);
}
