# Libros pushdown a MongoDB — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) o superpowers:executing-plans para implementar task-by-task. Los steps usan checkbox (`- [ ]`) syntax para tracking.

**Goal:** Eliminar los scans full-collection en memoria del backend. Hoy `LibroRepository.buscarPaginado` hace `mongo.findAll()` → 15.024 docs deserializados a JVM → filtros/sort/paginate en Kotlin. Resultado tras este plan: Mongo filtra + ordena + pagina; el backend trae sólo la página (≤ 12 docs por request).

**Architecture:**
- Reescribir `buscarPaginado` con `MongoTemplate` + `Criteria` dinámico + `Pageable`. Mongo ejecuta el `find` con `skip/limit/sort` server-side y el `countDocuments`.
- Crear índices Mongo via Flyway V8.kt (no via `@Indexed` para no depender de `auto-index-creation` y para que queden registrados en el control de versiones).
- Agregar variante paginada al endpoint `GET /api/usuarios/{id}/libros` que hoy devuelve 3758 docs para ana de un solo viaje.
- Fix colateral: `LibroRepository.save` calcula `next id` con `mongo.findAll().maxOfOrNull { it.id }` (otro scan completo). Reemplazar por `mongo.findTopByOrderByIdDesc()` o `mongoTemplate.findOne(sort by id desc)`.

**Tech Stack:** Spring Data MongoDB (`MongoTemplate`, `Criteria`, `Query`, `Pageable`), Flyway `BaseJavaMigration`, Kotlin 1.9, Spring Boot 3.3.

---

## Files to create / modify

### Crear
- `src/main/kotlin/db/migration/V8__create_libros_indexes.kt` — crea índices Mongo (no toca Postgres).

### Modificar
- `src/main/kotlin/ar/edu/unsam/phm/repository/LibroRepository.kt`
  - Inyectar `MongoTemplate`
  - Reescribir `buscarPaginado` con pushdown
  - Reescribir `save` para no scanear all docs al asignar id
  - Agregar `findPaginadoByDuenioId(usuarioId, page, size): PagedResponse<Libro>`
- `src/main/kotlin/ar/edu/unsam/phm/services/UsuarioService.kt`
  - `obtenerLibrosDeUsuario(usuarioId, page, size)` con paginación
  - `tieneReservasActivas`: usar nuevo `findLibroIdsByDuenioId` (proyección) en vez de cargar Libros completos sólo para `.map { it.id }`
- `src/main/kotlin/ar/edu/unsam/phm/repository/mongo/LibroMongoRepository.kt`
  - Agregar `findTopByOrderByIdDesc(): Libro?` (para `save`)
  - Agregar `@Query(value="{...}", fields="{'id': 1}") fun findLibroIdsByDuenioId(duenioId: Int): List<LibroIdOnly>` para la proyección
- `src/main/kotlin/ar/edu/unsam/phm/services/ReservaService.kt`
  - Usar `findLibroIdsByDuenioId` en la rama RECIBIDAS (hoy llama a `getTodosLosLibrosDeUsuario` sólo para `.map { it.id }`)
- `src/main/kotlin/ar/edu/unsam/phm/controllers/UsuarioController.kt`
  - `GET /api/usuarios/{id}/libros` acepta `page` y `size`, devuelve `PagedResponse<LibroUsuarioItemDTO>`

### NO se tocan
- `LibroMongoRepository` queries existentes (`findByIdAndActivoTrue`, `findByDuenioId`, etc.) — siguen ahí para callers no-paginados (no son críticos).
- `application-docker.yml` / `application-local.yml` — los índices se crean via V8.kt, no via `auto-index-creation`.
- Frontend — la firma del endpoint home (`/api/libros/home`) y su `PagedResponse` no cambian.

---

## Decisiones de diseño tomadas (no cambiar al implementar)

1. **`MongoTemplate` en lugar de Spring Data derived queries**: las queries son demasiado dinámicas (5+ filtros opcionales con AND, regex case-insensitive sobre 3 campos). Derived queries no escalan a este caso. `Criteria` se compone naturalmente con `if (filtro != null)`.

