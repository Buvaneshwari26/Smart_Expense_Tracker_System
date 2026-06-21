package com.tracker.exception;

import com.tracker.dto.ApiResponse;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<ErrorDetails>> handleResourceNotFoundException(
            ResourceNotFoundException exception, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
                LocalDateTime.now(), exception.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(
                new ApiResponse<>(false, exception.getMessage(), errorDetails), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<ErrorDetails>> handleBadRequestException(
            BadRequestException exception, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
                LocalDateTime.now(), exception.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(
                new ApiResponse<>(false, exception.getMessage(), errorDetails), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorDetails>> handleValidationException(
            MethodArgumentNotValidException exception, WebRequest request) {
        String validationErrors = exception.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), "Validation Failed", validationErrors);
        return new ResponseEntity<>(
                new ApiResponse<>(false, "Validation Failed", errorDetails), HttpStatus.BAD_REQUEST);
    }

    // ── Security Exception Handlers ──────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<ErrorDetails>> handleAccessDeniedException(
            AccessDeniedException exception, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
                LocalDateTime.now(), "Access Denied: " + exception.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(
                new ApiResponse<>(false, "Access Denied", errorDetails), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<ErrorDetails>> handleAuthenticationException(
            AuthenticationException exception, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
                LocalDateTime.now(), exception.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(
                new ApiResponse<>(false, "Authentication Failed: " + exception.getMessage(), errorDetails),
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<ErrorDetails>> handleUsernameNotFoundException(
            UsernameNotFoundException exception, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
                LocalDateTime.now(), exception.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(
                new ApiResponse<>(false, "User Not Found: " + exception.getMessage(), errorDetails),
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiResponse<ErrorDetails>> handleJwtException(
            JwtException exception, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
                LocalDateTime.now(), "Invalid or expired JWT token: " + exception.getMessage(),
                request.getDescription(false));
        return new ResponseEntity<>(
                new ApiResponse<>(false, "JWT Error", errorDetails), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorDetails>> handleGlobalException(
            Exception exception, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
                LocalDateTime.now(), exception.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(
                new ApiResponse<>(false, exception.getMessage(), errorDetails),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
