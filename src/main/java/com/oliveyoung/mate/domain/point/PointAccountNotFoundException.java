package com.oliveyoung.mate.domain.point;

import com.oliveyoung.mate.domain.point.vo.CrewId;

public class PointAccountNotFoundException extends RuntimeException {
    public PointAccountNotFoundException(CrewId crewId) {
        super("포인트 계좌를 찾을 수 없습니다. crewId=" + crewId.id());
    }
}