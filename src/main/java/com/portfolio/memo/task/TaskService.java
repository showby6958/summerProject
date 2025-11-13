package com.portfolio.memo.task;

import com.portfolio.memo.auth.CustomUserDetails;
import com.portfolio.memo.auth.User;
import com.portfolio.memo.auth.UserRepository;
import com.portfolio.memo.task.CustomException.ResourceNotFoundException;
import com.portfolio.memo.task.dto.TaskCreateRequest;
import com.portfolio.memo.task.dto.TaskResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    // 업무 생성
    @Transactional
    public Task createTask(TaskCreateRequest request, CustomUserDetails currentUser) {

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

        return taskRepository.save(task);
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
    public void deleteTask(Long taskId, CustomUserDetails currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(taskId));

        if (!task.getAssignee().getId().equals(currentUser.getUser().getId())) {
            throw new IllegalArgumentException("해당 업무를 삭제할 권한이 없습니다.");
        }

        taskRepository.delete(task);
    }

}
