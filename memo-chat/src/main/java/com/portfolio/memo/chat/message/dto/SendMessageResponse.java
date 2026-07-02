package com.portfolio.memo.chat.message.dto;

import com.portfolio.memo.chat.message.ChatMessage;
import lombok.*;

import java.time.LocalDateTime;

// 메시지 저장 결과 응답 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendMessageResponse {
    private Long messageId;
    private Long roomId;
    private Long senderId;
    private String senderName;
    private String content;
    private LocalDateTime sentAt;

    public static SendMessageResponse from(ChatMessage saved) {
        return SendMessageResponse.builder()
                .messageId(saved.getId())
                .roomId(saved.getRoomId())
                .senderId(saved.getSenderId())
                .senderName(saved.getSenderName())
                .content(saved.getContent())
                .sentAt(saved.getSentAt())
                .build();
    }
}
