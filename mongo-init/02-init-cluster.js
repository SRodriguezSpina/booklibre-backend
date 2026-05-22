// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Fase 2: configuraciÃ³n del cluster sharded (idempotente)
//
// Este script se ejecuta con mongosh conectado a mongos DESPUÃ‰S de que los
// replica sets ya fueron inicializados por 01-init-replicasets.sh.
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

// â”€â”€ 1. Agregar shards â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

// â”€â”€ 2. Habilitar sharding en la BD â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
print("=== Habilitando sharding en booklibreMongo ===");
try {
  sh.enableSharding("booklibreMongo");
  print("Sharding habilitado.");
} catch(e) {
  print("Sharding ya habilitado o error ignorable:", e.codeName || e.message);
}

// â”€â”€ 3. Particionar colecciones â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
print("=== Sharding colecciÃ³n 'libros' (hashed por _id) ===");
try {
  sh.shardCollection("booklibreMongo.libros", { _id: "hashed" });
  print("ColecciÃ³n 'libros' sharded.");
} catch(e) {
  print("'libros' ya sharded o error ignorable:", e.codeName || e.message);
}

print("=== Sharding colecciÃ³n 'click_logs' (hashed por libroId) ===");
try {
  sh.shardCollection("booklibreMongo.click_logs", { libroId: "hashed" });
  print("ColecciÃ³n 'click_logs' sharded.");
} catch(e) {
  print("'click_logs' ya sharded o error ignorable:", e.codeName || e.message);
}

print("=== Sharding colecciÃ³n 'calificaciones' (hashed por libroId) ===");
try {
  sh.shardCollection("booklibreMongo.calificaciones", { libroId: "hashed" });
  print("ColecciÃ³n 'calificaciones' sharded.");
} catch(e) {
  print("'calificaciones' ya sharded o error ignorable:", e.codeName || e.message);
}

// â”€â”€ 4. Crear usuario de aplicaciÃ³n â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
print("=== Verificando usuario de aplicaciÃ³n (booklibre_app) ===");
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

// â”€â”€ Estado final â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
print("=== Cluster inicializado. Estado final: ===");
printjson(sh.status());
