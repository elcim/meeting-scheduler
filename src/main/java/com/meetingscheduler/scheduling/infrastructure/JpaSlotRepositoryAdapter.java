package com.meetingscheduler.scheduling.infrastructure;

import com.meetingscheduler.scheduling.domain.Slot;
import com.meetingscheduler.scheduling.domain.SlotRepository;
import com.meetingscheduler.scheduling.domain.SlotSpan;
import com.meetingscheduler.scheduling.domain.SlotStatus;
import com.meetingscheduler.shared.domain.Page;
import com.meetingscheduler.shared.domain.PageQuery;
import com.meetingscheduler.shared.domain.TimeWindow;
import com.meetingscheduler.shared.infrastructure.SpringPages;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class JpaSlotRepositoryAdapter implements SlotRepository {

    /**
     * Id that matches no slot, used when an overlap check should not exclude anything.
     */
    private static final long NO_SLOT = -1L;

    private final SpringDataSlotRepository jpa;

    JpaSlotRepositoryAdapter(SpringDataSlotRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Slot save(Slot slot) {
        return jpa.save(slot);
    }

    @Override
    public List<Slot> saveAll(List<Slot> slots) {
        return jpa.saveAll(slots);
    }

    @Override
    public void delete(Slot slot) {
        jpa.delete(slot);
    }

    @Override
    public Optional<Slot> findInCalendar(Long slotId, Long calendarId) {
        return jpa.findInCalendar(slotId, calendarId);
    }

    @Override
    public Optional<Slot> findInCalendarForUpdate(Long slotId, Long calendarId) {
        return jpa.findInCalendarForUpdate(slotId, calendarId);
    }

    @Override
    public boolean existsOverlapping(Long calendarId, TimeWindow window) {
        return existsOverlappingExcluding(calendarId, window, NO_SLOT);
    }

    @Override
    public boolean existsOverlappingExcluding(Long calendarId, TimeWindow window, Long excludedSlotId) {
        return jpa.existsOverlapping(calendarId, window.from(), window.to(), excludedSlotId);
    }

    @Override
    public Page<Slot> findStartingIn(Long calendarId, TimeWindow window, SlotStatus status, PageQuery page) {
        var pageable = SpringPages.toPageable(page);
        var slots = status == null
                ? jpa.findStartingIn(calendarId, window.from(), window.to(), pageable)
                : jpa.findStartingInWithStatus(calendarId, status, window.from(), window.to(), pageable);
        return SpringPages.toPage(slots, slot -> slot);
    }

    @Override
    public List<SlotSpan> findSpansStartingIn(Long calendarId, TimeWindow window) {
        return jpa.findSpansStartingIn(calendarId, window.from(), window.to());
    }
}
