# Build stage — uses the project's own Maven wrapper, same as local builds.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# Run stage — smaller JRE-only image, no build tools or source in the final image.
FROM eclipse-temurin:21-jre AS run
WORKDIR /app
COPY --from=build /app/target/mapletalk-backend-0.0.1-SNAPSHOT.jar app.jar

# Documents the default local port only. The app actually binds to the PORT
# environment variable at runtime (server.port=${PORT:8080}), which Render
# supplies automatically.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
