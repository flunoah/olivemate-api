package com.oliveyoung.mate.domain.productrequest.model;

import com.oliveyoung.mate.domain.point.vo.CrewId;
import com.oliveyoung.mate.domain.point.vo.Money;
import java.time.LocalDateTime;
import java.util.UUID;

public class ProductRequest {

    public enum RequestType { NEW, CORRECTION }
    public enum RequestStatus { PENDING, APPROVED, REJECTED }

    private final UUID id;
    private final CrewId crewId;
    private final RequestType requestType;
    private final String productName;
    private final String brand;
    private final Money price;
    private final String note;
    private final UUID linkedProductId;
    private RequestStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    private ProductRequest(UUID id, CrewId crewId, RequestType requestType, String productName,
                            String brand, Money price, String note, UUID linkedProductId,
                            RequestStatus status, LocalDateTime createdAt, LocalDateTime reviewedAt) {
        this.id = id;
        this.crewId = crewId;
        this.requestType = requestType;
        this.productName = productName;
        this.brand = brand;
        this.price = price;
        this.note = note;
        this.linkedProductId = linkedProductId;
        this.status = status;
        this.createdAt = createdAt;
        this.reviewedAt = reviewedAt;
    }

    public static ProductRequest create(CrewId crewId, RequestType requestType, String productName,
                                         String brand, Money price, String note, UUID linkedProductId) {
        if (requestType == RequestType.CORRECTION && linkedProductId == null) {
            throw new IllegalArgumentException("정정 요청은 연결할 상품이 필요합니다.");
        }
        return new ProductRequest(
            UUID.randomUUID(), crewId, requestType, productName, brand, price, note,
            linkedProductId, RequestStatus.PENDING, LocalDateTime.now(), null
        );
    }

    // DB 복원용 — 반드시 이걸 써야 id/status/reviewedAt이 DB 값 그대로 복원됨
    public static ProductRequest reconstruct(UUID id, CrewId crewId, RequestType requestType, String productName,
                                              String brand, Money price, String note, UUID linkedProductId,
                                              RequestStatus status, LocalDateTime createdAt, LocalDateTime reviewedAt) {
        return new ProductRequest(id, crewId, requestType, productName, brand, price, note,
            linkedProductId, status, createdAt, reviewedAt);
    }

    public void approve() {
        if (status != RequestStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 요청입니다.");
        }
        this.status = RequestStatus.APPROVED;
        this.reviewedAt = LocalDateTime.now();
    }

    public void reject() {
        if (status != RequestStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 요청입니다.");
        }
        this.status = RequestStatus.REJECTED;
        this.reviewedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public CrewId getCrewId() { return crewId; }
    public RequestType getRequestType() { return requestType; }
    public String getProductName() { return productName; }
    public String getBrand() { return brand; }
    public Money getPrice() { return price; }
    public String getNote() { return note; }
    public UUID getLinkedProductId() { return linkedProductId; }
    public RequestStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
}
