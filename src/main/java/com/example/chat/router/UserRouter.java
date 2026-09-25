package com.example.chat.router;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import com.example.chat.controller.UserController;

@Configuration
public class UserRouter {

    @Bean
    public RouterFunction<ServerResponse> userRoutes(UserController userController) {
        return RouterFunctions.route()
                .GET("/api/users/me", userController::getCurrentUser)
                .PATCH("/api/users/me", userController::updateUsername)
                .POST("/api/users/me/avatar", userController::uploadAvatar)
                .PUT("/api/users/me/password", userController::changePassword)
                .GET("/api/users/search", userController::searchUsers)
                .build();
    }
}
