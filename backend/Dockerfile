# Use a lightweight Java 17 runtime
FROM eclipse-temurin:17-jre-alpine

# Set the working directory
WORKDIR /app

# Copy the built JAR file from the target folder into the container
# (The GitHub Action builds the JAR before this step)
COPY target/*.jar app.jar

# Expose port 8080
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]