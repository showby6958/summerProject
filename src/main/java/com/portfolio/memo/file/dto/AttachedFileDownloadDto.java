package com.portfolio.memo.file.dto;

import com.portfolio.memo.file.AttachedFile;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttachedFileDownloadDto {
    private Long id;
    private String originalFileName;
    private Long fileSize;
    private String downloadUrl;

    public static AttachedFileDownloadDto from(AttachedFile file) {
        return AttachedFileDownloadDto.builder()
                .id(file.getId())
                .originalFileName(file.getOriginalFileName())
                .fileSize(file.getFileSize())
                .downloadUrl("/api/files/" + file.getId() + "/download")
                .build();
    }
}
