#!/bin/bash
set -e

ADMIN_USER="admin"
ADMIN_PASS="booklibre_admin_secret"

wait_for_auth() {
  local host=$1
  echo ">>> Esperando a que el admin exista y acepte auth en $host..."
  until mongosh --host "$host" -u "$ADMIN_USER" -p "$ADMIN_PASS" --authenticationDatabase admin --quiet --eval "db.adminCommand('ping').ok" 2>/dev/null; do
    sleep 2
  done
  echo ">>> $host listo y autenticado"
}

wait_for_auth "configrs/configsvr1:27017,configsvr2:27017,configsvr3:27017"
wait_for_auth "shard1rs/shard1a:27017,shard1b:27017,shard1c:27017"
wait_for_auth "shard2rs/shard2a:27017,shard2b:27017,shard2c:27017"

echo ">>> Todos los replica sets estan inicializados localmente y el usuario admin esta listo."
