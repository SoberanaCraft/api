FROM gradle:7-jdk11 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle shadowJar --no-daemon

FROM openjdk:11
LABEL org.opencontainers.image.source="https://github.com/soberanacraft/api.git"
LABEL org.opencontainers.image.description="Imagem docker da api do soberanacraft"
LABEL org.opencontainers.image.licenses="CC0"

EXPOSE 8080:8080
WORKDIR /app
COPY --from=build /home/gradle/src/build/libs/soberana-backend-all.jar /app/soberana-api.jar
ENTRYPOINT ["java", "-jar", "soberana-api.jar"]