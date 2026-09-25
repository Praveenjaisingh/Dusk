package com.example.chat.router;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import com.example.chat.controller.MediaController;

@Configuration
public class MediaRouter {

    @Bean
    public RouterFunction<ServerResponse> mediaRoutes(MediaController mediaController) {
        return RouterFunctions.route()
                .POST("/api/media/upload", mediaController::upload)
                .build();
    }
}
