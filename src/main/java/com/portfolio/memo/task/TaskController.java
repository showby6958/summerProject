package com.portfolio.memo.task;

import com.portfolio.memo.auth.CustomUserDetails;
import com.portfolio.memo.task.dto.TaskCreateRequest;
import com.portfolio.memo.task.dto.TaskResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @RequestBody TaskCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        // 업무 생성 서비스
        Task createdTask = taskService.createTask(request, currentUser);

        // 응답 DTO로 변환
        TaskResponse response = TaskResponse.from(createdTask);

        return ResponseEntity.created(URI.create("/api/tasks/" + response.getId())).body(response);
    }
}
