# Etap 1: Budowanie aplikacji (Node.js)
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Etap 2: Serwowanie plików (Nginx)
FROM nginx:alpine
# Kopiujemy zbudowane pliki do serwera Nginx
# UWAGA: Jeśli używasz Vite, zmień /app/build na /app/dist
COPY --from=build /app/build /usr/share/nginx/html

# Konfiguracja dla React Router (żeby odświeżanie strony nie dawało 404)
# Tworzymy prosty config w locie
RUN echo 'server { \
    listen 80; \
    location / { \
        root /usr/share/nginx/html; \
        index index.html index.htm; \
        try_files $uri $uri/ /index.html; \
    } \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]