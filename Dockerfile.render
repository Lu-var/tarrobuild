FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY services/api-gateway/pom.xml services/api-gateway/
COPY services/auth-service/pom.xml services/auth-service/
COPY services/build-service/pom.xml services/build-service/
COPY services/category-service/pom.xml services/category-service/
COPY services/compatibility-service/pom.xml services/compatibility-service/
COPY services/estimate-service/pom.xml services/estimate-service/
COPY services/hardware-advisor-service/pom.xml services/hardware-advisor-service/
COPY services/notification-service/pom.xml services/notification-service/
COPY services/product-service/pom.xml services/product-service/
COPY services/provider-service/pom.xml services/provider-service/
COPY services/user-service/pom.xml services/user-service/
COPY services/discovery-server/pom.xml services/discovery-server/
COPY services/api-gateway/src services/api-gateway/src
RUN mvn package -pl services/api-gateway -am -DskipTests -B

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/services/api-gateway/target/*.jar app.jar
RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup && \
    mkdir -p logs && chown -R appuser:appgroup logs
COPY services/api-gateway/entrypoint.sh .
RUN chmod +x entrypoint.sh
USER appuser
EXPOSE 8080
ENTRYPOINT ["./entrypoint.sh"]
