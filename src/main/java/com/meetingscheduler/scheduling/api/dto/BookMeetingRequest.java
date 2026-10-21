package com.meetingscheduler.scheduling.api.dto;

import com.meetingscheduler.scheduling.application.MeetingDetails;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record BookMeetingRequest(
        @NotNull Long slotId,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description,
        @NotNull Set<Long> participantIds) {

    public MeetingDetails toDetails() {
        return new MeetingDetails(title, description, participantIds);
    }
}
