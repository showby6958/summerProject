package com.portfolio.memo.task;

import com.portfolio.memo.auth.CustomUserDetails;
import com.portfolio.memo.comment.CommentService;
import com.portfolio.memo.comment.dto.CommentDto;
import com.portfolio.memo.file.AttachedFileService;
import com.portfolio.memo.file.dto.AttachedFileDownloadDto;
import com.portfolio.memo.task.dto.TaskCreateRequest;
import com.portfolio.memo.task.dto.TaskDetailResponse;
import com.portfolio.memo.task.dto.TaskResponse;
import com.portfolio.memo.task.dto.TaskUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final AttachedFileService attachedFileService;
    private final CommentService commentService;

    // 업무 생성
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaskResponse> createTask(
            @RequestPart("taskCreateRequest") TaskCreateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files, // 파일 리스트
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        // 업무 생성 서비스
        Task createdTask = taskService.createTask(request, files, currentUser);

        // 응답 DTO로 변환
        TaskResponse response = TaskResponse.from(createdTask);

        return ResponseEntity.created(URI.create("/api/tasks/" + response.getId())).body(response);
    }

    // 업무 검색
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

    // 업무 삭제
    @DeleteMapping("/delete/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long taskId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        taskService.deleteTask(taskId, currentUser);
        return ResponseEntity.noContent().build();
    }

    // 업무 수정
    @PutMapping("/update/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long taskId,
            @RequestBody TaskUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        TaskResponse updatedTask = taskService.updateTask(taskId, request, currentUser);
        return ResponseEntity.ok(updatedTask);
    }



    // 업무 조회용 (1: 업무 기본 상세 조회, 2: 업무 첨부파일 조회, 3: 업무 댓글 조회)
    // 1. 업무 기본 상세 조회
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskDetailResponse> getTaskDetail(
            @PathVariable Long taskId) {

        return ResponseEntity.ok(taskService.getTaskDetail(taskId));
    }

    // 2. 업무 첨부파일 조회
    @GetMapping("/{taskId}/files")
    public ResponseEntity<List<AttachedFileDownloadDto>> getTaskFiles(
            @PathVariable Long taskId) {

        return ResponseEntity.ok(attachedFileService.getTaskFiles(taskId));
    }

    // 3. 업무 댓글 조회
    @GetMapping("/{taskId}/comments")
    public ResponseEntity<List<CommentDto>> getTaskComments(
            @PathVariable Long taskId) {

        return ResponseEntity.ok(commentService.getTaskComment(taskId));
    }

}
