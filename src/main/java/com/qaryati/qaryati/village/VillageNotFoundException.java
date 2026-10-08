package com.qaryati.qaryati.village;

public class VillageNotFoundException extends RuntimeException {
    public VillageNotFoundException(Long villageId) {
        super("Village with id " + villageId + " does not exist");
    }
}