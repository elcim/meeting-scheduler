package com.meetingscheduler.scheduling.api;

import com.meetingscheduler.scheduling.api.dto.AvailabilityResponse;
import com.meetingscheduler.scheduling.application.AvailabilityService;
import com.meetingscheduler.shared.domain.TimeWindow;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/users/{userId}/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    /**
     * Aggregated free/busy view of the slots starting within [from, to).
     *
     * @param zone time zone used to bucket slots into days, UTC by default
     */
    @GetMapping
    public AvailabilityResponse get(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String zone) {
        return AvailabilityResponse.from(availabilityService.getAvailability(userId, new TimeWindow(from, to), zone));
    }
}
