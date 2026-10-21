package com.meetingscheduler.scheduling.domain;

import com.meetingscheduler.shared.domain.TimeWindow;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Value object: aggregated free/busy view of the slots starting within a window.
 * {@code days} only contains days that have at least one slot, bucketed by the slot start in {@code zone}.
 */
public record Availability(TimeWindow window, ZoneId zone, Tally totals, List<Day> days) {

    public record Tally(int freeSlots, int busySlots, long freeMinutes, long busyMinutes) {

        /**
         * Free and busy slots together.
         */
        public int totalSlots() {
            return freeSlots + busySlots;
        }
    }

    public record Day(LocalDate date, Tally tally) {
    }

    /**
     * Totals the spans overall and per day, where the day of a span is the date its start falls on in {@code zone}.
     *
     * @param window the time frame {@code [from, to)}
     * @param zone time zone used to assign slots to days
     * @param spans the slots to summarize
     */
    public static Availability summarize(TimeWindow window, ZoneId zone, List<SlotSpan> spans) {
        Accumulator totals = new Accumulator();
        Map<LocalDate, Accumulator> perDay = new TreeMap<>();
        for (SlotSpan span : spans) {
            totals.add(span);
            perDay.computeIfAbsent(span.startTime().atZone(zone).toLocalDate(), day -> new Accumulator()).add(span);
        }
        List<Day> days = new ArrayList<>(perDay.size());
        perDay.forEach((date, accumulator) -> days.add(new Day(date, accumulator.toTally())));
        return new Availability(window, zone, totals.toTally(), days);
    }

    private static final class Accumulator {
        int freeSlots;
        int busySlots;
        long freeMinutes;
        long busyMinutes;

        void add(SlotSpan span) {
            if (span.status() == SlotStatus.FREE) {
                freeSlots++;
                freeMinutes += span.durationMinutes();
            } else {
                busySlots++;
                busyMinutes += span.durationMinutes();
            }
        }

        Tally toTally() {
            return new Tally(freeSlots, busySlots, freeMinutes, busyMinutes);
        }
    }
}
