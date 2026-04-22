CREATE VIEW vw_usuarios_con_reservas_devueltas AS
SELECT
    u.id AS usuario_id,
    u.nombre AS usuario_nombre,
    COALESCE(COUNT(r.id), 0) AS cantidad_reservas_devueltas
FROM usuarios u
         JOIN reservas r ON r.usuario_id = u.id
WHERE r.fecha_hasta < CURRENT_DATE
GROUP BY u.id, u.nombre
HAVING COUNT(r.id) > 2;