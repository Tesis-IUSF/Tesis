#!/usr/bin/env bash

# Detener el script inmediatamente si cualquier comando falla
set -e

# Trampa para mostrar un mensaje si el script se interrumpe por un error
trap 'echo "Error: El despliegue ha fallado en la línea $LINENO."' ERR

# Obtener la ruta absoluta del directorio donde se encuentra este script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Definir la ruta raíz del proyecto (un nivel arriba de scripts/)
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Definir la ruta absoluta al archivo docker-compose
COMPOSE_FILE="$PROJECT_ROOT/docker-compose.prod.yml"

echo "Iniciando el proceso de despliegue en producción..."

echo "Descargando la última versión de las imágenes Docker..."
docker pull suglin/backend:latest
docker pull suglin/frontend:latest

echo "Deteniendo y removiendo los contenedores anteriores..."
docker compose -f "$COMPOSE_FILE" down

echo "Levantando los nuevos contenedores en modo desacoplado (detached)..."
docker compose -f "$COMPOSE_FILE" up -d

echo "¡Despliegue completado con éxito!"