package com.oliveyoung.mate.presentation;

import com.oliveyoung.mate.domain.point.InsufficientPointException;
import com.oliveyoung.mate.domain.point.PointAccountNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400 — 잘못된 요청 (로그인 실패, 유효하지 않은 파라미터 등)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
    }

    // 409 — 상태 충돌 (중복 가입, 이미 초기화된 포인트, 중복 근무일 등)
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

    // 500 — 예상치 못한 예외는 상세 메시지 노출 없이 로그만 기록
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(500)
            .body(new ErrorResponse("INTERNAL_ERROR", "서버 오류가 발생했습니다."));
    }

    public record ErrorResponse(String code, String message) {}
}