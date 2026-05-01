FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY discovery-server/pom.xml discovery-server/pom.xml
COPY config-server/pom.xml config-server/pom.xml
COPY identity-service/pom.xml identity-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY inventory-service/pom.xml inventory-service/pom.xml
COPY cart-service/pom.xml cart-service/pom.xml
COPY voucher-service/pom.xml voucher-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY flash-sale-service/pom.xml flash-sale-service/pom.xml
COPY common/src common/src
COPY flash-sale-service/src flash-sale-service/src

RUN mvn -q -pl flash-sale-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/flash-sale-service/target/*.jar app.jar
EXPOSE 8093
ENTRYPOINT ["java", "-jar", "app.jar"]
