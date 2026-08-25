package com.oliveyoung.mate.presentation;

import com.oliveyoung.mate.application.product.ProductUploadException;
import com.oliveyoung.mate.domain.point.InsufficientPointException;
import com.oliveyoung.mate.domain.point.PointAccountNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final TelegramNotifier telegramNotifier;

    // 400 — Bean Validation 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .findFirst()
            .orElse("입력값이 올바르지 않습니다.");
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("INVALID_INPUT", message));
    }

    // 400 — 잘못된 요청
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
    }

    // 409 — 상태 충돌
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleConflict(IllegalStateException e) {
        return ResponseEntity.status(409)
            .body(new ErrorResponse("CONFLICT", e.getMessage()));
    }

    // 403
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(403)
            .body(new ErrorResponse("ACCESS_DENIED", "접근 권한이 없습니다."));
    }

    // 404
    @ExceptionHandler(PointAccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(PointAccountNotFoundException e) {
        return ResponseEntity.status(404)
            .body(new ErrorResponse("POINT_ACCOUNT_NOT_FOUND", e.getMessage()));
    }

    // 422
    @ExceptionHandler(InsufficientPointException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientPoint(InsufficientPointException e) {
        return ResponseEntity.status(422)
            .body(new ErrorResponse("INSUFFICIENT_POINT", e.getMessage()));
    }

    // 404
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException e) {
        return ResponseEntity.status(404)
            .body(new ErrorResponse("NOT_FOUND", "요청한 경로를 찾을 수 없습니다."));
    }

    // 400 — 상품 엑셀 업로드 파일이 유효하지 않음
    @ExceptionHandler(ProductUploadException.class)
    public ResponseEntity<ErrorResponse> handleProductUploadInvalid(ProductUploadException e) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("PRODUCT_UPLOAD_INVALID", e.getMessage()));
    }

    // 409 — DB 유니크 제약 위반 등 (정상적인 재시도 상황일 수 있어 슬랙 알림은 보내지 않음)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Data integrity violation: {}", e.getMessage());
        return ResponseEntity.status(409)
            .body(new ErrorResponse("CONFLICT", "이미 처리된 요청입니다."));
    }

    // 500 — 텔레그램 알림 추가
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception e,
            HttpServletRequest request) {
        log.error("Unhandled exception at {}", request.getRequestURI(), e);
        telegramNotifier.sendError(e, request.getRequestURI());
        return ResponseEntity.status(500)
            .body(new ErrorResponse("INTERNAL_ERROR", "서버 오류가 발생했습니다."));
    }

    public record ErrorResponse(String code, String message) {}
}