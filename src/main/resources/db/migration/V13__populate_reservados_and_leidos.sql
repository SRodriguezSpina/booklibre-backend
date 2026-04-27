-- V13__populate_reservados_and_leidos.sql
-- Populate the reservados and leidos counts for each user based on existing mock data
UPDATE usuarios u
SET reservados = COALESCE((SELECT COUNT(*) FROM reservas r WHERE r.usuario_id = u.id), 0),
    leidos = COALESCE((SELECT COUNT(*) FROM calificaciones c WHERE c.usuario_id = u.id), 0);
