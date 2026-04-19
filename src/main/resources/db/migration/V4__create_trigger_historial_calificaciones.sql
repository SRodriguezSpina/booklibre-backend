-- 1. Tabla de auditoría
CREATE TABLE historial_calificaciones_libro (
                                                id SERIAL PRIMARY KEY,
                                                libro_id INT NOT NULL,
                                                valor_anterior INT, -- Puede ser NULL si es la primera calificación
                                                valor_nuevo INT NOT NULL,
                                                fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                usuario_id INT NOT NULL, -- Para saber quién calificó
                                                FOREIGN KEY (libro_id) REFERENCES libros(id)
);

-- 2. Función del Trigger
CREATE OR REPLACE FUNCTION fn_auditar_cambio_calificacion()
    RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        INSERT INTO historial_calificaciones_libro (libro_id, valor_anterior, valor_nuevo, usuario_id)
        VALUES (NEW.libro_id, NULL, NEW.valor, NEW.usuario_id);
    ELSIF (TG_OP = 'UPDATE') THEN
        IF (OLD.valor IS DISTINCT FROM NEW.valor) THEN
            INSERT INTO historial_calificaciones_libro (libro_id, valor_anterior, valor_nuevo, usuario_id)
            VALUES (NEW.libro_id, OLD.valor, NEW.valor, NEW.usuario_id);
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 3. Registro del Trigger en la tabla de calificaciones
CREATE TRIGGER tr_audit_calificacion
    AFTER INSERT OR UPDATE ON calificaciones
    FOR EACH ROW
EXECUTE FUNCTION fn_auditar_cambio_calificacion();