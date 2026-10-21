package com.meetingscheduler.scheduling.domain;

import com.meetingscheduler.shared.domain.ConflictException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * Aggregate root: a {@code [startTime, endTime)} interval of a calendar that is either free or busy.
 * <p>
 * The slot guards the rules around its own state: a slot that holds a meeting ({@code booked}) is always busy and
 * can neither be moved, resized, freed nor deleted until the meeting is cancelled. The rule that slots of one
 * calendar must not overlap spans aggregates and is enforced by {@link SlotPlanner}.
 */
@Entity
@Table(name = "slot")
public class Slot {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "slot_seq")
    @SequenceGenerator(name = "slot_seq", sequenceName = "slot_seq", allocationSize = 50)
    private Long id;

    @Column(name = "calendar_id", nullable = false, updatable = false)
    private Long calendarId;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private SlotStatus status;

    /**
     * True while a meeting is scheduled in this slot. A booked slot is always BUSY.
     */
    @Column(nullable = false)
    private boolean booked;

    @Version
    private long version;

    protected Slot() {
    }

    private Slot(Long calendarId, Instant startTime, int durationMinutes) {
        this.calendarId = calendarId;
        this.status = SlotStatus.FREE;
        place(startTime, durationMinutes);
    }

    public static Slot create(Long calendarId, Instant startTime, int durationMinutes, SlotLimits limits) {
        limits.requireValidDuration(durationMinutes);
        return new Slot(calendarId, startTime, durationMinutes);
    }

    public void reschedule(Instant startTime, int durationMinutes, SlotLimits limits) {
        if (booked) {
            throw new ConflictException("Slot " + id + " has a meeting, cancel it before changing the slot time");
        }
        limits.requireValidDuration(durationMinutes);
        place(startTime, durationMinutes);
    }

    public void markFree() {
        if (booked) {
            throw new ConflictException("Slot " + id + " has a meeting, cancel it to free the slot");
        }
        this.status = SlotStatus.FREE;
    }

    public void markBusy() {
        this.status = SlotStatus.BUSY;
    }

    /**
     * Converts this free slot into a meeting, which makes the slot busy.
     */
    public Meeting bookMeeting(String title, String description, Set<Long> participantIds) {
        if (booked) {
            throw new ConflictException("Slot " + id + " already has a meeting");
        }
        if (status == SlotStatus.BUSY) {
            throw new ConflictException("Slot " + id + " is busy, mark it free before booking a meeting");
        }
        this.status = SlotStatus.BUSY;
        this.booked = true;
        return new Meeting(id, title, description, participantIds);
    }

    /**
     * Frees the slot after its meeting was cancelled.
     */
    public void releaseMeeting() {
        this.status = SlotStatus.FREE;
        this.booked = false;
    }

    public void requireDeletable() {
        if (booked) {
            throw new ConflictException("Slot " + id + " has a meeting, cancel it before deleting the slot");
        }
    }

    private void place(Instant startTime, int durationMinutes) {
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.endTime = startTime.plus(Duration.ofMinutes(durationMinutes));
    }

    public Long getId() {
        return id;
    }

    public Long getCalendarId() {
        return calendarId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public SlotStatus getStatus() {
        return status;
    }

    public boolean isBooked() {
        return booked;
    }
}
