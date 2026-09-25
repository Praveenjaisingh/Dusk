package com.example.chat.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.chat.entity.User;
import com.example.chat.exception.ResourceNotFoundException;
import com.example.chat.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
}
