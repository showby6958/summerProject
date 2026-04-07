package com.portfolio.memo.file;

import com.portfolio.memo.Task;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "attached_files")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachedFile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalFileName; // 원본 파일 이름

    @Column(nullable = false, unique = true)
    private String storedFileName; // 서버에 저장된 파일 이름(UUID)

    @Column(nullable = false)
    private String filePath; // 저장 경로

    private String fileType; // 파일 확장자

    private Long fileSize; // 파일 크기

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    // 양방향 연관관계 유지용 편의 메소드(AttachedFile <-> Task 간의 데이터 불일치 배제)
    public void setTask(Task task) {
        this.task = task;
        if (!task.getAttachedFiles().contains(this)) {
            task.getAttachedFiles().add(this);
        }
    }
}
