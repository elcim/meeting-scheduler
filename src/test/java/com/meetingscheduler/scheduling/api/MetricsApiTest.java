package com.meetingscheduler.scheduling.api;

import com.meetingscheduler.shared.api.ApiTestSupport;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureObservability
class MetricsApiTest extends ApiTestSupport {

    @Autowired
    private MeterRegistry registry;

    @Test
    void countsBookedAndCancelledMeetings() throws Exception {
        long organizer = createUser();
        long slot = createSlotId(organizer, "2031-01-01T09:00:00Z", 30);
        double booked = count("scheduler.meetings", "event", "booked");
        double cancelled = count("scheduler.meetings", "event", "cancelled");

        long meeting = idOf(bookMeeting(organizer, slot, "Sync"));
        mvc.perform(delete("/api/users/{u}/meetings/{m}", organizer, meeting)).andExpect(status().isNoContent());

        assertThat(count("scheduler.meetings", "event", "booked")).isEqualTo(booked + 1);
        assertThat(count("scheduler.meetings", "event", "cancelled")).isEqualTo(cancelled + 1);
    }

    @Test
    void countsCreatedSlotsSingleAndInBulk() throws Exception {
        long user = createUser();
        double before = count("scheduler.slots", "event", "created");

        createSlot(user, "2031-02-01T09:00:00Z", 30).andExpect(status().isCreated());
        mvc.perform(post("/api/users/{u}/slots/bulk", user).contentType(MediaType.APPLICATION_JSON).content("""
                        {"from": "2031-02-02T09:00:00Z", "to": "2031-02-02T11:00:00Z", "slotDurationMinutes": 30}"""))
                .andExpect(status().isCreated());

        assertThat(count("scheduler.slots", "event", "created")).isEqualTo(before + 1 + 4);
    }

    @Test
    void countsConflictsByOperation() throws Exception {
        long organizer = createUser();
        long slot = createSlotId(organizer, "2031-03-01T09:00:00Z", 30);
        double booking = count("scheduler.conflicts", "operation", "book_meeting");
        double creating = count("scheduler.conflicts", "operation", "create_slot");
        double deleting = count("scheduler.conflicts", "operation", "delete_slot");
        double updating = count("scheduler.conflicts", "operation", "update_slot");

        bookMeeting(organizer, slot, "First").andExpect(status().isCreated());
        bookMeeting(organizer, slot, "Second").andExpect(status().isConflict());
        createSlot(organizer, "2031-03-01T09:15:00Z", 30).andExpect(status().isConflict());
        deleteSlot(organizer, slot).andExpect(status().isConflict());
        patchSlot(organizer, slot, "{\"status\": \"FREE\"}").andExpect(status().isConflict());

        assertThat(count("scheduler.conflicts", "operation", "book_meeting")).isEqualTo(booking + 1);
        assertThat(count("scheduler.conflicts", "operation", "create_slot")).isEqualTo(creating + 1);
        assertThat(count("scheduler.conflicts", "operation", "delete_slot")).isEqualTo(deleting + 1);
        assertThat(count("scheduler.conflicts", "operation", "update_slot")).isEqualTo(updating + 1);
    }

    @Test
    void doesNotCountRefusedRequestsAsSuccesses() throws Exception {
        long organizer = createUser();
        long slot = createSlotId(organizer, "2031-04-01T09:00:00Z", 30);
        patchSlot(organizer, slot, "{\"status\": \"BUSY\"}").andExpect(status().isOk());
        double booked = count("scheduler.meetings", "event", "booked");

        bookMeeting(organizer, slot, "Blocked").andExpect(status().isConflict());

        assertThat(count("scheduler.meetings", "event", "booked")).isEqualTo(booked);
    }

    @Test
    void timesCalendarLockAndCountsRegisteredUsers() throws Exception {
        double users = count("scheduler.users.registered");
        long user = createUser();
        long lockWaits = registry.get("scheduler.calendar.lock.wait").timer().count();

        createSlot(user, "2031-05-01T09:00:00Z", 30).andExpect(status().isCreated());

        assertThat(count("scheduler.users.registered")).isEqualTo(users + 1);
        assertThat(registry.get("scheduler.calendar.lock.wait").timer().count()).isEqualTo(lockWaits + 1);
    }

    @Test
    void exposesMetricsForPrometheus() throws Exception {
        mvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("scheduler_meetings_total{")))
                .andExpect(content().string(containsString("scheduler_conflicts_total{")))
                .andExpect(content().string(containsString("scheduler_slots_total{")))
                .andExpect(content().string(containsString("application=\"meeting-scheduler\"")));
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    private double count(String name, String... tags) {
        return registry.get(name).tags(tags).counter().count();
    }
}
