package com.qaryati.qaryati.user;

public class UserRoleConflictException extends RuntimeException {
    public UserRoleConflictException(String message) {
        super(message);
    }
}