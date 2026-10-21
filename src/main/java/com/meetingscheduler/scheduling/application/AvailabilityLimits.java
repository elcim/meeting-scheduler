package com.meetingscheduler.scheduling.application;

/**
 * Policy limiting how large a time window an availability query may span.
 */
public record AvailabilityLimits(int maxWindowDays) {
}
