package com.example.chat.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import com.example.chat.entity.User;
import com.example.chat.service.UserService;
import com.example.chat.util.CurrentUserResolver;

@Component
public class UserController {

    private final UserService userService;
    private final CurrentUserResolver currentUserResolver;

    public UserController(UserService userService, CurrentUserResolver currentUserResolver) {
        this.userService = userService;
        this.currentUserResolver = currentUserResolver;
    }

    public ServerResponse getCurrentUser(ServerRequest request) throws Exception {
        try {
            User user = currentUserResolver.resolve();
            return ServerResponse.ok().body(Map.of(
                    "status", true,
                    "message", "Current user fetched successfully",
                    "data", toSummary(user)
            ));
        } catch (Exception e) {
            return ServerResponse.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", false,
                    "message", e.getMessage()
            ));
        }
    }

    public ServerResponse searchUsers(ServerRequest request) throws Exception {
        try {
            String username = request.param("username").orElse("");
            if (username.isBlank()) {
                return ServerResponse.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "status", false,
                        "message", "Query parameter 'username' is required"
                ));
            }

            List<Map<String, Object>> results = userService.searchUsers(username).stream()
                    .map(this::toSummary)
                    .toList();

            return ServerResponse.ok().body(Map.of(
                    "status", true,
                    "message", "Users fetched successfully",
                    "data", results
            ));
        } catch (Exception e) {
            return ServerResponse.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", false,
                    "message", e.getMessage()
            ));
        }
    }

    private Map<String, Object> toSummary(User user) {
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "status", user.getStatus().name()
        );
    }
}
