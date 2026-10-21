package com.meetingscheduler.scheduling.repository;

import com.meetingscheduler.scheduling.domain.Calendar;

import java.util.Optional;

/**
 * Port for storing and finding {@link Calendar calendars}.
 */
public interface CalendarRepository {

    /**
     * Stores the calendar.
     *
     * @param calendar the calendar to store
     */
    Calendar save(Calendar calendar);

    /**
     * Id of the calendar owned by the user, if there is one.
     *
     * @param ownerId id of the user owning the calendar
     */
    Optional<Long> findIdByOwner(Long ownerId);

    /**
     * Locks the calendar row until the end of the transaction, serializing slot layout changes of one user.
     * Must be called inside a transaction.
     *
     * @param ownerId id of the user owning the calendar
     */
    Optional<Calendar> findByOwnerForUpdate(Long ownerId);
}
