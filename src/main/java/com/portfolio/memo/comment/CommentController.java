package com.portfolio.memo.comment;

import com.portfolio.memo.auth.CustomUserDetails;
import com.portfolio.memo.comment.dto.CommentCreateRequest;
import com.portfolio.memo.comment.dto.CommentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/create/{taskId}")
    public ResponseEntity<CommentDto> createComment(
            @PathVariable Long taskId,
            @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        CommentDto dto = commentService.createComment(taskId, request, currentUser);

        return ResponseEntity.ok(dto);
    }
}