-- Función: búsqueda de libros con filtros opcionales, ordenamiento dinámico y paginación en BDD
CREATE OR REPLACE FUNCTION fn_buscar_libros(
    p_query        TEXT    DEFAULT '',
    p_generos      TEXT[]  DEFAULT NULL,   -- NULL → sin filtro por género
    p_paginas_min  INT     DEFAULT NULL,
    p_paginas_max  INT     DEFAULT NULL,
    p_isbn         TEXT    DEFAULT NULL,
    p_prestado_por TEXT    DEFAULT NULL,
    p_excluir_id   INT     DEFAULT NULL,   -- excluir libros del dueño con este id
    p_excluir_ids  INT[]   DEFAULT NULL,   -- excluir libros reservados (por id)
    p_sort_by      TEXT    DEFAULT 'titulo',
    p_limit        INT     DEFAULT NULL, 
    p_offset       INT     DEFAULT 0
)
RETURNS TABLE (libro_id INT, total BIGINT)
LANGUAGE plpgsql AS $$
BEGIN
    RETURN QUERY
    SELECT
        l.id,
        COUNT(*) OVER ()::BIGINT   -- total de filas antes de aplicar LIMIT (mismo patrón que fn_ultimas_calificaciones)
    FROM libros l
    JOIN usuarios u ON u.id = l.duenio_id
    WHERE l.activo = true
      AND (p_excluir_id  IS NULL OR l.duenio_id      != p_excluir_id)
      AND (p_excluir_ids IS NULL OR l.id              != ALL(p_excluir_ids))
      AND (p_query = ''  OR p_query IS NULL
           OR LOWER(l.titulo) LIKE '%' || LOWER(p_query) || '%'
           OR LOWER(l.autor)  LIKE '%' || LOWER(p_query) || '%')
      AND (p_generos      IS NULL OR l.genero = ANY(p_generos))
      AND (p_paginas_min  IS NULL OR l.cantidad_paginas >= p_paginas_min)
      AND (p_paginas_max  IS NULL OR l.cantidad_paginas <= p_paginas_max)
      AND (p_isbn IS NULL OR p_isbn = ''
           OR LOWER(l.isbn) LIKE '%' || LOWER(p_isbn) || '%')
      AND (p_prestado_por IS NULL OR p_prestado_por = ''
           OR LOWER(u.nombre) LIKE '%' || LOWER(p_prestado_por) || '%')
    ORDER BY
        CASE WHEN p_sort_by = 'autor'  THEN LOWER(l.autor)   END ASC,
        CASE WHEN p_sort_by = 'duenio' THEN LOWER(u.nombre)  END ASC,
        CASE WHEN p_sort_by NOT IN ('autor', 'duenio') THEN LOWER(l.titulo) END ASC
    LIMIT p_limit OFFSET p_offset;
END;
$$;
