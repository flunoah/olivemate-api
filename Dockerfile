FROM eclipse-temurin:21-jdk

WORKDIR /app

# ✅ 의존성만 먼저 복사 (캐시 활용)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

# ✅ 소스 나중에 복사 (소스 변경해도 의존성 캐시 유지)
COPY src src
RUN ./gradlew bootJar --no-daemon

EXPOSE 8080

CMD ["java", "-jar", "build/libs/mate-0.0.1-SNAPSHOT.jar"]