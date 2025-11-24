package com.portfolio.memo.chatroom.message;

import com.portfolio.memo.auth.UserRepository;
import com.portfolio.memo.chatroom.message.dto.ReadMessageResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ChatMessageReadStatusService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageReadStatusRepository readStatusRepository;

    // 메시지 읽음 상태/카운트 전용
    @Qualifier("chatCountRedisTemplate")
    private final StringRedisTemplate chatCountRedisTemplate;

    // 메시지 읽음 상태를 Redis에 초기화 (메시지 읽음 상태 캐싱용)
    public void initializeReadStatus(ChatRoomMessage message, int totalParticipants) {
        String unreadCountKey = "chat:message:" + message.getId() + ":unread";
        String readBySetKey = "chat:message:" + message.getId() + ":readBy";

        int unreadCount = totalParticipants - 1; // 보낸 사람 제외
        if (unreadCount > 0) {
            chatCountRedisTemplate.opsForValue().set(unreadCountKey, String.valueOf(unreadCount));
            chatCountRedisTemplate.expire(unreadCountKey, 12, TimeUnit.HOURS); // 12시간 후 자동 삭제
        }

        chatCountRedisTemplate.opsForSet().add(readBySetKey, String.valueOf(message.getSender().getId()));
        chatCountRedisTemplate.expire(readBySetKey, 12, TimeUnit.HOURS); // 12시간 후 자동 삭제
    }

    // 사용자가 메시지를 읽었을 때 호출
    @Transactional
    public ReadMessageResponse markAsRead(Long messageId, Long userId) {
        String unreadCountKey = "chat:message:" + messageId + ":unread";
        String readByKey = "chat:message:" + messageId + ":readBy";

        // 1. 사용자가 이미 메시지를 읽었는지 확인
        Boolean alreadyRead = chatCountRedisTemplate.opsForSet().isMember(readByKey, String.valueOf(userId));

        long unreadCount;

        // 2. 이미 읽은 경우와 처음 읽는 경우를 분기하여 처리
        if (Boolean.TRUE.equals(alreadyRead)) {
            // 이미 읽은 경우
            String countStr = chatCountRedisTemplate.opsForValue().get(unreadCountKey);
            unreadCount = (countStr != null) ? Long.parseLong(countStr) : 0;
        } else {
            // 처음 읽는 경우 - Redis Set에 사용자 ID 추가
            chatCountRedisTemplate.opsForSet().add(readByKey, String.valueOf(userId)); //
            // unreadCount를 1 감소 (atomic operation)
            Long decrementedCount = chatCountRedisTemplate.opsForValue().decrement(unreadCountKey);
            unreadCount = (decrementedCount != null) ? decrementedCount : 0;

            // DB에 읽음 상태 비동기 저장
            persistReadStatus(messageId, userId);
        }

        // 3. 응답 DTO 생성을 위해 메시지 정보 조회
        ChatRoomMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found with id: " + messageId));

        // 4. 최종 결과를 DTO로 만들어 반환
        return ReadMessageResponse.builder()
                .messageId(messageId)
                .roomId(message.getChatRoom().getId())
                .userId(userId)
                .unreadCount((int) unreadCount)
                .build();
    }

    @Async("taskExecutor") // 별도의 스레드 풀에서 비동기 실행
    @Transactional
    public void persistReadStatus(Long messageId, Long userId) {
        // 이미 DB에 저장되었는지 한 번 더 확인 (중복 저장 방지)
        if (!readStatusRepository.existsByChatMessageIdAndUserId(messageId, userId)) {
            ChatMessageReadStatus readStatus = new ChatMessageReadStatus(messageId, userId);
            readStatusRepository.save(readStatus);
        }
    }


}
