-- =========================================================
-- CINEX - CORRECCIÓN DEL ESTADO DE ENTRADAS PARA REEMBOLSOS
-- =========================================================
-- Corrige el error:
-- Data truncated for column 'estado' at row 1
--
-- La columna podía estar definida como ENUM y no aceptar
-- el valor Reembolsada.
-- =========================================================

ALTER TABLE entradas
MODIFY COLUMN estado VARCHAR(30) NULL DEFAULT 'Emitida';

UPDATE entradas e
INNER JOIN detalle_reembolsos dr
    ON dr.id_entrada = e.id_entrada
SET e.estado = 'Reembolsada'
WHERE e.estado IS NULL
   OR e.estado <> 'Reembolsada';

-- Comprobación opcional:
SELECT
    e.id_entrada,
    e.id_venta,
    e.id_funcion,
    e.id_asiento,
    e.estado
FROM entradas e
ORDER BY e.id_entrada DESC
LIMIT 30;
