package com.example.chat.router;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import com.example.chat.controller.MessageController;

@Configuration
public class MessageRouter {

    @Bean
    public RouterFunction<ServerResponse> messageRoutes(MessageController messageController) {
        return RouterFunctions.route()
                .POST("/api/messages", messageController::sendMessage)
                .GET("/api/conversations/{conversationId}/messages", messageController::getMessageHistory)
                .POST("/api/conversations/{conversationId}/messages/read", messageController::markAsRead)
                .build();
    }
}
