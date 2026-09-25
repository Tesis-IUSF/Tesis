# ==========================================
# Stage 1: Builder
# ==========================================
FROM node:18-alpine AS builder

# Establecer el directorio de trabajo
WORKDIR /app

# Copiar los archivos de dependencias
COPY package.json package-lock.json* ./

# Instalar las dependencias
RUN npm install

# Copiar el resto del código fuente
COPY . .

# Compilar el frontend (generará la carpeta dist)
RUN npm run build

# ==========================================
# Stage 2: Runtime
# ==========================================
FROM nginx:alpine

# Reemplazar la configuración por defecto de Nginx por la nuestra
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Crear el directorio raíz personalizado
WORKDIR /app/dist

# Copiar los archivos compilados desde el stage 1
COPY --from=builder /app/dist /app/dist

# Exponer el puerto 80
EXPOSE 80

# Iniciar Nginx
CMD ["nginx", "-g", "daemon off;"]