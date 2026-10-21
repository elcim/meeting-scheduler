package com.meetingscheduler.scheduling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * The personal calendar of a user, owning all of the user's time slots.
 * <p>
 * It is a domain concept only: the API addresses everything through the owning user, who is referenced by id as
 * the user lives in another bounded context. Its row also acts as the lock that serializes slot layout changes
 * (create, move) of one user so that slots of a calendar can never overlap.
 */
@Entity
@Table(name = "calendar")
public class Calendar {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "calendar_seq")
    @SequenceGenerator(name = "calendar_seq", sequenceName = "calendar_seq", allocationSize = 50)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false, unique = true)
    private Long ownerId;

    protected Calendar() {
    }

    public Calendar(Long ownerId) {
        this.ownerId = ownerId;
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }
}