2. **Búsqueda full-text con regex case-insensitive, no `$text`**: Mongo permite un solo text index por collection. Si más adelante se necesita ranking semántico se evalúa. Por ahora `Criteria.where("titulo").regex(query, "i")` con `$or` sobre `titulo|autor|descripcion` da el comportamiento actual del Kotlin code. Con index sobre `titulo` la regex `^prefix` usa el index; el `contains` no, pero filtra después de los otros criterios más selectivos.

3. **Índices en V8.kt (no `@Indexed`)**: los índices viven en el control de versiones de Flyway. Cualquier deploy nuevo los recibe. No dependemos de `auto-index-creation` (deprecated en Spring Data Mongo 4 y off por default). El comando `createIndex` con `background:true` evita lock en collections grandes.

4. **Índices a crear** (en V8):
   - `{ activo: 1 }` — todos los queries filtran por `activo:true`
   - `{ "duenio.id": 1 }` — perfil + `findByDuenioId`
   - `{ "duenio.id": 1, activo: 1 }` — perfil con filtro activo
   - `{ genero: 1 }` — filtro home
   - `{ isbn: 1 }` — filtro home
   - `{ titulo: 1 }` — sort default + búsqueda por prefijo
   - `{ autor: 1 }` — sort por autor
   - `{ calificacion: -1 }` — sort por rating
   - `{ activo: 1, titulo: 1 }` — compuesto para el sort default sobre activos
   
   NO se crea índice sobre `descripcion` (texto largo, casi no se filtra). NO se crea índice texto-único: lo dejamos como follow-up si la búsqueda regex sale lenta en producción.

5. **`save()` sin scan**: usar `mongo.findTopByOrderByIdDesc()?.id ?: 0` + 1. Con el índice `{ id: -1 }` (mongo lo crea automático sobre `_id`, no sobre `id` business). Necesitamos índice explícito sobre `id` — se agrega también en V8.

6. **Paginación del perfil**: el endpoint `GET /api/usuarios/{id}/libros` se vuelve paginado con `?page=0&size=10`. **Breaking change menor** para el frontend pero el frontend ya envía `page` y `size` (lo muestra el screenshot del perfil: "1-3 de 3758"). El frontend posiblemente pagina client-side hoy; con esto pasa a server-side y la response baja de 3758 → 10.

7. **No tocar el seed masivo**: el usuario explícitamente eligió "solo arreglar backend, dejar seed". Los 15k libros siguen repartidos round-robin entre 4 users.

8. **Compatibilidad con búsqueda case-insensitive**: la regex Mongo `i` flag hace lo mismo que el `.lowercase().contains()` actual. No hay regresión funcional.

---

## Tasks

### Task 1: Branch nueva y baseline

**Files:**
- (sin cambios)

