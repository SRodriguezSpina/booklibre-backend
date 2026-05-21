-- src/main/resources/db/migration/V6__create_funciones_y_views.sql
-- Las únicas funciones/views que el código Kotlin usa hoy:
--   - fn_reservas_anuales_usuario       (ReservaJpaRepository:45)
--   - vw_usuarios_con_reservas_devueltas (ReservaJpaRepository:58)
--   - fn_usuarios_con_mas_de_n_reservas  (ReservaJpaRepository:70)
--
-- Todas operan solo sobre tablas relacionales (reservas, usuarios) — no
-- dependen de libros ni calificaciones (que viven en Mongo).
-- Las funciones antiguas que dependían de libros (fn_buscar_libros,
-- fn_ultimas_calificaciones) y el trigger sobre calificaciones se
-- eliminaron junto con sus tablas.

-- ── fn_reservas_anuales_usuario ───────────────────────────────────────────
-- Devuelve las reservas del año en curso para un usuario, con info de
-- libro denormalizada. Como los libros están en Mongo, los campos de
-- libro (titulo, autor) quedan como NULL aquí; la app los rellena
-- después con un lookup a Mongo si es necesario.
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

-- ── vw_usuarios_con_reservas_devueltas ─────────────────────────────────────
-- Usuarios con más de 2 reservas devueltas (fecha_hasta en el pasado).
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

-- ── fn_usuarios_con_mas_de_n_reservas ─────────────────────────────────────
-- Usuarios con al menos N reservas totales.
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
