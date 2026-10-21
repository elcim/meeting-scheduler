package com.meetingscheduler.scheduling.api.dto;

import com.meetingscheduler.scheduling.application.UpdateSlotCommand;
import com.meetingscheduler.scheduling.domain.SlotStatus;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

/**
 * Partial update: only the provided fields are changed.
 */
public record UpdateSlotRequest(
        Instant startTime,
        @Positive Integer durationMinutes,
        SlotStatus status) {

    public UpdateSlotCommand toCommand() {
        return new UpdateSlotCommand(startTime, durationMinutes, status);
    }
}
