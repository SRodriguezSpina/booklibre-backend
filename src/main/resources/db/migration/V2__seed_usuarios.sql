-- src/main/resources/db/migration/V2__seed_usuarios.sql
-- Seed inicial de usuarios. Los valores de reservados y leidos
-- coinciden con lo que el seed bootstrap genera más adelante:
--   - juan@mail.com: 8 libros propios (libroId 1-4, 21-24), 3 reservas hechas
--   - emilia@example.com: 4 libros propios (libroId 13-16), 1 reserva hecha
--   - ana@example.com: 8 libros propios (libroId 9-12, 17-20), 0 reservas
--   - carlos@example.com: 4 libros propios (libroId 5-8), 1 reserva hecha

INSERT INTO usuarios (
    email, password, nombre, descripcion, celular, ciudad,
    tipo_usuario, bibliokarmas, imagen_url, fecha_registro, activo,
    reservados, leidos
) VALUES
    ('juan@mail.com',     '123', 'Juan Pérez',    'Lector intrépido',     '11556677', 'San Martín', 'LECTOR',            500,  'https://randomuser.me/api/portraits/men/1.jpg',   '2020-09-15', TRUE, 3, 8),
    ('emilia@example.com','123', 'Emilia Romero', 'Lectora Ávida',        '11223344', 'CABA',       'LECTOR_PUBLICADOR', 2345, 'https://randomuser.me/api/portraits/women/2.jpg', '2021-06-01', TRUE, 1, 4),
    ('ana@example.com',   '123', 'Ana García',    'Publicadora',          '11889900', 'Quilmes',    'PUBLICADOR',        1200, 'https://randomuser.me/api/portraits/women/3.jpg', '2022-02-10', TRUE, 0, 8),
    ('carlos@example.com','123', 'Carlos Ruiz',   'Fan de los clásicos',  '11334455', 'Lanús',      'LECTOR',            150,  'https://randomuser.me/api/portraits/men/4.jpg',   '2019-12-30', TRUE, 1, 4),
    ('admin@booklibre.com','admin123', 'Admin',   'Administrador',        NULL,        'CABA',      'ADMIN',             0,    NULL,                                              '2025-01-01', TRUE, 0, 0);
