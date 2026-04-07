package com.portfolio.memo.comment.dto;

import com.portfolio.memo.comment.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentDto {

    private Long id;
    private String content;
    private Long userId;
    private String userName;

    private LocalDateTime createdAt;

    public static CommentDto from(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .userId(comment.getUserId())
                .userName(comment.getUserName())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
