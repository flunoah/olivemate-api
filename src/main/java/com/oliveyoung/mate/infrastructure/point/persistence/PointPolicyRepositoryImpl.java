package com.oliveyoung.mate.infrastructure.point.persistence;

import com.oliveyoung.mate.domain.point.repository.PointPolicyRepository;
import com.oliveyoung.mate.domain.point.vo.PointPolicy;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PointPolicyRepositoryImpl implements PointPolicyRepository {

    @Override
    public Optional<PointPolicy> findActivePolicy() {
        // 현재는 기본 정책 반환 (4000원, 1일 후 지급, 30일 만료)
        return Optional.of(PointPolicy.defaultPolicy());
    }

    @Override
    public void save(PointPolicy policy, UUID createdBy) {
        // 추후 DB 저장 구현
    }
}