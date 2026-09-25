package com.kfokam.k48.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "relecture")
public class Relecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "exercice_id", nullable = false, unique = true)
    private Exercice exercice;

    @ManyToOne(optional = false)
    @JoinColumn(name = "relecteur_id", nullable = false)
    private Etudiant relecteur;

    @Column(name = "ouverte_at")
    private Instant ouverteAt;

    private Integer note;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "rendue_at")
    private Instant rendueAt;

    protected Relecture() {
    }

    public Relecture(Exercice exercice, Etudiant relecteur) {
        this.exercice = exercice;
        this.relecteur = relecteur;
    }

    public Long getId() {
        return id;
    }

    public Exercice getExercice() {
        return exercice;
    }

    public Etudiant getRelecteur() {
        return relecteur;
    }

    public Instant getOuverteAt() {
        return ouverteAt;
    }

    public void setOuverteAt(Instant ouverteAt) {
        this.ouverteAt = ouverteAt;
    }

    public Integer getNote() {
        return note;
    }

    public void setNote(Integer note) {
        this.note = note;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public Instant getRendueAt() {
        return rendueAt;
    }

    public void setRendueAt(Instant rendueAt) {
        this.rendueAt = rendueAt;
    }
}
