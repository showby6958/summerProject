package com.portfolio.memo.CustomException;

public class ResourceNotFoundException extends RuntimeException {
  public ResourceNotFoundException(String resourceName, Long id) {
    super(resourceName + " not found. id: " + id);
  }
}
