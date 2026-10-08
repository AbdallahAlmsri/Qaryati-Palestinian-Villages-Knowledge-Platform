package com.qaryati.qaryati.geocoding;

public class GeocodingUnavailableException extends RuntimeException {
    public GeocodingUnavailableException(String message) {
        super(message);
    }
}