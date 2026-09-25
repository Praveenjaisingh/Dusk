package com.example.chat.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Tells the browser which STUN / TURN servers to use for calls.
 *
 * STUN is enough when both people are on ordinary home/office networks.
 * Strict corporate firewalls and some mobile carriers need a TURN relay;
 * configure one with app.webrtc.turn-* (see application.properties).
 */
@Component
public class CallController {

    private final List<String> stunUrls;
    private final List<String> turnUrls;
    private final String turnUsername;
    private final String turnCredential;

    public CallController(
            @Value("${app.webrtc.stun-urls:stun:stun.l.google.com:19302}") String stunUrls,
            @Value("${app.webrtc.turn-urls:}") String turnUrls,
            @Value("${app.webrtc.turn-username:}") String turnUsername,
            @Value("${app.webrtc.turn-credential:}") String turnCredential) {
        this.stunUrls = splitCsv(stunUrls);
        this.turnUrls = splitCsv(turnUrls);
        this.turnUsername = turnUsername;
        this.turnCredential = turnCredential;
    }

    public ServerResponse iceServers(ServerRequest request) {
        List<Map<String, Object>> servers = new ArrayList<>();

        if (!stunUrls.isEmpty()) {
            servers.add(Map.of("urls", stunUrls));
        }

        if (!turnUrls.isEmpty()) {
            Map<String, Object> turn = new HashMap<>();
            turn.put("urls", turnUrls);
            turn.put("username", turnUsername);
            turn.put("credential", turnCredential);
            servers.add(turn);
        }

        return ServerResponse.ok().body(Map.of(
                "status", true,
                "message", "ICE servers fetched successfully",
                "data", Map.of("iceServers", servers)
        ));
    }

    private static List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