- [ ] **Step 1: Crear branch desde `fix/bootstrap-purista-flyway`** (porque el PR #128 todavía no se mergeó a migracionMongo, y este trabajo depende de los 15k libros del V7).

```bash
cd ~/Documents/phym/backend-2026-grupo-12
git checkout fix/bootstrap-purista-flyway
git checkout -b feat/libros-pushdown-mongo
```

- [ ] **Step 2: Confirmar que el stack levanta y la query lenta funciona (baseline)**

```bash
docker-compose down -v --remove-orphans
docker-compose up -d
until docker logs booklibre-backend 2>&1 | grep -q "Started BookLibreApplicationKt"; do sleep 5; done
# Baseline: medir tiempo del home con todos los libros
time docker exec booklibre-frontend sh -c 'wget -qO- "http://backend:8080/api/libros/home?page=0&size=6" 2>&1' | head -c 100
```

Expected: 200 OK, response time entre 500ms-2s (el bug que vamos a arreglar). Anotar el tiempo.

---

### Task 2: V8.kt — Crear índices Mongo

**Files:**
- Create: `src/main/kotlin/db/migration/V8__create_libros_indexes.kt`

- [ ] **Step 1: Crear V8.kt**

```kotlin
package db.migration

import com.mongodb.client.MongoClients
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V8__create_libros_indexes : BaseJavaMigration() {

    override fun migrate(context: Context) {
        val mongoUri = System.getenv("MONGODB_URI") ?: "mongodb://localhost:27017/booklibre"
        val dbName = mongoUri.substringAfterLast("/").substringBefore("?")

        MongoClients.create(mongoUri).use { client ->
            val collection = client.getDatabase(dbName).getCollection("libros")

            val opts = IndexOptions().background(true)

            collection.createIndex(Indexes.ascending("id"), opts)
            collection.createIndex(Indexes.ascending("activo"), opts)
            collection.createIndex(Indexes.ascending("duenio.id"), opts)
            collection.createIndex(Indexes.ascending("duenio.id", "activo"), opts)
            collection.createIndex(Indexes.ascending("genero"), opts)
            collection.createIndex(Indexes.ascending("isbn"), opts)
            collection.createIndex(Indexes.ascending("titulo"), opts)
            collection.createIndex(Indexes.ascending("autor"), opts)
            collection.createIndex(Indexes.descending("calificacion"), opts)
            collection.createIndex(Indexes.ascending("activo", "titulo"), opts)
        }
    }
}
```

- [ ] **Step 2: Validar que la migración corre y crea los índices**

```bash
docker-compose down -v --remove-orphans
docker-compose build backend
docker-compose up -d
until docker logs booklibre-backend 2>&1 | grep -q "Started BookLibreApplicationKt"; do sleep 5; done
docker logs booklibre-backend 2>&1 | grep "v8" | head -3
docker exec mongos mongosh booklibreMongo --quiet --eval 'db.libros.getIndexes().forEach(i => print(i.name))'
```

Expected:
- Log de Flyway: `Migrating schema "public" to version "8 - create libros indexes"`
- Lista de índices: `_id_`, `id_1`, `activo_1`, `duenio.id_1`, `duenio.id_1_activo_1`, `genero_1`, `isbn_1`, `titulo_1`, `autor_1`, `calificacion_-1`, `activo_1_titulo_1` (11 total)

- [ ] **Step 3: Commit V8**

```bash
git add src/main/kotlin/db/migration/V8__create_libros_indexes.kt
git commit -m "feat(flyway): V8 indices Mongo sobre libros para pushdown queries

Crea indices sobre id, activo, duenio.id, genero, isbn, titulo, autor,
calificacion y dos compuestos. background=true para evitar lock en
collections grandes."
```

---

### Task 3: Refactor `buscarPaginado` con MongoTemplate

**Files:**
- Modify: `src/main/kotlin/ar/edu/unsam/phm/repository/LibroRepository.kt`

- [ ] **Step 1: Inyectar `MongoTemplate` y reescribir `buscarPaginado`**

Reemplazar el constructor:

```kotlin
@Component
class LibroRepository(
    private val mongo: LibroMongoRepository,
    private val calificacionesMongo: CalificacionMongoRepository,
    private val usuarioJpa: UsuarioJpaRepository,
    private val historialJpa: HistorialPuntajeJpaRepository,
    private val mongoTemplate: org.springframework.data.mongodb.core.MongoTemplate,
)
```

Reemplazar el método entero `buscarPaginado` (líneas 63-106 del archivo actual) por:

```kotlin
fun buscarPaginado(filtros: LibroFiltros, page: Int, size: Int): PagedResponse<Libro> {
    val criteria = org.springframework.data.mongodb.core.query.Criteria.where("activo").`is`(true)

    val query = filtros.query.trim()
    if (query.isNotBlank()) {
        val escaped = java.util.regex.Pattern.quote(query)
        criteria.orOperator(
            org.springframework.data.mongodb.core.query.Criteria.where("titulo").regex(escaped, "i"),
            org.springframework.data.mongodb.core.query.Criteria.where("autor").regex(escaped, "i"),
            org.springframework.data.mongodb.core.query.Criteria.where("descripcion").regex(escaped, "i"),
        )
    }

    if (filtros.generos.isNotEmpty()) {
        criteria.and("genero").`in`(filtros.generos.map { it.trim().uppercase() })
    }
    filtros.paginasMin?.let { criteria.and("cantidadPaginas").gte(it) }
    filtros.paginasMax?.let { criteria.and("cantidadPaginas").lte(it) }
    filtros.isbn?.takeIf { it.isNotBlank() }?.let { criteria.and("isbn").`is`(it) }
    filtros.prestadoPor?.takeIf { it.isNotBlank() }?.let {
        criteria.and("duenio.nombre").regex(java.util.regex.Pattern.quote(it), "i")
    }
    filtros.excluirDuenioId?.let { criteria.and("duenio.id").ne(it) }
    filtros.libroIdsReservados?.takeIf { it.isNotEmpty() }?.let { ids ->
        criteria.and("id").nin(ids)
    }

    val sortField = when (filtros.sortBy.lowercase()) {
        "autor"        -> "autor"
        "calificacion" -> "calificacion"
        else           -> "titulo"
    }
    val sortDir = if (sortField == "calificacion")
        org.springframework.data.domain.Sort.Direction.DESC
    else
        org.springframework.data.domain.Sort.Direction.ASC

    val baseQuery = org.springframework.data.mongodb.core.query.Query(criteria)
    val total = mongoTemplate.count(baseQuery, Libro::class.java)

    val pageRequest = org.springframework.data.domain.PageRequest.of(page, size,
        org.springframework.data.domain.Sort.by(sortDir, sortField))
    val pagedQuery = baseQuery.with(pageRequest)
    val items = mongoTemplate.find(pagedQuery, Libro::class.java)

    val totalPages = if (total == 0L) 0 else kotlin.math.ceil(total.toDouble() / size).toInt()
    return PagedResponse(items, page, totalPages)
}
```

- [ ] **Step 2: Reescribir `save` para no escanear all libros al asignar id**

Reemplazar el método actual (líneas 50-55):

```kotlin
fun save(libro: Libro): Libro {
    if (libro.id == 0) {
        libro.id = (mongo.findTopByOrderByIdDesc()?.id ?: 0) + 1
    }
    return mongo.save(libro)
}
```

- [ ] **Step 3: Test — el home responde con timing decente**

```bash
docker-compose down -v --remove-orphans
docker-compose build backend
docker-compose up -d
until docker logs booklibre-backend 2>&1 | grep -q "Started BookLibreApplicationKt"; do sleep 5; done

# Medir endpoint home con sort default
time docker exec booklibre-frontend sh -c 'wget -qO- "http://backend:8080/api/libros/home?page=0&size=6" 2>&1' | head -c 200
echo ""
# Búsqueda por título
time docker exec booklibre-frontend sh -c 'wget -qO- "http://backend:8080/api/libros/home?query=1984&page=0&size=6" 2>&1' | head -c 200
echo ""
# Filtro por género
time docker exec booklibre-frontend sh -c 'wget -qO- "http://backend:8080/api/libros/home?generos=DRAMA&page=0&size=6" 2>&1' | head -c 200
```

Expected:
- Las 3 queries devuelven 200 OK
- Tiempo de respuesta **bajo 100ms cada una** (vs 500ms-2s baseline)
- Cada response tiene exactamente 6 libros (no más, no menos)

- [ ] **Step 4: Commit refactor**

```bash
git add src/main/kotlin/ar/edu/unsam/phm/repository/LibroRepository.kt
git commit -m "perf(libros): pushdown filtros, sort y paginacion a Mongo

buscarPaginado pasa de scan-all+filter-in-memory a MongoTemplate con
Criteria dinamico + Pageable. Aprovecha los indices de V8.

save() deja de hacer mongo.findAll() para asignar id; usa
findTopByOrderByIdDesc."
```

---

### Task 4: Agregar `findTopByOrderByIdDesc` al MongoRepository

**Files:**
- Modify: `src/main/kotlin/ar/edu/unsam/phm/repository/mongo/LibroMongoRepository.kt`

- [ ] **Step 1: Agregar la query derivada**

Agregar al final de la interface (antes del `}` final):

```kotlin
@Query("{}", sort = "{ 'id' : -1 }")
fun findTopByOrderByIdDesc(): Libro?
```

Notar: Spring Data Mongo soporta `findTopBy*` y `findFirstBy*` con sort. Sin el `@Query` explícito Spring puede generar mal el sort sobre `id` (porque también existe `mongoId`). El `@Query` con `sort` lo desambigua.

- [ ] **Step 2: Test — crear un libro nuevo via endpoint asigna id 15025**

```bash
docker-compose down -v --remove-orphans
docker-compose build backend
docker-compose up -d
until docker logs booklibre-backend 2>&1 | grep -q "Started BookLibreApplicationKt"; do sleep 5; done

# Crear libro via API
docker exec booklibre-frontend sh -c 'wget -qO- --post-data="{\"email\":\"juan@mail.com\",\"password\":\"123\"}" --header="Content-Type: application/json" http://backend:8080/api/login 2>&1' | python3 -c "import sys, json; print(json.load(sys.stdin)['token'])" > /tmp/jwt.txt
TOKEN=$(cat /tmp/jwt.txt)
docker exec booklibre-frontend sh -c "wget -qO- --post-data='{\"tipo\":\"Comun\",\"titulo\":\"Test Pushdown\",\"autor\":\"Test\",\"descripcion\":\"x\",\"genero\":\"DRAMA\",\"paginas\":100,\"isbn\":\"9999999999999\",\"idioma\":\"ESPANOL\",\"editorial\":\"Test\",\"fechaPublicacion\":\"2025-01-01\",\"estado\":\"BUENO\"}' --header='Content-Type: application/json' --header='Authorization: Bearer $TOKEN' 'http://backend:8080/api/libros/nuevo?usuarioId=1'" | head -c 300
```

Expected: response JSON con `"id":15025` (continúa después del último).

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/ar/edu/unsam/phm/repository/mongo/LibroMongoRepository.kt
git commit -m "feat(libros): findTopByOrderByIdDesc para asignar siguiente id sin scan"
```

---

### Task 5: Paginar `GET /api/usuarios/{id}/libros`

**Files:**
- Modify: `src/main/kotlin/ar/edu/unsam/phm/repository/LibroRepository.kt`
- Modify: `src/main/kotlin/ar/edu/unsam/phm/services/UsuarioService.kt`
- Modify: `src/main/kotlin/ar/edu/unsam/phm/controllers/UsuarioController.kt`

- [ ] **Step 1: Agregar `findPaginadoByDuenioId` al repo**

En `LibroRepository.kt`, agregar después de `getLibrosDeUsuario`:

```kotlin
fun findPaginadoByDuenioId(usuarioId: Int, page: Int, size: Int): PagedResponse<Libro> {
    val criteria = org.springframework.data.mongodb.core.query.Criteria
        .where("duenio.id").`is`(usuarioId)
    val baseQuery = org.springframework.data.mongodb.core.query.Query(criteria)
    val total = mongoTemplate.count(baseQuery, Libro::class.java)
    val pageRequest = org.springframework.data.domain.PageRequest.of(page, size,
        org.springframework.data.domain.Sort.by(
            org.springframework.data.domain.Sort.Direction.DESC, "fechaAgregado"))
    val items = mongoTemplate.find(baseQuery.with(pageRequest), Libro::class.java)
    val totalPages = if (total == 0L) 0 else kotlin.math.ceil(total.toDouble() / size).toInt()
    return PagedResponse(items, page, totalPages)
}
```

- [ ] **Step 2: Agregar metodo paginado al service**

En `UsuarioService.kt`, después de `obtenerLibrosDeUsuario`:

```kotlin
fun obtenerLibrosDeUsuarioPaginado(usuarioId: Int, page: Int, size: Int) =
    libroRepository.findPaginadoByDuenioId(usuarioId, page, size)
```

(El método viejo `obtenerLibrosDeUsuario` queda — no rompemos callers externos.)

- [ ] **Step 3: Modificar el endpoint del controller**

Reemplazar el endpoint `getLibrosDeUsuario` en `UsuarioController.kt` (líneas 46-48):

```kotlin
@GetMapping("/{id}/libros")
fun getLibrosDeUsuario(
    @PathVariable id: Int,
    @RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "10") size: Int,
): PagedResponse<LibroUsuarioItemDTO> {
    val paged = usuarioService.obtenerLibrosDeUsuarioPaginado(id, page, size)
    return PagedResponse(
        items = paged.items.map { it.toLibroUsuarioItemDTO() },
        page = paged.page,
        totalPages = paged.totalPages,
    )
}
```

(Agregar import de `PagedResponse` si no estaba.)

- [ ] **Step 4: Test — ana ve sus libros paginados**

```bash
docker-compose down -v --remove-orphans
docker-compose build backend
docker-compose up -d
until docker logs booklibre-backend 2>&1 | grep -q "Started BookLibreApplicationKt"; do sleep 5; done

