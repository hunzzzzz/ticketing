FROM openjdk:21-slim
VOLUME /tmp
CMD ["./gradlew", "clean", "bootJar"]
COPY /build/libs/*jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]