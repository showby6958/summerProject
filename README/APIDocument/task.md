# 업무 API (java/com/portfolio/memo/task)
사내 메신저 기반 업무 관리

## 업무 생성 POST /api/tasks/{id} [이동](#업무-생성)
1. 제목/설명 입력
2. 담당자 지정
3. 마감일 설정
4. 우선순위 선택(LOW/MEDIUM/HIGH/URGENT)
5. 업무상태 선택(TODO/IN_PROGRESS/DONE)
6. [추후 파일 등록 기능도 추가 예정]

## 업무 목록 조회
## 업무 상세 보기
## 업무 상태 변경
## 업무 수정
## 댓글 기킁
## 알림 기능




# API 세부사항
## 업무 생성
사용자가 업무를 생성할 수 있습니다.
- 제목(title)/설명(description): 업무 제목과 설명을 등록합니다.
- 담당자 지정(assignee): 해당 업무를 담당할 담당자의 ID를 등록합니다(담당자를 등록 하지 않을 시, 업무 등록자의 ID가 자동으로 배정됩니다.)
- 마감일 설정(dueDate): 해당 업무의 마감일을 설정합니다.
- 우선순위 설정(priority): 해당 업무의 우선순위(LOW/MEDIUM/HIGH/URGENT)를 설정합니다.
- 업무상태 설정(status): 해당 업무의 업무상태(TODO/IN_PROGRESS/DONE)를 설정합니다.
- [추후 파일 등록 기능도 추가 예정]
```
{
  "id": 1,
  "title": "새 업무 생성 테스트",
  "description": "새 업무 설명 123",
  "status": "TODO",
  "priority": "URGENT",
  "dueDate": "2025-09-30T03:00:00",
  "assigneeId": 3,
  "assigneeName": "홍길동",
  "createdAt": "2025-09-14T15:54:50.1785538"
}
```
