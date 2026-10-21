package com.meetingscheduler.scheduling.domain;

import com.meetingscheduler.shared.domain.InvalidRequestException;

import java.util.Set;

/**
 * Configurable invariants of meetings.
 */
public record MeetingLimits(int maxParticipants) {

    public void requireValidParticipants(Set<Long> participantIds) {
        if (participantIds.contains(null)) {
            throw new InvalidRequestException("participantIds must not contain null");
        }
        if (participantIds.size() > maxParticipants) {
            throw new InvalidRequestException("A meeting can have at most " + maxParticipants + " participants");
        }
    }
}
