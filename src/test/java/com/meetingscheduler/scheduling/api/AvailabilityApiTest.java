package com.meetingscheduler.scheduling.api;

import com.meetingscheduler.shared.api.ApiTestSupport;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AvailabilityApiTest extends ApiTestSupport {

    @Test
    void aggregatesFreeAndBusyTimePerDay() throws Exception {
        long user = createUser();
        long booked = createSlotId(user, "2030-08-01T09:00:00Z", 30);
        long blocked = createSlotId(user, "2030-08-01T10:00:00Z", 60);
        createSlot(user, "2030-08-01T12:00:00Z", 45).andExpect(status().isCreated());
        createSlot(user, "2030-08-02T09:00:00Z", 15).andExpect(status().isCreated());
        createSlot(user, "2030-09-15T09:00:00Z", 15).andExpect(status().isCreated());
        bookMeeting(user, booked, "Sync").andExpect(status().isCreated());
        patchSlot(user, blocked, "{\"status\": \"BUSY\"}").andExpect(status().isOk());

        mvc.perform(get("/api/users/{u}/availability", user)
                        .param("from", "2030-08-01T00:00:00Z").param("to", "2030-08-03T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zone").value("UTC"))
                .andExpect(jsonPath("$.totals.totalSlots").value(4))
                .andExpect(jsonPath("$.totals.freeSlots").value(2))
                .andExpect(jsonPath("$.totals.busySlots").value(2))
                .andExpect(jsonPath("$.totals.freeMinutes").value(60))
                .andExpect(jsonPath("$.totals.busyMinutes").value(90))
                .andExpect(jsonPath("$.days", hasSize(2)))
                .andExpect(jsonPath("$.days[0].date").value("2030-08-01"))
                .andExpect(jsonPath("$.days[0].freeMinutes").value(45))
                .andExpect(jsonPath("$.days[0].busyMinutes").value(90))
                .andExpect(jsonPath("$.days[1].date").value("2030-08-02"))
                .andExpect(jsonPath("$.days[1].freeSlots").value(1));
    }

    @Test
    void bucketsDaysInRequestedZone() throws Exception {
        long user = createUser();
        createSlot(user, "2030-08-01T23:00:00Z", 30).andExpect(status().isCreated());

        mvc.perform(get("/api/users/{u}/availability", user)
                        .param("from", "2030-08-01T00:00:00Z").param("to", "2030-08-03T00:00:00Z")
                        .param("zone", "Europe/Istanbul"))
                .andExpect(jsonPath("$.zone").value("Europe/Istanbul"))
                .andExpect(jsonPath("$.days[0].date").value("2030-08-02"));
    }

    @Test
    void rejectsInvalidWindowsAndZones() throws Exception {
        long user = createUser();

        mvc.perform(get("/api/users/{u}/availability", user)
                        .param("from", "2030-01-01T00:00:00Z").param("to", "2031-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/users/{u}/availability", user)
                        .param("from", "2030-08-02T00:00:00Z").param("to", "2030-08-01T00:00:00Z"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/users/{u}/availability", user)
                        .param("from", "2030-08-01T00:00:00Z").param("to", "2030-08-02T00:00:00Z")
                        .param("zone", "Mars/Olympus"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/users/{u}/availability", 999_999_999L)
                        .param("from", "2030-08-01T00:00:00Z").param("to", "2030-08-02T00:00:00Z"))
                .andExpect(status().isNotFound());
    }
}
