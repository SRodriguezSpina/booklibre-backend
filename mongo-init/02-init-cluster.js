// ─────────────────────────────────────────────────────────────────────────────
// Fase 2: configuración del cluster sharded
//
// Este script se ejecuta con mongosh conectado a mongos DESPUÉS de que los
// replica sets ya fueron inicializados por 01-init-replicasets.sh.
//
// Acciones:
//   1. Agrega los dos shards al cluster
//   2. Habilita sharding en la base de datos booklibreMongo
//   3. Particiona las colecciones con la shard key elegida
//
// ─────────────────────────────────────────────────────────────────────────────

// ── 1. Agregar shards ────────────────────────────────────────────────────────
print("=== Agregando Shard 1 (shard1rs) ===");
sh.addShard("shard1rs/shard1a:27017,shard1b:27017,shard1c:27017");

print("=== Agregando Shard 2 (shard2rs) ===");
sh.addShard("shard2rs/shard2a:27017,shard2b:27017,shard2c:27017");

print("Esperando registro de shards (15 s)...");
sleep(15000);

// ── 2. Habilitar sharding en la BD ───────────────────────────────────────────
print("=== Habilitando sharding en booklibreMongo ===");
sh.enableSharding("booklibreMongo");

// ── 3. Particionar colecciones ───────────────────────────────────────────────
//
// Colección: libros
//   Shard key: { _id: "hashed" }
//   Justificación: los IDs numéricos de libros tienen distribución uniforme
//   una vez hasheados. Evita hotspots en escritura (no secuencial), y como
//   los accesos son mayoritariamente por ID (getLibro, getLibroById), la
//   query es directed y no scatter-gather.
//
print("=== Sharding colección 'libros' (hashed por _id) ===");
sh.shardCollection("booklibreMongo.libros", { _id: "hashed" });

//
// Colección: click_logs
//   Shard key: { libroId: "hashed" }
//   Justificación: los clicks se distribuyen por libro entre ambos shards.
//   Usar fechaHora como shard key crearía un hotspot monotónico (todas las
//   escrituras irían al mismo shard mientras aumenta el tiempo). Con libroId
//   hasheado la carga se reparte uniformemente, y la aggregation pipeline de
//   "libro más clickeado" funciona en parallel scan sobre ambos shards antes
//   del merge en mongos.
//
print("=== Sharding colección 'click_logs' (hashed por libroId) ===");
sh.shardCollection("booklibreMongo.click_logs", { libroId: "hashed" });

//
// Colección: calificaciones
//   Shard key: { libroId: "hashed" }
//   Justificación: calificaciones es la colección de mayor crecimiento potencial
//   del sistema (N usuarios × M libros = volumen ilimitado de documentos). La
//   query más frecuente es "todas las calificaciones de un libro" (se ejecuta
//   cada vez que un usuario abre el detalle de un libro), por lo que usar
//   libroId como shard key garantiza Targeted Queries para ese patrón de acceso.
//   El hash evita hotspots incluso cuando un libro popular recibe muchas
//   calificaciones simultáneas.
//
print("=== Sharding colección 'calificaciones' (hashed por libroId) ===");
sh.shardCollection("booklibreMongo.calificaciones", { libroId: "hashed" });

// ── Estado final ─────────────────────────────────────────────────────────────
print("=== Cluster inicializado. Estado final: ===");
printjson(sh.status());
