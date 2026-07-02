package com.portfolio.memo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.memo.comment.CommentService;
import com.portfolio.memo.comment.dto.CommentDto;
import com.portfolio.memo.common.jwt.CustomUserPrincipal;
import com.portfolio.memo.file.AttachedFileService;
import com.portfolio.memo.file.dto.AttachedFileDownloadDto;
import com.portfolio.memo.dto.TaskCreateRequest;
import com.portfolio.memo.dto.TaskDetailResponse;
import com.portfolio.memo.dto.TaskResponse;
import com.portfolio.memo.dto.TaskUpdateRequest;
import lombok.RequiredArgsConstructor;
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
    private final ObjectMapper objectMapper;

    // 업무 생성
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaskResponse> createTask(
            @RequestPart(value = "taskCreateRequest") String requestJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files, // 파일 리스트
            @AuthenticationPrincipal CustomUserPrincipal currentUser
    ) throws JsonProcessingException {

        // 문자열로 받고 JSON 파싱(입력이 텍스트로 와도 파싱 가능)
        TaskCreateRequest request = objectMapper.readValue(requestJson, TaskCreateRequest.class);

        // 업무 생성 서비스
        Task createdTask = taskService.createTask(request, files, currentUser.getUserId(), currentUser.getUsername());

        // 응답 DTO로 변환
        TaskResponse response = TaskResponse.from(createdTask);

        return ResponseEntity
                .created(URI.create("/api/tasks/" + response.getId()))
                .body(response);
    }

    // 업무 검색
    @GetMapping("/search")
    public ResponseEntity<List<TaskResponse>> searchTask(
            @RequestParam(required = false) Boolean myTask,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) String assigneeName,
            @RequestParam(required = false) String memberName,
            @AuthenticationPrincipal CustomUserPrincipal currentUser) {


        List<TaskResponse> tasks = taskService.searchTask(myTask, status, title, priority, assigneeName, memberName, currentUser.getUserId());
        return ResponseEntity.ok(tasks);
    }

    // 업무 삭제
    @DeleteMapping("/delete/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long taskId,
            @AuthenticationPrincipal CustomUserPrincipal currentUser) {

        taskService.deleteTask(taskId, currentUser.getUserId());
        return ResponseEntity.noContent().build();
    }

    // 업무 수정
    @PutMapping("/update/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long taskId,
            @RequestBody TaskUpdateRequest request,
            @AuthenticationPrincipal CustomUserPrincipal currentUser) {

        TaskResponse updatedTask = taskService.updateTask(taskId, request, currentUser.getUserId());
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

//
//    @PostMapping("/{taskId}/assign")
//    public ResponseEntity<Void> assignMembers(
//            @PathVariable Long taskId,
//            @RequestBody AssignMemberRequestDto request) {
//
//        taskService.assignMembers(taskId, request.getMemberIds());
//
//        return ResponseEntity.ok().build();
//    }
//

}
