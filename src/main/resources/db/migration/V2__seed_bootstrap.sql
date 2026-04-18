-- ── Usuarios ──────────────────────────────────────────────────────────────────
INSERT INTO usuarios (email, password, nombre, descripcion, celular, ciudad, tipo_usuario, bibliokarmas, imagen_url, fecha_registro)
VALUES
    ('juan@mail.com',     '123', 'Juan Pérez',   'Lector intrépido',    '11556677', 'San Martín', 'LECTOR',            500,  'https://randomuser.me/api/portraits/men/1.jpg',   '2020-09-15'),
    ('emilia@example.com','123', 'Emilia Romero', 'Lectora Ávida',       '11223344', 'CABA',       'LECTOR_PUBLICADOR', 2345, 'https://randomuser.me/api/portraits/women/2.jpg', '2021-06-01'),
    ('ana@example.com',   '123', 'Ana García',   'Publicadora',         '11889900', 'Quilmes',    'PUBLICADOR',        1200, 'https://randomuser.me/api/portraits/women/3.jpg', '2022-02-10'),
    ('carlos@example.com','123', 'Carlos Ruiz',   'Fan de los clásicos', '11334455', 'Lanús',      'LECTOR',            150,  'https://randomuser.me/api/portraits/men/4.jpg',   '2019-12-30')
ON CONFLICT (email) DO NOTHING;

-- ── Libros Comunes ─────────────────────────────────────────────────────────────
INSERT INTO libros (tipo, titulo, autor, descripcion, genero, estado, disponibilidad, cantidad_paginas, isbn, idioma, editorial, fecha_publicacion, fecha_agregado, imagen_url, duenio_id)
SELECT 'Comun','1984','George Orwell',
    'Una distopía sobre el totalitarismo y la vigilancia del Estado.',
    'CIENCIA_FICCION','EXCELENTE','DISPONIBLE',328,'9780451524935','ESPANOL','Debolsillo',
    '1949-06-08','2025-01-05',
    'https://contentv2.tap-commerce.com/cover/large/9789875669284_1.jpg',
    u.id FROM usuarios u
WHERE u.email = 'juan@mail.com'
  AND NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '9780451524935');

INSERT INTO libros (tipo, titulo, autor, descripcion, genero, estado, disponibilidad, cantidad_paginas, isbn, idioma, editorial, fecha_publicacion, fecha_agregado, imagen_url, duenio_id)
SELECT 'Comun','Clean Code','Robert C. Martin',
    'Guía práctica para escribir código limpio y mantenible.',
    'DISENO','MUY_BUENO','DISPONIBLE',464,'9780132350884','INGLES','Prentice Hall',
    '2008-08-01','2025-01-06',
    'https://m.media-amazon.com/images/I/51E2055ZGUL._AC_UF1000,1000_QL80_.jpg',
    u.id FROM usuarios u
WHERE u.email = 'juan@mail.com'
  AND NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '9780132350884');

INSERT INTO libros (tipo, titulo, autor, descripcion, genero, estado, disponibilidad, cantidad_paginas, isbn, idioma, editorial, fecha_publicacion, fecha_agregado, imagen_url, duenio_id)
SELECT 'Comun','El Principito','Antoine de Saint-Exupéry',
    'Un clásico de la literatura universal sobre la infancia y la amistad.',
    'DRAMA','BUENO','DISPONIBLE',96,'9780156012195','ESPANOL','Salamandra',
    '1943-04-06','2025-01-07',
    'https://tienda.planetadelibros.com.ar/cdn/shop/products/portada_el-principito_antoine-de-saint-exupery_201507152131.jpg',
    u.id FROM usuarios u
WHERE u.email = 'juan@mail.com'
  AND NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '9780156012195');

INSERT INTO libros (tipo, titulo, autor, descripcion, genero, estado, disponibilidad, cantidad_paginas, isbn, idioma, editorial, fecha_publicacion, fecha_agregado, imagen_url, duenio_id)
SELECT 'Comun','Sapiens','Yuval Noah Harari',
    'Un recorrido por la historia de la humanidad desde los orígenes.',
    'AUTOAYUDA','EXCELENTE','DISPONIBLE',443,'9780062316097','ESPANOL','Debate',
    '2011-01-01','2025-01-08',
    'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSyAJxSA62_Z8k-pqxGp8x3Zp1bWoAtBjkZow&s',
    u.id FROM usuarios u
