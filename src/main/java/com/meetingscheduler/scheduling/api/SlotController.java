package com.meetingscheduler.scheduling.api;

import com.meetingscheduler.scheduling.api.dto.BulkCreateSlotsRequest;
import com.meetingscheduler.scheduling.api.dto.CreateSlotRequest;
import com.meetingscheduler.scheduling.api.dto.SlotResponse;
import com.meetingscheduler.scheduling.api.dto.UpdateSlotRequest;
import com.meetingscheduler.scheduling.application.SlotService;
import com.meetingscheduler.scheduling.domain.SlotStatus;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.List;

/**
 * REST endpoints for creating, listing, changing and deleting the slots of a user's calendar.
 */
@RestController
@RequestMapping("/api/users/{userId}/slots")
public class SlotController {

    private final SlotService slotService;

    public SlotController(SlotService slotService) {
        this.slotService = slotService;
    }

    /**
     * Creates a single slot.
     *
     * @param userId id of the user owning the slot
     * @param request the request body
     */
    @PostMapping
    public ResponseEntity<SlotResponse> create(@PathVariable Long userId, @Valid @RequestBody CreateSlotRequest request) {
        SlotResponse slot = SlotResponse.from(slotService.create(userId, request.toCommand()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(slot.id()).toUri();
        return ResponseEntity.created(location).body(slot);
    }

    /**
     * Splits a time frame into consecutive slots.
     *
     * @param userId id of the user owning the slot
     * @param request the request body
     */
    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<SlotResponse> createBulk(@PathVariable Long userId, @Valid @RequestBody BulkCreateSlotsRequest request) {
        return slotService.createBulk(userId, request.toCommand()).stream().map(SlotResponse::from).toList();
    }

    /**
     * Lists the slots starting within the time frame, optionally only the free or the busy ones.
     *
     * @param userId id of the user owning the slot
     * @param from start of the time frame, inclusive
     * @param to end of the time frame, exclusive
     * @param status only slots with this status, any if omitted
     * @param page zero-based page index
     * @param size page size, at most 200
     */
    @GetMapping
    public PageResponse<SlotResponse> list(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) SlotStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return PageResponse.of(
                slotService.list(userId, new TimeWindow(from, to), status, new PageQuery(page, size)),
                SlotResponse::from);
    }

    /**
     * Returns one slot of the user.
     *
     * @param userId id of the user owning the slot
     * @param slotId id of the slot
     */
    @GetMapping("/{slotId}")
    public SlotResponse get(@PathVariable Long userId, @PathVariable Long slotId) {
        return SlotResponse.from(slotService.get(userId, slotId));
    }

    /**
     * Moves, resizes and/or changes the status of a slot.
     *
     * @param userId id of the user owning the slot
     * @param slotId id of the slot
     * @param request the request body
     */
    @PatchMapping("/{slotId}")
    public SlotResponse update(@PathVariable Long userId, @PathVariable Long slotId,
                               @Valid @RequestBody UpdateSlotRequest request) {
        return SlotResponse.from(slotService.update(userId, slotId, request.toCommand()));
    }

    /**
     * Deletes a slot that holds no meeting.
     *
     * @param userId id of the user owning the slot
     * @param slotId id of the slot
     */
    @DeleteMapping("/{slotId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long userId, @PathVariable Long slotId) {
        slotService.delete(userId, slotId);
    }
}
