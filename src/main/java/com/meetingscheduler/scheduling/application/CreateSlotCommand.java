package com.meetingscheduler.scheduling.application;

import java.time.Instant;

public record CreateSlotCommand(Instant startTime, int durationMinutes) {
}
