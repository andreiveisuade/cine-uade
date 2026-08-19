# Multi-stage: el JDK y el código fuente quedan en la etapa de build y no viajan a la
# imagen final, que solo necesita un JRE y el jar.

# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# El pom va solo y primero: las dependencias cambian mucho menos que el código, así que
# mientras no se toque el pom esta capa se reusa y no vuelve a bajar Maven Central.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Los tests corren en la máquina de desarrollo con `mvn test`; acá solo se arma el
# artefacto. El plugin de Spring Boot deja las dependencias adentro del jar ejecutable.
RUN mvn -B -q package -DskipTests

# ---------- runtime ----------
FROM eclipse-temurin:21-jre-alpine

# Usuario propio: el proceso no necesita root para nada, y si algo se escapa del
# programa no arranca con permisos sobre el sistema de archivos de la imagen.
RUN addgroup -S cine && adduser -S -G cine cine

WORKDIR /app
COPY --from=build /build/target/cine-api.jar ./cine-api.jar

# GeneradorTicketTxt escribe en tickets/ relativo al working dir, así que el directorio
# tiene que existir y ser del usuario que corre el proceso.
RUN mkdir -p /app/tickets && chown -R cine:cine /app

USER cine

# Solo se publica dentro de la red interna de Docker: el único contenedor que expone un
# puerto al host es nginx, que hace de reverse proxy.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/cine-api.jar"]
