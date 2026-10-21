package com.meetingscheduler.scheduling.application;

import java.util.Set;

/**
 * What scheduling needs to know about the user context (anti-corruption port).
 */
public interface UserDirectory {

    /**
     * Whether a user with the id exists.
     *
     * @param userId id of the user
     */
    boolean exists(Long userId);

    /**
     * Whether every one of the ids belongs to an existing user.
     *
     * @param userIds ids of the users to check
     */
    boolean allExist(Set<Long> userIds);
}
