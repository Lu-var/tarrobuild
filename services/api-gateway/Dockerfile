FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY services/api-gateway/pom.xml services/api-gateway/
RUN mvn dependency:go-offline -pl services/api-gateway -am -q
COPY services/api-gateway/src services/api-gateway/src
RUN mvn package -pl services/api-gateway -am -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/services/api-gateway/target/*.jar app.jar
RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