WHERE u.email = 'juan@mail.com'
  AND NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '9780062316097');

INSERT INTO libros (tipo, titulo, autor, descripcion, genero, estado, disponibilidad, cantidad_paginas, isbn, idioma, editorial, fecha_publicacion, fecha_agregado, imagen_url, duenio_id)
SELECT 'Comun','Don Quijote de la Mancha','Miguel de Cervantes',
    'La obra cumbre de la literatura española sobre el caballero de La Mancha.',
    'LITERATURA_CLASICA','BUENO','DISPONIBLE',1023,'9788467027983','ESPANOL','Espasa',
    '1605-01-16','2024-09-10',
    'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQcMRZdGl9oMFn7_O4oyErwUXK9yrN3OcSe0w&s',
    u.id FROM usuarios u
WHERE u.email = 'carlos@example.com'
  AND NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '9788467027983');

INSERT INTO libros (tipo, titulo, autor, descripcion, genero, estado, disponibilidad, cantidad_paginas, isbn, idioma, editorial, fecha_publicacion, fecha_agregado, imagen_url, duenio_id)
SELECT 'Comun','Crimen y Castigo','Fiódor Dostoyevski',
    'Un estudiante comete un crimen y lidia con las consecuencias psicológicas.',
    'DRAMA','MUY_BUENO','DISPONIBLE',671,'9788420674469','ESPANOL','Alianza',
    '1866-01-01','2024-09-11',
    'https://www.edicontinente.com.ar/image/titulos/9788417477639.jpg',
    u.id FROM usuarios u
WHERE u.email = 'carlos@example.com'
  AND NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '9788420674469');

INSERT INTO libros (tipo, titulo, autor, descripcion, genero, estado, disponibilidad, cantidad_paginas, isbn, idioma, editorial, fecha_publicacion, fecha_agregado, imagen_url, duenio_id)
SELECT 'Comun','Dune','Frank Herbert',
    'La épica historia de Paul Atreides en el planeta desértico Arrakis.',
    'CIENCIA_FICCION','EXCELENTE','DISPONIBLE',688,'9780441013593','ESPANOL','Minotauro',
    '1965-08-01','2024-09-12',
    'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTrP4Ohql-5Cf2oMJNHvnj5V-ZVm3Q8rpNj0OJXuGa2dOgPMIVMchKJCRSDEJnbUsJ7mHh1C3xE3wfDDncifQkAiSixNji3-L2yN8laRAU',
    u.id FROM usuarios u
WHERE u.email = 'carlos@example.com'
  AND NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '9780441013593');

INSERT INTO libros (tipo, titulo, autor, descripcion, genero, estado, disponibilidad, cantidad_paginas, isbn, idioma, editorial, fecha_publicacion, fecha_agregado, imagen_url, duenio_id)
SELECT 'Comun','El alquimista','Paulo Coelho',
    'Un pastor andaluz viaja en busca de su tesoro personal.',
    'AUTOAYUDA','BUENO','DISPONIBLE',208,'9780062315007','ESPANOL','Planeta',
    '1988-01-01','2024-09-13',
    'https://m.media-amazon.com/images/I/71aFt4+OTOL.jpg',
    u.id FROM usuarios u
WHERE u.email = 'carlos@example.com'
  AND NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '9780062315007');

-- ── Libros Con Dedicatoria ─────────────────────────────────────────────────────
INSERT INTO libros (tipo, titulo, autor, descripcion, genero, estado, disponibilidad, cantidad_paginas, isbn, idioma, editorial, fecha_publicacion, fecha_agregado, imagen_url, duenio_id)
SELECT 'ConDedicatoria','Harry Potter y la Piedra Filosofal','J.K. Rowling',
    'Un joven descubre que es un mago y asiste a la escuela Hogwarts.',
    'LITERATURA_CLASICA','EXCELENTE','DISPONIBLE',309,'9788478884452','ESPANOL','Salamandra',
    '1997-06-26','2024-03-01',
    'https://images.cdn3.buscalibre.com/fit-in/360x360/d7/e7/d7e777ce24f10eabdaae28d65ab70bd3.jpg',
    u.id FROM usuarios u
