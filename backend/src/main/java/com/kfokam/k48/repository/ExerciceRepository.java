package com.kfokam.k48.repository;

import com.kfokam.k48.domain.Exercice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciceRepository extends JpaRepository<Exercice, Long> {

    boolean existsBySessionIdAndAuteurId(Long sessionId, Long auteurId);

    /** K48-10 : tous les exercices déposés par les étudiants d'une promotion, en un aller. */
    List<Exercice> findByAuteurIdIn(List<Long> auteurIds);

    /** RG13 : exercices d'une session, pour retenter l'attribution des relecteurs. */
    List<Exercice> findBySessionId(Long sessionId);

    /** EF11 : exercices déposés par un étudiant, le plus récent d'abord. */
    List<Exercice> findByAuteurIdOrderByDeposeAtDesc(Long auteurId);
}
