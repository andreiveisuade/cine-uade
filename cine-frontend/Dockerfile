# Etapa 1: Vite compila el front a estáticos. Node solo existe acá: no llega a la imagen final.
FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY index.html admin.html vite.config.js ./
COPY src/ ./src/
RUN npm run build

# Etapa 2: unprivileged corre como uid 101 y trae resueltos los paths de cache y pid.
FROM nginxinc/nginx-unprivileged:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080
