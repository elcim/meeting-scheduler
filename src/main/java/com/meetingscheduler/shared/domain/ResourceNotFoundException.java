package com.meetingscheduler.shared.domain;

/**
 * Signals that a requested resource does not exist.
 */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " " + id + " not found");
    }
}
