package org.chasapi.activityreccomender.exceptions;

public class ExternalServiceUnavailable extends RuntimeException {
    public ExternalServiceUnavailable(String message) {
        super(message);
    }
}
