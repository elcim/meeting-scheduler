package com.meetingscheduler.scheduling.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Value object: consecutive slots of {@code slotMinutes}, separated by {@code gapMinutes}, that fit a time window.
 * A trailing remainder shorter than a full slot is not part of the series.
 */
public record SlotSeries(Instant firstStart, int slotMinutes, int gapMinutes, int count) {

    public List<Instant> starts() {
        List<Instant> starts = new ArrayList<>(count);
        Instant start = firstStart;
        for (int i = 0; i < count; i++) {
            starts.add(start);
            start = start.plus(Duration.ofMinutes(slotMinutes + gapMinutes));
        }
        return starts;
    }

    public Instant end() {
        return firstStart.plus(Duration.ofMinutes((long) (slotMinutes + gapMinutes) * (count - 1) + slotMinutes));
    }
}
