package com.portfolio.memo.task;

import com.portfolio.memo.auth.CustomUserDetails;
import com.portfolio.memo.auth.User;
import com.portfolio.memo.auth.UserRepository;
import com.portfolio.memo.task.CustomException.ResourceNotFoundException;
import com.portfolio.memo.task.dto.TaskCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

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
}
