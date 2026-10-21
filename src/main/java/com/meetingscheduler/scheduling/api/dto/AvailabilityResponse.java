package com.meetingscheduler.scheduling.api.dto;

import com.meetingscheduler.scheduling.domain.Availability;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Aggregated free/busy view of the slots starting within [from, to).
 * {@code days} only contains days that have at least one slot, bucketed by the slot start in {@code zone}.
 */
public record AvailabilityResponse(
        Instant from,
        Instant to,
        String zone,
        Totals totals,
        List<Day> days) {

    public record Totals(int totalSlots, int freeSlots, int busySlots, long freeMinutes, long busyMinutes) {
    }

    public record Day(LocalDate date, int freeSlots, int busySlots, long freeMinutes, long busyMinutes) {
    }

    public static AvailabilityResponse from(Availability availability) {
        Availability.Tally totals = availability.totals();
        return new AvailabilityResponse(availability.window().from(), availability.window().to(),
                availability.zone().getId(),
                new Totals(totals.totalSlots(), totals.freeSlots(), totals.busySlots(), totals.freeMinutes(), totals.busyMinutes()),
                availability.days().stream()
                        .map(day -> new Day(day.date(), day.tally().freeSlots(), day.tally().busySlots(),
                                day.tally().freeMinutes(), day.tally().busyMinutes()))
                        .toList());
    }
}
