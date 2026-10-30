package com.qaryati.qaryati.governorate;

import jakarta.persistence.*;

@Entity
@Table(name = "municipalities")
public class Municipality {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "governorate_id", nullable = false)
    private Governorate governorate;

    @Column(nullable = false)
    private String name;

    protected Municipality() {
    }

    public Municipality(Governorate governorate, String name) {
        this.governorate = governorate;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public Governorate getGovernorate() {
        return governorate;
    }

    public String getName() {
        return name;
    }
}
