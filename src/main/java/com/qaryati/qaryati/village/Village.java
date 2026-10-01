package com.qaryati.qaryati.village;

import com.qaryati.qaryati.governorate.Governorate;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "villages")
public class Village {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "governorate_id", nullable = false)
    private Governorate governorate;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @Column(name = "location_description")
    private String locationDescription;

    @Column(name = "elevation_m")
    private Integer elevationM;

    protected Village() {
        // required by JPA
    }

    public Long getId() {
        return id;
    }

    public Governorate getGovernorate() {
        return governorate;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public String getLocationDescription() {
        return locationDescription;
    }

    public Integer getElevationM() {
        return elevationM;
    }
}