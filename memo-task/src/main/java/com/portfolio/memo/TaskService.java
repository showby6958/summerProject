package com.portfolio.memo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.memo.dto.*;
import com.portfolio.memo.file.AttachedFileService;
import com.portfolio.memo.CustomException.ResourceNotFoundException;
import com.portfolio.memo.client.AuthClient;
import com.portfolio.memo.client.dto.UserSummaryDto;
import com.portfolio.memo.outbox.domain.OutboxEvent;
import com.portfolio.memo.outbox.domain.OutboxEventRepository;
import com.portfolio.memo.outbox.domain.OutboxEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final AttachedFileService attachedFileService;
    private final AuthClient authClient;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;
    private final TaskAccessValidator taskAccessValidator;

    // 업무 생성
    @Transactional
    public Task createTask(TaskCreateRequest request, List<MultipartFile> files, Long currentUserId, String currentUserName) {

        // 입력 리스트 builder null 방지용
        List<Long> memberIds = (request.getMemberIds() == null)
                ? new ArrayList<>()
                : new ArrayList<>(request.getMemberIds());

        // 팀원 존재 여부 검증 + 이름 조회 (TaskResponse 표시 및 이름 검색용)
        List<String> memberNames = resolveMemberNames(memberIds);

        // 1. Task 저장
        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .assigneeId(currentUserId)
                .assigneeName(currentUserName)
                .memberIds(memberIds)
                .memberNames(memberNames)
                .dueDate(request.getDueDate())
                .priority(request.getPriority())
                .status(TaskStatus.TODO) // 생성 시 기본 상태는 'TODO'
                .build();

        Task savedTask = taskRepository.save(task); // Task 만 저장

        // 2.파일 업로드 로직은 AttachedFileService 에 위임
        attachedFileService.uploadFiles(savedTask, files);

        // 3. 알림 이벤트 DTO 생성 -> Outbox 기록
        // 알림 발송 -> 담당자 + 팀원에게만 알림 전송
        // recipients = assigneeId + memberIds (중복 제거)
        List<Long> recipients = buildRecipients(savedTask.getAssigneeId(), savedTask.getMemberIds());

        TaskNotificationEvent event = TaskNotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("TASK_CREATED")
                .occurredAt(LocalDateTime.now())
                .taskId(savedTask.getId())
                .actorId(currentUserId)
                .title(savedTask.getTitle())
                .assigneeId(savedTask.getAssigneeId())
                .memberIds(savedTask.getMemberIds())
                .recipientUserIds(recipients)
                .build();

        String payload = toJson(event);

        outboxEventRepository.save(
                OutboxEvent.pending(
                        OutboxEventType.TASK_CREATED,
                        "TASK",
                        savedTask.getId(),
                        payload
                )
        );

        return savedTask;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }

    private List<Long> buildRecipients(Long assigneeId, List<Long> memberIds) {
        LinkedHashSet<Long> set = new LinkedHashSet<>();
        if (assigneeId != null) set.add(assigneeId);
        if (memberIds != null) set.addAll(memberIds);
        return new ArrayList<>(set);
    }

    // 팀원 존재 여부 검증 + 이름 조회를 배치 API 한 번으로 처리 (팀원 수만큼 blocking 호출하는 것을 방지)
    private List<String> resolveMemberNames(List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, String> nameById = authClient.getUsers(memberIds).stream()
                .collect(Collectors.toMap(UserSummaryDto::getId, UserSummaryDto::getName));

        List<String> names = new ArrayList<>();
        for (Long memberId : memberIds) {
            String name = nameById.get(memberId);
            if (name == null) {
                throw new IllegalArgumentException("Member does not exist: " + memberId);
            }
            names.add(name);
        }
        return names;
    }




    // 업무 검색
    @Transactional(readOnly = true)
    public List<TaskResponse> searchTask(
            Boolean myTask,
            TaskStatus status,
            String title,
            TaskPriority priority,
            String assigneeName,
            String memberName,
            Long currentUserId) {

        Specification<Task> spec = Specification.allOf();

        // 내 업무만 보기 (currentUser = assignee)
        if (Boolean.TRUE.equals(myTask)) {
            spec = spec.and(TaskSpecification.withMyTask(currentUserId));
        }

        // 상태 필터링
        if (status != null) {
            spec = spec.and(TaskSpecification.withStatus(status));
        }

        // 제목 검색
        if (title != null && !title.isBlank()) {
            spec = spec.and(TaskSpecification.withTitleContaining(title));
        }

        // 담당자 이름 검색
        if (assigneeName != null && !assigneeName.isBlank()) {
            spec = spec.and(TaskSpecification.withAssigneeNameContaining(assigneeName));
        }

        // 팀원 이름 검색
        if (memberName != null && !memberName.isBlank()) {
            spec = spec.and(TaskSpecification.withMemberNameContaining(memberName));
        }

        // 우선도 필터링
        if (priority != null) {
            spec = spec.and(TaskSpecification.withPriority(priority));
        }

        List<Task> tasks = taskRepository.findAll(spec);

        return tasks.stream()
                .map(TaskResponse::from)
                .toList();
    }

    // 업무 삭제
    @Transactional
    public void deleteTask(Long taskId, Long currentUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        // 현재 요청자가 담당자일 경우만 삭제 가능
        if (!task.getAssigneeId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to delete this task.");
        }

        // DB row는 cascade로 삭제되지만, 디스크의 실제 파일은 별도로 정리
        attachedFileService.deleteFilesFromDisk(task.getAttachedFiles());

        taskRepository.delete(task);
    }

    // 업무 수정
    @Transactional
    public TaskResponse updateTask(Long taskId, TaskUpdateRequest request, Long currentUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        // 현재 로그인한 사용자가 해당 업무의 담당자인지 확인
        if (!task.getAssigneeId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to edit this task.");
        }

        // 담당자를 변경하는 경우, 새로운 담당자 정보를 조회
        Long newAssigneeId = request.getAssigneeId();
        String newAssigneeName = null;
        if (newAssigneeId != null) {
            // 사용자 존재 여부 검증 (유저 서비스 REST API 호출)
            if (!authClient.existsUser(newAssigneeId)) {
                throw new IllegalArgumentException("Assignee does not exist.");
            }

            newAssigneeName = authClient.getUsername(newAssigneeId);
        }

        // 팀원을 변경하는 경우, 새로운 팀원 정보 조회
        List<Long> newMemberIds = request.getMemberIds();
        List<String> newMemberNames = (newMemberIds != null) ? resolveMemberNames(newMemberIds) : null;

        task.update(
                request.getTitle(),
                request.getDescription(),
                request.getStatus(),
                request.getPriority(),
                request.getDueDate(),
                newAssigneeId,
                newAssigneeName,
                newMemberIds,
                newMemberNames
        );

        Task updatedTask = taskRepository.save(task);

        return TaskResponse.from(updatedTask);
    }

    // 업무 상세에서 상세 정보 조회 용 (담당자/팀원만 조회 가능)
    @Transactional(readOnly = true)
    public TaskDetailResponse getTaskDetail(Long taskId, Long currentUserId) {
        Task task = taskRepository.findTaskDetail(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        taskAccessValidator.validateParticipant(task, currentUserId);

        return TaskDetailResponse.from(task);
    }

    // 첨부파일/댓글 등 하위 리소스 조회 전, 담당자/팀원인지 검증
    @Transactional(readOnly = true)
    public void validateTaskAccess(Long taskId, Long currentUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        taskAccessValidator.validateParticipant(task, currentUserId);
    }

}
