# meeting-scheduler

Spring Boot (Java 21) backend for managing time slots, scheduling meetings and viewing availability.

## Run

```bash
mvn spring-boot:run     # http://localhost:8080
mvn test
mvn javadoc:javadoc     # API documentation of the code
```

Data is persisted to a file based H2 database in `./data`; the schema is managed by Flyway
(`src/main/resources/db/migration`). Point `spring.datasource.*` at another database (and add its driver)
to run against a server database; the migration uses portable SQL.

## Architecture

The project follows **Domain-Driven Design (DDD)**. The business rules live in a rich domain model, expressed in the
language of the domain (user, calendar, slot, meeting, availability), and are kept apart from the web and persistence
technology around it. DDD is applied in two steps:

* **Strategic design**: the system is split into two *bounded contexts*, `user` (who the users are) and `scheduling`
  (calendars, slots, meetings, availability), plus a small `shared` kernel. Each context has its own model and
  talks to the other only through events and ports.
* **Tactical design**: inside a context the model is built from DDD's building blocks: *aggregates*, *entities*,
  *value objects*, *domain services*, *domain events* and *repositories*, each described below.

Each context is layered, and dependencies point inwards only (`api` → `application` → `domain`; the `repository` and
`infrastructure` packages implement the ports that the inner layers define or consume).

