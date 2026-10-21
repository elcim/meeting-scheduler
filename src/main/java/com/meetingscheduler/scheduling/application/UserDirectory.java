package com.meetingscheduler.scheduling.application;

import java.util.Set;

/**
 * What scheduling needs to know about the user context (anti-corruption port).
 */
public interface UserDirectory {

    boolean exists(Long userId);

    boolean allExist(Set<Long> userIds);
}
