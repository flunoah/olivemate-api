package com.oliveyoung.mate.domain.point.repository;

import com.oliveyoung.mate.domain.point.vo.PointPolicy;
import java.util.Optional;
import java.util.UUID;

public interface PointPolicyRepository {
    Optional<PointPolicy> findActivePolicy();
    void save(PointPolicy policy, UUID createdBy);
}