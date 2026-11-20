package ro.wasteless.business.api;

import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import ro.wasteless.common.api.*;

@Order(-10)
@RestControllerAdvice(basePackages = {"ro.wasteless.business.api", "ro.wasteless.offer.api"})
public class CatalogExceptionHandler {
  @ExceptionHandler(CatalogValidationException.class)
  public ProblemDetail validation(CatalogValidationException e) {
    var p =
        ProblemResponses.of(
            HttpStatus.UNPROCESSABLE_CONTENT, ErrorCode.VALIDATION_FAILED, e.getMessage());
    p.setProperty("fieldErrors", e.fields());
    return p;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail beanValidation(MethodArgumentNotValidException e) {
    var p =
        ProblemResponses.of(
            HttpStatus.UNPROCESSABLE_CONTENT, ErrorCode.VALIDATION_FAILED, "Invalid request");
    p.setProperty(
        "fieldErrors",
        e.getBindingResult().getFieldErrors().stream()
            .map(
                f ->
                    java.util.Map.of(
                        "field", f.getField(), "message", String.valueOf(f.getDefaultMessage())))
            .toList());
    return p;
  }
}
