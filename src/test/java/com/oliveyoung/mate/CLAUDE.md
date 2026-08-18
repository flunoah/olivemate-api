# Tests

- `@SpringBootTest`는 최소화. 도메인 객체(`Point`, `PointLedger`)는 순수 JUnit으로 테스트 가능.
- 컨텍스트 로딩 테스트는 `.env` 환경변수가 필요하다.
- FIFO 차감·만료·취소 로직은 반드시 단위 테스트를 동반할 것.