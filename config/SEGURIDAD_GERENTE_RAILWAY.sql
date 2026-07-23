-- CINEX: soporte para contraseña temporal y cambio obligatorio del gerente.
-- La aplicación intenta crear esta columna automáticamente.

ALTER TABLE usuarios
ADD COLUMN debe_cambiar_contrasena TINYINT(1) NOT NULL DEFAULT 0;

-- No establezca este valor en 1 para todos los gerentes sin necesidad.
-- Se activa automáticamente al crear un gerente o restablecer su contraseña.
