# Deploy en la nube — BookLibre (Entrega 3, Parte 3)

Deploy del monolito políglota en **Render**, con **MongoDB Atlas** para la base
documental. Dominio: **booklibre.com.ar**.

## Topología

```
  booklibre.com.ar (apex + www)  ──▶  Render Static Site (frontend, Vite → dist)
                                              │ https
                                              ▼
  api.booklibre.com.ar  ──▶  Render Web Service (Docker) — Spring Boot
                                  REST /api/**  +  GraphQL /api/graphql
                                  │ interno        │ interno        │ mongodb+srv (TLS)
                                  ▼                ▼                ▼
                          Render PostgreSQL   Render Key Value   MongoDB Atlas M0
                            (gestionado)        (Valkey)          (gestionado)
```

App, Postgres y Key Value viven en la **misma región** de Render para usar conexiones
**internas** (sin egress, menor latencia, sin TLS). Solo Atlas se accede por internet.

## Recursos

| Recurso | Tipo Render | Notas |
|---|---|---|
| `booklibre-api` | Web Service (Docker) | usa el `Dockerfile` existente; perfil Spring `prod`; healthcheck `/api/libros/home?usuarioId=1` |
| `booklibre-kv` | Key Value (Valkey) | cache de la Parte 1 |
| `booklibre-db` | PostgreSQL | usuarios, reservas, historial |
| `booklibre-web` | Static Site | frontend Vite |
| MongoDB | **Atlas M0** | externo, base documental |

Todo se provisiona con el blueprint [`render.yaml`](../render.yaml).

## Configuración / secrets (nada de credenciales en el repo)

El perfil `prod` ([`application-prod.yml`](../src/main/resources/application-prod.yml)) toma
todo de variables de entorno. En Render:

| Variable | Origen |
|---|---|
| `SPRING_PROFILES_ACTIVE=prod` | render.yaml |
| `PORT` | inyectada por Render (la app escucha en `${PORT}`) |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | `fromDatabase: booklibre-db` (interno) |
| `REDIS_URL` | `fromService: booklibre-kv` (interno) |
| `MONGODB_URI` | **secret manual** — string SRV de Atlas |
| `JWT_SECRET` | **secret manual** — ≥32 caracteres (no el default) |
| `JWT_EXPIRATION=900000` | render.yaml |
| `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=70` | render.yaml (heap acotado para los 512 MB del free) |
| `CORS_ALLOWED_ORIGINS=https://booklibre.com.ar,https://www.booklibre.com.ar` | render.yaml |
| `VITE_API_URL=https://api.booklibre.com.ar/api` | render.yaml (build-time del frontend) |

## Decisiones obligatorias

### 1. MongoDB: instancia única administrada en Atlas M0

La aplicación se conecta a **una instancia/cluster administrado por Atlas M0**.
No configuramos ni mantenemos sharding o replica sets desde la aplicación o el
entorno local.

**Justificación:**
- El corrector debe poder acceder a la app **sin depender de nuestras máquinas**; un deploy
  totalmente en la nube es reproducible y siempre disponible.
- El plan gratuito no permite mantener el cluster sharded usado anteriormente.
- La aplicación solo requiere una URI (`MONGODB_URI`) y no depende de una shard key
  ni de topología de replica sets.
- En desarrollo, `docker-compose.yml` levanta una instancia Mongo standalone para
  que el backend funcione sin infraestructura adicional.

**Trade-off aceptado:** no se mantiene sharding ni alta disponibilidad administrada por
nosotros; Atlas es responsable de la infraestructura que ofrece su plan.

### 2. Web Service free: cold start

El plan free se **duerme tras 15 min** de inactividad (cold start de 30–60 s). Mitigación:
- Pinger externo (UptimeRobot / cron-job.org) a
  `https://api.booklibre.com.ar/api/libros/home?usuarioId=1` cada ~10 min.
- **Calentar manualmente** con un request ~2 min antes de la corrección presencial.

### 3. PostgreSQL free: expira a ~30 días

Todo el esquema + datos están versionados como migraciones **Flyway**
(`src/main/resources/db/migration/V*.sql` + `src/main/kotlin/db/migration/V*.kt`). Recrear la
base y reiniciar el servicio re-ejecuta todas las migraciones y resiembra los datos.

El baseline de migraciones para base virgen son **4 archivos**: `V1__init_schema.sql`
(tablas + funciones + views), `V2__seed_postgres.sql` (usuarios + reservas) y
`V3__seed_mongo.kt` (libros curados + masivos + calificaciones + reservas + índices),
`V4__expand_user_image_url.sql` (columna de imagen de perfil tipo `TEXT`).

