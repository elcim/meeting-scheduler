# meeting-scheduler

Spring Boot (Java 21) backend for managing time slots, scheduling meetings and viewing availability.

## Run

```bash
mvn spring-boot:run     # http://localhost:8080
mvn test
```

Data is persisted to a file based H2 database in `./data`; the schema is managed by Flyway
(`src/main/resources/db/migration`). Point `spring.datasource.*` at another database (and add its driver)
to run against a server database; the migration uses portable SQL.

## Architecture

The code is organized by bounded context, and each context by DDD layer. Dependencies point inwards only
(`api` → `application` → `domain` ← `infrastructure`). Persistence lives in each context's `repository` package: the
repository interfaces (ports), their Spring Data interfaces and JPA adapters.

```
com.meetingscheduler
├── shared          shared kernel: DomainException types, TimeWindow, Page/PageQuery, error handling (RFC 9457)
├── user            bounded context: who the users are
│   ├── domain          User (aggregate), EmailAddress (value object), UserRegistered (event)
│   ├── application     UserService (use cases, transactions), RegisterUserCommand
│   ├── repository      UserRepository (port); jpa: Spring Data interface and JPA adapter
│   └── api             UserController, request/response DTOs
└── scheduling      bounded context: calendars, slots, meetings, availability
    ├── domain          Calendar, Slot, Meeting (aggregates); SlotPlanner (domain service); Availability, SlotSeries,
    │                   SlotLimits, MeetingLimits (value objects)
    ├── application     SlotService, MeetingService, AvailabilityService; commands; MeetingView + MeetingQueries
    │                   (read model); UserDirectory (port to the user context); CalendarProvisioning (event listener)
    ├── repository      Calendar/Slot/Meeting repository ports; jpa: Spring Data interfaces and JPA adapters
    ├── infrastructure  configuration properties, wiring of domain services, UserDirectory adapter
    └── api             Slot/Meeting/Availability controllers and DTOs
```

* **Aggregates** reference each other by id only (`Slot.calendarId`, `Meeting.slotId`, `Calendar.ownerId`), never by
  object. They guard their own invariants: a `Slot` rejects double booking and refuses to be moved, freed or deleted
  while it holds a meeting; `Slot.bookMeeting` is the only way a `Meeting` comes into existence.
* **Domain service** `SlotPlanner` owns the rule that spans aggregates (slots of one calendar never overlap).
* **Repositories** are interfaces in the `repository` package of each context; the Spring Data interfaces and adapters that implement them are in its `jpa` subpackage.
* **Contexts talk through events and ports**: registering a user publishes `UserRegistered`, which scheduling
  handles (in the same transaction) by creating the user's calendar; scheduling asks the user context whether users
  exist through its own `UserDirectory` port.
* **CQRS-lite read side**: views that combine a meeting with its slot (`MeetingView`) come from `MeetingQueries`, a
  joined query, not from navigating between aggregates.
* Pragmatic trade-off: the aggregates carry JPA annotations instead of being mapped to separate persistence
  classes, so the domain depends on JPA/Hibernate annotations but on no Spring class.

## Design

* **Calendar is a domain concept only.** Every user gets exactly one calendar on creation which owns their slots.
  It never appears in the API; everything is addressed through `/api/users/{userId}`.
* **Slot**: a `[startTime, endTime)` interval with a status `FREE` or `BUSY`. Slots of one calendar never overlap.
  `booked` tells a slot that is busy because of a meeting from one that was blocked manually.
* **Meeting**: converts a free slot (making it busy) and adds title, description and participants (existing users).
  The slot owner is the organizer. Cancelling the meeting frees the slot. A slot with a meeting cannot be deleted,
  moved, resized or freed until the meeting is cancelled.

### Scale and concurrency

* Indexes `(calendar_id, start_time)` and `(calendar_id, status, start_time)` serve overlap checks, window
  listings and aggregations as range scans of a single calendar, independent of the total number of slots.
* All collection endpoints are time-window bound and paginated (max page size 200); the availability window is
  limited to 62 days. Availability is computed from a lightweight projection, not from entities.
* Ids come from sequences with an increment of 50 and JDBC batching is on, so bulk slot creation is batched.
* Creating/moving slots takes a pessimistic lock on the owner's calendar row, so concurrent requests cannot create
  overlapping slots, and users do not block each other. Booking locks the slot row, so of concurrent bookings of
  one slot exactly one wins (`409` for the others); a unique constraint on `meeting.slot_id` backs this up.
* Open-in-view is disabled and the lazy associations are batch fetched, avoiding N+1 queries.

## API

Timestamps are ISO-8601 instants (`2030-01-01T09:00:00Z`). Paged endpoints take a **zero-based** `page` (default `0`,
so `page=1` is the second page) and a `size` (default 50, max 200). Errors are RFC 9457 problem details
(`400` validation, `404` unknown resource, `409` conflict).

| Method & path | Purpose |
|---|---|
| `POST /api/users` | Create a user (and their calendar) `{name, email}` |
| `GET /api/users/{userId}` | Get a user |
| `POST /api/users/{userId}/slots` | Create a slot `{startTime, durationMinutes}` |
| `POST /api/users/{userId}/slots/bulk` | Split `[from, to)` into slots `{from, to, slotDurationMinutes, gapMinutes?}` (max 500) |
| `GET /api/users/{userId}/slots?from&to&status?&page?&size?` | Free/busy slots starting in the time frame |
| `GET /api/users/{userId}/slots/{slotId}` | Get a slot |
| `PATCH /api/users/{userId}/slots/{slotId}` | Change `startTime`, `durationMinutes` and/or `status` (`FREE`/`BUSY`) |
| `DELETE /api/users/{userId}/slots/{slotId}` | Delete a slot |
| `POST /api/users/{userId}/meetings` | Convert a free slot into a meeting `{slotId, title, description?, participantIds}` |
| `GET /api/users/{userId}/meetings?from&to` | Meetings held in the user's slots |
| `GET /api/users/{userId}/meetings/participating?from&to` | Meetings the user was invited to |
| `GET /api/users/{userId}/meetings/{meetingId}` | Get a meeting |
| `PUT /api/users/{userId}/meetings/{meetingId}` | Update title, description and participants |
| `DELETE /api/users/{userId}/meetings/{meetingId}` | Cancel the meeting and free its slot |
| `GET /api/users/{userId}/availability?from&to&zone?` | Aggregated free/busy counts and minutes, in total and per day |

Limits (slot duration 5-480 minutes, bulk size, availability window, participants) are configurable under
`scheduler.*` in `application.yml`.
