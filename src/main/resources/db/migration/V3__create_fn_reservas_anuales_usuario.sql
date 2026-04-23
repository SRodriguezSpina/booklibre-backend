CREATE FUNCTION fn_reservas_anuales_usuario(
    p_usuario_id INT
)
RETURNS TABLE (
    reserva_id INT,
    usuario_id INT,
    usuario_nombre VARCHAR,
    libro_id INT,
    libro_titulo VARCHAR,
    libro_autor VARCHAR,
    fecha_desde DATE,
    fecha_hasta DATE,
    anio_reserva INT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        r.id AS reserva_id,
        r.usuario_id,
        u.nombre AS usuario_nombre,
        r.libro_id,
        l.titulo AS libro_titulo,
        l.autor AS libro_autor,
        r.fecha_desde,
        r.fecha_hasta,
        EXTRACT(YEAR FROM r.fecha_desde)::INT AS anio_reserva
    FROM reservas r
    JOIN usuarios u ON u.id = r.usuario_id
    JOIN libros l   ON l.id = r.libro_id
    WHERE r.usuario_id = p_usuario_id
      AND EXTRACT(YEAR FROM r.fecha_desde)::INT = EXTRACT(YEAR FROM CURRENT_DATE)::INT;
END;
$$ LANGUAGE plpgsql;