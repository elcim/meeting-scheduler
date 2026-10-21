package com.meetingscheduler.user.application;

import com.meetingscheduler.shared.domain.ConflictException;
import com.meetingscheduler.shared.domain.ResourceNotFoundException;
import com.meetingscheduler.user.domain.EmailAddress;
import com.meetingscheduler.user.domain.User;
import com.meetingscheduler.user.domain.UserRegistered;
import com.meetingscheduler.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;

    public UserService(UserRepository userRepository, ApplicationEventPublisher events) {
        this.userRepository = userRepository;
        this.events = events;
    }

    /**
     * Registers the user. Listeners of {@link UserRegistered} run in the same transaction, so e.g. the user's calendar
     * is created atomically with the user.
     */
    @Transactional
    public User register(RegisterUserCommand command) {
        EmailAddress email = new EmailAddress(command.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("A user with email " + email.value() + " already exists");
        }
        User user = userRepository.save(User.register(command.name(), email));
        events.publishEvent(new UserRegistered(user.getId()));
        return user;
    }

    public User get(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
