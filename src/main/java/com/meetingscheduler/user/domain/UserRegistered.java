package com.meetingscheduler.user.domain;

/**
 * Domain event: a new user signed up. Other contexts react to it, e.g. scheduling provisions the user's calendar.
 */
public record UserRegistered(Long userId) {
}
