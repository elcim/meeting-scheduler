package com.meetingscheduler.scheduling.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Aggregate root: what is going to happen in a booked slot. It references the slot and its participants by id.
 * Meetings come into existence through {@link Slot#bookMeeting}, which keeps the slot in sync.
 */
@Entity
@Table(name = "meeting")
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "meeting_seq")
    @SequenceGenerator(name = "meeting_seq", sequenceName = "meeting_seq", allocationSize = 50)
    private Long id;

    @Column(name = "slot_id", nullable = false, updatable = false, unique = true)
    private Long slotId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 4000)
    private String description;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "meeting_participant", joinColumns = @JoinColumn(name = "meeting_id"))
    @Column(name = "user_id", nullable = false)
    private Set<Long> participantIds = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private long version;

    protected Meeting() {
    }

    Meeting(Long slotId, String title, String description, Set<Long> participantIds) {
        this.slotId = slotId;
        this.createdAt = Instant.now();
        update(title, description, participantIds);
    }

    public void update(String title, String description, Set<Long> participantIds) {
        this.title = title.trim();
        this.description = description;
        this.participantIds.retainAll(participantIds);
        this.participantIds.addAll(participantIds);
    }

    public Long getId() {
        return id;
    }

    public Long getSlotId() {
        return slotId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Set<Long> getParticipantIds() {
        return participantIds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
