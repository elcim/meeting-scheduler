package com.meetingscheduler.shared.domain;

/**
 * An input value violates an invariant regardless of the current state (bad duration, inverted time window, ...).
 */
public class InvalidRequestException extends DomainException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
