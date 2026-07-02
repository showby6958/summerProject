package com.portfolio.memo.CustomException;

public class ResourceNotFoundException extends RuntimeException {
  public ResourceNotFoundException(Long assigneeId) {
    super("assigneeId: " + assigneeId + "is not founded");
  }
}
