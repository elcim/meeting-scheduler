package com.meetingscheduler.scheduling.api;

import com.meetingscheduler.scheduling.api.dto.BookMeetingRequest;
import com.meetingscheduler.scheduling.api.dto.UpdateMeetingRequest;
import com.meetingscheduler.scheduling.api.dto.MeetingResponse;
import com.meetingscheduler.scheduling.application.MeetingService;
import com.meetingscheduler.shared.api.PageResponse;
import com.meetingscheduler.shared.domain.PageQuery;
import com.meetingscheduler.shared.domain.TimeWindow;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;

/**
 * REST endpoints for booking, viewing, updating and cancelling the meetings of a user.
 */
@RestController
@RequestMapping("/api/users/{userId}/meetings")
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    /**
     * Converts a free slot of the user into a meeting.
     *
     * @param userId id of the user owning the slot, who becomes the organizer
     * @param request the request body
     */
    @PostMapping
    public ResponseEntity<MeetingResponse> book(@PathVariable Long userId, @Valid @RequestBody BookMeetingRequest request) {
        MeetingResponse meeting = MeetingResponse.from(meetingService.book(userId, request.slotId(), request.toDetails()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(meeting.id()).toUri();
        return ResponseEntity.created(location).body(meeting);
    }

    /**
     * Meetings held in the user's own slots.
     *
     * @param userId id of the organizing user, the owner of the slot
     * @param from start of the time frame, inclusive
     * @param to end of the time frame, exclusive
     * @param page zero-based page index
     * @param size page size, at most 200
     */
    @GetMapping
    public PageResponse<MeetingResponse> listOrganized(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return PageResponse.of(
                meetingService.listOrganized(userId, new TimeWindow(from, to), new PageQuery(page, size)),
                MeetingResponse::from);
    }

    /**
     * Meetings the user was invited to.
     *
     * @param userId id of the invited user
     * @param from start of the time frame, inclusive
     * @param to end of the time frame, exclusive
     * @param page zero-based page index
     * @param size page size, at most 200
     */
    @GetMapping("/participating")
    public PageResponse<MeetingResponse> listParticipating(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return PageResponse.of(
                meetingService.listParticipating(userId, new TimeWindow(from, to), new PageQuery(page, size)),
                MeetingResponse::from);
    }

    /**
     * Returns one of the user's meetings.
     *
     * @param userId id of the organizing user, the owner of the slot
     * @param meetingId id of the meeting
     */
    @GetMapping("/{meetingId}")
    public MeetingResponse get(@PathVariable Long userId, @PathVariable Long meetingId) {
        return MeetingResponse.from(meetingService.get(userId, meetingId));
    }

    /**
     * Replaces the title, description and participants of a meeting.
     *
     * @param userId id of the organizing user, the owner of the slot
     * @param meetingId id of the meeting
     * @param request the request body
     */
    @PutMapping("/{meetingId}")
    public MeetingResponse update(@PathVariable Long userId, @PathVariable Long meetingId,
                                  @Valid @RequestBody UpdateMeetingRequest request) {
        return MeetingResponse.from(meetingService.update(userId, meetingId, request.toDetails()));
    }

    /**
     * Cancels the meeting; its slot becomes free again.
     *
     * @param userId id of the organizing user, the owner of the slot
     * @param meetingId id of the meeting
     */
    @DeleteMapping("/{meetingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long userId, @PathVariable Long meetingId) {
        meetingService.cancel(userId, meetingId);
    }
}
