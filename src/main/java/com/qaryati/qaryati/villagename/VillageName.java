package com.qaryati.qaryati.villagename;

import com.qaryati.qaryati.user.User;
import com.qaryati.qaryati.village.Village;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "village_names")
public class VillageName {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "village_id", nullable = false)
    private Village village;

    @Column(nullable = false)
    private String name;

    @Column(name = "name_type", nullable = false)
    private String nameType;

    private String language;

    @Column(name = "valid_from")
    private Integer validFrom;

    @Column(name = "valid_to")
    private Integer validTo;

    private String source;

    @Column(name = "name_origin", columnDefinition = "TEXT")
    private String nameOrigin;

    @Column(nullable = false)
    private String status;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    protected VillageName() {
    }

    public VillageName(Village village, String name, String nameType,
                       String language, Integer validFrom, Integer validTo,
                       String source, String nameOrigin, User createdBy) {
        this.village = village;
        this.name = name;
        this.nameType = nameType;
        this.language = language;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.source = source;
        this.nameOrigin = nameOrigin;
        this.status = "PENDING";
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Village getVillage() { return village; }
    public String getName() { return name; }
    public String getNameType() { return nameType; }
    public String getLanguage() { return language; }
    public Integer getValidFrom() { return validFrom; }
    public Integer getValidTo() { return validTo; }
    public String getSource() { return source; }
    public String getNameOrigin() { return nameOrigin; }
    public String getStatus() { return status; }
    public User getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public User getReviewedBy() { return reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
}