FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY services/product-service/pom.xml services/product-service/
RUN mvn dependency:go-offline -pl services/product-service -am -q
COPY services/product-service/src services/product-service/src
RUN mvn package -pl services/product-service -am -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/services/product-service/target/*.jar app.jar
RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
