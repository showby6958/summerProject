package com.portfolio.memo.task.CustomException;

public class ResourceNotFoundException extends RuntimeException {
  public ResourceNotFoundException(Long assigneeId) {
    super("assigneeId: " + assigneeId + "is not founded");
  }
}
