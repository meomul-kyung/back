# syntax=docker/dockerfile:1

# ===== 1) Build stage : JDK 21로 bootJar 빌드 =====
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Gradle wrapper + 빌드 설정 먼저 복사 → 의존성 레이어 캐싱 (소스만 바뀌면 재다운로드 안 함)
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# 소스 복사 후 실행 가능한 jar 빌드 (테스트는 제외해서 빌드 속도 확보)
COPY src src
RUN ./gradlew clean bootJar --no-daemon -x test

# ===== 2) Runtime stage : JRE 21로 실행 (이미지 경량화) =====
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# 빌드 스테이지에서 만든 jar만 가져옴
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
