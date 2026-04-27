UPDATE reservas
SET bibliokarmas = sub.total_karma
FROM (
    SELECT
        r.id,
        (5 * (r.fecha_hasta - r.fecha_desde + 1)) +
        CASE l.tipo
            WHEN 'Comun'          THEN l.cantidad_paginas * (CASE WHEN u.bibliokarmas < 1000 THEN 5 ELSE 2 END)
            WHEN 'ConDedicatoria' THEN 200
            WHEN 'Coleccionable'  THEN CEIL(u.bibliokarmas::numeric / 5.0)::int + l.cantidad_paginas
            ELSE 0
        END AS total_karma
    FROM reservas r
    JOIN libros   l ON l.id = r.libro_id
    JOIN usuarios u ON u.id = r.usuario_id
    WHERE r.bibliokarmas = 0
) sub
WHERE reservas.id = sub.id;
