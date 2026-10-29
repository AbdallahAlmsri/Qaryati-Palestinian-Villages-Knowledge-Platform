package com.qaryati.qaryati.villagename;

public class VillageNameNotFoundException extends RuntimeException {
    public VillageNameNotFoundException(Long nameId) {
        super("Village name with id " + nameId + " does not exist");
    }
}