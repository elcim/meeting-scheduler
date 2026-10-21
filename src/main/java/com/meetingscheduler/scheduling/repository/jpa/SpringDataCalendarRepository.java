package com.meetingscheduler.scheduling.repository.jpa;

import com.meetingscheduler.scheduling.domain.Calendar;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface SpringDataCalendarRepository extends JpaRepository<Calendar, Long> {

    @Query("select c.id from Calendar c where c.ownerId = :ownerId")
    Optional<Long> findIdByOwnerId(@Param("ownerId") Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Calendar c where c.ownerId = :ownerId")
    Optional<Calendar> findByOwnerIdForUpdate(@Param("ownerId") Long ownerId);
}
