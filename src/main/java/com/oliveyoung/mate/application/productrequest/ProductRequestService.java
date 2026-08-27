package com.oliveyoung.mate.application.productrequest;

import com.oliveyoung.mate.application.productrequest.command.SubmitProductRequestCommand;
import com.oliveyoung.mate.domain.point.vo.CrewId;
import com.oliveyoung.mate.domain.point.vo.Money;
import com.oliveyoung.mate.domain.product.model.Product;
import com.oliveyoung.mate.domain.product.repository.ProductRepository;
import com.oliveyoung.mate.domain.productrequest.model.ProductRequest;
import com.oliveyoung.mate.domain.productrequest.repository.ProductRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductRequestService {

    private final ProductRequestRepository productRequestRepository;
    private final ProductRepository        productRepository;

    @Transactional
    public void submit(SubmitProductRequestCommand cmd) {
        ProductRequest request = ProductRequest.create(
            CrewId.of(cmd.crewId()),
            cmd.requestType(),
            cmd.productName(),
            cmd.brand(),
            cmd.price() == null ? null : Money.of(cmd.price()),
            cmd.note(),
            cmd.linkedProductId()
        );
        productRequestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<ProductRequest> getPending() {
        return productRequestRepository.findAllByStatus(ProductRequest.RequestStatus.PENDING);
    }

    // 승인 시 NEW는 신규 Product 생성, CORRECTION은 연결된 Product를 갱신한다.
    // 크루가 가격을 모르면(price=null) 0원으로 임시 등록 — 이후 관리자 엑셀 업로드(goods_no upsert)로 정정된다.
    @Transactional
    public void approve(UUID id) {
        ProductRequest request = productRequestRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("요청을 찾을 수 없습니다."));

        if (request.getRequestType() == ProductRequest.RequestType.NEW) {
            String goodsNo = "REQ-" + request.getId();
            Money price = request.getPrice() != null ? request.getPrice() : Money.zero();
            Product product = Product.create(
                goodsNo, request.getBrand(), request.getProductName(), price, price, LocalDateTime.now()
            );
            productRepository.save(product);
        } else {
            Product product = productRepository.findById(request.getLinkedProductId())
                .orElseThrow(() -> new IllegalStateException("연결된 상품을 찾을 수 없습니다."));
            String brand = request.getBrand() != null ? request.getBrand() : product.getBrand();
            Money salePrice = request.getPrice() != null ? request.getPrice() : product.getSalePrice();
            product.update(brand, request.getProductName(), product.getRegularPrice(), salePrice, LocalDateTime.now());
            productRepository.save(product);
        }

        request.approve();
        productRequestRepository.save(request);
    }

    @Transactional
    public void reject(UUID id) {
        ProductRequest request = productRequestRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("요청을 찾을 수 없습니다."));
        request.reject();
        productRequestRepository.save(request);
    }
}
