-- Recalcula el promedio de calificaciones para todos los libros que tienen reseñas,
-- corrigiendo los registros insertados directamente en el seed sin pasar por el dominio.
UPDATE libros l
SET calificacion = sub.avg_valor
FROM (
    SELECT libro_id, ROUND(AVG(valor)::NUMERIC, 1) AS avg_valor
    FROM calificaciones
    GROUP BY libro_id
) sub
WHERE l.id = sub.libro_id;
