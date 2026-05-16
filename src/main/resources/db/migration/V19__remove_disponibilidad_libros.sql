-- V18: Eliminar columna disponibilidad de libros en PostgreSQL
-- La disponibilidad ya no se persiste como campo: se deriva consultando
-- si existe una reserva activa que se solape con el período pedido.
ALTER TABLE libros DROP COLUMN IF EXISTS disponibilidad;
