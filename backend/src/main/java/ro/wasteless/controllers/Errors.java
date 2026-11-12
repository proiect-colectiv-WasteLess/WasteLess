package ro.wasteless.controllers;
import ro.wasteless.services.ApiException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.*;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
@RestControllerAdvice
public class Errors {
    public record ErrorBody(Instant timestamp,int status,String message,List<String> details) {}
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorBody> api(ApiException e) { return response(e.status,e.getMessage(),List.of()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorBody> validation(MethodArgumentNotValidException e) {
        return response(400,"Invalid request",e.getBindingResult().getFieldErrors().stream()
            .map(f->f.getField()+": "+f.getDefaultMessage()).toList());
    }
    @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class,ConstraintViolationException.class})
    ResponseEntity<ErrorBody> invalid(Exception e) { return response(400,"Invalid request",List.of()); }
    @ExceptionHandler({DataIntegrityViolationException.class,ConcurrencyFailureException.class})
    ResponseEntity<ErrorBody> conflict(Exception e) { return response(409,"Data or availability conflict",List.of()); }
    static ResponseEntity<ErrorBody> response(int status,String message,List<String> details) {
        return ResponseEntity.status(status).body(new ErrorBody(Instant.now(),status,message,details));
    }
}