package com.meetingscheduler.scheduling.api.dto;

import com.meetingscheduler.scheduling.application.CreateSlotCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record CreateSlotRequest(
        @NotNull Instant startTime,
        @NotNull @Positive Integer durationMinutes) {

    public CreateSlotCommand toCommand() {
        return new CreateSlotCommand(startTime, durationMinutes);
    }
}
