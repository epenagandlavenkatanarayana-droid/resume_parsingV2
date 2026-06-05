# Stage 1: Build the application using Maven
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# Copy the pom.xml and download project dependencies (cached if unchanged)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source code and build the production jar file
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create a lightweight runtime image using eclipse-temurin JRE (Debian-based, NOT Alpine)
# Alpine (musl libc) breaks Firebase Admin SDK's gRPC native libraries
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the built jar from Stage 1
COPY --from=build /app/target/resume-parser-backend-0.0.1-SNAPSHOT.jar app.jar

# Expose Render's default port
EXPOSE 10000

# Run the jar directly. Spring Boot will read the PORT env variable automatically.
ENTRYPOINT ["java", "-jar", "app.jar"]
