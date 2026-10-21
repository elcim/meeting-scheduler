package com.meetingscheduler.user.domain;

import java.util.Collection;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    boolean existsById(Long id);

    boolean existsByEmail(EmailAddress email);

    /**
     * How many of the given ids belong to an existing user.
     */
    long countExisting(Collection<Long> ids);
}
