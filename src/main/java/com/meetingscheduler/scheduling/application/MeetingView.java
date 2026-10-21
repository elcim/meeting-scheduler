package com.meetingscheduler.scheduling.application;

import com.meetingscheduler.scheduling.domain.Meeting;
import com.meetingscheduler.scheduling.domain.Slot;

import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;

/**
 * Read model of a meeting together with the facts of its slot; the organizer is the owner of the slot's calendar.
 * It holds plain values only, so it can be used after the transaction ended.
 */
public record MeetingView(
        Long id,
        Long slotId,
        Long organizerId,
        String title,
        String description,
        Instant startTime,
        Instant endTime,
        Set<Long> participantIds,
        Instant createdAt) {

    public static MeetingView of(Meeting meeting, Slot slot, Long organizerId) {
        return new MeetingView(meeting.getId(), slot.getId(), organizerId, meeting.getTitle(), meeting.getDescription(),
                slot.getStartTime(), slot.getEndTime(), new TreeSet<>(meeting.getParticipantIds()), meeting.getCreatedAt());
    }
}
