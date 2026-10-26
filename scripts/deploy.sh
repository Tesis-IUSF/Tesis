#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="$PROJECT_ROOT/docker-compose.prod.yml"

cd "$PROJECT_ROOT"

on_error() {
    local exit_code=$?
    printf 'Error: el despliegue falló en la línea %s (código %s).\n' \
        "${BASH_LINENO[0]}" "$exit_code" >&2
    printf 'Los servicios podrían haber quedado parcialmente actualizados; revisa con:\n' >&2
    printf '  docker compose -f %q ps\n' "$COMPOSE_FILE" >&2
    printf '  docker compose -f %q logs --tail=100 backend frontend\n' "$COMPOSE_FILE" >&2
    exit "$exit_code"
}
trap on_error ERR

if [[ ! -f "$PROJECT_ROOT/.env" ]]; then
    printf 'Error: falta %s. Crea el archivo desde .env.example y configura los valores de producción.\n' \
        "$PROJECT_ROOT/.env" >&2
    exit 1
fi

if [[ ! -f "$COMPOSE_FILE" ]]; then
    printf 'Error: no se encontró el archivo Compose: %s\n' "$COMPOSE_FILE" >&2
    exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
    printf 'Error: Docker no está instalado o no está disponible en PATH.\n' >&2
    exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
    printf 'Error: Docker Compose v2 no está instalado o no está disponible.\n' >&2
    exit 1
fi

printf 'Validando configuración de producción...\n'
docker compose -f "$COMPOSE_FILE" config --quiet

printf 'Descargando las imágenes configuradas para backend y frontend...\n'
docker compose -f "$COMPOSE_FILE" pull backend frontend

printf 'Actualizando servicios y esperando que estén saludables (máximo 180 segundos)...\n'
docker compose -f "$COMPOSE_FILE" up -d --wait --wait-timeout 180

printf '\nEstado de los servicios:\n'
docker compose -f "$COMPOSE_FILE" ps

printf '\nDespliegue completado. Los volúmenes de base de datos e imágenes se conservaron.\n'
