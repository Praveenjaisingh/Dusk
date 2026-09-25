package com.example.chat.ws;

import java.security.Principal;
import java.util.Map;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.example.chat.dto.MessageRequest;
import com.example.chat.dto.TypingRequest;
import com.example.chat.entity.Message;
import com.example.chat.entity.User;
import com.example.chat.service.MessageService;
import com.example.chat.service.UserService;
import com.example.chat.util.RequestValidator;

/**
 * Real-time messaging over STOMP/WebSocket.
 *
 * Client connects to /ws, then sends to /app/chat.send with a MessageRequest
 * payload. The saved message is broadcast to every subscriber of
 * /topic/conversation/{conversationId}.
 */
@Controller
public class ChatWebSocketController {

    private final MessageService messageService;
    private final UserService userService;
    private final RequestValidator requestValidator;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(MessageService messageService,
                                    UserService userService,
                                    RequestValidator requestValidator,
                                    SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
        this.userService = userService;
        this.requestValidator = requestValidator;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.send")
    public void sendMessage(MessageRequest payload, Principal principal) {
        try {
            requestValidator.validate(payload);

            // principal.getName() is the authenticated user's email (see JwtChannelInterceptor / CustomUserDetailsService).
            User sender = userService.getUserByEmail(principal.getName());

            Message saved = messageService.sendMessage(sender.getId(), payload);

            Map<String, Object> outbound = new java.util.HashMap<>();
            outbound.put("id", saved.getId());
            outbound.put("conversationId", saved.getConversation().getId());
            outbound.put("senderId", saved.getSender().getId());
            outbound.put("senderUsername", saved.getSender().getUsername());
            outbound.put("content", saved.getContent());
            outbound.put("messageType", saved.getMessageType().name());
            outbound.put("createdAt", saved.getCreatedAt().toString());
            outbound.put("attachmentUrl", saved.getAttachmentUrl());
            outbound.put("attachmentType", saved.getAttachmentType());
            outbound.put("attachmentFileName", saved.getAttachmentFileName());
            outbound.put("attachmentSize", saved.getAttachmentSize());
            outbound.put("attachmentDuration", saved.getAttachmentDuration());

            messagingTemplate.convertAndSend(
                    "/topic/conversation/" + saved.getConversation().getId(), outbound);

        } catch (Exception ex) {
            // Notify only the sending user of the failure, on their private queue.
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    Map.of("status", false, "message", ex.getMessage())
            );
        }
    }

    @MessageMapping("/chat.typing")
    public void typing(TypingRequest payload, Principal principal) {
        requestValidator.validate(payload);
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + payload.getConversationId() + "/typing",
                Map.of("username", principal.getName())
        );
    }
}
