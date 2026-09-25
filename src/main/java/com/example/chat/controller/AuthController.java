package com.example.chat.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import com.example.chat.dto.LoginRequest;
import com.example.chat.dto.RegisterRequest;
import com.example.chat.entity.User;
import com.example.chat.exception.ValidationException;
import com.example.chat.service.AuthService;
import com.example.chat.util.RequestValidator;

@Component
public class AuthController {

    private final AuthService authService;
    private final RequestValidator requestValidator;

    public AuthController(AuthService authService, RequestValidator requestValidator) {
        this.authService = authService;
        this.requestValidator = requestValidator;
    }

    public ServerResponse register(ServerRequest request) throws Exception {
        try {
            RegisterRequest registerRequest = request.body(RegisterRequest.class);
            requestValidator.validate(registerRequest);

            User user = authService.register(registerRequest);

            return ServerResponse.status(HttpStatus.CREATED).body(Map.of(
                    "status", true,
                    "message", "User registered successfully",
                    "data", Map.of(
                            "id", user.getId(),
                            "username", user.getUsername(),
                            "email", user.getEmail()
                    )
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

    public ServerResponse login(ServerRequest request) throws Exception {
        try {
            LoginRequest loginRequest = request.body(LoginRequest.class);
            requestValidator.validate(loginRequest);

            String token = authService.login(loginRequest);

            return ServerResponse.ok().body(Map.of(
                    "status", true,
                    "message", "Login successful",
                    "data", Map.of("token", token)
            ));
        } catch (ValidationException ex) {
            return ServerResponse.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", false,
                    "message", "Validation failed",
                    "errors", ex.getErrors()
            ));
        } catch (BadCredentialsException ex) {
            return ServerResponse.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", false,
                    "message", "Invalid email or password"
            ));
        } catch (Exception e) {
            return ServerResponse.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", false,
                    "message", e.getMessage()
            ));
        }
    }
}
