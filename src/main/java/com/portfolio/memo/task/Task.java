package com.portfolio.memo.task;

import com.portfolio.memo.auth.User;
import com.portfolio.memo.comment.Comment;
import com.portfolio.memo.file.AttachedFile;
import com.portfolio.memo.task.dto.TaskUpdateRequest;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    // 담당자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

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
    @Column(name = "updated_at", updatable = false)
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
    public void update(TaskUpdateRequest request, User newAssignee) {
        Optional.ofNullable(request.getTitle()).ifPresent(this::setTitle);
        Optional.ofNullable(request.getDescription()).ifPresent(this::setDescription);
        Optional.ofNullable(request.getStatus()).ifPresent(this::setStatus);
        Optional.ofNullable(request.getPriority()).ifPresent(this::setPriority);
        Optional.ofNullable(request.getDueDate()).ifPresent(this::setDueDate);
        Optional.ofNullable(newAssignee).ifPresent(this::setAssignee);
    }
}