> ⚠️ **Acoplamiento Mongo ↔ Postgres.** `V3__seed_mongo` es una migración Java de Flyway: su
> estado "ya ejecutada" se guarda en `flyway_schema_history` **de Postgres**, pero escribe en
> **Atlas**. Para que recrear Postgres **no borre datos de usuarios** en Atlas, `V3` usa un
> marcador (`_seed_meta._id = "mongo-seed-v3"`) guardado **en Atlas**:
> - Primer deploy (Atlas virgen) → no hay marcador → siembra y escribe el marcador. ✅
> - Se **recrea Postgres** y Flyway re-ejecuta `V3` → el marcador existe → **se saltea sin
>   tocar `libros`**: clicks, reservas y libros creados por la app se conservan. ✅
> - El `drop()` sólo corre cuando el marcador **no** existe (un seed parcial previo, todavía
>   sin datos vivos), así que nunca destruye datos de usuarios.
>
> Para forzar un re-seed limpio (p. ej. resetear el demo): borrar la colección `_seed_meta`
> (y opcionalmente `libros`) en Atlas, y redeployar.

## MongoDB Atlas — setup (una vez)

1. Crear cluster **M0** (región cercana a Render).
2. Usuario `booklibre_app` con password fuerte; base `booklibreMongo`.
3. Network Access: `0.0.0.0/0` (el free de Render no tiene IP de egress estática; el
   usuario/password siguen protegiendo el acceso).
4. Copiar el string `mongodb+srv://booklibre_app:<pwd>@.../booklibreMongo?retryWrites=true&w=majority`
   al secret `MONGODB_URI`. Flyway lo siembra en el primer arranque.

## DNS (Hurricane Electric — dns.he.net)

La zona se gestiona en **Hurricane Electric Free DNS** (dns.he.net), no en el panel de nic.ar.

1. **Delegación (una vez):** crear la zona `booklibre.com.ar` en dns.he.net y, en el panel de
   **nic.ar**, apuntar los nameservers del dominio a los de HE:
   `ns1.he.net`, `ns2.he.net`, `ns3.he.net`, `ns4.he.net`, `ns5.he.net`.
   La propagación puede tardar hasta 24–48 h.

2. **Registros en dns.he.net.** HE Free DNS **no soporta ALIAS/ANAME en el apex**, así que el
   apex va por A record a la IP de Render (CNAME solo en subdominios):

   | Nombre | Tipo | Valor | Destino |
   |---|---|---|---|
   | `booklibre.com.ar` (apex) | **A** | IP que muestre Render (hoy `216.24.57.1`) | Static Site |
   | `www.booklibre.com.ar` | **CNAME** | `booklibre-web.onrender.com` | Static Site |
   | `api.booklibre.com.ar` | **CNAME** | `booklibre-api.onrender.com` | Web Service |

   > La IP del apex la dicta Render al agregar el Custom Domain: usar **el valor exacto que
   > muestre el dashboard**, no asumir `216.24.57.1` si Render indica otro.

3. **Custom Domains en Render:** agregar los tres dominios (apex, `www`, `api`) en sus
   servicios para que Render emita los certificados **TLS** automáticamente y verifique el DNS.
   Render redirige automáticamente entre apex y `www`.

## Orden de deploy

1. Crear Atlas M0 y cargar los secrets en Render.
2. Aplicar el blueprint (`render.yaml`) → crea db, key value, api, web.
3. Deploy del backend → verificar en logs que Flyway aplicó `V1`, `V2` y `V3` (seed de Mongo).
4. Deploy del frontend con `VITE_API_URL`.
5. Conectar dominios + DNS; ajustar CORS si se usa `www`.
6. Configurar el pinger de uptime.

## Verificación

- **Arranque:** logs muestran Flyway aplicando migraciones sin error; healthcheck en verde.
- **Datos:** `GET /api/libros/home?usuarioId=1` devuelve libros; Atlas muestra `libros` poblada.
- **Auth/REST:** login `admin@booklibre.com` / `admin123` devuelve JWT; endpoint admin responde.
- **GraphQL:** `/api/graphiql` resuelve el tablero de KPIs (cruza Postgres + Redis + Mongo).
- **Cache (Parte 1):** click en un libro → home sirve el ranking desde Redis.
- **Frontend e2e:** `https://booklibre.com.ar` permite login/reservas sin errores de CORS.
