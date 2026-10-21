package com.meetingscheduler.user.repository;

import com.meetingscheduler.user.domain.EmailAddress;
import com.meetingscheduler.user.domain.User;

import java.util.Collection;
import java.util.Optional;

/**
 * Port for storing and finding {@link User users}.
 */
public interface UserRepository {

    /**
     * Stores the user.
     *
     * @param user the user to store
     */
    User save(User user);

    /**
     * The user with the id, if there is one.
     *
     * @param id id of the user
     */
    Optional<User> findById(Long id);

    /**
     * Whether a user with the id exists.
     *
     * @param id id of the user
     */
    boolean existsById(Long id);

    /**
     * Whether a user with the email address exists.
     *
     * @param email the email address to look for
     */
    boolean existsByEmail(EmailAddress email);

    /**
     * How many of the given ids belong to an existing user.
     *
     * @param ids ids of the users to count
     */
    long countExisting(Collection<Long> ids);
}
