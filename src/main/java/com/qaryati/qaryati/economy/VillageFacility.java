package com.qaryati.qaryati.economy;

import com.qaryati.qaryati.user.User;
import com.qaryati.qaryati.village.Village;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "village_facilities")
public class VillageFacility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "village_id", nullable = false)
    private Village village;

    @Column(name = "facility_name", nullable = false, length = 150)
    private String facilityName;

    @Column(name = "facility_type", nullable = false, length = 100)
    private String facilityType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String source;

    @Column(nullable = false, length = 20)
    private String status;

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

    protected VillageFacility() {}

    public VillageFacility(Village village, String facilityName,
                           String facilityType, String description,
                           String source, User createdBy) {
        this.village = village;
        this.facilityName = facilityName;
        this.facilityType = facilityType;
        this.description = description;
        this.source = source;
        this.status = "PENDING";
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Village getVillage() { return village; }
    public String getFacilityName() { return facilityName; }
    public String getFacilityType() { return facilityType; }
    public String getDescription() { return description; }
    public String getSource() { return source; }
    public String getStatus() { return status; }
    public User getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public User getReviewedBy() { return reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }

    public void review(String newStatus, User reviewer) {
        if (!"VERIFIED".equals(newStatus) && !"REJECTED".equals(newStatus)) {
            throw new IllegalArgumentException("Invalid review status");
        }

        if (!"PENDING".equals(status)) {
            throw new IllegalStateException("Only PENDING records can be reviewed");
        }

        this.status = newStatus;
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
    }
}