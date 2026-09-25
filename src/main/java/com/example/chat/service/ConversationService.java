package com.example.chat.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.chat.entity.Conversation;
import com.example.chat.entity.User;
import com.example.chat.exception.ResourceNotFoundException;
import com.example.chat.exception.ValidationException;
import com.example.chat.repository.ConversationRepository;
import com.example.chat.repository.UserRepository;

import java.util.Map;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    public ConversationService(ConversationRepository conversationRepository, UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Conversation createOrGetDirectConversation(Long currentUserId, Long otherUserId) {
        if (currentUserId.equals(otherUserId)) {
            throw new ValidationException(Map.of("userId", "You cannot start a conversation with yourself"));
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));
        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + otherUserId));

        List<Conversation> existing = conversationRepository.findDirectConversationBetween(currentUserId, otherUserId);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        Conversation conversation = new Conversation();
        conversation.getParticipants().add(currentUser);
        conversation.getParticipants().add(otherUser);
        return conversationRepository.save(conversation);
    }

    @Transactional(readOnly = true)
    public List<Conversation> getConversationsForUser(Long userId) {
        return conversationRepository.findAllByParticipantId(userId);
    }

    @Transactional(readOnly = true)
    public Conversation getConversationById(Long id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public void assertUserIsParticipant(Long conversationId, Long userId) {
        Conversation conversation = getConversationById(conversationId);
        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getId().equals(userId));
        if (!isParticipant) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You are not a participant of this conversation");
        }
    }

    @Transactional
    public void deleteConversation(Long conversationId, Long userId) {
        assertUserIsParticipant(conversationId, userId);
        conversationRepository.deleteById(conversationId);
    }
}
