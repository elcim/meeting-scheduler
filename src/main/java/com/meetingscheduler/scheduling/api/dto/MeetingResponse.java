package com.meetingscheduler.scheduling.api.dto;

import com.meetingscheduler.scheduling.application.MeetingView;

import java.time.Instant;
import java.util.Set;

/**
 * Representation of a meeting, together with the time of the slot that holds it.
 */
public record MeetingResponse(
        Long id,
        Long slotId,
        Long organizerId,
        String title,
        String description,
        Instant startTime,
        Instant endTime,
        Set<Long> participantIds,
        Instant createdAt) {

    public static MeetingResponse from(MeetingView meeting) {
        return new MeetingResponse(meeting.id(), meeting.slotId(), meeting.organizerId(), meeting.title(),
                meeting.description(), meeting.startTime(), meeting.endTime(), meeting.participantIds(),
                meeting.createdAt());
    }
}
