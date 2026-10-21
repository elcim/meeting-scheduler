package com.meetingscheduler.scheduling.application;

import com.meetingscheduler.scheduling.domain.SlotStatus;

import java.time.Instant;

/**
 * Partial update: only the provided (non-null) fields are changed.
 */
public record UpdateSlotCommand(Instant startTime, Integer durationMinutes, SlotStatus status) {
}
