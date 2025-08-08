package com.portfolio.memo.chatroom;

import com.portfolio.memo.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageReadStatusRepository extends JpaRepository<ChatMessageReadStatus, Long> {

    boolean existsByChatMessageAndUser(ChatRoomMessage chatMessage, User user);

    int countByChatMessage(ChatRoomMessage chatMessage);

}
