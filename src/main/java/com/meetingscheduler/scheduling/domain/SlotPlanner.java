package com.meetingscheduler.scheduling.domain;

import com.meetingscheduler.scheduling.repository.SlotRepository;
import com.meetingscheduler.shared.domain.ConflictException;
import com.meetingscheduler.shared.domain.InvalidRequestException;
import com.meetingscheduler.shared.domain.TimeWindow;

import java.time.Instant;
import java.util.List;

/**
 * Domain service enforcing the rule that spans several slots: the slots of one calendar never overlap.
 * <p>
 * Callers must hold the lock of the {@link Calendar} (see {@link com.meetingscheduler.scheduling.repository.CalendarRepository#findByOwnerForUpdate}), otherwise
 * concurrent requests could both pass the overlap check.
 */
public class SlotPlanner {

    private final SlotRepository slotRepository;
    private final SlotLimits limits;

    public SlotPlanner(SlotRepository slotRepository, SlotLimits limits) {
        this.slotRepository = slotRepository;
        this.limits = limits;
    }

    /**
     * Fails if the duration is outside the allowed range, without touching the calendar.
     *
     * @param minutes duration in minutes
     */
    public void requireValidDuration(int minutes) {
        limits.requireValidDuration(minutes);
    }

    /**
     * Splits [from, to) into as many slots as fit; does not touch the calendar yet.
     *
     * @param from start of the time frame
     * @param to end of the time frame
     * @param slotMinutes duration of each slot in minutes
     * @param gapMinutes pause between consecutive slots in minutes
     */
    public SlotSeries planSeries(Instant from, Instant to, int slotMinutes, int gapMinutes) {
        limits.requireValidDuration(slotMinutes);
        TimeWindow window = new TimeWindow(from, to);

        long count = (window.length().toMinutes() + gapMinutes) / (slotMinutes + gapMinutes);
        if (count == 0) {
            throw new InvalidRequestException("The time frame is too short for a single slot");
        }
        if (count > limits.maxBulkSlots()) {
            throw new InvalidRequestException("At most " + limits.maxBulkSlots() + " slots can be created at once, requested " + count);
        }
        return new SlotSeries(from, slotMinutes, gapMinutes, (int) count);
    }

    /**
     * Creates a slot in the calendar, failing if it overlaps an existing one.
     *
     * @param calendar the calendar the slot is placed in
     * @param start start of the slot
     * @param minutes duration of the slot in minutes
     */
    public Slot place(Calendar calendar, Instant start, int minutes) {
        Slot slot = Slot.create(calendar.getId(), start, minutes, limits);
        if (slotRepository.existsOverlapping(calendar.getId(), new TimeWindow(slot.getStartTime(), slot.getEndTime()))) {
            throw overlap(slot);
        }
        return slot;
    }

    /**
     * Creates the slots of the series in the calendar, failing if the series overlaps an existing slot.
     *
     * @param calendar the calendar the slot is placed in
     * @param series the slots to create
     */
    public List<Slot> place(Calendar calendar, SlotSeries series) {
        List<Slot> slots = series.starts().stream()
                .map(start -> Slot.create(calendar.getId(), start, series.slotMinutes(), limits))
                .toList();
        // The slots of a series never overlap each other, so only the existing ones need checking.
        if (slotRepository.existsOverlapping(calendar.getId(), new TimeWindow(series.firstStart(), series.end()))) {
            throw new ConflictException("The time frame " + series.firstStart() + " - " + series.end()
                    + " overlaps an existing slot");
        }
        return slots;
    }

    /**
     * Moves and/or resizes the slot, failing if the new time overlaps another slot of the calendar.
     *
     * @param slot the slot to move or resize
     * @param start new start of the slot
     * @param minutes new duration in minutes
     */
    public void reschedule(Slot slot, Instant start, int minutes) {
        slot.reschedule(start, minutes, limits);
        if (slotRepository.existsOverlappingExcluding(slot.getCalendarId(),
                new TimeWindow(slot.getStartTime(), slot.getEndTime()), slot.getId())) {
            throw overlap(slot);
        }
    }

    private static ConflictException overlap(Slot slot) {
        return new ConflictException("The time frame " + slot.getStartTime() + " - " + slot.getEndTime()
                + " overlaps an existing slot");
    }
}
