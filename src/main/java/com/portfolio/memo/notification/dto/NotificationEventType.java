package com.portfolio.memo.notification.dto;

public enum NotificationEventType {

    TASK_ASSIGNED, // 업무가 배정됨
    TASK_UPDATED, // 업무 내용/상태 업데이트
    TASK_COMPLETED, // 업무 완료됨
    COMMENT_ADDED, // 댓글이 작성됨
    MESSAGE_RECEIVED, // 채팅 메시지 도착
    SYSTEM_ALERT // 시스템 알림
}