WHERE u.email = 'ana@example.com'
  AND NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '9788478884452');

INSERT INTO libros (tipo, titulo, autor, descripcion, genero, estado, disponibilidad, cantidad_paginas, isbn, idioma, editorial, fecha_publicacion, fecha_agregado, imagen_url, duenio_id)
SELECT 'ConDedicatoria','El Código Da Vinci','Dan Brown',
    'Un criptólogo descifra una serie de enigmas escondidos en obras de Da Vinci.',
    'DRAMA','MUY_BUENO','DISPONIBLE',489,'9788408174189','ESPANOL','Planeta',
    '2003-03-18','2024-03-02',
    'https://encrypted-tbn0.gstatic.com/shopping?q=tbn:ANd9GcSB8x01gOOsdG1D2IS0r3AquN2pGMtMnWLpFKPkF8pZZQ953Pbo1lHkAqxX1R_SWzHqmWeOC5Mcg7fbuJt_iLP76jzwWNtpbzPW3xMNlRZN',
    u.id FROM usuarios u
WHERE u.email = 'ana@example.com'
  AND NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '9788408174189');

INSERT INTO libros (tipo, titulo, autor, descripcion, genero, estado, disponibilidad, cantidad_paginas, isbn, idioma, editorial, fecha_publicacion, fecha_agregado, imagen_url, duenio_id)
SELECT 'ConDedicatoria','Crónica de una muerte anunciada','Gabriel García Márquez',
    'El relato del asesinato de Santiago Nasar en un pueblo del Caribe.',
    'LITERATURA_CLASICA','BUENO','DISPONIBLE',120,'9788497592437','ESPANOL','Diana',
    '1981-01-01','2024-03-03',
    'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSVTCjrjj-XTtQPa0m4C8zNQtyvHz1HeB4zsQ&s',
    u.id FROM usuarios u
WHERE u.email = 'ana@example.com'
  AND NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '9788497592437');

INSERT INTO libros (tipo, titulo, autor, descripcion, genero, estado, disponibilidad, cantidad_paginas, isbn, idioma, editorial, fecha_publicacion, fecha_agregado, imagen_url, duenio_id)
SELECT 'ConDedicatoria','Rayuela','Julio Cortázar',
    'Una novela experimental que puede leerse en múltiples órdenes.',
    'LITERATURA_CLASICA','MUY_BUENO','DISPONIBLE',600,'9788420471822','ESPANOL','Alfaguara',
    '1963-01-01','2024-03-04',
    'https://descargarlibrosenpdf.wordpress.com/wp-content/uploads/2017/05/rayuela-cortazar.jpg',
    u.id FROM usuarios u
WHERE u.email = 'ana@example.com'
  AND NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '9788420471822');

INSERT INTO libros (tipo, titulo, autor, descripcion, genero, estado, disponibilidad, cantidad_paginas, isbn, idioma, editorial, fecha_publicacion, fecha_agregado, imagen_url, duenio_id)
SELECT 'ConDedicatoria','Ficciones','Jorge Luis Borges',
    'Una colección de cuentos que mezclan lo fantástico con lo filosófico.',
    'LITERATURA_CLASICA','EXCELENTE','DISPONIBLE',224,'9788420633138','ESPANOL','Alianza',
    '1944-01-01','2024-06-01',
    'https://images.cdn1.buscalibre.com/fit-in/360x360/e4/a9/e4a975b4dce219d0b130b4d8915b8b88.jpg',
    u.id FROM usuarios u
WHERE u.email = 'emilia@example.com'
  AND NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '9788420633138');

INSERT INTO libros (tipo, titulo, autor, descripcion, genero, estado, disponibilidad, cantidad_paginas, isbn, idioma, editorial, fecha_publicacion, fecha_agregado, imagen_url, duenio_id)
SELECT 'ConDedicatoria','El nombre de la rosa','Umberto Eco',
    'Un monje investiga una serie de muertes misteriosas en una abadía medieval.',
    'DRAMA','BUENO','DISPONIBLE',500,'9788408081869','ESPANOL','Lumen',
    '1980-01-01','2024-06-02',
    'https://libroschorcha.wordpress.com/wp-content/uploads/2018/02/el-nombre-de-la-rosa-umberto-eco.jpg',
    u.id FROM usuarios u
