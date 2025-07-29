package com.portfolio.memo.chatroom;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatRoomMessage, Long> {
    List<ChatRoomMessage> findByChatRoom_IdOrderBySentAtAsc(Long chatRoomId);
}

