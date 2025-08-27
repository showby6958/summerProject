package com.portfolio.memo.chatroom.message;

public class MessageEditNotAllowedException extends RuntimeException {
    public MessageEditNotAllowedException(Long messageId, Long userId) {
        super("User " + userId + "is not allowed to edit message" + messageId);
    }
}
