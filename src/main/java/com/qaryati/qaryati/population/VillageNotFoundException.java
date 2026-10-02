package com.qaryati.qaryati.population;

public class VillageNotFoundException extends RuntimeException {
    public VillageNotFoundException(Long villageId) {
        super("Village with id " + villageId + " does not exist");
    }
}