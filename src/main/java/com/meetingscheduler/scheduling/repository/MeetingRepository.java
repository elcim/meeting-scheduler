package com.meetingscheduler.scheduling.repository;

import com.meetingscheduler.scheduling.domain.Meeting;

import java.util.Optional;

/**
 * Port for storing and finding {@link Meeting meetings}.
 */
public interface MeetingRepository {

    /**
     * Stores the meeting.
     *
     * @param meeting the meeting to store
     */
    Meeting save(Meeting meeting);

    /**
     * Removes the meeting.
     *
     * @param meeting the meeting to remove
     */
    void delete(Meeting meeting);

    /**
     * The meeting held in a slot of the given calendar.
     *
     * @param meetingId id of the meeting
     * @param calendarId id of the calendar
     */
    Optional<Meeting> findInCalendar(Long meetingId, Long calendarId);
}
