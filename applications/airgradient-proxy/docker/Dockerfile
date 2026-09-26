FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S app && adduser -S -G app app

WORKDIR /app

COPY airgradient-proxy.jar /app/airgradient-proxy.jar
COPY config/application.conf /app/config/application.conf

ENV JAVA_OPTS="-Xmx256m -Xms64m"

EXPOSE 8080

USER app

ENTRYPOINT ["java", \
  "-Dconfig.file=/app/config/application.conf", \
  "-jar", "/app/airgradient-proxy.jar"]
