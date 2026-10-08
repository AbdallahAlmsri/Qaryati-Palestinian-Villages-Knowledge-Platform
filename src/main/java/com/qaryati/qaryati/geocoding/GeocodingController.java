package com.qaryati.qaryati.geocoding;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/villages/{villageId}/geocode-check")
public class GeocodingController {

    private final GeocodingService geocodingService;

    public GeocodingController(GeocodingService geocodingService) {
        this.geocodingService = geocodingService;
    }

    @GetMapping
    public GeocodeCheckResponse checkCoordinates(@PathVariable Long villageId) {
        return geocodingService.checkCoordinates(villageId);
    }
}