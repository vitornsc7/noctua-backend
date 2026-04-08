FROM maven:3.9.6-eclipse-temurin-21 AS deps

WORKDIR /app

COPY pom.xml ./
RUN mvn dependency:go-offline -q

FROM deps AS builder

COPY src ./src
RUN mvn clean package -DskipTests -q

FROM deps AS dev

COPY . .
EXPOSE 8080
CMD ["mvn", "spring-boot:run"]

FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]