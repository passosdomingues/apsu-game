# ============================================================
# Dockerfile — As Águas de Apsu
# Multi-stage: build com Maven → runtime Java 21
# ============================================================

# ── STAGE 1: BUILD ──────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copiar pom.xml e código fonte para compilar o fat JAR
COPY pom.xml .
COPY src/ ./src/

RUN mvn package -DskipTests -q

# ── STAGE 2: RUNTIME ────────────────────────────────────────
FROM bellsoft/liberica-openjdk-debian:21

LABEL maintainer="Equipe As Águas de Apsu"
LABEL version="0.2.0"
LABEL description="As Águas de Apsu — Jogo 2D JavaFX de Mitologia Mesopotâmica"

# Instalar dependências X11 e Mesa/DRI para renderização gráfica no Linux
RUN apt-get update -qq && \
    apt-get install -y --no-install-recommends \
        libx11-6 \
        libxext6 \
        libxrender1 \
        libxtst6 \
        libxi6 \
        libxrandr2 \
        libgl1-mesa-dri \
        libgl1-mesa-glx \
        fonts-liberation \
        openjfx \
        libopenjfx-java \
        libopenjfx-jni \
        && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copiar o fat JAR gerado no stage de build (que já inclui todas as libs JavaFX)
COPY --from=builder /app/target/apsu-game-*.jar ./apsu-game.jar

ENV DISPLAY=:0

# Executar o jogo com aceleração GPU/X11 e fallback de software
ENTRYPOINT ["java", \
    "-Dprism.order=es2,sw,j2d", \
    "-Dprism.verbose=false", \
    "-jar", "apsu-game.jar"]
