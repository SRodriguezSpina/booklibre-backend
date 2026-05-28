// -----------------------------------------------------------------------------
// Fase 2: configuracion del cluster sharded (idempotente)
//
// Este script se ejecuta con mongosh conectado a mongos-router despues de que los
// replica sets ya fueron inicializados por 01-init-replicasets.sh.
// -----------------------------------------------------------------------------

// 0. Set default RW concern to support PSA shards
print("=== Configurando Write Concern para soportar shards PSA ===");
try {
  db.adminCommand({ setDefaultRWConcern: 1, defaultWriteConcern: { w: 1 } });
  print("Write concern global configurado a w:1.");
} catch(e) {
  print("Error al configurar write concern global:", e.codeName || e.message);
}

// 1. Agregar shards
print("=== Agregando Shard 1 (shard1rs) ===");
try {
  sh.addShard("shard1rs/shard1a:27017,shard1b:27017,shard1c:27017");
  print("Shard 1 agregado.");
} catch(e) {
  print("Shard 1 ya existe o error ignorable:", e.codeName || e.message);
}

print("=== Agregando Shard 2 (shard2rs) ===");
try {
  sh.addShard("shard2rs/shard2a:27017,shard2b:27017,shard2c:27017");
  print("Shard 2 agregado.");
} catch(e) {
  print("Shard 2 ya existe o error ignorable:", e.codeName || e.message);
}

print("Esperando registro de shards (15 s)...");
sleep(15000);

// 2. Habilitar sharding en la BD
print("=== Habilitando sharding en booklibreMongo ===");
try {
  sh.enableSharding("booklibreMongo");
  print("Sharding habilitado.");
} catch(e) {
  print("Sharding ya habilitado o error ignorable:", e.codeName || e.message);
}

// 3. Particionar colecciones (Consolidado: shardeamos solo 'libros' por 'libroId')
print("=== Sharding coleccion 'libros' (hashed por libroId) ===");
try {
  sh.shardCollection("booklibreMongo.libros", { libroId: "hashed" });
  print("Coleccion 'libros' sharded.");
} catch(e) {
  print("'libros' ya sharded o error ignorable:", e.codeName || e.message);
}

// 4. Crear usuario de aplicacion
print("=== Verificando usuario de aplicacion (booklibre_app) ===");
const existing = db.getSiblingDB("booklibreMongo").getUser("booklibre_app");
if (!existing) {
  db.getSiblingDB("booklibreMongo").createUser({
    user: "booklibre_app",
    pwd: "booklibre_app_secret",
    roles: [{ role: "readWrite", db: "booklibreMongo" }]
  });
  print("Usuario booklibre_app creado.");
} else {
  print("Usuario booklibre_app ya existe, continuando.");
}

// Estado final
print("=== Cluster inicializado. Estado final: ===");
printjson(sh.status());
