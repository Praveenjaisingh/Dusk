package com.example.chat.dto;

import jakarta.validation.constraints.NotNull;

public class TypingRequest {

    @NotNull(message = "conversationId is required")
    private Long conversationId;

    public TypingRequest() {
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }
}
