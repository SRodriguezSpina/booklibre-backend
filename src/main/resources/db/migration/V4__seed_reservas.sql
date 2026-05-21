-- src/main/resources/db/migration/V4__seed_reservas.sql
-- Las 5 reservas bootstrap referencian los libros por libro_id (1-24)
-- según el orden determinista del V3 seeder Mongo. No hay JOIN a libros
-- porque la tabla no existe en Postgres.
-- bibliokarmas queda en 0; la app lo recalcula en Reserva.confirmar(libro)
-- cuando se interactúa con la reserva.

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
