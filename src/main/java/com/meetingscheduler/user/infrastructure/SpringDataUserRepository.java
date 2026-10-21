package com.meetingscheduler.user.infrastructure;

import com.meetingscheduler.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

interface SpringDataUserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    long countByIdIn(Collection<Long> ids);
}
