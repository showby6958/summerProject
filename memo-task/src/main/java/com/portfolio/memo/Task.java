package com.portfolio.memo;

import com.portfolio.memo.comment.Comment;
import com.portfolio.memo.file.AttachedFile;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 제목
    @Column(nullable = false)
    private String title;

    // 설명
    private String description;

    // 마감일
    private LocalDateTime dueDate;

    // 담당자 ID
    @Column(name = "assignee_id", nullable = false)
    private Long assigneeId;

    // 담당자 이름 (TaskSpecification 에서 담당자 이름 검색에 사용)
    @Column(name = "assignee_name", nullable = false)
    private String assigneeName;

    // 팀원 (userId 리스트)
    @ElementCollection
    @CollectionTable(name = "task_members", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "member_id")
    private List<Long> memberIds = new ArrayList<>();

    // 팀원 이름 (TaskSpecification 에서 팀원 이름 검색에 사용)
    @ElementCollection
    @CollectionTable(name = "task_members_names", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "member_name")
    @Builder.Default
    private List<String> memberNames = new ArrayList<>();

    // 업무 처리 상태 (TODO/IN_PROGRESS/DONE)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    // 업무 우선순위
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority;

    // 생성 시간
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 업데이트 시간
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 첨부 파일
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AttachedFile> attachedFiles = new ArrayList<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }


    // 업무 수정 로직
    public void update(
            String title,
            String description,
            TaskStatus status,
            TaskPriority priority,
            LocalDateTime dueDate,
            Long newAssigneeId,
            String newAssigneeName,
            List<Long> newMemberIds,
            List<String> newMemberNames
    ) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (status != null) this.status = status;
        if (priority != null) this.priority = priority;
        if (dueDate != null) this.dueDate = dueDate;

        if (newAssigneeId != null) {
            this.assigneeId = newAssigneeId;
            this.assigneeName = newAssigneeName;
        }

        if (newMemberIds != null) {
            this.memberIds = newMemberIds;
            this.memberNames = newMemberNames;
        }

        this.updatedAt = LocalDateTime.now();
    }
}
