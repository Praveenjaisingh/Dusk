package com.example.chat.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.chat.dto.LoginRequest;
import com.example.chat.dto.RegisterRequest;
import com.example.chat.entity.PasswordResetToken;
import com.example.chat.entity.User;
import com.example.chat.exception.ValidationException;
import com.example.chat.repository.PasswordResetTokenRepository;
import com.example.chat.repository.UserRepository;
import com.example.chat.security.JwtService;

@Service
public class AuthService {

    /** How long a "forgot password" link stays valid, in minutes. */
    private static final long RESET_TOKEN_VALID_MINUTES = 10;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final String frontendBaseUrl;

    public AuthService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        AuthenticationManager authenticationManager,
                        UserDetailsService userDetailsService,
                        PasswordResetTokenRepository passwordResetTokenRepository,
                        EmailService emailService,
                        @Value("${app.frontend-base-url:http://localhost:8080}") String frontendBaseUrl) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException(Map.of("email", "Email is already registered"));
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ValidationException(Map.of("username", "Username is already taken"));
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.Role.USER);
        user.setStatus(User.Status.OFFLINE);

        return userRepository.save(user);
    }

    public String login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        var userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        return jwtService.generateToken(userDetails);
    }

    /**
     * Kicks off the "forgot password" flow: if the email belongs to an account,
     * emails a one-time reset link valid for {@value #RESET_TOKEN_VALID_MINUTES}
     * minutes. Silently does nothing when the email is unknown, so callers must
     * always show the same generic message and never reveal whether an account
     * exists for a given address.
     */
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            // Any older, still-active link for this user stops working once a new one is requested.
            passwordResetTokenRepository.invalidateActiveTokensForUser(user.getId());

            String token = UUID.randomUUID().toString();
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(RESET_TOKEN_VALID_MINUTES);
            passwordResetTokenRepository.save(new PasswordResetToken(token, user, expiresAt));

            String resetLink = frontendBaseUrl + "/index.html?resetToken=" + token;
            emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), resetLink);
        });
    }

    /**
     * Completes the "forgot password" flow: validates the token (must exist,
     * be unused and not expired) and sets the new password.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new ValidationException(Map.of("token", "This reset link is invalid")));

        if (!resetToken.isValid()) {
            throw new ValidationException(Map.of("token", "This reset link has expired or was already used"));
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }
}
