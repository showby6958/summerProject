package com.portfolio.memo.chatroom.message.dto;

import com.portfolio.memo.chatroom.message.ChatRoomMessage;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.format.DateTimeFormatter;

@Setter
@Getter
@NoArgsConstructor
// 클라이언트 -> 서버로 메시지 전송에 사용
public class ChatMessageDto {
    private String type;
    private Long roomId;
    private String senderName;
    private String message;
    private String sentAt;
    private String editedAt;
    private Long messageId;
    private boolean isDeleted;

    @Builder
    public ChatMessageDto(String type, Long roomId, String senderName, String message, String sentAt, String editedAt, Long messageId, boolean isDeleted) {
        this.type = type;
        this.roomId = roomId;
        this.messageId = messageId;
        this.senderName = senderName;
        this.message = message;
        this.sentAt = sentAt;
        this.editedAt = editedAt;
        this.isDeleted = isDeleted;

    }

    public static ChatMessageDto from(ChatRoomMessage chatRoomMessage) {
        // unreadCount는 서비스 레이어에서 계산 후 설정

        // editedAt -> 수정된 시간이 있다면 해당 시간을, 없다면 null 반환
        String editedTime = chatRoomMessage.getEditedAt() != null ? chatRoomMessage.getEditedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;

        return ChatMessageDto.builder()
                .messageId(chatRoomMessage.getId())
                .roomId(chatRoomMessage.getChatRoom().getId())
                .senderName(chatRoomMessage.getSender().getName())
                .message(chatRoomMessage.getMessage())
                .sentAt(chatRoomMessage.getSentAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .editedAt(editedTime)
                .build();
    }
}
