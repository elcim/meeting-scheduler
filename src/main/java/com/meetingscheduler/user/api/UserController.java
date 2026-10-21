package com.meetingscheduler.user.api;

import com.meetingscheduler.user.api.dto.CreateUserRequest;
import com.meetingscheduler.user.api.dto.UserResponse;
import com.meetingscheduler.user.application.RegisterUserCommand;
import com.meetingscheduler.user.application.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = UserResponse.from(userService.register(new RegisterUserCommand(request.name(), request.email())));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(user.id()).toUri();
        return ResponseEntity.created(location).body(user);
    }

    @GetMapping("/{userId}")
    public UserResponse get(@PathVariable Long userId) {
        return UserResponse.from(userService.get(userId));
    }
}
