# Stage 1: Build the application using Maven
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# Copy the pom.xml and download project dependencies (cached if unchanged)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source code and build the production jar file
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create a lightweight runtime image using eclipse-temurin JRE
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built jar from Stage 1
COPY --from=build /app/target/resume-parser-backend-0.0.1-SNAPSHOT.jar app.jar

# Expose default port (overridden dynamically by Render PORT env variable)
EXPOSE 8081

# Safe Entrypoint wrapping in a shell to ensure ${PORT} is expanded dynamically
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:8081} -jar app.jar"]