# ana tiene id 3 (LIBRO_PUBLICADOR)
docker exec booklibre-frontend sh -c 'wget -qO- "http://backend:8080/api/usuarios/3/libros?page=0&size=10" 2>&1' | python3 -c "import sys, json; d = json.load(sys.stdin); print('items:', len(d['items']), 'totalPages:', d['totalPages'])"
```

Expected: `items: 10  totalPages: 376` (3758/10).

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/ar/edu/unsam/phm/repository/LibroRepository.kt \
        src/main/kotlin/ar/edu/unsam/phm/services/UsuarioService.kt \
        src/main/kotlin/ar/edu/unsam/phm/controllers/UsuarioController.kt
git commit -m "feat(usuarios): paginar GET /api/usuarios/{id}/libros

El endpoint devolvia toda la lista (3758 docs para usuarios del seed
masivo). Ahora acepta ?page&size, devuelve PagedResponse igual que
/api/libros/home. Frontend ya enviaba estos params client-side."
```

---

### Task 6: Reemplazar `getTodosLosLibrosDeUsuario` en callers internos por proyección de IDs

**Files:**
- Modify: `src/main/kotlin/ar/edu/unsam/phm/repository/mongo/LibroMongoRepository.kt`
- Modify: `src/main/kotlin/ar/edu/unsam/phm/repository/LibroRepository.kt`
- Modify: `src/main/kotlin/ar/edu/unsam/phm/services/UsuarioService.kt`
- Modify: `src/main/kotlin/ar/edu/unsam/phm/services/ReservaService.kt`

