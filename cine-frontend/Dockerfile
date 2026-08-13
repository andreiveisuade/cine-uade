# Imagen unprivileged: corre como uid 101 y trae resueltos los paths de cache y pid,
# que en la nginx normal quieren escribir en /var/cache/nginx y /var/run como root.
FROM nginxinc/nginx-unprivileged:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf

WORKDIR /usr/share/nginx/html
COPY index.html admin.html ./
COPY js/ ./js/
COPY mock/ ./mock/

EXPOSE 8080
