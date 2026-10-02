package com.qaryati.qaryati.village;

public class GovernorateNotFoundException extends RuntimeException {
    public GovernorateNotFoundException(Long governorateId) {
        super("Governorate with id " + governorateId + " does not exist");
    }
}