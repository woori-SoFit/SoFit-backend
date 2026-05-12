FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN ./gradlew :sofit-admin:bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/sofit-admin/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]