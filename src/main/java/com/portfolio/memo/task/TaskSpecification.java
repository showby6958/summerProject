package com.portfolio.memo.task;

import com.portfolio.memo.auth.User;
import org.springframework.data.jpa.domain.Specification;

// 검색 조건을 정의 하는 Specification 클래스
public class TaskSpecification {

    // 내 업무만 보기 (담당자가 현재 사용자인 경우
    public static Specification<Task> withAssignee(User user) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("assignee"), user);
    }

    // 상태별 필터링
    public static Specification<Task> withStatus(TaskStatus status) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);
    }

    // 제목으로 검색
    public static Specification<Task> withTitleContaining(String keyword) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("title"), "%" + keyword + "%");
    }

    // 담당자 이름으로 검색
    public static Specification<Task> withAssigneeNameContaining(String keyword) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("assignee").get("name"), "%" + keyword + "%");
    }

    // 긴급도 필터링
    public static Specification<Task> withPriority(TaskPriority priority) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("priority"), priority);
    }
}
