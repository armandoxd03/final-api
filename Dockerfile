# Build stage
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies first to leverage Docker cache
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copy only the built JAR file
COPY --from=build /app/target/*.jar app.jar
# Add non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]