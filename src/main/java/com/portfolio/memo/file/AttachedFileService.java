package com.portfolio.memo.file;

import com.portfolio.memo.file.dto.AttachedFileDownloadDto;
import com.portfolio.memo.task.CustomException.ResourceNotFoundException;
import com.portfolio.memo.task.Task;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class AttachedFileService {

    private final AttachedFileRepository attachedFileRepository;

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

    // 파일 다운로드 DTO 조회
    @Transactional(readOnly = true)
    public AttachedFileDownloadDto getDownloadInfo(Long fileId) {
        AttachedFile file = attachedFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException(fileId));

        return AttachedFileDownloadDto.from(file);
    }

    // 실제 파일데이터를 byte[] 로 읽기
    @Transactional(readOnly = true)
    public byte[] loadFile(Long fileId) {
        AttachedFile file = attachedFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException(fileId));

        Path filePath = Paths.get(uploadDir).resolve(file.getStoredFileName()).toAbsolutePath();

        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("파일을 읽을 수 없습니다.: " + file.getOriginalFileName(), e);
        }
    }
}
