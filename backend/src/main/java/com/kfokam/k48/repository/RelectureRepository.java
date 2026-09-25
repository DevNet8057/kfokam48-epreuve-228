package com.kfokam.k48.repository;

import com.kfokam.k48.domain.Relecture;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RelectureRepository extends JpaRepository<Relecture, Long> {

    long countByRelecteurId(Long relecteurId);

    /** EF8 : relectures assignées et non rendues (RG19). */
    List<Relecture> findByRelecteurIdAndRendueAtIsNull(Long relecteurId);

    /** K48-10 : relectures en attente pour tous les relecteurs d'une promotion, en un aller. */
    List<Relecture> findByRelecteurIdInAndRendueAtIsNull(List<Long> relecteurIds);

    /** K48-10 : relectures reçues (en tant qu'auteur de l'exercice) pour le calcul de la moyenne (RG18). */
    List<Relecture> findByExercice_AuteurIdIn(List<Long> auteurIds);

    /** C2 (deux relecteurs) : toutes les lignes de relecture d'un exercice (une par relecteur assigné). */
    List<Relecture> findByExercice_Id(Long exerciceId);
}
