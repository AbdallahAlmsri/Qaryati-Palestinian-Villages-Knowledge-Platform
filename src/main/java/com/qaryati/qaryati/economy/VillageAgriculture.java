package com.qaryati.qaryati.economy;

import com.qaryati.qaryati.user.User;
import com.qaryati.qaryati.village.Village;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "village_agriculture")
public class VillageAgriculture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "village_id", nullable = false)
    private Village village;

    @Column(name = "crop_name", nullable = false, length = 150)
    private String cropName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String source;

    @Column(nullable = false, length = 20)
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

    protected VillageAgriculture() {}

    public VillageAgriculture(Village village, String cropName,
                              String description, String source, User createdBy) {
        this.village = village;
        this.cropName = cropName;
        this.description = description;
        this.source = source;
        this.status = "PENDING";
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Village getVillage() { return village; }
    public String getCropName() { return cropName; }
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