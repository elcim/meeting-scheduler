package com.meetingscheduler.scheduling.application;

import java.util.Set;

/**
 * What a meeting is about and who takes part in it, as supplied when booking or updating a meeting.
 */
public record MeetingDetails(String title, String description, Set<Long> participantIds) {
}
