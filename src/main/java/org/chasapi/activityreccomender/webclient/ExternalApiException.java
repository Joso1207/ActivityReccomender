package org.chasapi.activityreccomender.webclient;

public class ExternalApiException extends RuntimeException{

    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
