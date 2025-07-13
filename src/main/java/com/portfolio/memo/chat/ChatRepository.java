package com.portfolio.memo.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface ChatRepository extends JpaRepository<Chat, Long> {

}
