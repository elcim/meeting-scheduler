package com.meetingscheduler.user.application;

import com.meetingscheduler.user.domain.UserRegistered;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Business metrics of the user context. {@code scheduler.users.registered} counts registered users; it is fed by the
 * {@link UserRegistered} event and so only counts users whose registration committed.
 */
@Component
class UserMetrics {

    private final Counter usersRegistered;

    UserMetrics(MeterRegistry registry) {
        usersRegistered = Counter.builder("scheduler.users.registered")
                .description("Users registered").register(registry);
    }

    /**
     * Counts the registered user.
     *
     * @param event the registered user
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(UserRegistered event) {
        usersRegistered.increment();
    }
}
