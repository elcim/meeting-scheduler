package com.meetingscheduler.scheduling.repository;

import com.meetingscheduler.scheduling.domain.Calendar;

import java.util.Optional;

public interface CalendarRepository {

    Calendar save(Calendar calendar);

    Optional<Long> findIdByOwner(Long ownerId);

    /**
     * Locks the calendar row until the end of the transaction, serializing slot layout changes of one user.
     * Must be called inside a transaction.
     */
    Optional<Calendar> findByOwnerForUpdate(Long ownerId);
}
