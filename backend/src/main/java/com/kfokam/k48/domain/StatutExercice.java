package com.kfokam.k48.domain;

/** Cycle de vie D4. Correspond à la contrainte CHECK de exercice.statut (V1__schema_initial.sql). */
public enum StatutExercice {
    SANS_RELECTEUR,
    EN_ATTENTE_RELECTURE,
    EN_COURS_RELECTURE,
    RELU
}
