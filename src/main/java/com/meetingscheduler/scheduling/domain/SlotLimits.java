package com.meetingscheduler.scheduling.domain;

import com.meetingscheduler.shared.domain.InvalidRequestException;

/**
 * Configurable invariants of slot sizes.
 */
public record SlotLimits(int minDurationMinutes, int maxDurationMinutes, int maxBulkSlots) {

    /**
     * Fails if the duration is outside the allowed range.
     *
     * @param minutes duration in minutes
     */
    public void requireValidDuration(int minutes) {
        if (minutes < minDurationMinutes || minutes > maxDurationMinutes) {
            throw new InvalidRequestException("Slot duration must be between " + minDurationMinutes
                    + " and " + maxDurationMinutes + " minutes");
        }
    }
}