WHERE u.email = 'emilia@example.com'
  AND NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '9788408081869');

INSERT INTO libros (tipo, titulo, autor, descripcion, genero, estado, disponibilidad, cantidad_paginas, isbn, idioma, editorial, fecha_publicacion, fecha_agregado, imagen_url, duenio_id)
SELECT 'ConDedicatoria','Fundación','Isaac Asimov',
    'Un matemático prevé la caída del Imperio Galáctico y planea preservar el conocimiento.',
    'CIENCIA_FICCION','MUY_BUENO','DISPONIBLE',244,'9788435018029','ESPANOL','Minotauro',
    '1951-01-01','2024-06-03',
    'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR9t9VngDwUP_IlIKXTYUAJkRC11dlsBX7ncw&s',
    u.id FROM usuarios u
WHERE u.email = 'emilia@example.com'
  AND NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '9788435018029');

INSERT INTO libros (tipo, titulo, autor, descripcion, genero, estado, disponibilidad, cantidad_paginas, isbn, idioma, editorial, fecha_publicacion, fecha_agregado, imagen_url, duenio_id)
SELECT 'ConDedicatoria','Orgullo y Prejuicio','Jane Austen',
    'La historia de amor entre Elizabeth Bennet y el señor Darcy en la Inglaterra del siglo XIX.',
    'ROMANCE','EXCELENTE','DISPONIBLE',432,'9788491050421','ESPANOL','Austral',
    '1813-01-28','2024-06-04',
    'https://m.media-amazon.com/images/I/71Q1tPupKjL.jpg',
    u.id FROM usuarios u
WHERE u.email = 'emilia@example.com'
  AND NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '9788491050421');

-- ── Libros Coleccionables ──────────────────────────────────────────────────────
INSERT INTO libros (tipo, titulo, autor, descripcion, genero, estado, disponibilidad, cantidad_paginas, isbn, idioma, editorial, fecha_publicacion, fecha_agregado, imagen_url, duenio_id)
SELECT 'Coleccionable','El Señor de los Anillos','J.R.R. Tolkien',
    'La épica lucha entre el bien y el mal en la Tierra Media.',
    'CIENCIA_FICCION','EXCELENTE','DISPONIBLE',1200,'9780000003005','ESPANOL','Minotauro',
    '1954-07-29','2025-02-20',
    'https://d22fxaf9t8d39k.cloudfront.net/2e23a0a073388dbaef0aa42e5c907cd5d9c5e52545af57bdfbf7f983b41eefcf114336.jpg',
    u.id FROM usuarios u
WHERE u.email = 'ana@example.com'
  AND NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '9780000003005');

INSERT INTO libros (tipo, titulo, autor, descripcion, genero, estado, disponibilidad, cantidad_paginas, isbn, idioma, editorial, fecha_publicacion, fecha_agregado, imagen_url, duenio_id)
SELECT 'Coleccionable','Guerra y Paz','León Tolstói',
    'La monumental novela sobre la invasión napoleónica de Rusia.',
    'LITERATURA_CLASICA','MUY_BUENO','DISPONIBLE',1440,'9788420651835','ESPANOL','Alianza',
    '1869-01-01','2025-02-21',
    'https://acdn-us.mitiendanube.com/stores/542/287/products/guerra_y_paz_lev_tolstoi1-4249aaf56c93a9f4e716770091781341-640-0.webp',
    u.id FROM usuarios u
WHERE u.email = 'ana@example.com'
  AND NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '9788420651835');

INSERT INTO libros (tipo, titulo, autor, descripcion, genero, estado, disponibilidad, cantidad_paginas, isbn, idioma, editorial, fecha_publicacion, fecha_agregado, imagen_url, duenio_id)
SELECT 'Coleccionable','Los Miserables','Victor Hugo',
    'La historia de Jean Valjean y su redención en la Francia del siglo XIX.',
    'DRAMA','BUENO','DISPONIBLE',1900,'9788420651842','FRANCES','Alianza',
    '1862-01-01','2025-02-22',
    'https://http2.mlstatic.com/D_NQ_NP_612382-MLA42127442306_062020-O.webp',
    u.id FROM usuarios u
