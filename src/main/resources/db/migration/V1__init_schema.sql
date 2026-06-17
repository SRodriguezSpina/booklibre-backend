-- src/main/resources/db/migration/V1__init_schema.sql
-- Esquema relacional completo de Postgres (tablas + funciones + views).
-- Libros y calificaciones viven en MongoDB (poblados en V3).
-- Baseline para base virgen: no hay parches incrementales posteriores.

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
    es_admin        BOOLEAN      NOT NULL DEFAULT FALSE,
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
-- a Mongo. Hoy queda vacía; la app la popula cuando se agregan calificaciones.
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

-- ── Funciones y views ─────────────────────────────────────────────────────
-- Únicas funciones/views que el código Kotlin usa hoy:
--   - fn_reservas_anuales_usuario       (ReservaJpaRepository:45)
--   - vw_usuarios_con_reservas_devueltas (ReservaJpaRepository:58)
--   - fn_usuarios_con_mas_de_n_reservas  (ReservaJpaRepository:70)
-- Todas operan solo sobre tablas relacionales (reservas, usuarios).

-- fn_reservas_anuales_usuario: reservas del año en curso para un usuario, con
-- info de libro denormalizada. Los campos de libro quedan NULL (viven en Mongo);
-- la app los rellena con un lookup a Mongo si es necesario.
CREATE OR REPLACE FUNCTION fn_reservas_anuales_usuario(p_usuario_id INT)
RETURNS TABLE (
    reserva_id      INT,
    usuario_id      INT,
    usuario_nombre  TEXT,
    libro_id        INT,
    libro_titulo    TEXT,
    libro_autor     TEXT,
    fecha_desde     TIMESTAMP,
    fecha_hasta     TIMESTAMP,
    anio_reserva    INT
)
LANGUAGE plpgsql AS $$
BEGIN
    RETURN QUERY
    SELECT
        r.id,
        u.id,
        u.nombre::TEXT,
        r.libro_id,
        NULL::TEXT AS libro_titulo,
        NULL::TEXT AS libro_autor,
        r.fecha_desde,
        r.fecha_hasta,
        EXTRACT(YEAR FROM r.fecha_desde)::INT AS anio_reserva
    FROM reservas r
    JOIN usuarios u ON u.id = r.usuario_id
    WHERE r.usuario_id = p_usuario_id
      AND EXTRACT(YEAR FROM r.fecha_desde) = EXTRACT(YEAR FROM CURRENT_TIMESTAMP)
    ORDER BY r.fecha_desde ASC;
END;
$$;

-- vw_usuarios_con_reservas_devueltas: usuarios con más de 2 reservas devueltas.
CREATE OR REPLACE VIEW vw_usuarios_con_reservas_devueltas AS
SELECT
    u.id   AS usuario_id,
    u.nombre AS usuario_nombre,
    COUNT(r.id) AS cantidad_reservas_devueltas
FROM usuarios u
JOIN reservas r ON r.usuario_id = u.id
WHERE r.fecha_hasta < CURRENT_TIMESTAMP
GROUP BY u.id, u.nombre
HAVING COUNT(r.id) > 2;

-- fn_usuarios_con_mas_de_n_reservas: usuarios con al menos N reservas totales.
CREATE OR REPLACE FUNCTION fn_usuarios_con_mas_de_n_reservas(p_min_reservas INT)
RETURNS TABLE (
    usuario_id        INT,
    usuario_nombre    TEXT,
    cantidad_reservas BIGINT
)
LANGUAGE plpgsql AS $$
BEGIN
    RETURN QUERY
    SELECT
        u.id,
        u.nombre::TEXT,
        COUNT(r.id) AS cantidad_reservas
    FROM usuarios u
    JOIN reservas r ON r.usuario_id = u.id
    GROUP BY u.id, u.nombre
    HAVING COUNT(r.id) >= p_min_reservas;
END;
$$;
