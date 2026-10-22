# La imagen de Render: el backend sirve también el build de React, así web y API comparten origen https
# (cookie de sesión y vuelta de Mercado Pago sin proxy ni CORS). El compose local sigue con nginx.

# ---------- front ----------
FROM node:24-alpine AS front
WORKDIR /front
COPY cine-frontend/package.json cine-frontend/package-lock.json ./
RUN npm ci
COPY cine-frontend/index.html cine-frontend/vite.config.js ./
COPY cine-frontend/public ./public
COPY cine-frontend/src ./src
RUN npm run build

# ---------- back ----------
FROM maven:3.9-eclipse-temurin-21 AS back
WORKDIR /build
COPY cine-backend/pom.xml .
RUN mvn -B -q dependency:go-offline
COPY cine-backend/src ./src
# Spring Boot sirve classpath:/static en la raíz: el index.html del front queda como página de bienvenida.
COPY --from=front /front/dist ./src/main/resources/static
RUN mvn -B -q package -DskipTests

# ---------- runtime ----------
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S cine && adduser -S -G cine cine
WORKDIR /app
COPY --from=back /build/target/cine-api.war ./cine-api.war
RUN mkdir -p /app/tickets /app/imagenes && chown -R cine:cine /app
USER cine

ENV TZ=America/Argentina/Buenos_Aires
# Render free da 512 MB: con el 25 % de fábrica el heap queda en 128 MB.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/cine-api.war"]
