---
name: debug-build-failure
description: 빌드나 서버 실행이 안 될 때 사용. "빌드 안 돼", "gradlew build 실패", "이 에러 뭐야", "앱이 안 켜져", "컴파일 에러 났어", 또는 에러 로그를 그대로 붙여넣는 경우에 해당. 이 프로젝트에서 반복되는 환경 문제(.env 미로드, Gradle Daemon 캐싱, 의존성 scope, BouncyCastle 미등록)의 증상별 진단표를 제공한다.
---

# 빌드 실패 진단

## 증상별 원인

| 에러 메시지 | 원인 | 해결 |
|---|---|---|
| `Unable to determine Dialect without JDBC metadata` | `.env` 미로드 | `export $(grep -v '^#' .env \| xargs)` |
| `.env` 고쳤는데 계속 같은 에러 | Gradle Daemon이 구 환경변수 캐싱 | `./gradlew --stop` 후 재실행 |
| `Could not resolve placeholder 'x.y.z'` | 프로퍼티 미정의, 또는 `${}` 안에 변수명 대신 **값**을 넣음 | `${VAR_NAME}` 형태 확인 |
| `cannot access {클래스}` / `class file for ... not found` | 의존성이 `runtime` scope라 컴파일 classpath에 없음 | `build.gradle`에 `implementation`으로 명시 추가 |
| `NoSuchProviderException` | BouncyCastle Provider 미등록 | `static { Security.addProvider(new BouncyCastleProvider()); }` |
| `cannot find symbol: class {X}` | 파일 자체가 없거나, 그 파일에 별도 에러가 있어 타입 인식 실패 | 해당 파일 존재 여부부터 확인 |
| `{X} is already defined` | 같은 인터페이스/클래스를 중복 생성 | `find src -name "{X}.java"`로 중복 확인 |
| `must implement the inherited abstract method` | 인터페이스에만 메서드 추가하고 구현체 누락 | RepositoryImpl에 `@Override` 추가 |

## 표준 진단 순서

```bash
# 1. 환경변수 로드
export $(grep -v '^#' .env | xargs)
echo $DB_URL    # 비어있으면 이게 원인

# 2. 데몬 초기화
./gradlew --stop

# 3. 컴파일만 빠르게
./gradlew compileJava

# 4. 의존성 문제 의심 시
./gradlew build --refresh-dependencies
```

**주의**: VSCode의 problems 패널은 캐시 때문에 실제 `javac` 결과와 다를 수 있다. 최종 판단은 `./gradlew compileJava`로 한다.