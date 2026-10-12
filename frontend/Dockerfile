# Imagen del frontend de Decokasa (paso 1.4). Node compila el build de Vite; nginx lo sirve y envía /api al backend.
# Se construye desde frontend/ (docker compose lo hace con `context: ./frontend`).

# --- Compilación ---
FROM node:24-alpine AS compilacion
WORKDIR /fuente

COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .
RUN npm run build

# --- Servidor ---
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=compilacion /fuente/dist /usr/share/nginx/html

EXPOSE 80
