package com.qaryati.qaryati.history;

import com.qaryati.qaryati.user.User;
import com.qaryati.qaryati.village.Village;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "village_attributes")
public class VillageAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "village_id", nullable = false)
    private Village village;

    @Column(name = "attribute_name", nullable = false, length = 150)
    private String attributeName;

    @Column(name = "attribute_value", nullable = false, columnDefinition = "TEXT")
    private String attributeValue;

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

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    protected VillageAttribute() {
    }

    public VillageAttribute(Village village, String attributeName,
                            String attributeValue, String source, User createdBy) {
        this.village = village;
        this.attributeName = attributeName;
        this.attributeValue = attributeValue;
        this.source = source;
        this.status = "PENDING";
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Village getVillage() { return village; }
    public String getAttributeName() { return attributeName; }
    public String getAttributeValue() { return attributeValue; }
    public String getSource() { return source; }
    public String getStatus() { return status; }
    public User getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public User getReviewedBy() { return reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public String getReviewNotes() { return reviewNotes; }

    public void review(String newStatus, User reviewer) {
        review(newStatus, reviewer, null);
    }

    public void review(String newStatus, User reviewer, String reviewNotes) {
        if (!"VERIFIED".equals(newStatus) && !"REJECTED".equals(newStatus)) {
            throw new IllegalArgumentException("Invalid review status");
        }

        this.status = newStatus;
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
        this.reviewNotes = reviewNotes;
    }
}
