package com.portfolio.memo.chat;

import com.portfolio.memo.chat.dto.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    // 두 사용자 간의 모든 메시지를 시간순으로 조회
    List<Chat> findBySenderAndReceiverOrReceiverAndSenderOrderByTimestampAsc(String sender, String receiver, String receiver2, String sender2);
}
