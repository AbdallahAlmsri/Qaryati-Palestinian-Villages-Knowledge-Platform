package com.qaryati.qaryati.village;

import java.math.BigDecimal;

/** One row of the native nearby-villages query. Column aliases in the query match these getters. */
public interface NearbyVillageRow {

    Long getId();

    String getGovernorateName();

    BigDecimal getLatitude();

    BigDecimal getLongitude();

    String getLocationDescription();

    Double getDistanceKm();
}
