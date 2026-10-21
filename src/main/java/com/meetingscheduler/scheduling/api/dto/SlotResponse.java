package com.meetingscheduler.scheduling.api.dto;

import com.meetingscheduler.scheduling.domain.Slot;
import com.meetingscheduler.scheduling.domain.SlotStatus;

import java.time.Instant;

/**
 * Representation of a slot.
 */
public record SlotResponse(
        Long id,
        Instant startTime,
        Instant endTime,
        int durationMinutes,
        SlotStatus status,
        boolean booked) {

    public static SlotResponse from(Slot slot) {
        return new SlotResponse(slot.getId(), slot.getStartTime(), slot.getEndTime(),
                slot.getDurationMinutes(), slot.getStatus(), slot.isBooked());
    }
}
