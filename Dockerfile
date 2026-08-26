FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

# Tailwind CSS standalone CLI (Node 없이 build.gradle의 tailwindBuild 태스크가 사용)
RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates \
    && curl -sL -o /usr/local/bin/tailwindcss \
        https://github.com/tailwindlabs/tailwindcss/releases/latest/download/tailwindcss-linux-x64 \
    && chmod +x /usr/local/bin/tailwindcss \
    && rm -rf /var/lib/apt/lists/*

COPY src src
RUN ./gradlew bootJar --no-daemon

EXPOSE 8080

CMD ["java", "-jar", "build/libs/mate-0.0.1-SNAPSHOT.jar"]