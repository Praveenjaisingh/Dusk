package com.example.chat.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import com.example.chat.dto.CreateConversationRequest;
import com.example.chat.entity.Conversation;
import com.example.chat.entity.User;
import com.example.chat.exception.ResourceNotFoundException;
import com.example.chat.exception.ValidationException;
import com.example.chat.service.ConversationService;
import com.example.chat.util.CurrentUserResolver;
import com.example.chat.util.RequestValidator;

@Component
public class ConversationController {

    private final ConversationService conversationService;
    private final CurrentUserResolver currentUserResolver;
    private final RequestValidator requestValidator;

    public ConversationController(ConversationService conversationService,
                                   CurrentUserResolver currentUserResolver,
                                   RequestValidator requestValidator) {
        this.conversationService = conversationService;
        this.currentUserResolver = currentUserResolver;
        this.requestValidator = requestValidator;
    }

    public ServerResponse createConversation(ServerRequest request) throws Exception {
        try {
            CreateConversationRequest body = request.body(CreateConversationRequest.class);
            requestValidator.validate(body);

            User currentUser = currentUserResolver.resolve();
            Conversation conversation = conversationService.createOrGetDirectConversation(currentUser.getId(), body.getUserId());

            return ServerResponse.status(HttpStatus.CREATED).body(Map.of(
                    "status", true,
                    "message", "Conversation created successfully",
                    "data", toSummary(conversation)
            ));
        } catch (ValidationException ex) {
            return ServerResponse.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", false,
                    "message", "Validation failed",
                    "errors", ex.getErrors()
            ));
        } catch (ResourceNotFoundException ex) {
            return ServerResponse.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", false,
                    "message", ex.getMessage()
            ));
        } catch (Exception e) {
            return ServerResponse.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", false,
                    "message", e.getMessage()
            ));
        }
    }

    public ServerResponse getConversations(ServerRequest request) throws Exception {
        try {
            User currentUser = currentUserResolver.resolve();
            List<Map<String, Object>> conversations = conversationService.getConversationsForUser(currentUser.getId())
                    .stream()
                    .map(this::toSummary)
                    .toList();

            return ServerResponse.ok().body(Map.of(
                    "status", true,
                    "message", "Conversations fetched successfully",
                    "data", conversations
            ));
        } catch (Exception e) {
            return ServerResponse.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", false,
                    "message", e.getMessage()
            ));
        }
    }

    public ServerResponse deleteConversation(ServerRequest request) throws Exception {
        try {
            Long conversationId = Long.valueOf(request.pathVariable("id"));
            User currentUser = currentUserResolver.resolve();

            conversationService.deleteConversation(conversationId, currentUser.getId());

            return ServerResponse.ok().body(Map.of(
                    "status", true,
                    "message", "Conversation deleted successfully"
            ));
        } catch (NumberFormatException ex) {
            return ServerResponse.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", false,
                    "message", "Invalid conversation id"
            ));
        } catch (ResourceNotFoundException ex) {
            return ServerResponse.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", false,
                    "message", ex.getMessage()
            ));
        } catch (AccessDeniedException ex) {
            return ServerResponse.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", false,
                    "message", ex.getMessage()
            ));
        } catch (Exception e) {
            return ServerResponse.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", false,
                    "message", e.getMessage()
            ));
        }
    }

    private Map<String, Object> toSummary(Conversation conversation) {
        List<Map<String, Object>> participants = conversation.getParticipants().stream()
                .map(p -> {
                    Map<String, Object> summary = new java.util.HashMap<>();
                    summary.put("id", p.getId());
                    summary.put("username", p.getUsername());
                    summary.put("status", p.getStatus().name());
                    summary.put("avatarUrl", p.getAvatarUrl());
                    return summary;
                })
                .toList();

        return Map.of(
                "id", conversation.getId(),
                "participants", participants,
                "createdAt", conversation.getCreatedAt(),
                "updatedAt", conversation.getUpdatedAt()
        );
    }
}
