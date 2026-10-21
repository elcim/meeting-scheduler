package com.meetingscheduler.scheduling.application;

import java.util.Set;

public record MeetingDetails(String title, String description, Set<Long> participantIds) {
}
