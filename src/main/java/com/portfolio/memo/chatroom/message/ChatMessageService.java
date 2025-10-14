package com.portfolio.memo.chatroom.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.memo.RedisSerializer;
import com.portfolio.memo.auth.CustomUserDetails;
import com.portfolio.memo.auth.CustomUserDetailsService;
import com.portfolio.memo.auth.User;
import com.portfolio.memo.auth.UserRepository;
import com.portfolio.memo.chatroom.message.dto.ChatMessageDto;
import com.portfolio.memo.chatroom.message.dto.ChatMessageHistoryDto;
import com.portfolio.memo.chatroom.message.dto.MessageEditRequestDto;
import com.portfolio.memo.chatroom.room.ChatRoom;
import com.portfolio.memo.chatroom.room.ChatRoomRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageReadStatusRepository readStatusRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, String> redisTemplate;
//    private final ObjectMapper objectMapper;
    private final RedisSerializer redisSerializer;

    private static final long CACHE_MAX_SIZE = 100; // 캐시에 저장할 최신 메시지 수


    // 전송한 메시지를 저장하는 api
    @Transactional
    public ChatRoomMessage saveMessage(Long roomId, String senderEmail, String message) {
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + senderEmail));
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("채팅방을 찾을 수 없습니다: " + roomId));

        ChatRoomMessage chatMessage = ChatRoomMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .message(message)
                .build();

        // 1. DB에 메시지 저장
        ChatRoomMessage savedMessage = chatMessageRepository.save(chatMessage);

        // 2. Redis 캐싱 및 읽음 상태 처리
        updateRedisOnNewMessage(savedMessage, chatRoom.getParticipants().size());

        // 3. 저장된 엔티티 반환
        return savedMessage;
    }

    private void updateRedisOnNewMessage(ChatRoomMessage message, int totalParticipants) {
        // 1. 대화 내역 캐시(Sorted Set)에 새 메시지 추가
        String historyKey = "chat:room:" + message.getChatRoom().getId() + ":messages";
        ChatMessageHistoryDto messageDto = ChatMessageHistoryDto.fromEntity(message);
        String messageJson = redisSerializer.serialize(messageDto);
        long score = message.getId(); // ID를 score로 사용

        // 지난 대화 내역을 시간 순서로, 그리고 특정 범위를 효율적으로 조회하기 위해 Redis의 Sorted Set 사용
        redisTemplate.opsForZSet().add(historyKey, messageJson, score);

        // 2. 캐시 크기 관리 (오래된 메시지 삭제)
        Long size = redisTemplate.opsForZSet().size(historyKey);
        if (size != null && size > CACHE_MAX_SIZE) {
            redisTemplate.opsForZSet().removeRange(historyKey, 0, size - CACHE_MAX_SIZE - 1);
        }

        // 3. 읽음 처리(unreadCount)를 위한 정보 초기화
        String unreadCountKey = "chat:message:" + message.getId() + ":unread";
        String readBySetKey = "chat:message:" + message.getId() + ":readBy";

        // 보낸 사람을 제외한 참여자 수를 unreadCount로 설정 (Redis에 저장하려면 문자열(String)으로 변환해서 저장 -> String.valueOf() )
        int unreadCount = totalParticipants - 1;
        redisTemplate.opsForValue().set(unreadCountKey, String.valueOf(unreadCount));
        redisTemplate.expire(unreadCountKey, 24, TimeUnit.HOURS); // 24시간 후 자동 삭제

        // 보낸 사람을 읽은 사람(readBy) Set에 추가(add) (message.getSender.getId() => 메시지를 보낸 사람 ID)
        redisTemplate.opsForSet().add(readBySetKey, String.valueOf(message.getSender().getId()));
        redisTemplate.expire(readBySetKey, 24, TimeUnit.HOURS); // 24시간 후 자동 삭제
    }

    @Transactional
    public ChatMessageDto markAsRead(Long messageId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다: " + userEmail));

        // readBySetKey : 메시지를 읽은 사람 ID Set
        String readBySetKey = "chat:message:" + messageId + ":readBy";
        // unreadCountKey : 메시지를 아직 안 읽은 사람 수
        String unreadCountKey = "chat:message:" + messageId + ":unread";

        // 1. Redis Set에 사용자를 추가하고 성공 여부 확인 (Set 특성상 이미 존재하면 추가 안됨)
        Long result = redisTemplate.opsForSet().add(readBySetKey, String.valueOf(user.getId()));
        long unreadCount = -1; // 초기값 -1

        // 2. 처음 읽은 경우에만 unreadCount 1 감소
        if (result != null && result > 0) { // result > 0 -> 사용자 처음으로 메시지 읽음
            Long decrementedCount = redisTemplate.opsForValue().decrement(unreadCountKey); // Redis에서 unreadCount 1감소하고 그 값을 가져옴
            unreadCount = (decrementedCount != null) ? decrementedCount : 0;

            // 4. (비동기) DB에 읽음 상태 영구 저장
            persistReadStatus(messageId, user.getId());
        } else {
            // 이미 읽은 경우, 현재 unreadCount 값을 Redis에서 가져옴
            // Redis에서 가져온 unreadCount(문자열)를 정수(Long.parseLong())로 변환해서 가져옴
            String countStr = redisTemplate.opsForValue().get(unreadCountKey);
            unreadCount = (countStr != null) ? Long.parseLong(countStr) : 0;
        }

        ChatRoomMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("메시지를 찾지 못했습니다: " + messageId));

        // 5. DTO 생성해서 반환
        return ChatMessageDto.builder()
                .type("READ")
                .messageId(messageId)
                .roomId(message.getChatRoom().getId())
                .unreadCount((int) unreadCount)
                .build();
    }

    @Async("taskExecutor") // 별도의 스레드 풀에서 비동기 실행
    @Transactional
    public void persistReadStatus(Long messageId, Long userId) {
        ChatRoomMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found with id: " + messageId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        // 이미 DB에 저장되었는지 한 번 더 확인 (중복 저장 방지)
        if (!readStatusRepository.existsByChatMessageAndUser(message, user)) {
            ChatMessageReadStatus readStatus = new ChatMessageReadStatus(message, user);
            readStatusRepository.save(readStatus);
        }
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

        // 3. Redis에서 삭제할 JSON을 미리 생성
        String messageJsonToRemove = null;
        ChatMessageHistoryDto messageDto = ChatMessageHistoryDto.fromEntity(message);
        messageJsonToRemove = redisSerializer.serialize(messageDto);

        // 4. Soft Delete 방식
        message.setDeleted(true);
        message.setDeletedAt(LocalDateTime.now());

        // 5. DB에 저장
        ChatRoomMessage deleteMessage = chatMessageRepository.save(message);

        // 6. 미리 생성한 JSON으로 Redis 캐시에서 해당 메시지 제거
        if (messageJsonToRemove != null) {
            String historyKey = "chat:room:" + roomId + ":messages";

            // 제거 성공 확인용 로그
             Long removedCount = redisTemplate.opsForZSet().remove(historyKey, messageJsonToRemove);
             log.info("삭제 여부 확인 : {}", removedCount);
        }

        // 7. DTO로 변환하여 WebSocket 전송
        ChatMessageDto deleteMessageDto = ChatMessageDto.from(deleteMessage);
        deleteMessageDto.setType("DELETE"); // 클라이언트가 메시지 삭제를 인지하도록 타입 설정

        String destination = "/topic/chat/rooms/" + deleteMessage.getChatRoom().getId();
        messagingTemplate.convertAndSend(destination, deleteMessageDto);
    }
}
