package com.portfolio.memo.task;

import com.portfolio.memo.auth.CustomUserDetails;
import com.portfolio.memo.task.dto.TaskCreateRequest;
import com.portfolio.memo.task.dto.TaskResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/create")
    public ResponseEntity<TaskResponse> createTask(
            @RequestBody TaskCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        // 업무 생성 서비스
        Task createdTask = taskService.createTask(request, currentUser);

        // 응답 DTO로 변환
        TaskResponse response = TaskResponse.from(createdTask);

        return ResponseEntity.created(URI.create("/api/tasks/" + response.getId())).body(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<TaskResponse>> searchTask(
            @RequestParam(required = false) Boolean myTask,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String assigneeName,
            @RequestParam(required = false) TaskPriority priority,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        List<TaskResponse> tasks = taskService.searchTask(myTask, status, title, assigneeName, priority, currentUser);
        return ResponseEntity.ok(tasks);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long taskId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        taskService.deleteTask(taskId, currentUser);
        return ResponseEntity.noContent().build();
    }


}
