INSERT INTO usuarios (email, password, nombre, descripcion, celular, ciudad, tipo_usuario, bibliokarmas, imagen_url, fecha_registro)
VALUES (
    'admin@booklibre.com',
    '$2b$10$59BLQYgjAwSv.HgXQxD6mepQO7q.tSjWhbst0DRF1thdGGCuLCRbm',
    'Admin',
    '',
    '00000000000',
    'CABA',
    'ADMIN',
    0,
    NULL,
    CURRENT_DATE
)
ON CONFLICT (email) DO NOTHING;
