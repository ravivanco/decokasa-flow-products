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
# nginx.conf es una plantilla: al arrancar, la imagen oficial reemplaza ${ARCHIVOS_CUERPO_MAXIMO_MIB} (envsubst solo
# toca variables de entorno definidas; $host, $uri, etc. quedan igual). El valor lo calcula limite-archivos.envsh.
RUN rm -f /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY limite-archivos.envsh /docker-entrypoint.d/15-limite-archivos.envsh
RUN chmod 0755 /docker-entrypoint.d/15-limite-archivos.envsh
COPY --from=compilacion /fuente/dist /usr/share/nginx/html

EXPOSE 80
