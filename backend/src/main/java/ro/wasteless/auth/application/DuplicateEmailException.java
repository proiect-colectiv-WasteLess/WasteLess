package ro.wasteless.auth.application;

import org.springframework.http.HttpStatus;
import ro.wasteless.common.api.*;

public class DuplicateEmailException extends DomainException {
  public DuplicateEmailException() {
    super(ErrorCode.CONFLICT, HttpStatus.CONFLICT, "Email is already registered");
  }
}
