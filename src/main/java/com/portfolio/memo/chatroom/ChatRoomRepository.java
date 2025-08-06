package com.portfolio.memo.chatroom;

import com.portfolio.memo.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    List<ChatRoom> findByParticipantsContaining(User user);
}
