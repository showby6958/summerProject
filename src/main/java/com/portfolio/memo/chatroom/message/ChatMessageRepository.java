package com.portfolio.memo.chatroom.message;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatRoomMessage, Long> {
    List<ChatRoomMessage> findByChatRoomIdAndIsDeletedFalseOrderBySentAtAsc(Long chatRoomId);
}

