package com.portfolio.memo;

import com.portfolio.memo.dto.TaskNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final SimpMessagingTemplate messagingTemplate;

    // user destination: /user/queue/notifications 구독하면 됨
    public void dispatchToUser(Long userId, TaskNotificationEvent event) {
        if (userId == null) return;

        String user = String.valueOf(userId);
        try {
            messagingTemplate.convertAndSendToUser(user, "/queue/notifications", event);
        } catch (Exception ex) {
            log.warn("Failed to send notification to userId = {} eventId = {} type = {}", userId, event.getEventId(), event.getEventType(), ex);
        }
    }
}
