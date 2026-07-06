package com.portfolio.memo.file;

import com.portfolio.memo.common.jwt.CustomUserPrincipal;
import com.portfolio.memo.file.dto.AttachedFileDownloadDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class AttachedFileController {

    private final AttachedFileService attachedFileService;

    @GetMapping("/{fileId}/download")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal CustomUserPrincipal currentUser) {

        AttachedFileDownloadDto dto = attachedFileService.getDownloadInfo(fileId, currentUser.getUserId());
        byte[] fileData = attachedFileService.loadFile(fileId, currentUser.getUserId());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + dto.getOriginalFileName() + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(dto.getFileSize()))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(fileData);
    }
}
