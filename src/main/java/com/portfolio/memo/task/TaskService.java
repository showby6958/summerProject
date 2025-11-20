package com.portfolio.memo.task;

import com.portfolio.memo.auth.CustomUserDetails;
import com.portfolio.memo.auth.User;
import com.portfolio.memo.auth.UserRepository;
import com.portfolio.memo.file.AttachedFile;
import com.portfolio.memo.file.AttachedFileRepository;
import com.portfolio.memo.file.AttachedFileService;
import com.portfolio.memo.task.CustomException.ResourceNotFoundException;
import com.portfolio.memo.task.dto.TaskCreateRequest;
import com.portfolio.memo.task.dto.TaskDetailResponse;
import com.portfolio.memo.task.dto.TaskResponse;
import com.portfolio.memo.task.dto.TaskUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final AttachedFileService attachedFileService;

    // 업무 생성
    @Transactional
    public Task createTask(TaskCreateRequest request, List<MultipartFile> files, CustomUserDetails currentUser) {

       User assignee;
       // 요청에 담당자 Id가 있으면, 해당 사용자를 담당자로 지정
       if (request.getAssigneeId() != null) {
           assignee = userRepository.findById(request.getAssigneeId())
                   .orElseThrow(() -> new ResourceNotFoundException(request.getAssigneeId()));
       } else {
           // 요청에 담당자 ID가 없으면, 현재 로그인한 사용자를 담당자로 지정
           assignee = userRepository.findById(currentUser.getUser().getId())
                   .orElseThrow(() -> new ResourceNotFoundException(currentUser.getUser().getId()));
       }

        // Task 엔티티 생성
        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .assignee(assignee)
                .dueDate(request.getDueDate())
                .priority(request.getPriority())
                .status(TaskStatus.TODO) // 생성 시 기본 상태는 'TODO'
                .build();

        Task savedTask = taskRepository.save(task); // Task 만 저장

        // 파일 업로드 로직은 AttachedFileService 에 위임
        attachedFileService.uploadFiles(savedTask, files);

        return savedTask;
    }

    // 업무 검색
    @Transactional(readOnly = true)
    public List<TaskResponse> searchTask(
            Boolean myTask,
            TaskStatus status,
            String title,
            String assigneeName,
            TaskPriority priority,
            CustomUserDetails currentUser) {

        Specification<Task> spec = Specification.allOf();

        // 내 업무만 보기 (currentUser = assignee)
        if (Boolean.TRUE.equals(myTask)) {
            spec = spec.and(TaskSpecification.withAssignee(currentUser.getUser()));
        }

        // 상태 필터링
        if (status != null) {
            spec = spec.and(TaskSpecification.withStatus(status));
        }

        // 제목 검색
        if (title != null && !title.isBlank()) {
            spec = spec.and(TaskSpecification.withTitleContaining(title));
        }

        // 담당자 이름 검색
        if (assigneeName != null && !assigneeName.isBlank()) {
            spec = spec.and(TaskSpecification.withAssigneeNameContaining(assigneeName));
        }

        // 우선도 필터링
        if (priority != null) {
            spec = spec.and(TaskSpecification.withPriority(priority));
        }

        List<Task> tasks = taskRepository.findAll(spec);

        return tasks.stream()
                .map(TaskResponse::from)
                .toList();
    }

    // 업무 삭제
    @Transactional
    public void deleteTask(Long taskId, CustomUserDetails currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(taskId));

        if (!task.getAssignee().getId().equals(currentUser.getUser().getId())) {
            throw new IllegalArgumentException("해당 업무를 삭제할 권한이 없습니다.");
        }

        taskRepository.delete(task);
    }

    // 업무 수정
    @Transactional
    public TaskResponse updateTask(Long taskId, TaskUpdateRequest request, CustomUserDetails currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(taskId));

        // 현재 로그인한 사용자가 해당 업무의 담당자인지 확인
        if (!task.getAssignee().getId().equals(currentUser.getUser().getId())) {
            throw new IllegalArgumentException("해당 업무를 수정할 권한이 없습니다.");
        }

        // 담당자를 변경하는 경우, 새로운 담당자 정보를 조회
        User newAssignee = null;
        if (request.getAssigneeId() != null) {
            newAssignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException(request.getAssigneeId()));
        }

        // Task 엔티티에 update 매서드 호출해서 필드 업데이트
        task.update(request, newAssignee);

        Task updatedTask = taskRepository.save(task);

        return TaskResponse.from(updatedTask);
    }

    @Transactional(readOnly = true)
    public TaskDetailResponse getTaskDetail(Long taskId, CustomUserDetails currentUser) {
        Task task = taskRepository.findByIdWithFiles(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(taskId));

        return TaskDetailResponse.from(task);
    }

}
