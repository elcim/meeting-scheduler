package com.meetingscheduler.scheduling.infrastructure;

import com.meetingscheduler.scheduling.application.UserDirectory;
import com.meetingscheduler.user.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Answers the scheduling context's questions about users by asking the user context's repository.
 */
@Component
class UserDirectoryAdapter implements UserDirectory {

    private final UserRepository userRepository;

    UserDirectoryAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean exists(Long userId) {
        return userRepository.existsById(userId);
    }

    @Override
    public boolean allExist(Set<Long> userIds) {
        return userIds.isEmpty() || userRepository.countExisting(userIds) == userIds.size();
    }
}
