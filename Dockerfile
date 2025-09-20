########################
# 1. Build stage
########################
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# gradle wrapper と gradle フォルダをコピー
COPY gradlew gradlew
COPY gradle gradle

# ビルドスクリプトをコピー
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

# 依存関係をキャッシュしてダウンロード
RUN ./gradlew --no-daemon dependencies || return 0

# ソースコード(Jooqによる自動生成含む)をコピー
COPY src ./src
COPY ./build/generated-sources ./build/generated-sources

# Spring Boot 実行可能 jar をビルド
RUN ./gradlew --no-daemon bootJar

########################
# 2. Runtime stage
########################
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

# 非特権ユーザーを作成
ARG UID=10001
RUN adduser \
    --disabled-password \
    --gecos "" \
    --home "/nonexistent" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid "${UID}" \
    appuser
USER appuser

# build stage から jar をコピー
COPY --chown=appuser:appuser --from=build /app/build/libs/app.jar app.jar

# 起動コマンド
ENTRYPOINT ["java", "-jar", "app.jar"]
