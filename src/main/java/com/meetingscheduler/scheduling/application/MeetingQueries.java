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

    Optional<MeetingView> findViewInCalendar(Long meetingId, Long calendarId);

    /**
     * Meetings held in slots of the calendar that start within the window.
     */
    Page<MeetingView> findOrganizedBy(Long calendarId, TimeWindow window, PageQuery page);

    /**
     * Meetings the user was invited to that start within the window.
     */
    Page<MeetingView> findParticipatedBy(Long userId, TimeWindow window, PageQuery page);
}
