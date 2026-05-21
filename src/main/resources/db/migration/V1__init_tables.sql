-- src/main/resources/db/migration/V1__init_tables.sql
-- Esquema final de Postgres: solo tablas relacionales.
-- Libros y calificaciones viven en MongoDB (poblados en V3 y V5).

-- ── usuarios ──────────────────────────────────────────────────────────────
CREATE TABLE usuarios (
    id              SERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    nombre          VARCHAR(255) NOT NULL,
    descripcion     VARCHAR(500),
    celular         VARCHAR(20),
    ciudad          VARCHAR(100),
    tipo_usuario    VARCHAR(50)  NOT NULL,
    bibliokarmas    INT          NOT NULL DEFAULT 0,
    imagen_url      VARCHAR(2048),
    fecha_registro  DATE         NOT NULL DEFAULT CURRENT_DATE,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    reservados      INT          NOT NULL DEFAULT 0,
    leidos          INT          NOT NULL DEFAULT 0,
    CONSTRAINT uk_usuarios_email   UNIQUE (email)
);

-- Constraint celular único parcial (NULL permitidos, no-NULL únicos)
CREATE UNIQUE INDEX uk_usuarios_celular_partial
    ON usuarios (celular)
    WHERE celular IS NOT NULL;

-- ── reservas ──────────────────────────────────────────────────────────────
-- libro_id es referencia "soft" a un documento en booklibreMongo.libros.
-- NO hay FK porque Postgres no puede validar referencias a Mongo.
-- La integridad se mantiene en el servicio (LibroRepository.getById).
CREATE TABLE reservas (
    id            SERIAL    PRIMARY KEY,
    libro_id      INT       NOT NULL,
    usuario_id    INT       NOT NULL,
    fecha_desde   TIMESTAMP NOT NULL,
    fecha_hasta   TIMESTAMP NOT NULL,
    bibliokarmas  INT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_reservas_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_reservas_libro_id   ON reservas (libro_id);
CREATE INDEX idx_reservas_usuario_id ON reservas (usuario_id);
CREATE INDEX idx_reservas_fechas     ON reservas (fecha_desde, fecha_hasta);

-- ── historial_calificaciones_libro ────────────────────────────────────────
-- Mapeado por la entidad JPA HistorialPuntaje. libro_id es referencia soft
-- a Mongo. Hoy queda vacía; la app la popula cuando se agregan
-- calificaciones (feature pendiente fuera de este PR).
CREATE TABLE historial_calificaciones_libro (
    id                  SERIAL    PRIMARY KEY,
    libro_id            INT       NOT NULL,
    valor_anterior      INT,
    valor_nuevo         INT       NOT NULL,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_id          INT       NOT NULL
);

CREATE INDEX idx_historial_libro_id   ON historial_calificaciones_libro (libro_id);
CREATE INDEX idx_historial_usuario_id ON historial_calificaciones_libro (usuario_id);
