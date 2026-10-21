package com.meetingscheduler.scheduling.infrastructure;

import com.meetingscheduler.scheduling.domain.Calendar;
import com.meetingscheduler.scheduling.domain.CalendarRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class JpaCalendarRepositoryAdapter implements CalendarRepository {

    private final SpringDataCalendarRepository jpa;

    JpaCalendarRepositoryAdapter(SpringDataCalendarRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Calendar save(Calendar calendar) {
        return jpa.save(calendar);
    }

    @Override
    public Optional<Long> findIdByOwner(Long ownerId) {
        return jpa.findIdByOwnerId(ownerId);
    }

    @Override
    public Optional<Calendar> findByOwnerForUpdate(Long ownerId) {
        return jpa.findByOwnerIdForUpdate(ownerId);
    }
}
