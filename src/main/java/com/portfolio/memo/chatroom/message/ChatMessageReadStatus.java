package com.portfolio.memo.chatroom.message;

import com.portfolio.memo.auth.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "chat_message_read_status",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_message_user",
                        columnNames = {"chat_message_id", "user_id"}
                )
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageReadStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_message_id", nullable = false)
    private Long chatMessageId;

    @Column(name = "user_id", nullable = false)
    private Long userId;


    public ChatMessageReadStatus(Long chatMessageId, Long userId) {
        this.chatMessageId = chatMessageId;
        this.userId = userId;
    }
}