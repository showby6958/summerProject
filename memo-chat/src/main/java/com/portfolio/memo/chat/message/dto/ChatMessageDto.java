package com.portfolio.memo.chat.message.dto;

import com.portfolio.memo.chat.message.ChatMessage;
import lombok.*;

import java.time.LocalDateTime;


@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// 클라이언트 -> 서버로 메시지 전송에 사용
public class ChatMessageDto {
    private String type;

    private Long messageId;
    private Long roomId;

    private Long senderId;
    private String senderName;

    private String content;
    private LocalDateTime sentAt;

    private LocalDateTime editedAt;

    private boolean deleted;
    private LocalDateTime deletedAt;


    public static ChatMessageDto from(ChatMessage m) {
        boolean deleted = m.isDeleted();

        return ChatMessageDto.builder()
                .type("MESSAGE")
                .messageId(m.getId())
                .roomId(m.getRoomId())
                .senderId(m.getSenderId())
                .senderName(m.getSenderName())
                .content(deleted ? "삭제된 메시지입니다." : m.getContent())
                .sentAt(m.getSentAt())
                .editedAt(m.getEditedAt())
                .deleted(deleted)
                .deletedAt(m.getDeletedAt())
                .build();
    }

}
