package com.portfolio.memo.comment;

import com.portfolio.memo.auth.CustomUserDetails;
import com.portfolio.memo.auth.User;
import com.portfolio.memo.auth.UserRepository;
import com.portfolio.memo.comment.dto.CommentCreateRequest;
import com.portfolio.memo.comment.dto.CommentDto;
import com.portfolio.memo.task.CustomException.ResourceNotFoundException;
import com.portfolio.memo.task.Task;
import com.portfolio.memo.task.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentDto createComment(Long taskId, CommentCreateRequest request, CustomUserDetails currentUser) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(taskId));

        User user = userRepository.findById(currentUser.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException(currentUser.getUser().getId()));

        Comment comment = Comment.builder()
                .content(request.getContent())
                .task(task)
                .user(user)
                .build();

        Comment saved = commentRepository.save(comment);

        return CommentDto.from(saved);
    }

    // 업무 상세에서 댓글 조회 용
    @Transactional(readOnly = true)
    public List<CommentDto> getTaskComment(Long taskId) {
        return commentRepository.findCommentsByTaskId(taskId)
                .stream()
                .map(CommentDto::from)
                .toList();
    }

    @Transactional
    public void deleteComment(Long commentId, CustomUserDetails currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(commentId));

        boolean isAuthor = comment.getUser().getId().equals(currentUser.getUser().getId());

        if (!isAuthor) {
            throw new AccessDeniedException("이 댓글을 삭제할 권한이 없습니다.");
        }

        commentRepository.delete(comment);

    }

}
