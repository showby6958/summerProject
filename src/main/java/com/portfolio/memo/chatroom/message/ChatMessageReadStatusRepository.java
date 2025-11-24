package com.portfolio.memo.chatroom.message;

import com.portfolio.memo.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageReadStatusRepository extends JpaRepository<ChatMessageReadStatus, Long> {

    boolean existsByChatMessageIdAndUserId(Long messageId, Long userId);
}
