# Multi-stage Dockerfile for ScanPay Backend (Ktor + Astra Cassandra)
# Stage 1: Build stage using official Gradle image
FROM gradle:8.10-jdk17-alpine AS builder

WORKDIR /app

# Exclude Android app module during backend container build
ENV BUILD_BACKEND_ONLY=true

# Copy project files
COPY . .

# Support both repository-root context and backend-subfolder context
RUN if [ -d "backend" ]; then \
        echo "Building from repository root..." && \
        (chmod +x ./gradlew 2>/dev/null || true) && \
        (./gradlew :backend:installDist --no-daemon || gradle :backend:installDist --no-daemon) && \
        mkdir -p /dist && cp -r backend/build/install/backend/* /dist/; \
    else \
        echo "Building from backend directory..." && \
        gradle installDist --no-daemon && \
        mkdir -p /dist && cp -r build/install/backend/* /dist/; \
    fi

# Stage 2: Lightweight runtime stage
FROM eclipse-temurin:17-jre-jammy

# Create non-root user for security
RUN useradd -m -u 1000 scanpay

WORKDIR /app

# Copy built distribution from builder stage
COPY --from=builder /dist /app

# Ensure correct permissions
RUN chmod +x /app/bin/backend && chown -R scanpay:scanpay /app

USER scanpay

# Port configuration (Render assigns PORT dynamically)
ENV PORT=5000
EXPOSE 5000

ENTRYPOINT ["bin/backend"]

