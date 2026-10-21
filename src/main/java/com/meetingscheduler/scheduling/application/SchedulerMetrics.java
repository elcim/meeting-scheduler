package com.meetingscheduler.scheduling.application;

import com.meetingscheduler.scheduling.domain.Calendar;
import com.meetingscheduler.shared.domain.ConflictException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Business metrics of the scheduling use cases, published through Micrometer. The domain stays free of metrics code;
 * the application services report the outcome of their use cases here.
 * <p>
 * Meters are tagged by outcome only, never by user, slot or meeting, so their number stays bounded.
 * <ul>
 *     <li>{@code scheduler.meetings} counts meetings by {@code event} ({@code booked}, {@code cancelled}).</li>
 *     <li>{@code scheduler.slots} counts slots by {@code event} ({@code created}, one by one or in bulk).</li>
 *     <li>{@code scheduler.conflicts} counts requests refused with a conflict, by {@link Operation}.</li>
 *     <li>{@code scheduler.calendar.lock.wait} times how long taking the calendar lock takes, which shows contention
 *     between concurrent slot changes of one user.</li>
 * </ul>
 */
@Component
class SchedulerMetrics {

    /**
     * The use cases that can be refused with a conflict.
     */
    enum Operation {
        BOOK_MEETING, CREATE_SLOT, CREATE_SLOTS_BULK, UPDATE_SLOT, DELETE_SLOT;

        String tag() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private final Counter meetingsBooked;
    private final Counter meetingsCancelled;
    private final Counter slotsCreated;
    private final Map<Operation, Counter> conflicts = new EnumMap<>(Operation.class);
    private final Timer calendarLockWait;

    SchedulerMetrics(MeterRegistry registry) {
        meetingsBooked = Counter.builder("scheduler.meetings").tag("event", "booked")
                .description("Meetings booked and cancelled").register(registry);
        meetingsCancelled = Counter.builder("scheduler.meetings").tag("event", "cancelled")
                .description("Meetings booked and cancelled").register(registry);
        slotsCreated = Counter.builder("scheduler.slots").tag("event", "created")
                .description("Slots created, one by one or in bulk").register(registry);
        for (Operation operation : Operation.values()) {
            conflicts.put(operation, Counter.builder("scheduler.conflicts").tag("operation", operation.tag())
                    .description("Requests refused because they conflict with existing data").register(registry));
        }
        calendarLockWait = Timer.builder("scheduler.calendar.lock.wait")
                .description("Time spent waiting for the lock of a calendar").register(registry);
    }

    /**
     * Counts a booked meeting once the surrounding transaction committed.
     */
    void meetingBooked() {
        afterCommit(meetingsBooked::increment);
    }

    /**
     * Counts a cancelled meeting once the surrounding transaction committed.
     */
    void meetingCancelled() {
        afterCommit(meetingsCancelled::increment);
    }

    /**
     * Counts created slots once the surrounding transaction committed.
     *
     * @param count how many slots were created
     */
    void slotsCreated(int count) {
        afterCommit(() -> slotsCreated.increment(count));
    }

    /**
     * Runs the action and counts it if it is refused with a {@link ConflictException}, which is rethrown.
     *
     * @param operation the use case being run
     * @param action the part of the use case that can be refused
     */
    <T> T countConflicts(Operation operation, Supplier<T> action) {
        try {
            return action.get();
        } catch (ConflictException e) {
            conflicts.get(operation).increment();
            throw e;
        }
    }

    /**
     * Same as {@link #countConflicts(Operation, Supplier)} for an action without a result.
     *
     * @param operation the use case being run
     * @param action the part of the use case that can be refused
     */
    void countConflictsOf(Operation operation, Runnable action) {
        countConflicts(operation, () -> {
            action.run();
            return null;
        });
    }

    /**
     * Times how long the action takes to obtain the lock of a calendar.
     *
     * @param lock takes the lock
     */
    Calendar timeCalendarLock(Supplier<Calendar> lock) {
        return calendarLockWait.record(lock);
    }

    /**
     * Runs the action when the current transaction commits, so a rolled back use case is not counted. Without a
     * transaction the action runs right away.
     */
    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
