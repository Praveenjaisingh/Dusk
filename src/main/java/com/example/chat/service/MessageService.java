package com.example.chat.service;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.chat.dto.MessageRequest;
import com.example.chat.entity.Conversation;
import com.example.chat.entity.Message;
import com.example.chat.entity.User;
import com.example.chat.exception.ResourceNotFoundException;
import com.example.chat.exception.ValidationException;
import com.example.chat.repository.MessageRepository;
import com.example.chat.repository.UserRepository;

import java.util.Map;

@Service
public class MessageService {

    // Attachments must point at a file we stored ourselves (see FileStorageService).
    // Without this check a client could send any string here (e.g. a javascript: URL
    // or a third-party link) and every recipient's browser would render it.
    private static final Pattern STORED_FILE_URL = Pattern.compile("^/uploads/[A-Za-z0-9][A-Za-z0-9._-]*$");

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ConversationService conversationService;

    public MessageService(MessageRepository messageRepository,
                           UserRepository userRepository,
                           ConversationService conversationService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.conversationService = conversationService;
    }

    @Transactional
    public Message sendMessage(Long senderId, MessageRequest request) {
        conversationService.assertUserIsParticipant(request.getConversationId(), senderId);

        boolean hasContent = request.getContent() != null && !request.getContent().isBlank();
        if (!hasContent && !request.hasAttachment()) {
            throw new ValidationException(Map.of("content", "Message must have text content or an attachment"));
        }

        if (request.hasAttachment() && !STORED_FILE_URL.matcher(request.getAttachmentUrl()).matches()) {
            throw new ValidationException(Map.of("attachmentUrl",
                    "attachmentUrl must be a file uploaded via /api/media/upload"));
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + senderId));
        Conversation conversation = conversationService.getConversationById(request.getConversationId());

        Message.MessageType type;
        try {
            type = Message.MessageType.valueOf(request.getMessageType().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(Map.of("messageType",
                    "messageType must be one of CHAT, JOIN, LEAVE, IMAGE, VIDEO, AUDIO, VOICE, FILE"));
        }

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(hasContent ? request.getContent() : "");
        message.setMessageType(type);
        message.setRead(false);

        if (request.hasAttachment()) {
            message.setAttachmentUrl(request.getAttachmentUrl());
            message.setAttachmentType(request.getAttachmentType());
            message.setAttachmentFileName(request.getAttachmentFileName());
            message.setAttachmentSize(request.getAttachmentSize());
            message.setAttachmentDuration(request.getAttachmentDuration());
        }

        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public Page<Message> getMessageHistory(Long conversationId, Long requesterId, int page, int size) {
        conversationService.assertUserIsParticipant(conversationId, requesterId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Message> getAllMessages(Long conversationId, Long requesterId) {
        conversationService.assertUserIsParticipant(conversationId, requesterId);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    @Transactional
    public int markAsRead(Long conversationId, Long requesterId) {
        conversationService.assertUserIsParticipant(conversationId, requesterId);
        return messageRepository.markConversationMessagesAsRead(conversationId, requesterId);
    }

    @Transactional(readOnly = true)
    public long countUnread(Long conversationId, Long requesterId) {
        conversationService.assertUserIsParticipant(conversationId, requesterId);
        return messageRepository.countByConversationIdAndSenderIdNotAndIsReadFalse(conversationId, requesterId);
    }
}
