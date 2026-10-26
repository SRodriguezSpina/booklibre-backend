# Programación con Herramientas Modernas
## Backend Booklibre grupo-12

## Desarrollo local

El backend usa PostgreSQL para usuarios y reservas, MongoDB para libros y Redis
para caches. El perfil `local` (predeterminado) espera estos servicios en
`localhost` y MongoDB standalone; no requiere sharding ni replica sets.

### Requisitos

- Docker Compose
- Java 21 para ejecutar el backend fuera de Docker

### Levantar todo con Docker

Desde esta carpeta:

```powershell
docker compose up --build
```

La aplicación queda en `http://localhost:8080`, Swagger UI en
`http://localhost:8080/swagger-ui.html` y GraphiQL en
`http://localhost:8080/api/graphiql`.

### Ejecutar el backend desde el IDE o Gradle

Primero se levantan únicamente las dependencias:

```powershell
docker compose up -d postgres mongo redis
```

Luego, desde esta carpeta:

```powershell
.\gradlew.bat bootRun
```

El perfil `local` es el predeterminado. Al iniciar por primera vez, Flyway
crea el esquema y los datos iniciales en PostgreSQL y MongoDB. Para usar una
instancia Mongo distinta, definir `MONGODB_URI`; por ejemplo,
`mongodb://localhost:27017/booklibreMongo`.

Para detener los servicios:

```powershell
docker compose down
```

Los datos de PostgreSQL y MongoDB se conservan en volúmenes de Docker.
Redis se usa como cache y puede reconstruirse desde los datos persistidos.

## Pruebas

```powershell
.\gradlew.bat test
```
