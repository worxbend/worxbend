FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

COPY airgradient-proxy.jar /app/airgradient-proxy.jar
COPY config/application.conf /app/config/application.conf

ENV JAVA_OPTS="-Xmx256m -Xms64m"

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Dconfig.file=/app/config/application.conf", \
  "-jar", "/app/airgradient-proxy.jar"]
