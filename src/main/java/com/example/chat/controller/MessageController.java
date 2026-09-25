package com.example.chat.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import com.example.chat.dto.MessageRequest;
import com.example.chat.entity.Message;
import com.example.chat.entity.User;
import com.example.chat.exception.ResourceNotFoundException;
import com.example.chat.exception.ValidationException;
import com.example.chat.service.MessageService;
import com.example.chat.util.CurrentUserResolver;
import com.example.chat.util.RequestValidator;

@Component
public class MessageController {

    private final MessageService messageService;
    private final CurrentUserResolver currentUserResolver;
    private final RequestValidator requestValidator;

    public MessageController(MessageService messageService,
                              CurrentUserResolver currentUserResolver,
                              RequestValidator requestValidator) {
        this.messageService = messageService;
        this.currentUserResolver = currentUserResolver;
        this.requestValidator = requestValidator;
    }

    /** REST fallback for sending a message (real-time delivery normally goes through the /app/chat.send WebSocket destination). */
    public ServerResponse sendMessage(ServerRequest request) throws Exception {
        try {
            MessageRequest body = request.body(MessageRequest.class);
            requestValidator.validate(body);

            User currentUser = currentUserResolver.resolve();
            Message message = messageService.sendMessage(currentUser.getId(), body);

            return ServerResponse.status(HttpStatus.CREATED).body(Map.of(
                    "status", true,
                    "message", "Message sent successfully",
                    "data", toSummary(message)
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

    public ServerResponse getMessageHistory(ServerRequest request) throws Exception {
        try {
            Long conversationId = Long.valueOf(request.pathVariable("conversationId"));
            int page = Integer.parseInt(request.param("page").orElse("0"));
            int size = Integer.parseInt(request.param("size").orElse("20"));

            User currentUser = currentUserResolver.resolve();
            Page<Message> messages = messageService.getMessageHistory(conversationId, currentUser.getId(), page, size);

            List<Map<String, Object>> data = messages.getContent().stream()
                    .map(this::toSummary)
                    .toList();

            return ServerResponse.ok().body(Map.of(
                    "status", true,
                    "message", "Message history fetched successfully",
                    "data", data,
                    "page", messages.getNumber(),
                    "totalPages", messages.getTotalPages(),
                    "totalElements", messages.getTotalElements()
            ));
        } catch (NumberFormatException ex) {
            return ServerResponse.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", false,
                    "message", "Invalid conversationId, page or size"
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

    public ServerResponse markAsRead(ServerRequest request) throws Exception {
        try {
            Long conversationId = Long.valueOf(request.pathVariable("conversationId"));
            User currentUser = currentUserResolver.resolve();

            int updated = messageService.markAsRead(conversationId, currentUser.getId());

            return ServerResponse.ok().body(Map.of(
                    "status", true,
                    "message", "Messages marked as read",
                    "data", Map.of("updatedCount", updated)
            ));
        } catch (NumberFormatException ex) {
            return ServerResponse.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", false,
                    "message", "Invalid conversationId"
            ));
        } catch (Exception e) {
            return ServerResponse.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", false,
                    "message", e.getMessage()
            ));
        }
    }

    private Map<String, Object> toSummary(Message message) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", message.getId());
        map.put("conversationId", message.getConversation().getId());
        map.put("senderId", message.getSender().getId());
        map.put("senderUsername", message.getSender().getUsername());
        map.put("content", message.getContent());
        map.put("messageType", message.getMessageType().name());
        map.put("isRead", message.isRead());
        map.put("createdAt", message.getCreatedAt());
        map.put("attachmentUrl", message.getAttachmentUrl());
        map.put("attachmentType", message.getAttachmentType());
        map.put("attachmentFileName", message.getAttachmentFileName());
        map.put("attachmentSize", message.getAttachmentSize());
        map.put("attachmentDuration", message.getAttachmentDuration());
        return map;
    }
}
