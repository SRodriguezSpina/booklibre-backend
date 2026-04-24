ALTER TABLE usuarios DROP CONSTRAINT uk_usuarios_celular;

CREATE UNIQUE INDEX uk_usuarios_celular
    ON usuarios (celular)
    WHERE celular <> '';
