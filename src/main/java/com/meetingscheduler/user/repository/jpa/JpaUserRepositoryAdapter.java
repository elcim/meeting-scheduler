package com.meetingscheduler.user.repository.jpa;

import com.meetingscheduler.user.domain.EmailAddress;
import com.meetingscheduler.user.domain.User;
import com.meetingscheduler.user.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

/**
 * JPA adapter of {@link UserRepository}.
 */
@Repository
class JpaUserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository jpa;

    JpaUserRepositoryAdapter(SpringDataUserRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public User save(User user) {
        return jpa.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpa.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpa.existsById(id);
    }

    @Override
    public boolean existsByEmail(EmailAddress email) {
        return jpa.existsByEmail(email.value());
    }

    @Override
    public long countExisting(Collection<Long> ids) {
        return jpa.countByIdIn(ids);
    }
}
