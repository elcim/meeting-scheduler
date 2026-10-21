package com.meetingscheduler.user.application;

/**
 * Input for registering a user.
 */
public record RegisterUserCommand(String name, String email) {
}
