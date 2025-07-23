package com.portfolio.memo.chatroom;

import com.portfolio.memo.auth.User;
import com.portfolio.memo.chatroom.ChatRoomMessage;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "chat_rooms")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatRoom {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "chat_room_participants",
            joinColumns = @JoinColumn(name = "chat_room_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<User> participants = new HashSet<>();

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatRoomMessage> messages = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

}
