FROM eclipse-temurin:21-jre
WORKDIR /app
COPY sofit-user/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]