FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY services/auth-service/pom.xml services/auth-service/
RUN mvn dependency:go-offline -pl services/auth-service -am -q
COPY services/auth-service/src services/auth-service/src
RUN mvn package -pl services/auth-service -am -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/services/auth-service/target/*.jar app.jar
RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
