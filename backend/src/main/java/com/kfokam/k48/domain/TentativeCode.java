package com.kfokam.k48.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** RG7 : compteur de codes inconnus consécutifs et fin de blocage, un par étudiant. */
@Entity
@Table(name = "tentative_code")
public class TentativeCode {

    @Id
    @Column(name = "etudiant_id")
    private Long etudiantId;

    @Column(name = "echecs_consecutifs", nullable = false)
    private int echecsConsecutifs;

    @Column(name = "bloque_jusqu_a")
    private Instant bloqueJusquA;

    protected TentativeCode() {
    }

    public TentativeCode(Long etudiantId) {
        this.etudiantId = etudiantId;
    }

    public Long getEtudiantId() {
        return etudiantId;
    }

    public int getEchecsConsecutifs() {
        return echecsConsecutifs;
    }

    public Instant getBloqueJusquA() {
        return bloqueJusquA;
    }

    public void enregistrerEchec() {
        echecsConsecutifs++;
    }

    public void bloquerJusqua(Instant fin) {
        bloqueJusquA = fin;
    }

    public void reinitialiser() {
        echecsConsecutifs = 0;
        bloqueJusquA = null;
    }
}
