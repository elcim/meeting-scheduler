package com.meetingscheduler.scheduling.application;

import java.time.Instant;

/**
 * Input for creating a single slot.
 */
public record CreateSlotCommand(Instant startTime, int durationMinutes) {
}
