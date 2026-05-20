-- V20__seed_massive_libros.sql
--
-- Genera 15.000 libros vía generate_series para poder demostrar sharding
-- con datos reales distribuidos entre los dos shards de MongoDB.
--
-- Características:
--   · Idempotente: el WHERE NOT EXISTS evita duplicados en reinicios.
--   · No incluye disponibilidad (eliminada en V19).
--   · Distribuye libros entre todos los usuarios no-admin existentes.
--   · V18 los migrará automáticamente a MongoDB al arrancar el backend.

WITH duenios AS (
    SELECT id,
           ROW_NUMBER() OVER (ORDER BY id) - 1 AS idx
    FROM   usuarios
    WHERE  tipo_usuario <> 'ADMIN'
),
total AS (
    SELECT COUNT(*) AS n FROM duenios
)
INSERT INTO libros (
    tipo, titulo, autor, descripcion, genero, estado,
    cantidad_paginas, isbn, idioma, editorial,
    fecha_publicacion, fecha_agregado, imagen_url, duenio_id, activo
)
SELECT
    -- tipo: 60% Comun, 25% ConDedicatoria, 15% Coleccionable
    (ARRAY['Comun','Comun','Comun','Comun','ConDedicatoria','ConDedicatoria',
           'Coleccionable'])[(i % 7) + 1],

    -- titulo: combinación de 12 prefijos × 12 sufijos = 144 variantes
    'Libro #' || i || ': ' ||
    (ARRAY[
        'El misterio de',   'La sombra de',   'Entre las ruinas de', 'Memorias de',
        'El secreto de',    'La historia de', 'Más allá de',         'El viaje a',
        'La caída de',      'El origen de',   'Cartas desde',        'El último'
    ])[(i % 12) + 1] || ' ' ||
    (ARRAY[
        'la oscuridad', 'los sueños',          'la humanidad',    'el tiempo',
        'los mares',    'la luz',              'las estrellas',   'el silencio',
        'la esperanza', 'los recuerdos',       'la ciudad perdida','el futuro'
    ])[((i / 12) % 12) + 1],

    -- autor: 16 autores ficticios
    (ARRAY[
        'María González',   'Carlos Rodríguez', 'Laura Martínez',  'Juan López',
        'Ana Fernández',    'Pedro Sánchez',    'Sofía Ramírez',   'Diego Torres',
        'Valentina Castro', 'Martín Flores',    'Lucía Moreno',    'Andrés Jiménez',
        'Camila Ruiz',      'Roberto Díaz',     'Isabel Herrera',  'Fernando Vargas'
    ])[(i % 16) + 1],

    -- descripcion
    'Descripción del libro número ' || i || '. Una obra que explora temas de ' ||
    (ARRAY[
        'aventura', 'amor', 'ciencia', 'filosofía', 'historia',
        'tecnología', 'naturaleza', 'sociedad', 'política', 'arte'
    ])[(i % 10) + 1] || ' desde una perspectiva única y enriquecedora.',

    -- genero
    (ARRAY[
        'CIENCIA_FICCION', 'ROMANCE',   'TERROR',    'FANTASIA',
        'MISTERIO',        'HISTORIA',  'BIOGRAFIA', 'DISENO',
        'CIENCIA_FICCION', 'ROMANCE',   'FANTASIA',  'MISTERIO'
    ])[(i % 12) + 1],

    -- estado
    (ARRAY['EXCELENTE','MUY_BUENO','BUENO','REGULAR','EXCELENTE','MUY_BUENO'])[(i % 6) + 1],

    -- cantidad_paginas: entre 150 y 849
    150 + (i % 700),

    -- isbn: único garantizado (978 + número de 10 dígitos a partir del millardo)
    '978' || LPAD((1000000000 + i)::TEXT, 10, '0'),

    -- idioma: mayoría español
    (ARRAY['ESPANOL','INGLES','ESPANOL','ESPANOL','FRANCES','INGLES','PORTUGUES','ESPANOL'])[(i % 8) + 1],

    -- editorial: 15 editoriales rotando
    (ARRAY[
        'Planeta','Alfaguara','Anagrama','Tusquets','Salamandra',
        'Destino','Seix Barral','Random House','Penguin','Paidós',
        'Siglo XXI','FCE','Norma','Sudamericana','Emecé'
    ])[(i % 15) + 1],

    -- fecha_publicacion: distribuida entre 1950 y ~2023
    ('1950-01-01'::DATE + ((i * 17) % 27000) * INTERVAL '1 day'),

    -- fecha_agregado: distribuida en los últimos 2 años
    ('2023-01-01'::DATE + (i % 730) * INTERVAL '1 day'),

    -- imagen_url: placeholder de picsum con seed único
    'https://picsum.photos/seed/libro' || i || '/200/300',

    -- duenio_id: rotando entre todos los usuarios no-admin
    (SELECT id FROM duenios WHERE idx = (i % (SELECT n FROM total))),

    -- activo
    TRUE

FROM generate_series(1, 15000) AS g(i)
-- idempotencia: no inserta si el ISBN ya existe
WHERE NOT EXISTS (
    SELECT 1 FROM libros
    WHERE  isbn = '978' || LPAD((1000000000 + i)::TEXT, 10, '0')
);