Los dos callers internos de `getTodosLosLibrosDeUsuario` (`UsuarioService.tieneReservasActivas` y `ReservaService` rama RECIBIDAS) sólo usan `.map { it.id }`. Cargar 3758 Libro docs sólo para extraer los IDs es desperdicio. Agregamos una proyección que devuelve sólo los IDs.

- [ ] **Step 1: Agregar proyección al MongoRepository**

En `LibroMongoRepository.kt`, agregar:

```kotlin
data class LibroIdProjection(val id: Int)

@Query(value = "{ 'duenio.id' : ?0 }", fields = "{ 'id' : 1, '_id' : 0 }")
fun findLibroIdsByDuenioId(duenioId: Int): List<LibroIdProjection>
```

Nota: `LibroIdProjection` se puede definir en el mismo archivo o en un package de DTOs. Para mantenerlo cerca del uso, va en el mismo file.

- [ ] **Step 2: Wrapper en LibroRepository**

En `LibroRepository.kt`, agregar:

```kotlin
fun findLibroIdsByDuenioId(usuarioId: Int): List<Int> =
    mongo.findLibroIdsByDuenioId(usuarioId).map { it.id }
```

- [ ] **Step 3: Reemplazar en UsuarioService.tieneReservasActivas**

```kotlin
private fun tieneReservasActivas(usuarioId: Int): Boolean {
    val ahora = java.time.LocalDateTime.now()
    if (reservaJpa.existsByUsuarioIdAndFechaHastaAfter(usuarioId, ahora)) return true
    val librosDelUsuario = libroRepository.findLibroIdsByDuenioId(usuarioId)
    return librosDelUsuario.isNotEmpty() &&
        reservaJpa.existsByLibroIdInAndFechaHastaAfter(librosDelUsuario, ahora)
}
```

