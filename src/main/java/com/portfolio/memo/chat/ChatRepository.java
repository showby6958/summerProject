package com.portfolio.memo.chat;

import com.portfolio.memo.chat.dto.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    // 두 사용자 간의 모든 메시지를 시간순으로 조회
    List<Chat> findBySenderAndReceiverOrReceiverAndSenderOrderByTimestampAsc(String sender, String receiver, String receiver2, String sender2);

    // 두 사용자 간의 모든 대화 내용을 시간 순으로 조회
    // JPQL 쿼리로 양방향 관계를 모두 조회
    @Query("SELECT c FROM Chat c WHERE (c.sender = :user1Email AND c.receiver = :user2Email) OR (c.sender = :user2Email AND c.receiver = :user1Email) ORDER BY c.timestamp ASC")
    List<Chat> findConversationHistory(@Param("user1Email") String user1Email, @Param("user2Email") String user2Email);
}
