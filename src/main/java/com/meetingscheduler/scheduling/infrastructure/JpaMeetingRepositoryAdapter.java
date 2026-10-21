package com.meetingscheduler.scheduling.infrastructure;

import com.meetingscheduler.scheduling.application.MeetingQueries;
import com.meetingscheduler.scheduling.application.MeetingView;
import com.meetingscheduler.scheduling.domain.Meeting;
import com.meetingscheduler.scheduling.domain.MeetingRepository;
import com.meetingscheduler.shared.domain.Page;
import com.meetingscheduler.shared.domain.PageQuery;
import com.meetingscheduler.shared.domain.TimeWindow;
import com.meetingscheduler.shared.infrastructure.SpringPages;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Persistence of the meeting aggregate (write side) and its joined read model.
 */
@Repository
class JpaMeetingRepositoryAdapter implements MeetingRepository, MeetingQueries {

    private final SpringDataMeetingRepository jpa;

    JpaMeetingRepositoryAdapter(SpringDataMeetingRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Meeting save(Meeting meeting) {
        return jpa.save(meeting);
    }

    @Override
    public void delete(Meeting meeting) {
        jpa.delete(meeting);
    }

    @Override
    public Optional<Meeting> findInCalendar(Long meetingId, Long calendarId) {
        return jpa.findInCalendar(meetingId, calendarId);
    }

    @Override
    public Optional<MeetingView> findViewInCalendar(Long meetingId, Long calendarId) {
        return jpa.findRowInCalendar(meetingId, calendarId).map(MeetingRow::toView);
    }

    @Override
    public Page<MeetingView> findOrganizedBy(Long calendarId, TimeWindow window, PageQuery page) {
        return SpringPages.toPage(
                jpa.findOrganizedBy(calendarId, window.from(), window.to(), SpringPages.toPageable(page)),
                MeetingRow::toView);
    }

    @Override
    public Page<MeetingView> findParticipatedBy(Long userId, TimeWindow window, PageQuery page) {
        return SpringPages.toPage(
                jpa.findParticipatedBy(userId, window.from(), window.to(), SpringPages.toPageable(page)),
                MeetingRow::toView);
    }
}
