package com.meetingscheduler.scheduling.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurable limits of the scheduler, bound from the {@code scheduler.*} properties.
 */
@ConfigurationProperties(prefix = "scheduler")
public record SchedulerProperties(SlotProperties slot, AvailabilityProperties availability, MeetingProperties meeting) {

    /**
     * Limits of slot durations and of bulk slot creation.
     */
    public record SlotProperties(int minDurationMinutes, int maxDurationMinutes, int maxBulkSlots) {
    }

    /**
     * Limits of availability queries.
     */
    public record AvailabilityProperties(int maxWindowDays) {
    }

    /**
     * Limits of meetings.
     */
    public record MeetingProperties(int maxParticipants) {
    }
}
