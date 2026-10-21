package com.meetingscheduler.scheduling.domain;

import java.util.Optional;

public interface MeetingRepository {

    Meeting save(Meeting meeting);

    void delete(Meeting meeting);

    /**
     * The meeting held in a slot of the given calendar.
     */
    Optional<Meeting> findInCalendar(Long meetingId, Long calendarId);
}
