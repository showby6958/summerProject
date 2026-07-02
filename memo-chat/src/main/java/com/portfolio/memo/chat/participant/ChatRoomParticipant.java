package com.portfolio.memo.chat.participant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "chat_room_participant",
        indexes = {
                @Index(name = "idx_participant_room", columnList = "room_id"),
                @Index(name = "idx_participant_user", columnList = "user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_room_user", columnNames = {"room_id", "user_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomParticipant {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_name", nullable = true)
    private String userName;

    @Builder
    public ChatRoomParticipant(Long roomId, Long userId, String userName) {
        this.roomId = roomId;
        this.userId = userId;
        this.userName = userName;
    }
}
