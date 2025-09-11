package com.portfolio.memo.chatroom.message;

import com.portfolio.memo.auth.CustomUserDetails;
import com.portfolio.memo.auth.CustomUserDetailsService;
import com.portfolio.memo.auth.User;
import com.portfolio.memo.auth.UserRepository;
import com.portfolio.memo.chatroom.message.dto.ChatMessageDto;
import com.portfolio.memo.chatroom.message.dto.MessageEditRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageReadStatusRepository readStatusRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ChatMessageDto markAsRead(Long messageId, String userEmail) {
        // 1. 필수 엔티티 조회
        ChatRoomMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("메세지를 찾지 못했습니다: " + messageId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다: " + userEmail));

        // 2. 이미 읽었는지 확인하여 중복 저장 방지
        boolean alreadyRead = readStatusRepository.existsByChatMessageAndUser(message, user);

        if (!alreadyRead) {
            // 3. 읽지 않았다면, 새로운 읽음 상태(ChatMessageReadStatus)를 생성하고 저장
            ChatMessageReadStatus readStatus = new ChatMessageReadStatus(message, user);
            readStatusRepository.save(readStatus);
        }

        // 4. 업데이트된 '안 읽은 사람 수' 계산
        // unreadCount = 채팅방 전체 인원 - 메시지 읽은 인원
        int totalParticipants = message.getChatRoom().getParticipants().size();
        int readCount = readStatusRepository.countByChatMessage(message);
        int unreadCount = totalParticipants - readCount;

        // 5. 클라이언트에 전송할 DTO를 생성해서 반환
        return ChatMessageDto.builder()
                .type("READ")
                .messageId(messageId)
                .roomId(message.getChatRoom().getId())
                .unreadCount(unreadCount)
                .build();
    }

    @Transactional
    public ChatMessageDto editMessage(Long roomId, Long messageId, MessageEditRequestDto messageEditRequestDto, CustomUserDetails userDetails) {
        // 1. 메시지 (존재여부) 조회
        ChatRoomMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다. ID: " + messageId));

        // 2. 수정권한 확인 (메시지 작성자와 요청자가 동일한지 확인)
        if (!message.getSender().getId().equals(userDetails.getUser().getId())) {
            // Custom Exception 사용
            throw new MessageEditNotAllowedException(messageId, userDetails.getUser().getId());
        }

        // 3. 새 메시지 내용, 수정 시간 업데이트
        message.setMessage(messageEditRequestDto.getNewMessage());
        message.setEditedAt(LocalDateTime.now());

        // 4. DB에 저장 (Transactional 어노테이션으로 인해 메서드 종료 시 자동 저장)
        ChatRoomMessage updatedMessage = chatMessageRepository.save(message);

        // 5. DTO로 변환
        ChatMessageDto updatedMessageDto = ChatMessageDto.from(updatedMessage);
        updatedMessageDto.setType("EDIT"); // 클라이언트가 메시지 수정을 인지하도록 타입 설정

        // 6. WebSocket으로 채팅방에 있는 모든 클라이언트에게 수정된 메시지 전송
        String destination = "topic/chat/room/" + updatedMessage.getChatRoom().getId();
        messagingTemplate.convertAndSend(destination, updatedMessageDto);

        // 7. 수정된 메시지 정보 반환
        return updatedMessageDto;
    }

    @Transactional
    public void deleteMessage(Long roomId, Long messageId, CustomUserDetails userDetails) {

        // 1. 메시지 조회
        ChatRoomMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다." + messageId));

        // 2. 삭제 권한 확인(메시지 작성자와 요청자가 동일한지)
        if (!message.getSender().getId().equals(userDetails.getUser().getId())) {
            throw new MessageDeleteNotAllowed(messageId, userDetails.getUser().getId());
        }

        // 3. Soft Delete 방식 (나중에 soft + hard delete 형식으로 변경 )
        message.setMessage("삭제된 메시지입니다.");
        message.setDeletedAt(LocalDateTime.now());

        // 4. DB에 저장
        ChatRoomMessage deleteMessage = chatMessageRepository.save(message);

        // 5. DTO로 변환하여 WebSocket 전송
        ChatMessageDto deleteMessageDto = ChatMessageDto.from(deleteMessage);
        deleteMessageDto.setType("DELETE"); // 클라이언트가 메시지 삭제를 인지하도록 타입 설정

        String destination = "/topic/chat/rooms/" + deleteMessage.getChatRoom().getId();
        messagingTemplate.convertAndSend(destination, deleteMessageDto);
    }
}
