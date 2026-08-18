package com.oliveyoung.mate.application;

import java.time.LocalDate;

/**
 * 배치/스케줄러 실행 결과 집계 (텔레그램 리포트용).
 *
 * @param skipped 정상적으로 건너뛴 건 (중복 등록, 결근, 종료일 초과 등) — 장애가 아님
 * @param failed  예상치 못한 예외로 처리하지 못한 건 — 에러 알림 대상
 */
public record JobReport(String jobName, LocalDate date, int success, int skipped, int failed) {

    public static JobReport of(String jobName, LocalDate date, int success, int failed) {
        return new JobReport(jobName, date, success, 0, failed);
    }
}
