package com.spacecargo.stowage.exception;

/** Thrown when a requested entity (item, container) does not exist. Maps to HTTP 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
