package com.qaryati.qaryati.history;

import com.qaryati.qaryati.user.User;
import com.qaryati.qaryati.village.Village;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "landmarks")
public class Landmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "village_id", nullable = false)
    private Village village;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "landmark_type", length = 100)
    private String landmarkType;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

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

    protected Landmark() {
    }

    public Landmark(Village village, String name, String description,
                    String landmarkType, BigDecimal latitude,
                    BigDecimal longitude, String source, User createdBy) {
        this.village = village;
        this.name = name;
        this.description = description;
        this.landmarkType = landmarkType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.source = source;
        this.status = "PENDING";
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Village getVillage() { return village; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getLandmarkType() { return landmarkType; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
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
        this.status = newStatus;
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
    }
}