package com.portfolio.memo.chat.message;

import com.portfolio.memo.chat.message.dto.ChatMessageDto;
import com.portfolio.memo.chat.message.dto.EditMessageRequest;
import com.portfolio.memo.chat.message.dto.SendMessageRequest;
import com.portfolio.memo.chat.message.dto.SendMessageResponse;
import com.portfolio.memo.common.jwt.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms")
public class ChatMessageController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatMessageService chatMessageService;

    @PostMapping("/{roomId}/messages")
    public ResponseEntity<SendMessageResponse> sendMessage(
            @PathVariable Long roomId,
            @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal CustomUserPrincipal currentUser
    ) {
        SendMessageResponse response = chatMessageService.send(roomId, request, currentUser);
        messagingTemplate.convertAndSend("/topic/chat/rooms/" + roomId, response);

        return ResponseEntity.ok(response);
    }


//    // 메시지 읽음 처리 엔드포인트
//    // messageId, roomId를 보내면 서버는 읽음 처리를 하고 해당 방 구독자에게 읽음 상태가 업데이트되었음을 알림
//    @MessageMapping("/chat.readMessage")
//    public void readMessage(@Payload ReadMessageRequest request, @AuthenticationPrincipal CustomUserPrincipal currentUser) {
//
//        Long userId = currentUser.getUserId();
//
//        // 1. 읽음 상태 저장
//        ReadMessageResponse response = chatMessageReadStatusService.markAsRead(request.getMessageId(), userId);
//
//        // 2. 업데이트된 readCount를 브로드캐스트
//        messagingTemplate.convertAndSend("/topic/chat/rooms/" + request.getRoomId() + "/read", response);
//    }


    // 메시지 수정 엔드포인트
    @PatchMapping("/{roomId}/messages/{messageId}")
    public ResponseEntity<ChatMessageDto> editMessage(
            @PathVariable Long roomId,
            @PathVariable Long messageId,
            @RequestBody EditMessageRequest editRequest,
            @AuthenticationPrincipal CustomUserPrincipal currentUser) {

        // 1. 서비스 호출하여 메시지 수정 및 브로드캐스팅할 수정된 메시지 DTO 받음
        ChatMessageDto updatedMessage = chatMessageService.editMessage(
                roomId,
                messageId,
                editRequest,
                currentUser
        );

        // 2. WebSocket으로 채팅방에 있는 모든 클라이언트에게 수정된 메시지 전송
        String destination = "/topic/chat/rooms/" + updatedMessage.getRoomId();
        messagingTemplate.convertAndSend(destination, updatedMessage);

        return ResponseEntity.ok(updatedMessage);
    }

    // 메시지 삭제 엔드포인트
    @DeleteMapping("/{roomId}/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long roomId,
            @PathVariable Long messageId,
            @AuthenticationPrincipal CustomUserPrincipal currentUser) {

        // 1. 서비스 호출하여 메시지 삭제 및 브로드캐스팅할 DTO 받음
        ChatMessageDto deletedMessage = chatMessageService.deleteMessage(roomId, messageId, currentUser);

        // 2. WebSocket 토픽으로 메시지 삭제 정보 전송
        String destination = "/topic/chat/rooms/" + deletedMessage.getRoomId();
        messagingTemplate.convertAndSend(destination, deletedMessage);

        return ResponseEntity.ok().build();
    }
}




