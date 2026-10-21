package com.meetingscheduler.scheduling.api;

import com.meetingscheduler.shared.api.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MeetingApiTest extends ApiTestSupport {

    @Test
    void convertsFreeSlotIntoMeeting() throws Exception {
        long organizer = createUser();
        long guest = createUser();
        long slot = createSlotId(organizer, "2030-05-01T09:00:00Z", 30);

        bookMeeting(organizer, slot, "Planning", guest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Planning"))
                .andExpect(jsonPath("$.description").value("details"))
                .andExpect(jsonPath("$.slotId").value(slot))
                .andExpect(jsonPath("$.organizerId").value(organizer))
                .andExpect(jsonPath("$.participantIds", contains((int) guest)))
                .andExpect(jsonPath("$.startTime").value("2030-05-01T09:00:00Z"))
                .andExpect(jsonPath("$.endTime").value("2030-05-01T09:30:00Z"));

        getSlot(organizer, slot)
                .andExpect(jsonPath("$.status").value("BUSY"))
                .andExpect(jsonPath("$.booked").value(true));
    }

    @Test
    void cannotBookTakenOrBusySlotsOrUnknownParticipants() throws Exception {
        long organizer = createUser();
        long slot = createSlotId(organizer, "2030-05-01T09:00:00Z", 30);
        long busySlot = createSlotId(organizer, "2030-05-01T10:00:00Z", 30);
        patchSlot(organizer, busySlot, "{\"status\": \"BUSY\"}").andExpect(status().isOk());

        bookMeeting(organizer, slot, "Unknown guest", 999_999_999L).andExpect(status().isBadRequest());
        bookMeeting(organizer, slot, "First").andExpect(status().isCreated());
        bookMeeting(organizer, slot, "Second").andExpect(status().isConflict());
        bookMeeting(organizer, busySlot, "Busy").andExpect(status().isConflict());
        bookMeeting(createUser(), slot, "Not mine").andExpect(status().isNotFound());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/users/{u}/slots/{s}/meeting", organizer, slot)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bookedSlotIsProtectedUntilMeetingIsCancelled() throws Exception {
        long organizer = createUser();
        long slot = createSlotId(organizer, "2030-05-01T09:00:00Z", 30);
        long meeting = idOf(bookMeeting(organizer, slot, "Sync"));

        deleteSlot(organizer, slot).andExpect(status().isConflict());
        patchSlot(organizer, slot, "{\"status\": \"FREE\"}").andExpect(status().isConflict());
        patchSlot(organizer, slot, "{\"durationMinutes\": 60}").andExpect(status().isConflict());

        mvc.perform(delete("/api/users/{u}/meetings/{m}", organizer, meeting)).andExpect(status().isNoContent());
        mvc.perform(get("/api/users/{u}/meetings/{m}", organizer, meeting)).andExpect(status().isNotFound());
        getSlot(organizer, slot)
                .andExpect(jsonPath("$.status").value("FREE"))
                .andExpect(jsonPath("$.booked").value(false));
        bookMeeting(organizer, slot, "Again").andExpect(status().isCreated());
    }

    @Test
    void updatesMeetingDetailsAndParticipants() throws Exception {
        long organizer = createUser();
        long first = createUser();
        long second = createUser();
        long slot = createSlotId(organizer, "2030-05-01T09:00:00Z", 30);
        long meeting = idOf(bookMeeting(organizer, slot, "Sync", first));

        updateMeeting(organizer, meeting, """
                {"title": "Sync v2", "description": "new agenda", "participantIds": [%d]}""".formatted(second))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Sync v2"))
                .andExpect(jsonPath("$.description").value("new agenda"))
                .andExpect(jsonPath("$.participantIds", contains((int) second)));
        updateMeeting(createUser(), meeting, """
                {"title": "Hijack", "participantIds": []}""").andExpect(status().isNotFound());
    }

    @Test
    void listsOrganizedAndParticipatingMeetings() throws Exception {
        long organizer = createUser();
        long guest = createUser();
        bookMeeting(organizer, createSlotId(organizer, "2030-06-01T09:00:00Z", 30), "Morning", guest)
                .andExpect(status().isCreated());
        bookMeeting(organizer, createSlotId(organizer, "2030-06-01T15:00:00Z", 30), "Afternoon")
                .andExpect(status().isCreated());
        bookMeeting(organizer, createSlotId(organizer, "2030-07-01T09:00:00Z", 30), "Next month", guest)
                .andExpect(status().isCreated());

        mvc.perform(get("/api/users/{u}/meetings", organizer)
                        .param("from", "2030-06-01T00:00:00Z").param("to", "2030-06-02T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].title", contains("Morning", "Afternoon")));
        mvc.perform(get("/api/users/{u}/meetings/participating", guest)
                        .param("from", "2030-06-01T00:00:00Z").param("to", "2030-08-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].title", contains("Morning", "Next month")));
        mvc.perform(get("/api/users/{u}/meetings", guest)
                        .param("from", "2030-06-01T00:00:00Z").param("to", "2030-08-01T00:00:00Z"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
