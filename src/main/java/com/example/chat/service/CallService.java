package com.example.chat.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.chat.entity.Conversation;
import com.example.chat.entity.User;

@Service
public class CallService {

    private final ConversationService conversationService;

    public CallService(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    /**
     * Emails (= WebSocket principal names) of everyone in the conversation except
     * the sender. Throws AccessDeniedException if the sender is not a participant,
     * so nobody can ring or send signals into a conversation they don't belong to.
     */
    @Transactional(readOnly = true)
    public List<String> otherParticipantEmails(Long conversationId, Long senderId) {
        conversationService.assertUserIsParticipant(conversationId, senderId);
        Conversation conversation = conversationService.getConversationById(conversationId);
        return conversation.getParticipants().stream()
                .filter(p -> !p.getId().equals(senderId))
                .map(User::getEmail)
                .toList();
    }
}
