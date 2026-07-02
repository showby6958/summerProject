package com.portfolio.memo.chat.message;

public class MessageDeleteNotAllowed extends RuntimeException {
    public MessageDeleteNotAllowed(Long messageId, Long userId) {
        super("User " + userId + " is not allowed to delete message " + messageId);
    }
}
