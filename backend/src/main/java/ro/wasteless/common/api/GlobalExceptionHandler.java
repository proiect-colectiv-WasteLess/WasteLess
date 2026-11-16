package ro.wasteless.common.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(DomainException.class)
  ProblemDetail domain(DomainException e) {
    if (java.util.Set.of(
            ErrorCode.CONFLICT, ErrorCode.ORDER_NOT_CANCELABLE, ErrorCode.INVALID_PICKUP_CODE)
        .contains(e.code()))
      org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class)
          .warn("Order action rejected: {}", e.code());
    return ProblemResponses.of(e.status(), e.code(), e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail validation(MethodArgumentNotValidException e) {
    var p =
        ProblemResponses.of(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Invalid request");
    p.setProperty(
        "fieldErrors",
        e.getBindingResult().getFieldErrors().stream()
            .map(
                f ->
                    java.util.Map.of(
                        "field",
                        f.getField(),
                        "message",
                        f.getDefaultMessage() == null ? "Invalid value" : f.getDefaultMessage()))
            .toList());
    return p;
  }

  @ExceptionHandler({
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
    org.springframework.web.bind.MissingRequestHeaderException.class,
    org.springframework.web.bind.MissingServletRequestParameterException.class,
    jakarta.validation.ConstraintViolationException.class
  })
  ProblemDetail invalid(Exception e) {
    return ProblemResponses.of(
        HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Invalid request");
  }

  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail forbidden(Exception e) {
    return ProblemResponses.of(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Access denied");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ProblemDetail conflict(Exception e) {
    return ProblemResponses.of(
        HttpStatus.CONFLICT, ErrorCode.CONFLICT, "Resource conflicts with existing data");
  }
}
