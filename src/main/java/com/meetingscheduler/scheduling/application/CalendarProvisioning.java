package com.meetingscheduler.scheduling.application;

import com.meetingscheduler.scheduling.domain.Calendar;
import com.meetingscheduler.scheduling.domain.CalendarRepository;
import com.meetingscheduler.user.domain.UserRegistered;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Every user gets exactly one calendar. It is created synchronously in the transaction that registers the user.
 */
@Component
class CalendarProvisioning {

    private final CalendarRepository calendarRepository;

    CalendarProvisioning(CalendarRepository calendarRepository) {
        this.calendarRepository = calendarRepository;
    }

    @EventListener
    void on(UserRegistered event) {
        calendarRepository.save(new Calendar(event.userId()));
    }
}