- [ ] **Step 4: Reemplazar en ReservaService**

Localizar la línea 38 (rama RECIBIDAS) y cambiar:

```kotlin
val librosDelUsuario = libroRepository.findLibroIdsByDuenioId(usuarioId)
```

(El nombre de la variable se mantiene, sólo cambia el método llamado y el tipo de la variable a `List<Int>`.)

- [ ] **Step 5: Test — flujos siguen andando**

```bash
docker-compose down -v --remove-orphans
docker-compose build backend
docker-compose up -d
until docker logs booklibre-backend 2>&1 | grep -q "Started BookLibreApplicationKt"; do sleep 5; done

# Verifica que ana puede ver reservas (rama RECIBIDAS) sin lag
docker exec booklibre-frontend sh -c 'wget -qO- --post-data="{\"email\":\"ana@example.com\",\"password\":\"123\"}" --header="Content-Type: application/json" http://backend:8080/api/login 2>&1' | python3 -c "import sys, json; print(json.load(sys.stdin)['token'])" > /tmp/jwt.txt
TOKEN=$(cat /tmp/jwt.txt)
time docker exec booklibre-frontend sh -c "wget -qO- --header='Authorization: Bearer $TOKEN' 'http://backend:8080/api/reservas?usuarioId=3&tipo=RECIBIDAS' 2>&1" | head -c 200
```

