package com.portfolio.memo.file.dto;

import com.portfolio.memo.file.AttachedFile;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttachedFileDto {
    private Long id;
    private String originalFileName;
    private String fileType;
    private Long fileSize;

    public static AttachedFileDto from(AttachedFile attachedFile) {
        return AttachedFileDto.builder()
                .id(attachedFile.getId())
                .originalFileName(attachedFile.getOriginalFileName())
                .fileType(attachedFile.getFileType())
                .fileSize(attachedFile.getFileSize())
                .build();
    }
}
