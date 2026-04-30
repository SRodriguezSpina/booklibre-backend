-- Change fecha_desde / fecha_hasta from DATE to TIMESTAMP to preserve time precision

-- Drop views that depend on the columns being altered
DROP VIEW IF EXISTS vw_usuarios_con_reservas_devueltas;

ALTER TABLE reservas
    ALTER COLUMN fecha_desde TYPE TIMESTAMP USING fecha_desde::TIMESTAMP,
    ALTER COLUMN fecha_hasta TYPE TIMESTAMP USING fecha_hasta::TIMESTAMP;

-- Recreate vw_usuarios_con_reservas_devueltas using TIMESTAMP comparison
CREATE VIEW vw_usuarios_con_reservas_devueltas AS
SELECT
    u.id AS usuario_id,
    u.nombre AS usuario_nombre,
    COALESCE(COUNT(r.id), 0) AS cantidad_reservas_devueltas
FROM usuarios u
         JOIN reservas r ON r.usuario_id = u.id
WHERE r.fecha_hasta < CURRENT_TIMESTAMP
GROUP BY u.id, u.nombre
HAVING COUNT(r.id) > 2;

-- Recreate fn_reservas_anuales_usuario returning TIMESTAMP instead of DATE
DROP FUNCTION IF EXISTS fn_reservas_anuales_usuario(integer);
CREATE OR REPLACE FUNCTION fn_reservas_anuales_usuario(
    p_usuario_id INT
)
RETURNS TABLE (
    reserva_id     INT,
    usuario_id     INT,
    usuario_nombre VARCHAR,
    libro_id       INT,
    libro_titulo   VARCHAR,
    libro_autor    VARCHAR,
    fecha_desde    TIMESTAMP,
    fecha_hasta    TIMESTAMP,
    anio_reserva   INT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        r.id                                           AS reserva_id,
        r.usuario_id,
        u.nombre                                       AS usuario_nombre,
        r.libro_id,
        l.titulo                                       AS libro_titulo,
        l.autor                                        AS libro_autor,
        r.fecha_desde,
        r.fecha_hasta,
        EXTRACT(YEAR FROM r.fecha_desde)::INT          AS anio_reserva
    FROM reservas r
    JOIN usuarios u ON u.id = r.usuario_id
    JOIN libros   l ON l.id = r.libro_id
    WHERE r.usuario_id = p_usuario_id
      AND EXTRACT(YEAR FROM r.fecha_desde)::INT = EXTRACT(YEAR FROM CURRENT_DATE)::INT;
END;
$$ LANGUAGE plpgsql;
