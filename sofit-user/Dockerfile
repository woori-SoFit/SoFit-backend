FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN ./gradlew :sofit-user:bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/sofit-user/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]