package com.example.chat.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class MessageRequest {

    @NotNull(message = "conversationId is required")
    private Long conversationId;

    @Size(max = 4000, message = "content must be at most 4000 characters")
    private String content;

    private String messageType = "CHAT";

    // Optional attachment, set after uploading a file via POST /api/media/upload.
    private String attachmentUrl;
    private String attachmentType;
    private String attachmentFileName;
    private Long attachmentSize;

    // Voice messages: recording length in seconds.
    @Min(value = 0, message = "attachmentDuration must not be negative")
    @Max(value = 86400, message = "attachmentDuration is too large")
    private Integer attachmentDuration;

    public MessageRequest() {
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public String getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(String attachmentType) {
        this.attachmentType = attachmentType;
    }

    public String getAttachmentFileName() {
        return attachmentFileName;
    }

    public void setAttachmentFileName(String attachmentFileName) {
        this.attachmentFileName = attachmentFileName;
    }

    public Long getAttachmentSize() {
        return attachmentSize;
    }

    public void setAttachmentSize(Long attachmentSize) {
        this.attachmentSize = attachmentSize;
    }

    public Integer getAttachmentDuration() {
        return attachmentDuration;
    }

    public void setAttachmentDuration(Integer attachmentDuration) {
        this.attachmentDuration = attachmentDuration;
    }

    public boolean hasAttachment() {
        return attachmentUrl != null && !attachmentUrl.isBlank();
    }
}
