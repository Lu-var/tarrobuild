FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY services/category-service/pom.xml services/category-service/
RUN mvn dependency:go-offline -pl services/category-service -am -q
COPY services/category-service/src services/category-service/src
RUN mvn package -pl services/category-service -am -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/services/category-service/target/*.jar app.jar
RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
