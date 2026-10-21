package com.meetingscheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of the meeting scheduler: a Spring Boot application exposing the REST API.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MeetingSchedulerApplication {

    /**
     * Starts the application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(MeetingSchedulerApplication.class, args);
    }
}
