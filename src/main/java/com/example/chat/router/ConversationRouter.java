package com.example.chat.router;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import com.example.chat.controller.ConversationController;

@Configuration
public class ConversationRouter {

    @Bean
    public RouterFunction<ServerResponse> conversationRoutes(ConversationController conversationController) {
        return RouterFunctions.route()
                .POST("/api/conversations", conversationController::createConversation)
                .GET("/api/conversations", conversationController::getConversations)
                .DELETE("/api/conversations/{id}", conversationController::deleteConversation)
                .build();
    }
}
