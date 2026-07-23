-- =============================================================
-- CINEX - Módulo de notificaciones y reembolsos
-- La aplicación crea estas tablas automáticamente.
-- Este archivo se incluye como respaldo para Railway/MySQL.
-- =============================================================

CREATE TABLE IF NOT EXISTS notificaciones_cambios (
    id_notificacion INT AUTO_INCREMENT PRIMARY KEY,
    tipo_elemento VARCHAR(20) NOT NULL,
    id_pelicula INT NULL,
    id_funcion INT NULL,
    titulo_elemento VARCHAR(255) NOT NULL,
    descripcion VARCHAR(600) NOT NULL,
    datos_anteriores TEXT NULL,
    datos_nuevos TEXT NULL,
    usuario_admin VARCHAR(100) NOT NULL,
    fecha_cambio DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado_reembolso VARCHAR(30) NOT NULL DEFAULT 'Pendiente',
    usuario_gerente VARCHAR(100) NULL,
    fecha_decision DATETIME NULL,
    INDEX idx_notif_funcion (id_funcion),
    INDEX idx_notif_pelicula (id_pelicula),
    INDEX idx_notif_estado (estado_reembolso),
    INDEX idx_notif_fecha (fecha_cambio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS reembolsos (
    id_reembolso INT AUTO_INCREMENT PRIMARY KEY,
    id_venta INT NOT NULL,
    id_cliente INT NOT NULL,
    id_funcion INT NOT NULL,
    id_notificacion INT NULL,
    usuario_taquillero VARCHAR(100) NOT NULL,
    fecha_reembolso DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metodo_reembolso VARCHAR(30) NOT NULL DEFAULT 'Efectivo',
    monto_total DECIMAL(10,2) NOT NULL DEFAULT 0,
    estado_reembolso VARCHAR(20) NOT NULL,
    motivo VARCHAR(500) NULL,
    INDEX idx_reembolso_venta (id_venta),
    INDEX idx_reembolso_funcion (id_funcion),
    INDEX idx_reembolso_fecha (fecha_reembolso)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS detalle_reembolsos (
    id_detalle INT AUTO_INCREMENT PRIMARY KEY,
    id_reembolso INT NOT NULL,
    id_entrada INT NOT NULL,
    id_asiento INT NULL,
    asiento VARCHAR(20) NOT NULL,
    tipo_entrada VARCHAR(100) NULL,
    precio_original DECIMAL(10,2) NOT NULL DEFAULT 0,
    monto_reembolsado DECIMAL(10,2) NOT NULL DEFAULT 0,
    UNIQUE KEY uk_detalle_entrada (id_entrada),
    INDEX idx_detalle_reembolso (id_reembolso)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


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
