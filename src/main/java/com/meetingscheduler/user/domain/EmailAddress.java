package com.meetingscheduler.user.domain;

import com.meetingscheduler.shared.domain.InvalidRequestException;

import java.util.Locale;

/**
 * Value object: an email address in its canonical form (trimmed, lower case).
 */
public record EmailAddress(String value) {

    public EmailAddress {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException("Email must not be blank");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
    }
}
