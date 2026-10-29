package com.qaryati.qaryati.population;

public class PopulationRecordNotFoundException extends RuntimeException {
    public PopulationRecordNotFoundException(Long recordId) {
        super("Population record with id " + recordId + " does not exist");
    }
}