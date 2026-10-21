package com.meetingscheduler.scheduling.application;

import java.time.Instant;

/**
 * Splits [from, to) into consecutive slots of {@code slotDurationMinutes}, separated by {@code gapMinutes}.
 */
public record BulkCreateSlotsCommand(Instant from, Instant to, int slotDurationMinutes, int gapMinutes) {
}