WHERE u.email = 'ana@example.com'
  AND NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '9788420651842');

INSERT INTO libros (tipo, titulo, autor, descripcion, genero, estado, disponibilidad, cantidad_paginas, isbn, idioma, editorial, fecha_publicacion, fecha_agregado, imagen_url, duenio_id)
SELECT 'Coleccionable','En busca del tiempo perdido','Marcel Proust',
    'Una exploración de la memoria, el tiempo y la identidad en la alta sociedad francesa.',
    'LITERATURA_CLASICA','EXCELENTE','DISPONIBLE',4215,'9788420652009','FRANCES','Alianza',
    '1913-11-14','2025-02-23',
    'https://sbslibreria.vtexassets.com/arquivos/ids/5092712-1200-auto?v=638863180619070000&width=1200&height=auto&aspect=true',
    u.id FROM usuarios u
WHERE u.email = 'ana@example.com'
  AND NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '9788420652009');

INSERT INTO libros (tipo, titulo, autor, descripcion, genero, estado, disponibilidad, cantidad_paginas, isbn, idioma, editorial, fecha_publicacion, fecha_agregado, imagen_url, duenio_id)
SELECT 'Coleccionable','El Quijote Ilustrado','Miguel de Cervantes',
    'Edición de colección con ilustraciones originales del siglo XVII.',
    'LITERATURA_CLASICA','EXCELENTE','DISPONIBLE',1200,'9788467035001','ESPANOL','Espasa Edición Especial',
    '1605-01-16','2025-03-01',
    'https://images.cdn3.buscalibre.com/fit-in/360x360/3a/fc/3afcfcb813bc98ee2f549f21cb4a16b2.jpg',
    u.id FROM usuarios u
WHERE u.email = 'juan@mail.com'
  AND NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '9788467035001');

INSERT INTO libros (tipo, titulo, autor, descripcion, genero, estado, disponibilidad, cantidad_paginas, isbn, idioma, editorial, fecha_publicacion, fecha_agregado, imagen_url, duenio_id)
SELECT 'Coleccionable','Cien años de soledad','Gabriel García Márquez',
    'La saga de la familia Buendía en el mítico pueblo de Macondo.',
    'LITERATURA_CLASICA','EXCELENTE','DISPONIBLE',471,'9780307474728','ESPANOL','RAE Edición Conmemorativa',
    '1967-05-30','2025-03-02',
    'https://www.rae.es/sites/default/files/styles/obra_portada_ficha/public/portada_cien_anos_de_soledad_0.jpg?itok=DDV1xNqg',
    u.id FROM usuarios u
WHERE u.email = 'juan@mail.com'
  AND NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '9780307474728');

INSERT INTO libros (tipo, titulo, autor, descripcion, genero, estado, disponibilidad, cantidad_paginas, isbn, idioma, editorial, fecha_publicacion, fecha_agregado, imagen_url, duenio_id)
SELECT 'Coleccionable','Ulises','James Joyce',
    'Un día en la vida de Leopold Bloom en Dublín, obra clave del modernismo.',
    'LITERATURA_CLASICA','MUY_BUENO','DISPONIBLE',933,'9788420651866','INGLES','Alianza Edición Numerada',
    '1922-02-02','2025-03-03',
    'https://http2.mlstatic.com/D_NQ_NP_728018-MLU70496468924_072023-O.webp',
    u.id FROM usuarios u
WHERE u.email = 'juan@mail.com'
  AND NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '9788420651866');

INSERT INTO libros (tipo, titulo, autor, descripcion, genero, estado, disponibilidad, cantidad_paginas, isbn, idioma, editorial, fecha_publicacion, fecha_agregado, imagen_url, duenio_id)
SELECT 'Coleccionable','Moby Dick','Herman Melville',
    'La obsesiva cacería de la ballena blanca por el capitán Ahab.',
    'DRAMA','BUENO','DISPONIBLE',720,'9788420651873','INGLES','Alianza Edición Especial',
    '1851-10-18','2025-03-04',
    'https://m.media-amazon.com/images/I/81OthjkJBuL.jpg',
    u.id FROM usuarios u
