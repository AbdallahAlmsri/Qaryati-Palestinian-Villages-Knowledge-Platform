package com.qaryati.qaryati.governorate;

import jakarta.persistence.*;

@Entity
@Table(name = "governorates")
public class Governorate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    protected Governorate() {
        
    }

    public Governorate(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}