package com.kfokam.k48.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "promotion")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    protected Promotion() {
    }

    public Promotion(String nom) {
        this.nom = nom;
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }
}
