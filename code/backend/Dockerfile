FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache tzdata curl \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

WORKDIR /app

COPY oa-web/target/oa-web-1.0.0.jar app.jar

RUN addgroup -S oa && adduser -S oa -G oa \
    && chown -R oa:oa /app

USER oa:oa

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD curl -sf http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
