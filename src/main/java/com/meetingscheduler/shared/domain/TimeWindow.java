package com.meetingscheduler.shared.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * Value object for the half-open interval [from, to).
 */
public record TimeWindow(Instant from, Instant to) {

    public TimeWindow {
        if (!from.isBefore(to)) {
            throw new InvalidRequestException("'from' must be before 'to'");
        }
    }

    public Duration length() {
        return Duration.between(from, to);
    }
}
