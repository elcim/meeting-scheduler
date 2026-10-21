package com.meetingscheduler.scheduling.repository.jpa;

import com.meetingscheduler.scheduling.domain.Meeting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/**
 * Meetings and slots are separate aggregates referencing each other by id, so queries that filter by slot facts
 * join them explicitly.
 */
interface SpringDataMeetingRepository extends JpaRepository<Meeting, Long> {

    @Query("""
            select m from Meeting m join Slot s on s.id = m.slotId
            where m.id = :id and s.calendarId = :calendarId
            """)
    Optional<Meeting> findInCalendar(@Param("id") Long id, @Param("calendarId") Long calendarId);

    @Query("""
            select new com.meetingscheduler.scheduling.repository.jpa.MeetingRow(m, s, c.ownerId)
            from Meeting m join Slot s on s.id = m.slotId join Calendar c on c.id = s.calendarId
            where m.id = :id and s.calendarId = :calendarId
            """)
    Optional<MeetingRow> findRowInCalendar(@Param("id") Long id, @Param("calendarId") Long calendarId);

    @Query(value = """
            select new com.meetingscheduler.scheduling.repository.jpa.MeetingRow(m, s, c.ownerId)
            from Meeting m join Slot s on s.id = m.slotId join Calendar c on c.id = s.calendarId
            where s.calendarId = :calendarId and s.startTime >= :from and s.startTime < :to
            order by s.startTime, m.id
            """,
            countQuery = """
                    select count(m) from Meeting m join Slot s on s.id = m.slotId
                    where s.calendarId = :calendarId and s.startTime >= :from and s.startTime < :to
                    """)
    Page<MeetingRow> findOrganizedBy(@Param("calendarId") Long calendarId,
                                     @Param("from") Instant from,
                                     @Param("to") Instant to,
                                     Pageable pageable);

    @Query(value = """
            select new com.meetingscheduler.scheduling.repository.jpa.MeetingRow(m, s, c.ownerId)
            from Meeting m join Slot s on s.id = m.slotId join Calendar c on c.id = s.calendarId
            where :userId member of m.participantIds and s.startTime >= :from and s.startTime < :to
            order by s.startTime, m.id
            """,
            countQuery = """
                    select count(m) from Meeting m join Slot s on s.id = m.slotId
                    where :userId member of m.participantIds and s.startTime >= :from and s.startTime < :to
                    """)
    Page<MeetingRow> findParticipatedBy(@Param("userId") Long userId,
                                        @Param("from") Instant from,
                                        @Param("to") Instant to,
                                        Pageable pageable);
}
