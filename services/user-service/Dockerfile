FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/*.jar app.jar
RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup
USER appuser
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]