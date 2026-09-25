package com.example.chat.ws;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Controller;

import com.example.chat.dto.CallSignal;
import com.example.chat.entity.User;
import com.example.chat.exception.ValidationException;
import com.example.chat.service.CallService;
import com.example.chat.service.UserService;
import com.example.chat.util.RequestValidator;

/**
 * WebRTC signaling relay for voice and video calls.
 *
 * The audio/video itself flows browser-to-browser; this class only forwards the
 * small signaling messages (invite / accept / offer / answer / ICE candidates / end).
 *
 * Messages are delivered to each recipient's *private* user queue
 * (/user/queue/call), never to a shared topic, because they carry network
 * details of the caller.
 */
@Controller
public class CallSignalingController {

    /** For these, the sender's own other sessions (e.g. a second browser tab) are told too, so they stop ringing. */
    private static final Set<String> ECHO_TO_SENDER = Set.of("accept", "reject");

    private static final String CALL_QUEUE = "/queue/call";

    private final CallService callService;
    private final UserService userService;
    private final RequestValidator requestValidator;
    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry userRegistry;

    public CallSignalingController(CallService callService,
                                   UserService userService,
                                   RequestValidator requestValidator,
                                   SimpMessagingTemplate messagingTemplate,
                                   SimpUserRegistry userRegistry) {
        this.callService = callService;
        this.userService = userService;
        this.requestValidator = requestValidator;
        this.messagingTemplate = messagingTemplate;
        this.userRegistry = userRegistry;
    }

    @MessageMapping("/call.signal")
    public void signal(CallSignal payload, Principal principal) {
        try {
            requestValidator.validate(payload);

            // principal.getName() is the authenticated user's email (see JwtChannelInterceptor).
            User sender = userService.getUserByEmail(principal.getName());
            List<String> recipients = callService.otherParticipantEmails(payload.getConversationId(), sender.getId());

            // fromUserId / fromUsername always come from the authenticated session, never from the client.
            Map<String, Object> outbound = new HashMap<>();
            outbound.put("conversationId", payload.getConversationId());
            outbound.put("type", payload.getType());
            outbound.put("callId", payload.getCallId());
            outbound.put("callType", payload.getCallType());
            outbound.put("sdp", payload.getSdp());
            outbound.put("candidate", payload.getCandidate());
            outbound.put("fromUserId", sender.getId());
            outbound.put("fromUsername", sender.getUsername());

            if ("invite".equals(payload.getType())) {
                boolean anyoneOnline = recipients.stream().anyMatch(email -> userRegistry.getUser(email) != null);
                if (!anyoneOnline) {
                    // Nobody to ring: tell the caller right away instead of letting them wait for the timeout.
                    Map<String, Object> unavailable = new HashMap<>();
                    unavailable.put("conversationId", payload.getConversationId());
                    unavailable.put("type", "unavailable");
                    unavailable.put("callId", payload.getCallId());
                    messagingTemplate.convertAndSendToUser(principal.getName(), CALL_QUEUE, unavailable);
                    return;
                }
            }

            for (String email : recipients) {
                messagingTemplate.convertAndSendToUser(email, CALL_QUEUE, outbound);
            }
            if (ECHO_TO_SENDER.contains(payload.getType())) {
                messagingTemplate.convertAndSendToUser(principal.getName(), CALL_QUEUE, outbound);
            }

        } catch (Exception ex) {
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    Map.of("status", false, "message", describe(ex))
            );
        }
    }

    private String describe(Exception ex) {
        if (ex instanceof ValidationException ve && !ve.getErrors().isEmpty()) {
            return ve.getErrors().values().stream().collect(Collectors.joining("; "));
        }
        return ex.getMessage() != null ? ex.getMessage() : "Call signaling failed";
    }
}
