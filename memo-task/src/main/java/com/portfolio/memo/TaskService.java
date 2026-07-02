package com.portfolio.memo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.memo.dto.*;
import com.portfolio.memo.file.AttachedFileService;
import com.portfolio.memo.CustomException.ResourceNotFoundException;
import com.portfolio.memo.client.AuthClient;
import com.portfolio.memo.outbox.domain.OutboxEvent;
import com.portfolio.memo.outbox.domain.OutboxEventRepository;
import com.portfolio.memo.outbox.domain.OutboxEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final AttachedFileService attachedFileService;
    private final AuthClient authClient;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;

    // 업무 생성
    @Transactional
    public Task createTask(TaskCreateRequest request, List<MultipartFile> files, Long currentUserId, String currentUserName) {

        // 입력 리스트 builder null 방지용
        List<Long> memberIds = (request.getMemberIds() == null)
                ? new ArrayList<>()
                : new ArrayList<>(request.getMemberIds());

        // 1. Task 저장
        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .assigneeId(currentUserId)
                .assigneeName(currentUserName)
                .memberIds(memberIds)
                .dueDate(request.getDueDate())
                .priority(request.getPriority())
                .status(TaskStatus.TODO) // 생성 시 기본 상태는 'TODO'
                .build();

        Task savedTask = taskRepository.save(task); // Task 만 저장

        // 2.파일 업로드 로직은 AttachedFileService 에 위임
        attachedFileService.uploadFiles(savedTask, files);

        // 3. 알림 이벤트 DTO 생성 -> Outbox 기록
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
                .orElseThrow(() -> new ResourceNotFoundException(taskId));

        // 현재 요청자가 담당자일 경우만 삭제 가능
        if (!task.getAssigneeId().equals(currentUserId)) {
            throw new IllegalArgumentException("You do not have permission to delete this task.");
        }

        taskRepository.delete(task);
    }

    // 업무 수정
    @Transactional
    public TaskResponse updateTask(Long taskId, TaskUpdateRequest request, Long currentUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(taskId));

        // 현재 로그인한 사용자가 해당 업무의 담당자인지 확인
        if (!task.getAssigneeId().equals(currentUserId)) {
            throw new IllegalArgumentException("You do not have permission to edit this task.");
        }

        // 담당자를 변경하는 경우, 새로운 담당자 정보를 조회
        Long newAssigneeId = request.getAssigneeId();
        if (newAssigneeId != null) {
            // 사용자 존재 여부 검증 (유저 서비스 REST API 호출)
            if (!authClient.existsUser(newAssigneeId)) {
                throw new IllegalArgumentException("Assignee does not exist.");
            }

        }

        // 팀원을 변경하는 경우, 새로운 팀원 정보 조회
        List<Long> newMemberIds = request.getMemberIds();
        if (newMemberIds != null) {
            for (Long newMemberId : newMemberIds) {
                if (!authClient.existsUser(newMemberId)) {
                    throw new IllegalArgumentException("Member does not exist.");
                }
            }
        }

        task.update(
                request.getTitle(),
                request.getDescription(),
                request.getStatus(),
                request.getPriority(),
                request.getDueDate(),
                newAssigneeId,
                newMemberIds
        );

        Task updatedTask = taskRepository.save(task);

        return TaskResponse.from(updatedTask);
    }

    // 업무 상세에서 상세 정보 조회 용
    @Transactional(readOnly = true)
    public TaskDetailResponse getTaskDetail(Long taskId) {
        Task task = taskRepository.findTaskDetail(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(taskId));

        return TaskDetailResponse.from(task);
    }

//
//    @Transactional
//    // 업무 팀원 배정 + 업무 배정 알림(담당자 알림 X)
//    public Task assignMembers(Long taskId, List<Long> memberIds) {
//
//        Task task = taskRepository.findById(taskId)
//                .orElseThrow(() -> new RuntimeException("Task not found"));
//
//        List<User> members = userRepository.findAllById(memberIds);
//
//        // task에 팀원 저장
//        task.setMembers(members);
//        taskRepository.save(task);
//
//        // 여러명의 팀원에게 Redis Publish로 1명씩 전송(Redis Pub/Sub는 한 번에 1명에게만 전송되어야함)
//        for (User member : members) {
//            NotificationEventDto event = NotificationEventDto.builder()
//                    .userId(member.getId())
//                    .type(NotificationEventType.TASK_ASSIGNED)
//                    .message("새로운 업무가 배정되었습니다.")
//                    .data(Map.of("taskId", taskId))
//                    .timestamp(System.currentTimeMillis())
//                    .build();
//
//            // Redis Publish
//            notificationPublisher.publish(event);
//        }
//
//        return task;
//    }

}
