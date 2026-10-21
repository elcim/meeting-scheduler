package com.meetingscheduler.scheduling.domain;

import java.time.Instant;

/**
 * Lightweight projection of a slot used for aggregations, avoids materializing aggregates.
 */
public record SlotSpan(Instant startTime, int durationMinutes, SlotStatus status) {
}
