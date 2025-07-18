package com.portfolio.memo.chat;

import com.portfolio.memo.chat.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
// ChatController 로 부터 ChatMessage DTO를 받아서 Chat 엔티티로 변환 후
// ChatRepositoy를 통해 데이터베이스에 저장함
public class ChatService {

    private final ChatRepository chatRepository;

    @Transactional
    public void saveMessage(ChatMessage chatMessage) {
        // ChatMessage DTO를 Chat 엔티티로 반환
        Chat chat = Chat.builder()
                .sender(chatMessage.getSender())
                .receiver(chatMessage.getReceiver())
                .content(chatMessage.getContent())
                .timestamp(chatMessage.getTimestamp())
                .build();

        chatRepository.save(chat);
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getMessageHistory(String currentUserEmail, String otherUserEmail) {
        List<Chat> chats = chatRepository.findBySenderAndReceiverOrReceiverAndSenderOrderByTimestampAsc(currentUserEmail, otherUserEmail, otherUserEmail, currentUserEmail);

        return chats.stream().map(chat -> {
            ChatMessage msg = new ChatMessage();
            msg.setSender(chat.getSender());
            msg.setReceiver(chat.getReceiver());
            msg.setContent(chat.getContent());
            msg.setTimestamp(chat.getTimestamp());
            return msg;
        }).collect(Collectors.toList());
    }
}
