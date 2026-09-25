package com.example.chat.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import com.example.chat.dto.ChangePasswordRequest;
import com.example.chat.dto.UpdateUsernameRequest;
import com.example.chat.entity.User;
import com.example.chat.exception.ValidationException;
import com.example.chat.service.FileStorageService;
import com.example.chat.service.UserService;
import com.example.chat.util.CurrentUserResolver;
import com.example.chat.util.RequestValidator;

import jakarta.servlet.http.Part;

@Component
public class UserController {

    private final UserService userService;
    private final CurrentUserResolver currentUserResolver;
    private final RequestValidator requestValidator;
    private final FileStorageService fileStorageService;

    public UserController(UserService userService,
                           CurrentUserResolver currentUserResolver,
                           RequestValidator requestValidator,
                           FileStorageService fileStorageService) {
        this.userService = userService;
        this.currentUserResolver = currentUserResolver;
        this.requestValidator = requestValidator;
        this.fileStorageService = fileStorageService;
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

    /** PATCH /api/users/me — updates the current user's username. */
    public ServerResponse updateUsername(ServerRequest request) throws Exception {
        try {
            UpdateUsernameRequest body = request.body(UpdateUsernameRequest.class);
            requestValidator.validate(body);

            User currentUser = currentUserResolver.resolve();
            User updated = userService.updateUsername(currentUser.getId(), body.getUsername());

            return ServerResponse.ok().body(Map.of(
                    "status", true,
                    "message", "Username updated successfully",
                    "data", toSummary(updated)
            ));
        } catch (ValidationException ex) {
            return ServerResponse.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", false,
                    "message", "Validation failed",
                    "errors", ex.getErrors()
            ));
        } catch (Exception e) {
            return ServerResponse.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", false,
                    "message", e.getMessage()
            ));
        }
    }

    /** POST /api/users/me/avatar — uploads a new profile image for the current user. */
    public ServerResponse uploadAvatar(ServerRequest request) throws Exception {
        try {
            User currentUser = currentUserResolver.resolve();

            MultiValueMap<String, Part> multipartData = request.multipartData();
            Part file = multipartData.getFirst("file");

            if (file == null || file.getSize() == 0) {
                throw new ValidationException(Map.of("file", "File is required"));
            }

            Map<String, Object> stored = fileStorageService.store(file);
            String category = String.valueOf(stored.get("category"));
            if (!"IMAGE".equals(category)) {
                throw new ValidationException(Map.of("file", "Profile photo must be an image"));
            }

            User updated = userService.updateAvatar(currentUser.getId(), String.valueOf(stored.get("url")));

            return ServerResponse.status(HttpStatus.CREATED).body(Map.of(
                    "status", true,
                    "message", "Profile photo updated successfully",
                    "data", toSummary(updated)
            ));
        } catch (ValidationException ex) {
            return ServerResponse.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", false,
                    "message", "Validation failed",
                    "errors", ex.getErrors()
            ));
        } catch (Exception e) {
            return ServerResponse.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", false,
                    "message", e.getMessage()
            ));
        }
    }

    /** PUT /api/users/me/password — changes the current user's password (requires the current one). */
    public ServerResponse changePassword(ServerRequest request) throws Exception {
        try {
            ChangePasswordRequest body = request.body(ChangePasswordRequest.class);
            requestValidator.validate(body);

            User currentUser = currentUserResolver.resolve();
            userService.changePassword(currentUser.getId(), body.getCurrentPassword(), body.getNewPassword());

            return ServerResponse.ok().body(Map.of(
                    "status", true,
                    "message", "Password updated successfully"
            ));
        } catch (ValidationException ex) {
            return ServerResponse.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", false,
                    "message", "Validation failed",
                    "errors", ex.getErrors()
            ));
        } catch (Exception e) {
            return ServerResponse.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", false,
                    "message", e.getMessage()
            ));
        }
    }

    private Map<String, Object> toSummary(User user) {
        Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("id", user.getId());
        summary.put("username", user.getUsername());
        summary.put("email", user.getEmail());
        summary.put("status", user.getStatus().name());
        summary.put("avatarUrl", user.getAvatarUrl());
        return summary;
    }
}
