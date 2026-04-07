package com.portfolio.memo;

import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

// 검색 조건을 정의 하는 Specification 클래스
public class TaskSpecification {

    // 내 업무만 보기
    public static Specification<Task> withMyTask(Long currentUserId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.equal(root.get("assigneeId"), currentUserId),
                criteriaBuilder.isMember(currentUserId, root.get("memberIds"))
        );
    }

    // 업무 상태별 필터링
    public static Specification<Task> withStatus(TaskStatus status) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);
    }

    // 업무 제목으로 검색  (like 검색 - 추후 성능 문제 발생 가능)
    public static Specification<Task> withTitleContaining(String keyword) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("title"), "%" + keyword + "%");
    }

    // 담당자 이름으로 검색 (like 검색 - 추후 성능 문제 발생 가능)
    public static Specification<Task> withAssigneeNameContaining(String keyword) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("assigneeName"), "%" + keyword + "%");
    }

    // 팀원 이름으로 검색 (like 검색 - 추후 성능 문제 발생 가능)
    public static Specification<Task> withMemberNameContaining(String keyword) {
        return (root, query, criteriaBuilder) -> {
            Join<Task, String> memberNames = root.join("memberNames"); // task_member_names 컬랜션과 조인
            return criteriaBuilder.like(memberNames, "%" + keyword + "%");
        };
    }

    // 업무 긴급도 필터링
    public static Specification<Task> withPriority(TaskPriority priority) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("priority"), priority);
    }
}
