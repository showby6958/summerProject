package com.portfolio.memo.comment;

import com.portfolio.memo.comment.dto.CommentCreateRequest;
import com.portfolio.memo.comment.dto.CommentDto;
import com.portfolio.memo.CustomException.ResourceNotFoundException;
import com.portfolio.memo.Task;
import com.portfolio.memo.TaskAccessValidator;
import com.portfolio.memo.TaskRepository;
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
    private final TaskAccessValidator taskAccessValidator;

    @Transactional
    public CommentDto createComment(Long taskId, CommentCreateRequest request, Long userId, String userName) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        // 담당자/팀원만 댓글 작성 가능
        taskAccessValidator.validateParticipant(task, userId);

        Comment comment = Comment.builder()
                .content(request.getContent())
                .task(task)
                .userId(userId)
                .userName(userName)
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
    public void deleteComment(Long commentId, Long userId) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        boolean isAuthor = comment.getUserId().equals(userId);

        if (!isAuthor) {
            throw new AccessDeniedException("You do not have permission to delete this comment.");
        }

        commentRepository.delete(comment);

    }

}
