package com.example.chat.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.chat.entity.User;
import com.example.chat.exception.ResourceNotFoundException;
import com.example.chat.exception.ValidationException;
import com.example.chat.repository.UserRepository;

import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<User> searchUsers(String username) {
        if (username == null || username.isBlank()) {
            return List.of();
        }
        return userRepository.findByUsernameContainingIgnoreCase(username.trim());
    }

    @Transactional
    public void setStatus(Long userId, User.Status status) {
        User user = getUserById(userId);
        user.setStatus(status);
        userRepository.save(user);
    }

    /**
     * Updates the current user's display username, rejecting the change if
     * another account already has it.
     */
    @Transactional
    public User updateUsername(Long userId, String newUsername) {
        User user = getUserById(userId);

        String trimmed = newUsername.trim();
        if (!trimmed.equalsIgnoreCase(user.getUsername()) && userRepository.existsByUsername(trimmed)) {
            throw new ValidationException(Map.of("username", "Username is already taken"));
        }

        user.setUsername(trimmed);
        return userRepository.save(user);
    }

    /** Updates the current user's avatar to a freshly uploaded file's URL. */
    @Transactional
    public User updateAvatar(Long userId, String avatarUrl) {
        User user = getUserById(userId);
        user.setAvatarUrl(avatarUrl);
        return userRepository.save(user);
    }

    /**
     * Changes the current user's password. Only succeeds when currentPassword
     * matches the password already on file, so an attacker with a stolen
     * session token still cannot lock the real owner out of their account.
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ValidationException(Map.of("currentPassword", "Current password is incorrect"));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
