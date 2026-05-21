#!/bin/bash
# â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
# Fase 1: inicializa los tres replica sets independientes
# Cada rs.initiate() se ejecuta con --host apuntando al nodo primario
# pero el script corre desde el contenedor mongo-rs-init (fuera del host).
#
# Con keyFile activo, la Localhost Exception solo aplica si te conectÃ¡s
# desde el mismo host (127.0.0.1). Para evitar esto, usamos un script
# que hace exec dentro de cada contenedor.
# â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
set -e

ADMIN_USER="admin"
ADMIN_PASS="booklibre_admin_secret"

# â”€â”€â”€ Helper: esperar a que mongosh pueda conectarse sin auth â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
wait_for_mongo() {
  local host=$1
  local max=30
  local i=0
  echo ">>> Esperando que $host acepte conexiones..."
  until mongosh --host "$host" --port 27017 --quiet --eval "db.adminCommand('ping').ok" 2>/dev/null; do
    i=$((i+1))
    if [ $i -ge $max ]; then
      echo "ERROR: $host no respondiÃ³ a tiempo"
      exit 1
    fi
    sleep 2
  done
  echo ">>> $host listo"
}

# â”€â”€â”€ Helper: esperar a que un replica set haya elegido primary â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
# La elecciÃ³n puede tardar significativamente mÃ¡s en hosts lentos (Docker
# Desktop en Windows/Mac arm). Sin esto, los pasos [4-6/6] fallan con
# "not primary" o exit codes raros si el RS todavÃ­a no eligiÃ³ leader.
wait_for_primary() {
  local rs=$1
  local hosts=$2
  local max=60
  local i=0
  echo ">>> Esperando primary de $rs..."
  while [ $i -lt $max ]; do
    if mongosh --host "$hosts" --quiet --eval '
      const p = rs.status().members.find(m => m.stateStr === "PRIMARY");
      if (p) { quit(0); } else { quit(1); }
    ' >/dev/null 2>&1; then
      echo ">>> $rs primary listo"
      return 0
    fi
    i=$((i+1))
    sleep 2
  done
  echo "ERROR: $rs no eligio primary tras $((max*2))s"
  exit 1
}

# â”€â”€â”€ [1/3] Config Servers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
echo ">>> [1/3] Iniciando replica set configrs"
wait_for_mongo configsvr1
mongosh --host configsvr1 --port 27017 --quiet --eval '
try {
  rs.initiate({
    _id: "configrs",
    configsvr: true,
    members: [
      { _id: 0, host: "configsvr1:27017" },
      { _id: 1, host: "configsvr2:27017" },
      { _id: 2, host: "configsvr3:27017" }
    ]
  });
  print("configrs: initiate OK");
} catch(e) {
  if (e.codeName === "AlreadyInitialized") { print("configrs: ya inicializado"); }
  else { throw e; }
}
'

# â”€â”€â”€ [2/3] Shard 1 â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
echo ">>> [2/3] Iniciando replica set shard1rs"
wait_for_mongo shard1a
mongosh --host shard1a --port 27017 --quiet --eval '
try {
  rs.initiate({
    _id: "shard1rs",
    members: [
      { _id: 0, host: "shard1a:27017" },
      { _id: 1, host: "shard1b:27017" },
      { _id: 2, host: "shard1c:27017" }
    ]
  });
  print("shard1rs: initiate OK");
} catch(e) {
  if (e.codeName === "AlreadyInitialized") { print("shard1rs: ya inicializado"); }
  else { throw e; }
}
'

# â”€â”€â”€ [3/3] Shard 2 â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
echo ">>> [3/3] Iniciando replica set shard2rs"
wait_for_mongo shard2a
mongosh --host shard2a --port 27017 --quiet --eval '
try {
  rs.initiate({
    _id: "shard2rs",
    members: [
      { _id: 0, host: "shard2a:27017" },
      { _id: 1, host: "shard2b:27017" },
      { _id: 2, host: "shard2c:27017" }
    ]
  });
  print("shard2rs: initiate OK");
} catch(e) {
  if (e.codeName === "AlreadyInitialized") { print("shard2rs: ya inicializado"); }
  else { throw e; }
}
'

# â”€â”€â”€ Esperar a que cada replica set haya elegido primary â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
wait_for_primary "configrs" "configrs/configsvr1:27017,configsvr2:27017,configsvr3:27017"
wait_for_primary "shard1rs" "shard1rs/shard1a:27017,shard1b:27017,shard1c:27017"
wait_for_primary "shard2rs" "shard2rs/shard2a:27017,shard2b:27017,shard2c:27017"

# â”€â”€â”€ [4/6] Usuario admin en configrs â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
# Se conecta al replica set por nombre para que mongosh ruteÃ© al primary
# (cualquiera de los 3 hosts puede ser primary tras la elecciÃ³n)
echo ">>> [4/6] Creando usuario admin en configrs"
mongosh --host "configrs/configsvr1:27017,configsvr2:27017,configsvr3:27017" --quiet --eval '
try {
  db.getSiblingDB("admin").createUser({
    user: "admin",
    pwd: "booklibre_admin_secret",
    roles: [{ role: "root", db: "admin" }]
  });
  print("Usuario admin creado en configrs");
} catch(e) {
  if (e.codeName === "Location51003" || e.code === 51003) { print("Usuario admin ya existe en configrs"); }
  else { throw e; }
}
'

# â”€â”€â”€ [5/6] Usuario admin en shard1rs â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
echo ">>> [5/6] Creando usuario admin en shard1rs"
mongosh --host "shard1rs/shard1a:27017,shard1b:27017,shard1c:27017" --quiet --eval '
try {
  db.getSiblingDB("admin").createUser({
    user: "admin",
    pwd: "booklibre_admin_secret",
    roles: [{ role: "root", db: "admin" }]
  });
  print("Usuario admin creado en shard1rs");
} catch(e) {
  if (e.codeName === "Location51003" || e.code === 51003) { print("Usuario admin ya existe en shard1rs"); }
  else { throw e; }
}
'

# â”€â”€â”€ [6/6] Usuario admin en shard2rs â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
echo ">>> [6/6] Creando usuario admin en shard2rs"
mongosh --host "shard2rs/shard2a:27017,shard2b:27017,shard2c:27017" --quiet --eval '
try {
  db.getSiblingDB("admin").createUser({
    user: "admin",
    pwd: "booklibre_admin_secret",
    roles: [{ role: "root", db: "admin" }]
  });
  print("Usuario admin creado en shard2rs");
} catch(e) {
  if (e.codeName === "Location51003" || e.code === 51003) { print("Usuario admin ya existe en shard2rs"); }
  else { throw e; }
}
'

echo ">>> Replica sets inicializados y usuarios admin creados. mongos puede arrancar."
