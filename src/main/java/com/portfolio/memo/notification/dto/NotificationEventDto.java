package com.portfolio.memo.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEventDto {

    private Long userId; // 알림 받을 사용자 ID
    private NotificationEventType type; // 알림 종류(Enum) (예: "TASK_ASSIGNED", "MESSAGE_RECEIVED")
    private String message; // 알림 내용 (예: 업무가 배정되었습니다.)
    private Object data; // 추가 데이터 (taskId, roomId 등)
    private long timestamp; // 생성시간(직렬화 비용을 낮추기 위해서 LocalDate 대신 long으로 사용)
}
