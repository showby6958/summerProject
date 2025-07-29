package com.portfolio.memo.chatroom;

import com.portfolio.memo.chatroom.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatRoomService chatRoomService;
    private final SimpMessageSendingOperations messagingTemplate;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(ChatMessageDto chatMessageDto, Authentication authentication) {
        // 1. 현재 인증된 사용자의 이메일 가져오기
        String senderEmail = authentication.getName();

        // 2. 메시지를 DB에 저장
        ChatRoomMessage saveMessage = chatRoomService.saveMessage(
                chatMessageDto.getRoomId(),
                senderEmail,
                chatMessageDto.getMessage()
        );

        // 3. DTO를 다시 구성해서 클라이언트에 전송 (보낸 사람 이름, 시간 정보 등 포함)
        ChatMessageDto sendMessageDto = ChatMessageDto.builder()
                .roomId(saveMessage.getChatRoom().getId())
                .sender(saveMessage.getSender().getName())
                .message(saveMessage.getMessage())
                .sentAt(saveMessage.getSentAt().toString())
                .build();

        // 4. 해당 채팅방을 구독하는 모든 클라이언트에게 메시지 브로드캐스팅
        messagingTemplate.convertAndSend("/topic/chat/rooms/" + saveMessage.getChatRoom().getId(), sendMessageDto);

    }
}
