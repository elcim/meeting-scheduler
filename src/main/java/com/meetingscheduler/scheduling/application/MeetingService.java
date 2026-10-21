package com.meetingscheduler.scheduling.application;

import com.meetingscheduler.scheduling.domain.Meeting;
import com.meetingscheduler.scheduling.domain.MeetingLimits;
import com.meetingscheduler.scheduling.domain.MeetingRepository;
import com.meetingscheduler.scheduling.domain.Slot;
import com.meetingscheduler.scheduling.domain.SlotRepository;
import com.meetingscheduler.shared.domain.InvalidRequestException;
import com.meetingscheduler.shared.domain.Page;
import com.meetingscheduler.shared.domain.PageQuery;
import com.meetingscheduler.shared.domain.ResourceNotFoundException;
import com.meetingscheduler.shared.domain.TimeWindow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final SlotRepository slotRepository;
    private final MeetingQueries meetingQueries;
    private final UserDirectory userDirectory;
    private final CalendarLookup calendarLookup;
    private final MeetingLimits limits;

    MeetingService(MeetingRepository meetingRepository, SlotRepository slotRepository, MeetingQueries meetingQueries,
                   UserDirectory userDirectory, CalendarLookup calendarLookup, MeetingLimits limits) {
        this.meetingRepository = meetingRepository;
        this.slotRepository = slotRepository;
        this.meetingQueries = meetingQueries;
        this.userDirectory = userDirectory;
        this.calendarLookup = calendarLookup;
        this.limits = limits;
    }

    /**
     * Converts a free slot of the user into a meeting, which makes the slot busy.
     */
    @Transactional
    public MeetingView book(Long userId, Long slotId, MeetingDetails details) {
        requireValidParticipants(details);
        Long calendarId = calendarLookup.calendarIdOf(userId);

        // The row lock makes concurrent attempts to book the same slot queue up, and only the first one wins.
        Slot slot = slotRepository.findInCalendarForUpdate(slotId, calendarId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot", slotId));

        Meeting meeting = meetingRepository.save(
                slot.bookMeeting(details.title(), details.description(), details.participantIds()));
        return MeetingView.of(meeting, slot, userId);
    }

    public MeetingView get(Long userId, Long meetingId) {
        Long calendarId = calendarLookup.calendarIdOf(userId);
        return meetingQueries.findViewInCalendar(meetingId, calendarId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting", meetingId));
    }

    @Transactional
    public MeetingView update(Long userId, Long meetingId, MeetingDetails details) {
        requireValidParticipants(details);
        Long calendarId = calendarLookup.calendarIdOf(userId);
        Meeting meeting = findOwned(meetingId, calendarId);
        meeting.update(details.title(), details.description(), details.participantIds());
        Slot slot = slotRepository.findInCalendar(meeting.getSlotId(), calendarId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot", meeting.getSlotId()));
        return MeetingView.of(meeting, slot, userId);
    }

    /**
     * Cancels the meeting and frees its slot again.
     */
    @Transactional
    public void cancel(Long userId, Long meetingId) {
        Long calendarId = calendarLookup.calendarIdOf(userId);
        Meeting meeting = findOwned(meetingId, calendarId);
        Slot slot = slotRepository.findInCalendarForUpdate(meeting.getSlotId(), calendarId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot", meeting.getSlotId()));
        meetingRepository.delete(meeting);
        slot.releaseMeeting();
    }

    /**
     * Meetings organized by the user (held in the user's slots) that start within the window.
     */
    public Page<MeetingView> listOrganized(Long userId, TimeWindow window, PageQuery page) {
        Long calendarId = calendarLookup.calendarIdOf(userId);
        return meetingQueries.findOrganizedBy(calendarId, window, page);
    }

    /**
     * Meetings the user was invited to that start within the window.
     */
    public Page<MeetingView> listParticipating(Long userId, TimeWindow window, PageQuery page) {
        if (!userDirectory.exists(userId)) {
            throw new ResourceNotFoundException("User", userId);
        }
        return meetingQueries.findParticipatedBy(userId, window, page);
    }

    private Meeting findOwned(Long meetingId, Long calendarId) {
        return meetingRepository.findInCalendar(meetingId, calendarId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting", meetingId));
    }

    private void requireValidParticipants(MeetingDetails details) {
        limits.requireValidParticipants(details.participantIds());
        if (!userDirectory.allExist(details.participantIds())) {
            throw new InvalidRequestException("participantIds contains unknown users");
        }
    }
}
