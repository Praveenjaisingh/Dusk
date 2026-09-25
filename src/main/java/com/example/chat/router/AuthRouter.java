package com.example.chat.router;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import com.example.chat.controller.AuthController;

@Configuration
public class AuthRouter {

    @Bean
    public RouterFunction<ServerResponse> authRoutes(AuthController authController) {
        return RouterFunctions.route()
                .POST("/api/auth/register", authController::register)
                .POST("/api/auth/login", authController::login)
                .POST("/api/auth/forgot-password", authController::forgotPassword)
                .POST("/api/auth/reset-password", authController::resetPassword)
                .build();
    }
}
