package com.portfolio.memo.comment;

import com.portfolio.memo.comment.dto.CommentCreateRequest;
import com.portfolio.memo.comment.dto.CommentDto;
import com.portfolio.memo.common.jwt.CustomUserPrincipal;
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
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        Long userId = principal.getUserId();
        String userName = principal.getUsername();

        CommentDto dto = commentService.createComment(taskId, request, userId, userName);

        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/delete/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        commentService.deleteComment(commentId, principal.getUserId());

        return ResponseEntity.noContent().build();

    }
}