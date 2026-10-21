package com.meetingscheduler.scheduling.api;

import com.meetingscheduler.shared.api.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SlotApiTest extends ApiTestSupport {

    @Test
    void createsSlotWithConfiguredDuration() throws Exception {
        long user = createUser();

        createSlot(user, "2030-01-01T09:00:00Z", 45)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startTime").value("2030-01-01T09:00:00Z"))
                .andExpect(jsonPath("$.endTime").value("2030-01-01T09:45:00Z"))
                .andExpect(jsonPath("$.durationMinutes").value(45))
                .andExpect(jsonPath("$.status").value("FREE"))
                .andExpect(jsonPath("$.booked").value(false));
    }

    @Test
    void rejectsInvalidDurationsAndBodies() throws Exception {
        long user = createUser();

        createSlot(user, "2030-01-01T09:00:00Z", 1).andExpect(status().isBadRequest());
        createSlot(user, "2030-01-01T09:00:00Z", 10_000).andExpect(status().isBadRequest());
        mvc.perform(post("/api/users/{u}/slots", user).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.startTime").exists());
    }

    @Test
    void rejectsOverlappingSlotsButAllowsAdjacentOnes() throws Exception {
        long user = createUser();
        createSlot(user, "2030-01-01T09:00:00Z", 60).andExpect(status().isCreated());

        createSlot(user, "2030-01-01T09:30:00Z", 60).andExpect(status().isConflict());
        createSlot(user, "2030-01-01T08:30:00Z", 60).andExpect(status().isConflict());
        createSlot(user, "2030-01-01T10:00:00Z", 60).andExpect(status().isCreated());
        createSlot(user, "2030-01-01T08:00:00Z", 60).andExpect(status().isCreated());
    }

    @Test
    void usersHaveIndependentCalendars() throws Exception {
        long first = createUser();
        long second = createUser();
        long slot = createSlotId(first, "2030-01-01T09:00:00Z", 60);

        createSlot(second, "2030-01-01T09:00:00Z", 60).andExpect(status().isCreated());
        getSlot(second, slot).andExpect(status().isNotFound());
        deleteSlot(second, slot).andExpect(status().isNotFound());
    }

    @Test
    void unknownUserIsNotFound() throws Exception {
        createSlot(999_999_999L, "2030-01-01T09:00:00Z", 30).andExpect(status().isNotFound());
    }

    @Test
    void marksSlotsBusyAndFree() throws Exception {
        long user = createUser();
        long slot = createSlotId(user, "2030-01-01T09:00:00Z", 30);

        patchSlot(user, slot, "{\"status\": \"BUSY\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BUSY"));
        patchSlot(user, slot, "{\"status\": \"FREE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FREE"));
        patchSlot(user, slot, "{}").andExpect(status().isBadRequest());
    }

    @Test
    void modifiesSlotTimeWithoutOverlappingOthers() throws Exception {
        long user = createUser();
        long slot = createSlotId(user, "2030-01-01T09:00:00Z", 30);
        createSlot(user, "2030-01-01T10:00:00Z", 30).andExpect(status().isCreated());

        patchSlot(user, slot, "{\"durationMinutes\": 60}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endTime").value("2030-01-01T10:00:00Z"));
        patchSlot(user, slot, "{\"durationMinutes\": 90}").andExpect(status().isConflict());
        patchSlot(user, slot, "{\"startTime\": \"2030-01-01T08:00:00Z\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startTime").value("2030-01-01T08:00:00Z"))
                .andExpect(jsonPath("$.endTime").value("2030-01-01T09:00:00Z"));
    }

    @Test
    void deletesSlot() throws Exception {
        long user = createUser();
        long slot = createSlotId(user, "2030-01-01T09:00:00Z", 30);

        deleteSlot(user, slot).andExpect(status().isNoContent());
        getSlot(user, slot).andExpect(status().isNotFound());
        createSlot(user, "2030-01-01T09:00:00Z", 30).andExpect(status().isCreated());
    }

    @Test
    void createsSlotsInBulk() throws Exception {
        long user = createUser();
        String body = """
                {"from": "2030-02-01T09:00:00Z", "to": "2030-02-01T12:00:00Z", "slotDurationMinutes": 30, "gapMinutes": 15}""";

        // 09:00-09:30, 09:45-10:15, ... : 4 full slots fit in 3 hours (4 * 45 - 15 = 165 minutes, a 5th would end at 12:45)
        mvc.perform(post("/api/users/{u}/slots/bulk", user).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[1].startTime").value("2030-02-01T09:45:00Z"))
                .andExpect(jsonPath("$[3].endTime").value("2030-02-01T11:45:00Z"));

        mvc.perform(post("/api/users/{u}/slots/bulk", user).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/users/{u}/slots/bulk", user).contentType(MediaType.APPLICATION_JSON).content("""
                        {"from": "2030-03-01T00:00:00Z", "to": "2030-06-01T00:00:00Z", "slotDurationMinutes": 5}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsSlotsOfTimeFrameFilteredByStatusWithPaging() throws Exception {
        long user = createUser();
        long busy = createSlotId(user, "2030-04-01T09:00:00Z", 30);
        createSlot(user, "2030-04-01T10:00:00Z", 30).andExpect(status().isCreated());
        createSlot(user, "2030-04-01T11:00:00Z", 30).andExpect(status().isCreated());
        createSlot(user, "2030-04-02T09:00:00Z", 30).andExpect(status().isCreated());
        patchSlot(user, busy, "{\"status\": \"BUSY\"}").andExpect(status().isOk());

        mvc.perform(get("/api/users/{u}/slots", user)
                        .param("from", "2030-04-01T00:00:00Z").param("to", "2030-04-02T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].startTime").value("2030-04-01T09:00:00Z"));
        mvc.perform(get("/api/users/{u}/slots", user)
                        .param("from", "2030-04-01T00:00:00Z").param("to", "2030-04-03T00:00:00Z")
                        .param("status", "FREE"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[?(@.status == 'BUSY')]", hasSize(0)));
        mvc.perform(get("/api/users/{u}/slots", user)
                        .param("from", "2030-04-01T00:00:00Z").param("to", "2030-04-03T00:00:00Z")
                        .param("status", "BUSY"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/users/{u}/slots", user)
                        .param("from", "2030-04-01T00:00:00Z").param("to", "2030-04-03T00:00:00Z")
                        .param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/api/users/{u}/slots", user)
                        .param("from", "2030-04-03T00:00:00Z").param("to", "2030-04-01T00:00:00Z"))
                .andExpect(status().isBadRequest());
    }
}
