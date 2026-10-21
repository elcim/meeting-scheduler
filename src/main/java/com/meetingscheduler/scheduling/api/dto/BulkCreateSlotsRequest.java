package com.meetingscheduler.scheduling.api.dto;

import com.meetingscheduler.scheduling.application.BulkCreateSlotsCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

/**
 * Splits [from, to) into consecutive slots of {@code slotDurationMinutes}, separated by {@code gapMinutes}.
 * A trailing remainder shorter than a full slot is not created.
 */
public record BulkCreateSlotsRequest(
        @NotNull Instant from,
        @NotNull Instant to,
        @NotNull @Positive Integer slotDurationMinutes,
        @PositiveOrZero Integer gapMinutes) {

    public BulkCreateSlotsCommand toCommand() {
        return new BulkCreateSlotsCommand(from, to, slotDurationMinutes, gapMinutes == null ? 0 : gapMinutes);
    }
}
