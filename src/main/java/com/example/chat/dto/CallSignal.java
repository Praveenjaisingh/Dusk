package com.example.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * One WebRTC signaling message, sent to /app/call.signal and relayed to the
 * other participant(s) of the conversation on /user/queue/call.
 *
 * Call flow:
 *   caller: invite  -> callee: accept | reject | busy
 *   caller: offer   -> callee: answer
 *   both:   candidate (many)
 *   either: end
 */
public class CallSignal {

    @NotNull(message = "conversationId is required")
    private Long conversationId;

    @NotBlank(message = "type is required")
    @Pattern(regexp = "invite|accept|reject|busy|offer|answer|candidate|end",
            message = "type must be one of invite, accept, reject, busy, offer, answer, candidate, end")
    private String type;

    /** Random id chosen by the caller; lets clients ignore signals from a stale/other call. */
    @NotBlank(message = "callId is required")
    @Size(max = 64, message = "callId must be at most 64 characters")
    private String callId;

    @Pattern(regexp = "audio|video", message = "callType must be audio or video")
    private String callType;

    /** Session description (offer / answer). */
    @Size(max = 30000, message = "sdp is too large")
    private String sdp;

    /** JSON-serialised RTCIceCandidate. */
    @Size(max = 4000, message = "candidate is too large")
    private String candidate;

    public CallSignal() {
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getCallType() {
        return callType;
    }

    public void setCallType(String callType) {
        this.callType = callType;
    }

    public String getSdp() {
        return sdp;
    }

    public void setSdp(String sdp) {
        this.sdp = sdp;
    }

    public String getCandidate() {
        return candidate;
    }

    public void setCandidate(String candidate) {
        this.candidate = candidate;
    }
}
