package com.meetingscheduler.shared.domain;

/**
 * Base of all errors raised by the domain and application layers. The API layer translates the subtypes to HTTP statuses.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
