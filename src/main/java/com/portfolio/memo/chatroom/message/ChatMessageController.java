package com.portfolio.memo.chatroom.message;

import com.portfolio.memo.auth.CustomUserDetails;
import com.portfolio.memo.chatroom.message.dto.ChatMessageDto;
import com.portfolio.memo.chatroom.message.dto.ChatMessageHistoryDto;
import com.portfolio.memo.chatroom.message.dto.MessageEditRequestDto;
import com.portfolio.memo.chatroom.message.dto.ReadMessageRequest;
import com.portfolio.memo.chatroom.room.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatRoomService chatRoomService;
    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatMessageService chatMessageService;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(ChatMessageDto chatMessageDto, Authentication authentication) {
        // 1. 현재 인증된 사용자의 이메일 가져오기
        String senderEmail = authentication.getName();

        // 2. 메시지를 DB에 저장
        ChatRoomMessage savedMessage = chatRoomService.saveMessage(
                chatMessageDto.getRoomId(),
                senderEmail,
                chatMessageDto.getMessage()
        );

        // 3. DTO를 다시 구성해서 클라이언트에 전송 (보낸 사람 이름, 시간 정보 등 포함)
        ChatMessageHistoryDto broadcastMessage = ChatMessageHistoryDto.fromEntity(savedMessage);

        // 4. 해당 채팅방을 구독하는 모든 클라이언트에게 메시지 브로드캐스팅
        messagingTemplate.convertAndSend("/topic/chat/rooms/" + savedMessage.getChatRoom().getId(), broadcastMessage);

    }


    // 메시지 읽음 처리 엔드포인트
    // messageId, roomId를 보내면 서버는 읽음 처리를 하고 해당 방 구독자에게 읽음 상태가 업데이트되었음을 알림
    @MessageMapping("/chat.readMessage")
    public void readMessage(@Payload ReadMessageRequest request, Authentication authentication) {

        String userEmail = authentication.getName();

        // 1. chatMessageService 호출해서 메시지 읽음으로 표시, 업데이트된 정보 받음
        ChatMessageDto updatedMessageDto = chatMessageService.markAsRead(request.getMessageId(), userEmail);

        // 2. 해당 채티방의 '/read' 토픽을 구독하는 클라이언트에게 업데이트된 정보 전송
        // 클라이언트는 이 정보를 받아서 messageId에 해당하는 메시지의 unreadCount를 업데이트
        messagingTemplate.convertAndSend("/topic/chat/rooms/" + request.getRoomId(), updatedMessageDto);
    }


    // 메시지 수정 엔드포인트
    @PatchMapping("/rooms/{roomId}/messages/{messageId}")
    public ResponseEntity<ChatMessageDto> editMessage(
            @PathVariable Long roomId,
            @PathVariable Long messageId,
            @RequestBody MessageEditRequestDto messageEditRequestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ChatMessageDto updatedMessageDto = chatMessageService.editMessage(
                roomId, messageId, messageEditRequestDto, userDetails);

        return ResponseEntity.ok(updatedMessageDto);
    }
}