```
com.meetingscheduler
├── shared          shared kernel: DomainException types, TimeWindow, Page/PageQuery, error handling (RFC 9457)
├── user            bounded context: who the users are
│   ├── domain          User (aggregate), EmailAddress (value object), UserRegistered (domain event)
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

### DDD building blocks

* **Aggregates** are the consistency boundaries. `User`, `Calendar`, `Slot` and `Meeting` are aggregate roots that
  reference each other by id only (`Slot.calendarId`, `Meeting.slotId`, `Calendar.ownerId`), never by object. They guard
  their own invariants: a `Slot` rejects double booking and refuses to be moved, freed or deleted while it holds a
  meeting; `Slot.bookMeeting` is the only way a `Meeting` comes into existence.
* **Value objects** are immutable and compared by value: `EmailAddress`, `TimeWindow`, `SlotSeries`, `SlotLimits`,
  `MeetingLimits`, `Availability`.
* **Domain service** `SlotPlanner` owns the rule that spans aggregates (slots of one calendar never overlap), which
  no single `Slot` can enforce on its own.
* **Domain events**: registering a user publishes `UserRegistered`.
* **Repositories** are collection-like interfaces (ports) in the `repository` package of each context, with one per
  aggregate root. The Spring Data interfaces and the adapters that implement them are in its `jpa` subpackage, so the
  rest of the code never sees Spring Data. `SlotPlanner` is a domain class that uses the `SlotRepository` port, so
  `domain` and `repository` reference each other; the domain still depends on no Spring class.
* **Application services** (`SlotService`, `MeetingService`, `AvailabilityService`, `UserService`) are thin: they
  orchestrate one use case per method, own the transaction and delegate the rules to the domain.
* **Contexts talk through events and ports** (anti-corruption): registering a user publishes `UserRegistered`, which
  scheduling handles (in the same transaction) by creating the user's calendar; scheduling asks the user context
  whether users exist through its own `UserDirectory` port.
* **CQRS-lite read side**: views that combine a meeting with its slot (`MeetingView`) come from `MeetingQueries`, a
  joined query, not from navigating between aggregates.
* Pragmatic trade-off: the aggregates carry JPA annotations instead of being mapped to separate persistence
  classes, so the domain depends on JPA/Hibernate annotations but on no Spring class.

### Documentation

Every class, and every method apart from trivial ones (constructors, getters, overrides, DTO mappers), has Javadoc
that describes its purpose and parameters; the domain rules are stated on the aggregates and domain services. Generate the
HTML with `mvn javadoc:javadoc` (output in `target/reports/apidocs`).

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

The examples assume the application runs on `localhost:8080` and that users `1` and `2` exist, user `1` having the slots and meeting they refer to.

| Method & path | Purpose | curl |
|---|---|---|
| `POST /api/users` | Create a user (and their calendar) `{name, email}` | `curl -X POST http://localhost:8080/api/users -H 'Content-Type: application/json' -d '{"name":"Ada","email":"ada@example.com"}'` |
| `GET /api/users/{userId}` | Get a user | `curl http://localhost:8080/api/users/1` |
| `POST /api/users/{userId}/slots` | Create a slot `{startTime, durationMinutes}` | `curl -X POST http://localhost:8080/api/users/1/slots -H 'Content-Type: application/json' -d '{"startTime":"2030-01-01T09:00:00Z","durationMinutes":30}'` |
| `POST /api/users/{userId}/slots/bulk` | Split `[from, to)` into slots `{from, to, slotDurationMinutes, gapMinutes?}` (max 500) | `curl -X POST http://localhost:8080/api/users/1/slots/bulk -H 'Content-Type: application/json' -d '{"from":"2030-01-02T09:00:00Z","to":"2030-01-02T12:00:00Z","slotDurationMinutes":30,"gapMinutes":10}'` |
| `GET /api/users/{userId}/slots?from&to&status?&page?&size?` | Free/busy slots starting in the time frame | `curl 'http://localhost:8080/api/users/1/slots?from=2030-01-01T00:00:00Z&to=2030-01-08T00:00:00Z&status=FREE&page=0&size=20'` |
| `GET /api/users/{userId}/slots/{slotId}` | Get a slot | `curl http://localhost:8080/api/users/1/slots/1` |
| `PATCH /api/users/{userId}/slots/{slotId}` | Change `startTime`, `durationMinutes` and/or `status` (`FREE`/`BUSY`) | `curl -X PATCH http://localhost:8080/api/users/1/slots/1 -H 'Content-Type: application/json' -d '{"status":"BUSY"}'` |
| `DELETE /api/users/{userId}/slots/{slotId}` | Delete a slot | `curl -X DELETE http://localhost:8080/api/users/1/slots/1` |
| `POST /api/users/{userId}/meetings` | Convert a free slot into a meeting `{slotId, title, description?, participantIds}` | `curl -X POST http://localhost:8080/api/users/1/meetings -H 'Content-Type: application/json' -d '{"slotId":2,"title":"Sync","description":"Weekly sync","participantIds":[2]}'` |
| `GET /api/users/{userId}/meetings?from&to` | Meetings held in the user's slots | `curl 'http://localhost:8080/api/users/1/meetings?from=2030-01-01T00:00:00Z&to=2030-01-08T00:00:00Z'` |
| `GET /api/users/{userId}/meetings/participating?from&to` | Meetings the user was invited to | `curl 'http://localhost:8080/api/users/2/meetings/participating?from=2030-01-01T00:00:00Z&to=2030-01-08T00:00:00Z'` |
| `GET /api/users/{userId}/meetings/{meetingId}` | Get a meeting | `curl http://localhost:8080/api/users/1/meetings/1` |
| `PUT /api/users/{userId}/meetings/{meetingId}` | Update title, description and participants | `curl -X PUT http://localhost:8080/api/users/1/meetings/1 -H 'Content-Type: application/json' -d '{"title":"Sync","description":"Moved to Friday","participantIds":[2]}'` |
| `DELETE /api/users/{userId}/meetings/{meetingId}` | Cancel the meeting and free its slot | `curl -X DELETE http://localhost:8080/api/users/1/meetings/1` |
| `GET /api/users/{userId}/availability?from&to&zone?` | Aggregated free/busy counts and minutes, in total and per day | `curl 'http://localhost:8080/api/users/1/availability?from=2030-01-01T00:00:00Z&to=2030-01-08T00:00:00Z&zone=Europe/Berlin'` |

Limits (slot duration 5-480 minutes, bulk size, availability window, participants) are configurable under
`scheduler.*` in `application.yml`.
