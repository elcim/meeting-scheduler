package com.meetingscheduler.shared.domain;

/**
 * An operation violates a business rule given the current state (overlapping slots, double booking, ...).
 */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }
}
