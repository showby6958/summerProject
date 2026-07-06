package com.portfolio.memo;

import com.portfolio.memo.common.jwt.CustomUserPrincipal;
import com.portfolio.memo.domain.Notification;
import com.portfolio.memo.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // 알림 목록 조회 (재접속 시 오프라인 동안 쌓인 알림 확인용)
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @AuthenticationPrincipal CustomUserPrincipal currentUser) {

        List<Notification> notifications = notificationService.getNotifications(currentUser.getUserId(), unreadOnly);

        return ResponseEntity.ok(
                notifications.stream().map(NotificationResponse::from).toList()
        );
    }

    // 안 읽은 알림 개수
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal CustomUserPrincipal currentUser) {
        return ResponseEntity.ok(notificationService.getUnreadCount(currentUser.getUserId()));
    }

    // 알림 읽음 처리
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal CustomUserPrincipal currentUser) {

        notificationService.markRead(notificationId, currentUser.getUserId());
        return ResponseEntity.noContent().build();
    }
}