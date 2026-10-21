-- Sequences use an increment of 50 so Hibernate can hand out ids in memory (pooled optimizer)
-- and batch inserts without a round trip per row.
CREATE SEQUENCE app_user_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE calendar_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE slot_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE meeting_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE app_user (
    id         BIGINT                   NOT NULL PRIMARY KEY,
    name       VARCHAR(100)             NOT NULL,
    email      VARCHAR(254)             NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_app_user_email UNIQUE (email)
);

-- Every user owns exactly one calendar. It is a domain concept only and is never exposed by the API.
CREATE TABLE calendar (
    id      BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    CONSTRAINT uq_calendar_user UNIQUE (user_id),
    CONSTRAINT fk_calendar_user FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE TABLE slot (
    id               BIGINT                   NOT NULL PRIMARY KEY,
    calendar_id      BIGINT                   NOT NULL,
    start_time       TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time         TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_minutes INT                      NOT NULL,
    status           VARCHAR(10)              NOT NULL,
    booked           BOOLEAN                  NOT NULL,
    version          BIGINT                   NOT NULL,
    CONSTRAINT fk_slot_calendar FOREIGN KEY (calendar_id) REFERENCES calendar (id),
    CONSTRAINT ck_slot_range CHECK (end_time > start_time),
    CONSTRAINT ck_slot_status CHECK (status IN ('FREE', 'BUSY'))
);

-- Serves overlap checks and time window range scans for one calendar.
CREATE INDEX idx_slot_calendar_start ON slot (calendar_id, start_time);
-- Serves free/busy filtered window queries and aggregations.
CREATE INDEX idx_slot_calendar_status_start ON slot (calendar_id, status, start_time);

CREATE TABLE meeting (
    id          BIGINT                   NOT NULL PRIMARY KEY,
    slot_id     BIGINT                   NOT NULL,
    title       VARCHAR(200)             NOT NULL,
    description VARCHAR(4000),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    version     BIGINT                   NOT NULL,
    -- A slot can back at most one meeting, even under concurrent booking attempts.
    CONSTRAINT uq_meeting_slot UNIQUE (slot_id),
    CONSTRAINT fk_meeting_slot FOREIGN KEY (slot_id) REFERENCES slot (id)
);

CREATE TABLE meeting_participant (
    meeting_id BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    PRIMARY KEY (meeting_id, user_id),
    CONSTRAINT fk_participant_meeting FOREIGN KEY (meeting_id) REFERENCES meeting (id) ON DELETE CASCADE,
    CONSTRAINT fk_participant_user FOREIGN KEY (user_id) REFERENCES app_user (id)
);

-- "Meetings I participate in" lookups.
CREATE INDEX idx_participant_user ON meeting_participant (user_id, meeting_id);
