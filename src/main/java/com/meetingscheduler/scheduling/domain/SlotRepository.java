package com.meetingscheduler.scheduling.domain;

import com.meetingscheduler.shared.domain.Page;
import com.meetingscheduler.shared.domain.PageQuery;
import com.meetingscheduler.shared.domain.TimeWindow;

import java.util.List;
import java.util.Optional;

public interface SlotRepository {

    Slot save(Slot slot);

    List<Slot> saveAll(List<Slot> slots);

    void delete(Slot slot);

    Optional<Slot> findInCalendar(Long slotId, Long calendarId);

    /**
     * Same as {@link #findInCalendar} but locks the slot row until the end of the transaction.
     */
    Optional<Slot> findInCalendarForUpdate(Long slotId, Long calendarId);

    /**
     * Whether any slot of the calendar intersects the window.
     */
    boolean existsOverlapping(Long calendarId, TimeWindow window);

    /**
     * Whether any slot of the calendar other than {@code excludedSlotId} intersects the window.
     */
    boolean existsOverlappingExcluding(Long calendarId, TimeWindow window, Long excludedSlotId);

    /**
     * Slots starting within the window, optionally restricted to one status ({@code null} for any), by start time.
     */
    Page<Slot> findStartingIn(Long calendarId, TimeWindow window, SlotStatus status, PageQuery page);

    List<SlotSpan> findSpansStartingIn(Long calendarId, TimeWindow window);
}
