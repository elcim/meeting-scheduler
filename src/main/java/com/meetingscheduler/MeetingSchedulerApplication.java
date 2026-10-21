package com.meetingscheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MeetingSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeetingSchedulerApplication.class, args);
    }
}
