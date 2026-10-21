package com.meetingscheduler.scheduling.application;

import com.meetingscheduler.shared.domain.Page;
import com.meetingscheduler.shared.domain.PageQuery;
import com.meetingscheduler.shared.domain.TimeWindow;

import java.util.Optional;

/**
 * Read side for meetings. Meetings and slots are separate aggregates, so views that combine both are served here
 * instead of being assembled from the aggregates one by one.
 */
public interface MeetingQueries {

    /**
     * The meeting, if it is held in a slot of the calendar.
     *
     * @param meetingId id of the meeting
     * @param calendarId id of the calendar
     */
    Optional<MeetingView> findViewInCalendar(Long meetingId, Long calendarId);

    /**
     * Meetings held in slots of the calendar that start within the window.
     *
     * @param calendarId id of the calendar
     * @param window the time frame {@code [from, to)}
     * @param page which page to fetch
     */
    Page<MeetingView> findOrganizedBy(Long calendarId, TimeWindow window, PageQuery page);

    /**
     * Meetings the user was invited to that start within the window.
     *
     * @param userId id of the invited user
     * @param window the time frame {@code [from, to)}
     * @param page which page to fetch
     */
    Page<MeetingView> findParticipatedBy(Long userId, TimeWindow window, PageQuery page);
}
