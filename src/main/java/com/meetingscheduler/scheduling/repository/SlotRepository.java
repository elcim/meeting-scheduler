package com.meetingscheduler.scheduling.repository;

import com.meetingscheduler.scheduling.domain.Slot;
import com.meetingscheduler.scheduling.domain.SlotSpan;
import com.meetingscheduler.scheduling.domain.SlotStatus;
import com.meetingscheduler.shared.domain.Page;
import com.meetingscheduler.shared.domain.PageQuery;
import com.meetingscheduler.shared.domain.TimeWindow;

import java.util.List;
import java.util.Optional;

/**
 * Port for storing and finding {@link Slot slots}; all lookups are scoped to one calendar.
 */
public interface SlotRepository {

    /**
     * Stores the slot.
     *
     * @param slot the slot to store
     */
    Slot save(Slot slot);

    /**
     * Stores the slots in one batch.
     *
     * @param slots the slots to store
     */
    List<Slot> saveAll(List<Slot> slots);

    /**
     * Removes the slot.
     *
     * @param slot the slot to remove
     */
    void delete(Slot slot);

    /**
     * The slot, if it belongs to the calendar.
     *
     * @param slotId id of the slot
     * @param calendarId id of the calendar
     */
    Optional<Slot> findInCalendar(Long slotId, Long calendarId);

    /**
     * Same as {@link #findInCalendar} but locks the slot row until the end of the transaction.
     *
     * @param slotId id of the slot
     * @param calendarId id of the calendar
     */
    Optional<Slot> findInCalendarForUpdate(Long slotId, Long calendarId);

    /**
     * Whether any slot of the calendar intersects the window.
     *
     * @param calendarId id of the calendar
     * @param window the time frame {@code [from, to)}
     */
    boolean existsOverlapping(Long calendarId, TimeWindow window);

    /**
     * Whether any slot of the calendar other than {@code excludedSlotId} intersects the window.
     *
     * @param calendarId id of the calendar
     * @param window the time frame {@code [from, to)}
     * @param excludedSlotId id of a slot that is not taken into account
     */
    boolean existsOverlappingExcluding(Long calendarId, TimeWindow window, Long excludedSlotId);

    /**
     * Slots starting within the window, optionally restricted to one status ({@code null} for any), by start time.
     *
     * @param calendarId id of the calendar
     * @param window the time frame {@code [from, to)}
     * @param status only slots with this status, any if {@code null}
     * @param page which page to fetch
     */
    Page<Slot> findStartingIn(Long calendarId, TimeWindow window, SlotStatus status, PageQuery page);

    /**
     * Start, duration and status of the slots starting within the window, by start time; a cheap projection for
     * aggregations.
     *
     * @param calendarId id of the calendar
     * @param window the time frame {@code [from, to)}
     */
    List<SlotSpan> findSpansStartingIn(Long calendarId, TimeWindow window);
}
