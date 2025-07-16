FROM maven:3.8-openjdk-11 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package

FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=build /app/target/marketplace.jar app.jar
COPY config.properties .
COPY orders.json .
ENTRYPOINT ["java", "-jar", "app.jar"]
