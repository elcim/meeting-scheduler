package com.meetingscheduler.shared.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:meeting-scheduler-test;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
public abstract class ApiTestSupport {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected long createUser() throws Exception {
        String body = """
                {"name": "Test User", "email": "%s@example.com"}""".formatted(UUID.randomUUID());
        return idOf(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions createSlot(long userId, String start, int minutes) throws Exception {
        return mvc.perform(post("/api/users/{u}/slots", userId).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"startTime": "%s", "durationMinutes": %d}""".formatted(start, minutes)));
    }

    protected long createSlotId(long userId, String start, int minutes) throws Exception {
        return idOf(createSlot(userId, start, minutes));
    }

    protected ResultActions patchSlot(long userId, long slotId, String json) throws Exception {
        return mvc.perform(patch("/api/users/{u}/slots/{s}", userId, slotId)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions deleteSlot(long userId, long slotId) throws Exception {
        return mvc.perform(delete("/api/users/{u}/slots/{s}", userId, slotId));
    }

    protected ResultActions getSlot(long userId, long slotId) throws Exception {
        return mvc.perform(get("/api/users/{u}/slots/{s}", userId, slotId));
    }

    protected ResultActions bookMeeting(long userId, long slotId, String title, long... participants) throws Exception {
        StringBuilder ids = new StringBuilder();
        for (long p : participants) {
            ids.append(ids.isEmpty() ? "" : ",").append(p);
        }
        return mvc.perform(post("/api/users/{u}/slots/{s}/meeting", userId, slotId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "%s", "description": "details", "participantIds": [%s]}""".formatted(title, ids)));
    }

    protected ResultActions updateMeeting(long userId, long meetingId, String json) throws Exception {
        return mvc.perform(put("/api/users/{u}/meetings/{m}", userId, meetingId)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long idOf(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) throws Exception {
        return idOf(mvc.perform(request));
    }

    protected long idOf(ResultActions result) throws Exception {
        String content = result.andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(content);
        return json.get("id").asLong();
    }
}
