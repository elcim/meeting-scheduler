package com.meetingscheduler.scheduling.repository.jpa;

import com.meetingscheduler.scheduling.application.MeetingView;
import com.meetingscheduler.scheduling.domain.Meeting;
import com.meetingscheduler.scheduling.domain.Slot;

/**
 * Query result joining a meeting with its slot and the owner of the slot's calendar.
 */
record MeetingRow(Meeting meeting, Slot slot, Long organizerId) {

    MeetingView toView() {
        return MeetingView.of(meeting, slot, organizerId);
    }
}