Expected: response < 200ms (vs el lag previo de cargar 3758 docs).

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/ar/edu/unsam/phm/repository/mongo/LibroMongoRepository.kt \
        src/main/kotlin/ar/edu/unsam/phm/repository/LibroRepository.kt \
        src/main/kotlin/ar/edu/unsam/phm/services/UsuarioService.kt \
        src/main/kotlin/ar/edu/unsam/phm/services/ReservaService.kt
git commit -m "perf(libros): proyeccion findLibroIdsByDuenioId para callers internos

tieneReservasActivas y ReservaService rama RECIBIDAS solo necesitan
los IDs de los libros del duenio, no los docs completos. Mongo devuelve
solo el campo id (3KB para 3758 IDs vs 3MB para 3758 docs)."
```

---

### Task 7: Verificación end-to-end

**Files:**
- (sin cambios — solo verificación)

- [ ] **Step 1: Cold start completo**

```bash
docker-compose down -v --remove-orphans
docker-compose build backend
docker-compose up -d
until docker logs booklibre-backend 2>&1 | grep -q "Started BookLibreApplicationKt"; do sleep 5; done
```

- [ ] **Step 2: Verificar V8 corrió y los índices existen**

```bash
docker logs booklibre-backend 2>&1 | grep "Successfully applied" | tail -2
docker exec mongos mongosh booklibreMongo --quiet --eval 'db.libros.getIndexes().forEach(i => print(i.name))'
```

Expected: 11 índices (`_id_` + los 10 que agregamos).

- [ ] **Step 3: Smoke tests de endpoints**

```bash
# Login
docker exec booklibre-frontend sh -c 'wget -qO- --post-data="{\"email\":\"ana@example.com\",\"password\":\"123\"}" --header="Content-Type: application/json" http://backend:8080/api/login 2>&1' | python3 -c "import sys, json; print(json.load(sys.stdin)['token'])" > /tmp/jwt.txt
TOKEN=$(cat /tmp/jwt.txt)

# Home — debe responder rápido
echo "--- home default ---"
time docker exec booklibre-frontend sh -c 'wget -qO- "http://backend:8080/api/libros/home?page=0&size=6"' | python3 -c "import sys, json; d = json.load(sys.stdin); print('items:', len(d['items']), 'totalPages:', d['totalPages'])"

# Home con búsqueda
echo "--- home buscar 'sombra' ---"
time docker exec booklibre-frontend sh -c 'wget -qO- "http://backend:8080/api/libros/home?query=sombra&page=0&size=6"' | python3 -c "import sys, json; d = json.load(sys.stdin); print('items:', len(d['items']), 'totalPages:', d['totalPages'])"

# Home con filtro
echo "--- home genero=DRAMA ---"
time docker exec booklibre-frontend sh -c 'wget -qO- "http://backend:8080/api/libros/home?generos=DRAMA&page=0&size=6"' | python3 -c "import sys, json; d = json.load(sys.stdin); print('items:', len(d['items']), 'totalPages:', d['totalPages'])"

