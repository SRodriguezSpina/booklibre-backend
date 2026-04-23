CREATE OR REPLACE FUNCTION fn_usuarios_con_mas_de_n_reservas(p_min_reservas INT)
RETURNS TABLE (
    usuario_id INT,
    usuario_nombre TEXT,
    cantidad_reservas BIGINT
)
LANGUAGE sql
AS $$
    SELECT
        u.id,
        u.nombre::TEXT,
        COUNT(r.id)
    FROM usuarios u
    JOIN reservas r ON r.usuario_id = u.id
    GROUP BY u.id, u.nombre
    HAVING COUNT(r.id) > p_min_reservas
    ORDER BY COUNT(r.id) DESC, u.nombre ASC;
$$;


UPDATE usuarios
SET password = '$2a$10$sutnW21UygvrIfeDZvdVWObvSj1lmCQGEyRTe7r9frVEfn.wtLD4y'
WHERE email IN (
    'juan@mail.com',
    'emilia@example.com',
    'ana@example.com',
    'carlos@example.com'
)
AND password = '123';
