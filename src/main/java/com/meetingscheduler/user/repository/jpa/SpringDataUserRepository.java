package com.meetingscheduler.user.repository.jpa;

import com.meetingscheduler.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

/**
 * Spring Data repository for {@link User}, used by {@link JpaUserRepositoryAdapter}.
 */
interface SpringDataUserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    long countByIdIn(Collection<Long> ids);
}
