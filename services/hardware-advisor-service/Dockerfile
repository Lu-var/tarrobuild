FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY services/hardware-advisor-service/pom.xml services/hardware-advisor-service/
RUN mvn dependency:go-offline -pl services/hardware-advisor-service -am -q
COPY services/hardware-advisor-service/src services/hardware-advisor-service/src
RUN mvn package -pl services/hardware-advisor-service -am -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/services/hardware-advisor-service/target/*.jar app.jar
RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
