package com.portfolio.memo;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

// 업무 담당자 또는 팀원만 해당 업무(상세/첨부파일/댓글)에 접근할 수 있도록 검증
@Component
public class TaskAccessValidator {

    public void validateParticipant(Task task, Long userId) {
        boolean isAssignee = task.getAssigneeId().equals(userId);
        boolean isMember = task.getMemberIds() != null && task.getMemberIds().contains(userId);

        if (!isAssignee && !isMember) {
            throw new AccessDeniedException("You do not have access to this task.");
        }
    }
}