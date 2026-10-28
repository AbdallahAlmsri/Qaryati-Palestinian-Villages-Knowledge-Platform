package com.qaryati.qaryati.geocoding;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class MissingVerifiedArabicNameException extends RuntimeException {

    public MissingVerifiedArabicNameException(String message) {
        super(message);
    }
}