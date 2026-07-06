package com.portfolio.memo.file;

import com.portfolio.memo.file.dto.AttachedFileDownloadDto;
import com.portfolio.memo.CustomException.ResourceNotFoundException;
import com.portfolio.memo.Task;
import com.portfolio.memo.TaskAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachedFileService {

    private final AttachedFileRepository attachedFileRepository;
    private final TaskAccessValidator taskAccessValidator;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Transactional
    public void uploadFiles(Task task, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath); // 디렉토리가 없으면 생성
        } catch (IOException e) {
            throw new RuntimeException("업로드 디렉토리를 생성할 수 없습니다.", e);
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            String originalFileName = file.getOriginalFilename(); // 클라이언트가 업로드할 때 가진 원본 파일명 반환

            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")); // 파일 확장자 추출
            }

            String storedFileName = UUID.randomUUID().toString() + fileExtension; // UUID로 고유한 파일 이름 생성(파일명 충돌 방지, 원본 파일명 유출 방지)

            Path targetLocation = uploadPath.resolve(storedFileName); // 실제 저장할 경로 생성

            try {
                Files.copy(file.getInputStream(), targetLocation); // 파일의 바이트를 읽어(getInputStream)와서 저장(.copy)

                // AttachedFile 엔티티 생성 및 저장
                AttachedFile attachedFile = AttachedFile.builder()
                        .originalFileName(originalFileName)
                        .storedFileName(storedFileName)
                        .filePath(uploadDir) //  저장 경로 (설정 값)
                        .fileType(file.getContentType())
                        .fileSize(file.getSize())
                        .task(task)
                        .build();
                attachedFileRepository.save(attachedFile);
            } catch (IOException e) {
                throw new RuntimeException("파일을 저장할 수 없습니다: " + originalFileName, e);
            }
        }
    }

    // 파일 다운로드 DTO 조회 (담당자/팀원만 가능)
    @Transactional(readOnly = true)
    public AttachedFileDownloadDto getDownloadInfo(Long fileId, Long currentUserId) {
        AttachedFile file = attachedFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("AttachedFile", fileId));

        taskAccessValidator.validateParticipant(file.getTask(), currentUserId);

        return AttachedFileDownloadDto.from(file);
    }

    // 실제 파일데이터를 byte[] 로 읽기 (담당자/팀원만 가능)
    @Transactional(readOnly = true)
    public byte[] loadFile(Long fileId, Long currentUserId) {
        AttachedFile file = attachedFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("AttachedFile", fileId));

        taskAccessValidator.validateParticipant(file.getTask(), currentUserId);

        Path filePath = Paths.get(uploadDir).resolve(file.getStoredFileName()).toAbsolutePath();

        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("파일을 읽을 수 없습니다.: " + file.getOriginalFileName(), e);
        }
    }

    // 업무 상세 페이지에서 파일 목록 조회용
    public List<AttachedFileDownloadDto> getTaskFiles(long taskId) {
        return attachedFileRepository.findFilesByTaskId(taskId)
                .stream()
                .map(AttachedFileDownloadDto::from)
                .toList();
    }

    // Task 삭제 시 DB row는 cascade로 지워지지만, 디스크의 실제 파일은 별도로 정리해야 함
    public void deleteFilesFromDisk(List<AttachedFile> files) {
        for (AttachedFile file : files) {
            Path filePath = Paths.get(uploadDir).resolve(file.getStoredFileName()).toAbsolutePath();
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // 디스크 정리 실패가 업무 삭제 자체를 막으면 안 되므로 로그만 남기고 계속 진행
                log.warn("Failed to delete file from disk: {}", filePath, e);
            }
        }
    }
}
