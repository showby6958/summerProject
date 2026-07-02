package com.portfolio.memo.chat.room.dto;

import com.portfolio.memo.chat.message.ChatMessage;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatHistoryMessageDto {

    private Long messageId;
    private Long roomId;

    private Long senderId;
    private String senderName;

    private String content;
    private LocalDateTime sentAt;

    private boolean deleted;

    public static ChatHistoryMessageDto from(ChatMessage entity) {
        return ChatHistoryMessageDto.builder()
                .messageId(entity.getId())
                .roomId(entity.getRoomId())
                .senderId(entity.getSenderId())
                .senderName(entity.getSenderName())
                .content(entity.getContent())
                .sentAt(entity.getSentAt())
                .deleted(entity.isDeleted())
                .build();
    }

    // 클라이언트 노출용: deleted가 true면 content "삭제된 메시지입니다."로 마스킹
    public static ChatHistoryMessageDto forClient(ChatMessage entity) {
        String content = entity.isDeleted() ? "삭제된 메시지입니다." : entity.getContent();

        return ChatHistoryMessageDto.builder()
                .messageId(entity.getId())
                .roomId(entity.getRoomId())
                .senderId(entity.getSenderId())
                .senderName(entity.getSenderName())
                .content(content)
                .sentAt(entity.getSentAt())
                .deleted(entity.isDeleted())
                .build();
    }

    // 삭제 캐시 덮어쓰기 전용:
    public static ChatHistoryMessageDto deletedPlaceholder(ChatMessage entity) {
        return ChatHistoryMessageDto.builder()
                .messageId(entity.getId())
                .roomId(entity.getRoomId())
                .senderId(entity.getSenderId())
                .senderName(entity.getSenderName())
                .content("삭제된 메시지입니다.")
                .sentAt(entity.getSentAt())
                .deleted(entity.isDeleted())
                .build();
    }
}
