package com.kfokam.k48.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "exercice")
public class Exercice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private SessionCours session;

    @ManyToOne(optional = false)
    @JoinColumn(name = "auteur_id", nullable = false)
    private Etudiant auteur;

    @Column(nullable = false, length = 2048)
    private String lien;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutExercice statut;

    @Column(name = "depose_at", nullable = false)
    private Instant deposeAt;

    protected Exercice() {
    }

    public Exercice(SessionCours session, Etudiant auteur, String lien, StatutExercice statut, Instant deposeAt) {
        this.session = session;
        this.auteur = auteur;
        this.lien = lien;
        this.statut = statut;
        this.deposeAt = deposeAt;
    }

    public Long getId() {
        return id;
    }

    public SessionCours getSession() {
        return session;
    }

    public Etudiant getAuteur() {
        return auteur;
    }

    public String getLien() {
        return lien;
    }

    /** EF12 / RG16 : remplaçable tant qu'aucune relecture n'a commencé et session non clôturée. */
    public void setLien(String lien) {
        this.lien = lien;
    }

    public StatutExercice getStatut() {
        return statut;
    }

    public void setStatut(StatutExercice statut) {
        this.statut = statut;
    }

    public Instant getDeposeAt() {
        return deposeAt;
    }
}
