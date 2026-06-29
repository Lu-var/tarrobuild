FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY services/notification-service/pom.xml services/notification-service/
RUN mvn dependency:go-offline -pl services/notification-service -am -q
COPY services/notification-service/src services/notification-service/src
RUN mvn package -pl services/notification-service -am -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/services/notification-service/target/*.jar app.jar
RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
