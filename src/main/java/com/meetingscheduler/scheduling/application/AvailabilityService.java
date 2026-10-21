package com.meetingscheduler.scheduling.application;

import com.meetingscheduler.scheduling.domain.Availability;
import com.meetingscheduler.scheduling.repository.SlotRepository;
import com.meetingscheduler.shared.domain.InvalidRequestException;
import com.meetingscheduler.shared.domain.TimeWindow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;

/**
 * Use case for the aggregated free/busy view of a user's calendar.
 */
@Service
@Transactional(readOnly = true)
public class AvailabilityService {

    private final SlotRepository slotRepository;
    private final CalendarLookup calendarLookup;
    private final AvailabilityLimits limits;

    AvailabilityService(SlotRepository slotRepository, CalendarLookup calendarLookup, AvailabilityLimits limits) {
        this.slotRepository = slotRepository;
        this.calendarLookup = calendarLookup;
        this.limits = limits;
    }

    /**
     * Totals the free and busy slots starting within the window, overall and per day.
     *
     * @param userId id of the user owning the calendar
     * @param window the time frame {@code [from, to)}
     * @param zoneId time zone used to bucket slots into days, UTC when {@code null}
     */
    public Availability getAvailability(Long userId, TimeWindow window, String zoneId) {
        if (window.length().toDays() > limits.maxWindowDays()) {
            throw new InvalidRequestException("The time frame can span at most " + limits.maxWindowDays() + " days");
        }
        ZoneId zone = parseZone(zoneId);
        Long calendarId = calendarLookup.calendarIdOf(userId);

        return Availability.summarize(window, zone, slotRepository.findSpansStartingIn(calendarId, window));
    }

    private ZoneId parseZone(String zoneId) {
        try {
            return zoneId == null ? ZoneId.of("UTC") : ZoneId.of(zoneId);
        } catch (DateTimeException e) {
            throw new InvalidRequestException("Unknown time zone: " + zoneId);
        }
    }
}
