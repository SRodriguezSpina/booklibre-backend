CREATE VIEW vw_reservas_anuales_usuario AS
SELECT
    r.id AS reserva_id,
    r.usuario_id,
    u.nombre AS usuario_nombre,
    r.libro_id,
    l.titulo AS libro_titulo,
    l.autor AS libro_autor,
    r.fecha_desde,
    r.fecha_hasta,
    EXTRACT(YEAR FROM r.fecha_desde) AS anio_reserva
FROM reservas r
JOIN usuarios u ON u.id = r.usuario_id
JOIN libros l ON l.id = r.libro_id
WHERE EXTRACT(YEAR FROM r.fecha_desde) = EXTRACT(YEAR FROM CURRENT_DATE);