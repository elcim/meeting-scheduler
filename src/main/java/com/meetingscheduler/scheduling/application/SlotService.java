package com.meetingscheduler.scheduling.application;

import com.meetingscheduler.scheduling.domain.Calendar;
import com.meetingscheduler.scheduling.domain.Slot;
import com.meetingscheduler.scheduling.domain.SlotPlanner;
import com.meetingscheduler.scheduling.repository.SlotRepository;
import com.meetingscheduler.scheduling.domain.SlotSeries;
import com.meetingscheduler.scheduling.domain.SlotStatus;
import com.meetingscheduler.shared.domain.InvalidRequestException;
import com.meetingscheduler.shared.domain.Page;
import com.meetingscheduler.shared.domain.PageQuery;
import com.meetingscheduler.shared.domain.ResourceNotFoundException;
import com.meetingscheduler.shared.domain.TimeWindow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Use cases around slots: creating (also in bulk), listing, changing and deleting them.
 */
@Service
@Transactional(readOnly = true)
public class SlotService {

    private final SlotRepository slotRepository;
    private final SlotPlanner slotPlanner;
    private final CalendarLookup calendarLookup;

    SlotService(SlotRepository slotRepository, SlotPlanner slotPlanner, CalendarLookup calendarLookup) {
        this.slotRepository = slotRepository;
        this.slotPlanner = slotPlanner;
        this.calendarLookup = calendarLookup;
    }

    /**
     * Creates a slot, failing if it overlaps an existing one.
     *
     * @param userId id of the user owning the slot
     * @param command start and duration of the slot
     */
    @Transactional
    public Slot create(Long userId, CreateSlotCommand command) {
        slotPlanner.requireValidDuration(command.durationMinutes());
        Calendar calendar = calendarLookup.lockCalendarOf(userId);

        return slotRepository.save(slotPlanner.place(calendar, command.startTime(), command.durationMinutes()));
    }

    /**
     * Creates the consecutive slots that fill a time frame; either all of them are created or none.
     *
     * @param userId id of the user owning the slot
     * @param command time frame, slot duration and gap between slots
     */
    @Transactional
    public List<Slot> createBulk(Long userId, BulkCreateSlotsCommand command) {
        SlotSeries series = slotPlanner.planSeries(command.from(), command.to(),
                command.slotDurationMinutes(), command.gapMinutes());
        Calendar calendar = calendarLookup.lockCalendarOf(userId);

        return slotRepository.saveAll(slotPlanner.place(calendar, series));
    }

    /**
     * Returns a slot of the user.
     *
     * @param userId id of the user owning the slot
     * @param slotId id of the slot
     */
    public Slot get(Long userId, Long slotId) {
        Long calendarId = calendarLookup.calendarIdOf(userId);
        return slotRepository.findInCalendar(slotId, calendarId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot", slotId));
    }

    /**
     * Lists the slots starting within the window, optionally restricted to one status.
     *
     * @param userId id of the user owning the slot
     * @param window the time frame {@code [from, to)}
     * @param status only slots with this status, any if {@code null}
     * @param page which page to fetch
     */
    public Page<Slot> list(Long userId, TimeWindow window, SlotStatus status, PageQuery page) {
        Long calendarId = calendarLookup.calendarIdOf(userId);
        return slotRepository.findStartingIn(calendarId, window, status, page);
    }

    /**
     * Moves, resizes and/or changes the status of a slot, failing if the new time overlaps another slot.
     *
     * @param userId id of the user owning the slot
     * @param slotId id of the slot
     * @param command the new start, duration and/or status; at least one is required
     */
    @Transactional
    public Slot update(Long userId, Long slotId, UpdateSlotCommand command) {
        boolean changesTime = command.startTime() != null || command.durationMinutes() != null;
        if (!changesTime && command.status() == null) {
            throw new InvalidRequestException("Nothing to update, provide startTime, durationMinutes or status");
        }

        // Moving or resizing a slot can create overlaps, so it takes the calendar lock like creation does.
        Long calendarId = changesTime ? calendarLookup.lockCalendarOf(userId).getId() : calendarLookup.calendarIdOf(userId);
        Slot slot = slotRepository.findInCalendarForUpdate(slotId, calendarId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot", slotId));

        if (changesTime) {
            Instant start = command.startTime() != null ? command.startTime() : slot.getStartTime();
            int duration = command.durationMinutes() != null ? command.durationMinutes() : slot.getDurationMinutes();
            slotPlanner.reschedule(slot, start, duration);
        }
        if (command.status() == SlotStatus.FREE) {
            slot.markFree();
        } else if (command.status() == SlotStatus.BUSY) {
            slot.markBusy();
        }
        return slot;
    }

    /**
     * Deletes a slot, unless it holds a meeting.
     *
     * @param userId id of the user owning the slot
     * @param slotId id of the slot
     */
    @Transactional
    public void delete(Long userId, Long slotId) {
        Long calendarId = calendarLookup.calendarIdOf(userId);
        Slot slot = slotRepository.findInCalendarForUpdate(slotId, calendarId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot", slotId));
        slot.requireDeletable();
        slotRepository.delete(slot);
    }
}
