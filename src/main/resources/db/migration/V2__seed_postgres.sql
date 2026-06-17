-- src/main/resources/db/migration/V2__seed_postgres.sql
-- Seed relacional inicial: usuarios + reservas.
--
-- Usuarios (reservados/leidos coinciden con el dataset de libros del V3):
--   - juan@mail.com:      libros propios 1-4 y 21-24, 3 reservas hechas
--   - emilia@example.com: libros propios 13-16, 1 reserva hecha
--   - ana@example.com:    libros propios 9-12 y 17-20, 0 reservas
--   - carlos@example.com: libros propios 5-8, 1 reserva hecha
--   - admin@booklibre.com: ADMIN (es_admin = TRUE)
--
-- Passwords son BCrypt-hash de '123' (lectores/publicadores) y 'admin123' (admin).
-- es_admin solo es TRUE para el usuario ADMIN.

INSERT INTO usuarios (
    email, password, nombre, descripcion, celular, ciudad,
    tipo_usuario, bibliokarmas, imagen_url, fecha_registro, activo,
    reservados, leidos, es_admin
) VALUES
    ('juan@mail.com',     '$2y$10$fk2FmCajJ/lBG65Z0sX3IephxhhafLBIbsQQlAEcVPAPdClsHzfzu', 'Juan Pérez',    'Lector intrépido',     '11556677', 'San Martín', 'LECTOR',            500,  'https://randomuser.me/api/portraits/men/1.jpg',   '2020-09-15', TRUE, 3, 8, FALSE),
    ('emilia@example.com', '$2y$10$fk2FmCajJ/lBG65Z0sX3IephxhhafLBIbsQQlAEcVPAPdClsHzfzu', 'Emilia Romero', 'Lectora Ávida',        '11223344', 'CABA',       'LECTOR_PUBLICADOR', 2345, 'https://randomuser.me/api/portraits/women/2.jpg', '2021-06-01', TRUE, 1, 4, FALSE),
    ('ana@example.com',   '$2y$10$fk2FmCajJ/lBG65Z0sX3IephxhhafLBIbsQQlAEcVPAPdClsHzfzu', 'Ana García',    'Publicadora',          '11889900', 'Quilmes',    'PUBLICADOR',        1200, 'https://randomuser.me/api/portraits/women/3.jpg', '2022-02-10', TRUE, 0, 8, FALSE),
    ('carlos@example.com','$2y$10$fk2FmCajJ/lBG65Z0sX3IephxhhafLBIbsQQlAEcVPAPdClsHzfzu', 'Carlos Ruiz',   'Fan de los clásicos',  '11334455', 'Lanús',      'LECTOR',            150,  'https://randomuser.me/api/portraits/men/4.jpg',   '2019-12-30', TRUE, 1, 4, FALSE),
    ('admin@booklibre.com','$2y$10$SSKHM9xkkFBQaTOWQ7224.XX8D8Y9MeIOfcz8FQuJK2tBNc93937.', 'Admin',   'Administrador',        '00000000000', 'CABA',      'ADMIN',             0,    NULL,                                              '2025-01-01', TRUE, 0, 0, TRUE);

-- Reservas bootstrap. libro_id (1-24) referencia documentos de booklibreMongo.libros
-- según el orden determinista del seed Mongo (V3). bibliokarmas queda en 0; la app
-- lo recalcula en Reserva.confirmar(libro).

-- juan reserva Harry Potter (libroId 9) del 2026-03-01 al 2026-03-10
INSERT INTO reservas (libro_id, usuario_id, fecha_desde, fecha_hasta, bibliokarmas)
VALUES (9, (SELECT id FROM usuarios WHERE email = 'juan@mail.com'),
        TIMESTAMP '2026-03-01 00:00:00', TIMESTAMP '2026-03-10 23:59:59', 0);

-- juan reserva Ficciones (libroId 13) del 2026-03-17 al 2026-04-01
INSERT INTO reservas (libro_id, usuario_id, fecha_desde, fecha_hasta, bibliokarmas)
VALUES (13, (SELECT id FROM usuarios WHERE email = 'juan@mail.com'),
        TIMESTAMP '2026-03-17 00:00:00', TIMESTAMP '2026-04-01 23:59:59', 0);

-- emilia reserva 1984 (libroId 1) del 2026-03-10 al 2026-03-30
INSERT INTO reservas (libro_id, usuario_id, fecha_desde, fecha_hasta, bibliokarmas)
VALUES (1, (SELECT id FROM usuarios WHERE email = 'emilia@example.com'),
        TIMESTAMP '2026-03-10 00:00:00', TIMESTAMP '2026-03-30 23:59:59', 0);

-- carlos reserva Crónica (libroId 11) del 2026-02-10 al 2026-02-28
INSERT INTO reservas (libro_id, usuario_id, fecha_desde, fecha_hasta, bibliokarmas)
VALUES (11, (SELECT id FROM usuarios WHERE email = 'carlos@example.com'),
        TIMESTAMP '2026-02-10 00:00:00', TIMESTAMP '2026-02-28 23:59:59', 0);

-- juan reserva Código Da Vinci (libroId 10) del 2025-12-15 al 2025-12-29
INSERT INTO reservas (libro_id, usuario_id, fecha_desde, fecha_hasta, bibliokarmas)
VALUES (10, (SELECT id FROM usuarios WHERE email = 'juan@mail.com'),
        TIMESTAMP '2025-12-15 00:00:00', TIMESTAMP '2025-12-29 23:59:59', 0);
