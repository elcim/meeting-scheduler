package com.meetingscheduler.scheduling.application;

import com.meetingscheduler.scheduling.domain.Calendar;
import com.meetingscheduler.scheduling.domain.CalendarRepository;
import com.meetingscheduler.shared.domain.ResourceNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Resolves the calendar of a user, so the use cases can speak in terms of users only.
 */
@Component
class CalendarLookup {

    private final CalendarRepository calendarRepository;

    CalendarLookup(CalendarRepository calendarRepository) {
        this.calendarRepository = calendarRepository;
    }

    Long calendarIdOf(Long userId) {
        return calendarRepository.findIdByOwner(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    /**
     * Must be called inside a transaction; the lock is held until it ends.
     */
    Calendar lockCalendarOf(Long userId) {
        return calendarRepository.findByOwnerForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
