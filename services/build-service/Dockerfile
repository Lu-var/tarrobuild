FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY services/build-service/pom.xml services/build-service/
RUN mvn dependency:go-offline -pl services/build-service -am -q
COPY services/build-service/src services/build-service/src
RUN mvn package -pl services/build-service -am -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/services/build-service/target/*.jar app.jar
RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
