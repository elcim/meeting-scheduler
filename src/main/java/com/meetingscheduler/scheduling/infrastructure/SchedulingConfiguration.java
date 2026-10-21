package com.meetingscheduler.scheduling.infrastructure;

import com.meetingscheduler.scheduling.application.AvailabilityLimits;
import com.meetingscheduler.scheduling.domain.MeetingLimits;
import com.meetingscheduler.scheduling.domain.SlotLimits;
import com.meetingscheduler.scheduling.domain.SlotPlanner;
import com.meetingscheduler.scheduling.repository.SlotRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the framework-free domain services and policies with the configured limits.
 */
@Configuration(proxyBeanMethods = false)
class SchedulingConfiguration {

    @Bean
    SlotLimits slotLimits(SchedulerProperties properties) {
        SchedulerProperties.SlotProperties slot = properties.slot();
        return new SlotLimits(slot.minDurationMinutes(), slot.maxDurationMinutes(), slot.maxBulkSlots());
    }

    @Bean
    MeetingLimits meetingLimits(SchedulerProperties properties) {
        return new MeetingLimits(properties.meeting().maxParticipants());
    }

    @Bean
    AvailabilityLimits availabilityLimits(SchedulerProperties properties) {
        return new AvailabilityLimits(properties.availability().maxWindowDays());
    }

    @Bean
    SlotPlanner slotPlanner(SlotRepository slotRepository, SlotLimits slotLimits) {
        return new SlotPlanner(slotRepository, slotLimits);
    }
}