WHERE u.email = 'juan@mail.com'
  AND NOT EXISTS (SELECT 1 FROM libros WHERE isbn = '9788420651873');

-- ── Reservas ──────────────────────────────────────────────────────────────────
INSERT INTO reservas (libro_id, usuario_id, fecha_desde, fecha_hasta)
SELECT l.id, u.id, DATE '2026-03-01', DATE '2026-03-10'
FROM libros l
JOIN usuarios u ON u.email = 'juan@mail.com'
WHERE l.isbn = '9788478884452'
  AND NOT EXISTS (
      SELECT 1 FROM reservas r
      WHERE r.libro_id = l.id
        AND r.usuario_id = u.id
        AND r.fecha_desde = DATE '2026-03-01'
        AND r.fecha_hasta = DATE '2026-03-10'
  );

INSERT INTO reservas (libro_id, usuario_id, fecha_desde, fecha_hasta)
SELECT l.id, u.id, DATE '2026-03-17', DATE '2026-04-01'
FROM libros l
JOIN usuarios u ON u.email = 'juan@mail.com'
WHERE l.isbn = '9788420633138'
  AND NOT EXISTS (
      SELECT 1 FROM reservas r
      WHERE r.libro_id = l.id
        AND r.usuario_id = u.id
        AND r.fecha_desde = DATE '2026-03-17'
        AND r.fecha_hasta = DATE '2026-04-01'
  );

INSERT INTO reservas (libro_id, usuario_id, fecha_desde, fecha_hasta)
SELECT l.id, u.id, DATE '2026-03-10', DATE '2026-03-30'
FROM libros l
JOIN usuarios u ON u.email = 'emilia@example.com'
WHERE l.isbn = '9780451524935'
  AND NOT EXISTS (
      SELECT 1 FROM reservas r
      WHERE r.libro_id = l.id
        AND r.usuario_id = u.id
        AND r.fecha_desde = DATE '2026-03-10'
        AND r.fecha_hasta = DATE '2026-03-30'
  );

INSERT INTO reservas (libro_id, usuario_id, fecha_desde, fecha_hasta)
SELECT l.id, u.id, DATE '2026-02-10', DATE '2026-02-28'
FROM libros l
JOIN usuarios u ON u.email = 'carlos@example.com'
WHERE l.isbn = '9788497592437'
  AND NOT EXISTS (
      SELECT 1 FROM reservas r
      WHERE r.libro_id = l.id
        AND r.usuario_id = u.id
        AND r.fecha_desde = DATE '2026-02-10'
        AND r.fecha_hasta = DATE '2026-02-28'
  );

INSERT INTO reservas (libro_id, usuario_id, fecha_desde, fecha_hasta)
SELECT l.id, u.id, DATE '2025-12-15', DATE '2025-12-29'
FROM libros l
JOIN usuarios u ON u.email = 'juan@mail.com'
WHERE l.isbn = '9788408174189'
  AND NOT EXISTS (
      SELECT 1 FROM reservas r
      WHERE r.libro_id = l.id
        AND r.usuario_id = u.id
        AND r.fecha_desde = DATE '2025-12-15'
        AND r.fecha_hasta = DATE '2025-12-29'
  );
INSERT INTO calificaciones (libro_id, usuario_id, valor, comentario)
SELECT l.id, u.id, 5, 'Una obra maestra absoluta. Orwell adelantó décadas lo que vendría.'
FROM libros l
JOIN usuarios u ON u.email = 'emilia@example.com'
WHERE l.isbn = '9780451524935';

INSERT INTO calificaciones (libro_id, usuario_id, valor, comentario)
SELECT l.id, u.id, 4, 'Impactante y perturbadora. Lectura obligatoria.'
FROM libros l
JOIN usuarios u ON u.email = 'ana@example.com'
WHERE l.isbn = '9780451524935';

INSERT INTO calificaciones (libro_id, usuario_id, valor, comentario)
SELECT l.id, u.id, 5, 'De las mejores que leí. El concepto del doblepensar es genial.'
FROM libros l
JOIN usuarios u ON u.email = 'carlos@example.com'
WHERE l.isbn = '9780451524935';

