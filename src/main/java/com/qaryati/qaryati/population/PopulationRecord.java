package com.qaryati.qaryati.population;

import com.qaryati.qaryati.user.User;
import com.qaryati.qaryati.village.Village;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "population_records")
public class PopulationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "village_id", nullable = false)
    private Village village;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer population;

    @Column(nullable = false)
    private String source;

    @Column(name = "source_url")
    private String sourceUrl;

    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PopulationStatus status;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    protected PopulationRecord() {

    }

    public PopulationRecord(Village village, Integer year, Integer population, String source,
                            String sourceUrl, String notes, User createdBy) {
        this.village = village;
        this.year = year;
        this.population = population;
        this.source = source;
        this.sourceUrl = sourceUrl;
        this.notes = notes;
        this.status = PopulationStatus.PENDING;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Village getVillage() { return village; }
    public Integer getYear() { return year; }
    public Integer getPopulation() { return population; }
    public String getSource() { return source; }
    public String getSourceUrl() { return sourceUrl; }
    public String getNotes() { return notes; }
    public PopulationStatus getStatus() { return status; }
    public User getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public User getReviewedBy() { return reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }

    public void review(PopulationStatus newStatus, User reviewer) {
        this.status = newStatus;
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
    }
}
