package com.portfolio.memo.chatroom.message.dto;

import com.portfolio.memo.chatroom.message.ChatRoomMessage;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// 서버 -> 클라이언트로 메시지 전송에 사용
public class ChatMessageHistoryDto {
    private Long messageId;
    private String senderName;
    private String message;
    private LocalDateTime sentAt;

    public static ChatMessageHistoryDto from(ChatRoomMessage entity) {
        return ChatMessageHistoryDto.builder()
                .messageId(entity.getId())
                .senderName(entity.getSender().getName())
                .message(entity.getMessage())
                .sentAt(entity.getSentAt())
                .build();
    }
}
