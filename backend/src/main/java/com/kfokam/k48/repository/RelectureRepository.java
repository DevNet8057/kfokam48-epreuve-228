package com.kfokam.k48.repository;

import com.kfokam.k48.domain.Relecture;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RelectureRepository extends JpaRepository<Relecture, Long> {

    long countByRelecteurId(Long relecteurId);
}
