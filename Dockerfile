# Build stage: compila o projeto Maven
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml ./
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage: imagem mais leve com JDK apenas para execução
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copia o JAR gerado do build para a imagem final
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]