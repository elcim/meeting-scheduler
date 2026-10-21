package com.meetingscheduler.scheduling.repository.jpa;

import com.meetingscheduler.scheduling.domain.Slot;
import com.meetingscheduler.scheduling.domain.SlotSpan;
import com.meetingscheduler.scheduling.domain.SlotStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface SpringDataSlotRepository extends JpaRepository<Slot, Long> {

    @Query("select s from Slot s where s.id = :id and s.calendarId = :calendarId")
    Optional<Slot> findInCalendar(@Param("id") Long id, @Param("calendarId") Long calendarId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Slot s where s.id = :id and s.calendarId = :calendarId")
    Optional<Slot> findInCalendarForUpdate(@Param("id") Long id, @Param("calendarId") Long calendarId);

    /**
     * Whether any slot of the calendar intersects [start, end), ignoring the slot with id {@code excludedId}.
     */
    @Query("""
            select count(s) > 0 from Slot s
            where s.calendarId = :calendarId
              and s.startTime < :end and s.endTime > :start
              and s.id <> :excludedId
            """)
    boolean existsOverlapping(@Param("calendarId") Long calendarId,
                              @Param("start") Instant start,
                              @Param("end") Instant end,
                              @Param("excludedId") Long excludedId);

    @Query(value = """
            select s from Slot s
            where s.calendarId = :calendarId and s.startTime >= :from and s.startTime < :to
            order by s.startTime, s.id
            """,
            countQuery = """
                    select count(s) from Slot s
                    where s.calendarId = :calendarId and s.startTime >= :from and s.startTime < :to
                    """)
    Page<Slot> findStartingIn(@Param("calendarId") Long calendarId,
                              @Param("from") Instant from,
                              @Param("to") Instant to,
                              Pageable pageable);

    @Query(value = """
            select s from Slot s
            where s.calendarId = :calendarId and s.status = :status
              and s.startTime >= :from and s.startTime < :to
            order by s.startTime, s.id
            """,
            countQuery = """
                    select count(s) from Slot s
                    where s.calendarId = :calendarId and s.status = :status
                      and s.startTime >= :from and s.startTime < :to
                    """)
    Page<Slot> findStartingInWithStatus(@Param("calendarId") Long calendarId,
                                        @Param("status") SlotStatus status,
                                        @Param("from") Instant from,
                                        @Param("to") Instant to,
                                        Pageable pageable);

    @Query("""
            select new com.meetingscheduler.scheduling.domain.SlotSpan(s.startTime, s.durationMinutes, s.status)
            from Slot s
            where s.calendarId = :calendarId and s.startTime >= :from and s.startTime < :to
            order by s.startTime
            """)
    List<SlotSpan> findSpansStartingIn(@Param("calendarId") Long calendarId,
                                       @Param("from") Instant from,
                                       @Param("to") Instant to);
}
