package com.meetingscheduler.scheduling.application;

import com.meetingscheduler.scheduling.domain.Calendar;
import com.meetingscheduler.scheduling.repository.CalendarRepository;
import com.meetingscheduler.shared.domain.ResourceNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Resolves the calendar of a user, so the use cases can speak in terms of users only.
 */
@Component
class CalendarLookup {

    private final CalendarRepository calendarRepository;
    private final SchedulerMetrics metrics;

    CalendarLookup(CalendarRepository calendarRepository, SchedulerMetrics metrics) {
        this.calendarRepository = calendarRepository;
        this.metrics = metrics;
    }

    /**
     * Id of the user's calendar; fails with {@link ResourceNotFoundException} for an unknown user.
     *
     * @param userId id of the user owning the calendar
     */
    Long calendarIdOf(Long userId) {
        return calendarRepository.findIdByOwner(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    /**
     * Must be called inside a transaction; the lock is held until it ends.
     *
     * @param userId id of the user owning the calendar
     */
    Calendar lockCalendarOf(Long userId) {
        return metrics.timeCalendarLock(() -> calendarRepository.findByOwnerForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId)));
    }
}
