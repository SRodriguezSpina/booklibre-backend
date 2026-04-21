-- V5: Función de últimas calificaciones para DetalleLibro

-- Función: últimas N reseñas de un libro con nombre de usuario y total general
CREATE OR REPLACE FUNCTION fn_ultimas_calificaciones(
    p_libro_id INT,
    p_limite   INT DEFAULT NULL   -- NULL → sin LIMIT (devuelve todas)
)
RETURNS TABLE (
    usuario_id           INT,
    nombre_usuario       TEXT,
    valor                INT,
    comentario           TEXT,
    total_calificaciones BIGINT
)
LANGUAGE plpgsql AS $$
BEGIN
    RETURN QUERY
    SELECT
        c.usuario_id,
        u.nombre::TEXT,
        c.valor,
        c.comentario::TEXT,
        COUNT(*) OVER ()::BIGINT
    FROM calificaciones c
    JOIN usuarios u ON u.id = c.usuario_id
    WHERE c.libro_id = p_libro_id
    ORDER BY c.id DESC
    LIMIT p_limite;
END;
$$;

