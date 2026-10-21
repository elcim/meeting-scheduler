package com.meetingscheduler.scheduling.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scheduler")
public record SchedulerProperties(SlotProperties slot, AvailabilityProperties availability, MeetingProperties meeting) {

    public record SlotProperties(int minDurationMinutes, int maxDurationMinutes, int maxBulkSlots) {
    }

    public record AvailabilityProperties(int maxWindowDays) {
    }

    public record MeetingProperties(int maxParticipants) {
    }
}
