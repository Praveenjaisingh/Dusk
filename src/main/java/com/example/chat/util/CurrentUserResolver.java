package com.example.chat.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.chat.entity.User;
import com.example.chat.service.UserService;

@Component
public class CurrentUserResolver {

    private final UserService userService;

    public CurrentUserResolver(UserService userService) {
        this.userService = userService;
    }

    /**
     * The JwtAuthenticationFilter authenticates using the user's email as the
     * Spring Security "username", so the Authentication name here is the email.
     */
    public User resolve() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.core.AuthenticationException("Not authenticated") {
            };
        }
        String email = authentication.getName();
        return userService.getUserByEmail(email);
    }
}