# Profile ana
echo "--- profile ana ---"
time docker exec booklibre-frontend sh -c 'wget -qO- "http://backend:8080/api/usuarios/3/libros?page=0&size=10"' | python3 -c "import sys, json; d = json.load(sys.stdin); print('items:', len(d['items']), 'totalPages:', d['totalPages'])"
```

Expected: las 4 queries < 200ms cada una. Cada una con `items` igual al tamaño de página solicitado.

- [ ] **Step 4: Logs sin errores**

```bash
docker logs booklibre-backend 2>&1 | grep -E "ERROR|Exception" | head -5
```

Expected: vacío.

- [ ] **Step 5: Smoke manual del frontend**

Abrir http://localhost:5173. Loguear como `juan@mail.com` / `123`. Verificar:
- Home carga el catálogo rápido (sin lag perceptible)
- Búsqueda por título responde rápido
- Filtros por género funcionan
- "Mi Perfil" → "Gestión de Mis Libros" muestra los libros paginados (no carga 3758 de una)
- Click en un libro abre detalle sin errores

- [ ] **Step 6: Push y abrir PR**

El PR base es `migracionMongo` pero como `fix/bootstrap-purista-flyway` (PR #128) todavía no se mergeó, este PR se va a poder mergear sólo después de #128 o lo combinamos.

Opciones (decidir cuando llegue el momento):
- Si #128 ya está mergeado → PR a migracionMongo
- Si no → PR a `fix/bootstrap-purista-flyway` (encadenado)

```bash
git push -u origin feat/libros-pushdown-mongo
gh pr create --base <migracionMongo o fix/bootstrap-purista-flyway> --head feat/libros-pushdown-mongo \
  --title "perf(libros): pushdown de queries a Mongo + indices" \
  --body "$(cat <<'EOF'
## Resumen

`buscarPaginado` ya no carga 15.024 libros en memoria; Mongo filtra, ordena y pagina server-side. Indices V8 en `id`, `activo`, `duenio.id`, `genero`, `isbn`, `titulo`, `autor`, `calificacion` y 2 compuestos.

## Bug que arregla

Hoy `LibroRepository.buscarPaginado` hacía `mongo.findAll()` → 15k docs deserializados → filtros/sort/paginate en Kotlin. Cada request al home descargaba 15 MB y procesaba todo en JVM para devolver 6 libros.

## Cambios principales

- `LibroRepository.buscarPaginado`: scan-all → `MongoTemplate` con `Criteria` dinamico + `Pageable`
- `LibroRepository.save`: `mongo.findAll().maxOf{id}+1` → `findTopByOrderByIdDesc().id+1`
- `UsuarioController.getLibrosDeUsuario`: lista completa → `PagedResponse<>` paginado
- `UsuarioService.tieneReservasActivas` y `ReservaService` RECIBIDAS: cargan 3758 docs solo para `.map{id}` → proyección `findLibroIdsByDuenioId` (3KB en vez de 3MB)
- V8.kt: 10 indices Mongo background=true

## Verificado

- [ ] Home `?page=0&size=6` < 200ms (vs 500ms-2s)
- [ ] Home con `query=sombra` < 200ms
- [ ] Profile ana `?page=0&size=10` devuelve `items: 10, totalPages: 376`
- [ ] 11 indices en `db.libros.getIndexes()`
- [ ] Logs limpios sin errores
EOF
)"
```

---

## Self-review checklist (para implementador)

Antes de cerrar el PR:

- **Spec coverage:**
  - V8 índices ✓
  - `buscarPaginado` pushdown ✓
  - `save` sin scan ✓
  - `findTopByOrderByIdDesc` ✓
  - Profile paginado ✓
  - Proyección de IDs ✓
- **No placeholders:** ningún `TODO`/`TBD` en código.
- **Type consistency:** `Pageable`, `Criteria`, `Query` imports todos en español/inglés consistente.
- **Compat frontend:** firma de `/api/libros/home` no cambia. La de `/api/usuarios/{id}/libros` SI cambia de `List<>` a `PagedResponse<>` — verificar que el frontend acepta o ajusta.
- **Tests Spring:** correr `./gradlew test` antes del PR si hay tests del LibroService/Repository.

---

## Riesgos conocidos

1. **Regex `i` flag y performance:** `Criteria.regex("query", "i")` sin `^` anclado no usa el index. Si la búsqueda con muchos libros sale lenta, el follow-up sería un text index. Para 15k libros no debería ser problema.

2. **Frontend del perfil:** si hoy el frontend paginaba client-side (recibiendo 3758 docs), con el cambio a server-side el comportamiento puede cambiar (ej. botones de "next page" que antes eran instantáneos ahora pegan al backend). Verificar en smoke manual.

3. **`MongoTemplate` y `_class`:** el `find(query, Libro::class.java)` devuelve subclases polimórficas (LibroComun, LibroConDedicatoria, LibroColeccionable). Spring Data Mongo lo maneja automático via `_class`. Si en algún momento sale mal el cast, verificar que `Libro` sigue siendo `abstract class` con `@Document`.
