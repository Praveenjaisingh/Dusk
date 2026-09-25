package com.example.chat.router;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import com.example.chat.controller.CallController;

@Configuration
public class CallRouter {

    @Bean
    public RouterFunction<ServerResponse> callRoutes(CallController callController) {
        return RouterFunctions.route()
                .GET("/api/calls/ice-servers", callController::iceServers)
                .build();
    }
}