INSERT INTO calificaciones (libro_id, usuario_id, valor, comentario)
SELECT l.id, u.id, 4, 'Muy útil para cualquier desarrollador. Cambió cómo escribo código.'
FROM libros l
JOIN usuarios u ON u.email = 'emilia@example.com'
WHERE l.isbn = '9780132350884';

INSERT INTO calificaciones (libro_id, usuario_id, valor, comentario)
SELECT l.id, u.id, 5, 'Te cambia la perspectiva del mundo. Increíble.'
FROM libros l
JOIN usuarios u ON u.email = 'emilia@example.com'
WHERE l.isbn = '9780062316097';

INSERT INTO calificaciones (libro_id, usuario_id, valor, comentario)
SELECT l.id, u.id, 4, 'Muy bien escrito, aunque algunas partes son discutibles.'
FROM libros l
JOIN usuarios u ON u.email = 'carlos@example.com'
WHERE l.isbn = '9780062316097';

INSERT INTO calificaciones (libro_id, usuario_id, valor, comentario)
SELECT l.id, u.id, 5, 'Un clásico eterno. La magia de Hogwarts nunca envejece.'
FROM libros l
JOIN usuarios u ON u.email = 'juan@mail.com'
WHERE l.isbn = '9788478884452';

INSERT INTO calificaciones (libro_id, usuario_id, valor, comentario)
SELECT l.id, u.id, 5, 'La leí de chica y la sigo amando. Pura nostalgia.'
FROM libros l
JOIN usuarios u ON u.email = 'emilia@example.com'
WHERE l.isbn = '9788478884452';

INSERT INTO calificaciones (libro_id, usuario_id, valor, comentario)
SELECT l.id, u.id, 4, 'Entretenidísima. Entiende por qué arrasó en todo el mundo.'
FROM libros l
JOIN usuarios u ON u.email = 'carlos@example.com'
WHERE l.isbn = '9788478884452';

INSERT INTO calificaciones (libro_id, usuario_id, valor, comentario)
SELECT l.id, u.id, 5, 'Borges es insuperable. Cada cuento es un universo.'
FROM libros l
JOIN usuarios u ON u.email = 'juan@mail.com'
WHERE l.isbn = '9788420633138';

INSERT INTO calificaciones (libro_id, usuario_id, valor, comentario)
SELECT l.id, u.id, 4, 'La forma de leerla en saltos es una experiencia única.'
FROM libros l
JOIN usuarios u ON u.email = 'juan@mail.com'
WHERE l.isbn = '9788420471822';

INSERT INTO calificaciones (libro_id, usuario_id, valor, comentario)
SELECT l.id, u.id, 3, 'Interesante pero exige mucho del lector. No es para todos.'
FROM libros l
JOIN usuarios u ON u.email = 'carlos@example.com'
WHERE l.isbn = '9788420471822';

INSERT INTO calificaciones (libro_id, usuario_id, valor, comentario)
SELECT l.id, u.id, 5, 'La fantasía épica por excelencia. Tolkien es un genio.'
FROM libros l
JOIN usuarios u ON u.email = 'juan@mail.com'
WHERE l.isbn = '9780000003005';

INSERT INTO calificaciones (libro_id, usuario_id, valor, comentario)
SELECT l.id, u.id, 5, 'Esta edición de colección es una joya. El libro también.'
FROM libros l
JOIN usuarios u ON u.email = 'emilia@example.com'
WHERE l.isbn = '9780000003005';

INSERT INTO calificaciones (libro_id, usuario_id, valor, comentario)
SELECT l.id, u.id, 4, 'Largo pero cada página vale la pena.'
FROM libros l
JOIN usuarios u ON u.email = 'carlos@example.com'
WHERE l.isbn = '9780000003005';

INSERT INTO calificaciones (libro_id, usuario_id, valor, comentario)
SELECT l.id, u.id, 5, 'El realismo mágico en su máxima expresión.'
FROM libros l
JOIN usuarios u ON u.email = 'emilia@example.com'
WHERE l.isbn = '9780307474728';

INSERT INTO calificaciones (libro_id, usuario_id, valor, comentario)
SELECT l.id, u.id, 4, 'Una saga familiar fascinante. García Márquez es único.'
FROM libros l
JOIN usuarios u ON u.email = 'ana@example.com'
WHERE l.isbn = '9780307474728';