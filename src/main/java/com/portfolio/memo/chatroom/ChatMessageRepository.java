package com.portfolio.memo.chatroom;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatRoomMessage, Long> {
}
