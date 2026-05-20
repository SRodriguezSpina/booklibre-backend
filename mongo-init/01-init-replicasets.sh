#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# Fase 1: inicializa los tres replica sets independientes
#   · configrs  → 3 config servers
#   · shard1rs  → 3 nodos del Shard 1
#   · shard2rs  → 3 nodos del Shard 2
#
# Este script es ejecutado por el contenedor mongo-rs-init ANTES de que
# arranque mongos. Una vez que termina con exit 0, docker-compose levanta
# mongos y luego corre 02-init-cluster.js.
# ─────────────────────────────────────────────────────────────────────────────
set -e

echo ">>> [1/3] Iniciando replica set de Config Servers (configrs)"
mongosh --host configsvr1 --port 27017 --quiet --eval '
rs.initiate({
  _id: "configrs",
  configsvr: true,
  members: [
    { _id: 0, host: "configsvr1:27017" },
    { _id: 1, host: "configsvr2:27017" },
    { _id: 2, host: "configsvr3:27017" }
  ]
});
print("configrs iniciado:", JSON.stringify(rs.status().ok));
'

echo ">>> [2/3] Iniciando replica set Shard 1 (shard1rs)"
mongosh --host shard1a --port 27017 --quiet --eval '
rs.initiate({
  _id: "shard1rs",
  members: [
    { _id: 0, host: "shard1a:27017" },
    { _id: 1, host: "shard1b:27017" },
    { _id: 2, host: "shard1c:27017" }
  ]
});
print("shard1rs iniciado:", JSON.stringify(rs.status().ok));
'

echo ">>> [3/3] Iniciando replica set Shard 2 (shard2rs)"
mongosh --host shard2a --port 27017 --quiet --eval '
rs.initiate({
  _id: "shard2rs",
  members: [
    { _id: 0, host: "shard2a:27017" },
    { _id: 1, host: "shard2b:27017" },
    { _id: 2, host: "shard2c:27017" }
  ]
});
print("shard2rs iniciado:", JSON.stringify(rs.status().ok));
'

echo ">>> Esperando elección de primarios (15 s)..."
sleep 15

echo ">>> Replica sets inicializados. mongos puede arrancar."
