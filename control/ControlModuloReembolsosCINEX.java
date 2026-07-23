package control;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Crea las tablas auxiliares del módulo de cambios, autorizaciones y reembolsos.
 * Se ejecuta automáticamente la primera vez que se usa el módulo.
 */
public final class ControlModuloReembolsosCINEX {

    private static volatile boolean estructuraLista = false;

    private ControlModuloReembolsosCINEX() {
    }

    public static void asegurarEstructura() {
        if (estructuraLista) {
            return;
        }

        synchronized (ControlModuloReembolsosCINEX.class) {
            if (estructuraLista) {
                return;
            }

            try (Connection con = BDCINEX.conectar();
                 Statement st = con.createStatement()) {

                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS notificaciones_cambios ("
                                + "id_notificacion INT AUTO_INCREMENT PRIMARY KEY,"
                                + "tipo_elemento VARCHAR(20) NOT NULL,"
                                + "id_pelicula INT NULL,"
                                + "id_funcion INT NULL,"
                                + "titulo_elemento VARCHAR(255) NOT NULL,"
                                + "descripcion VARCHAR(600) NOT NULL,"
                                + "datos_anteriores TEXT NULL,"
                                + "datos_nuevos TEXT NULL,"
                                + "usuario_admin VARCHAR(100) NOT NULL,"
                                + "fecha_cambio DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                                + "estado_reembolso VARCHAR(30) NOT NULL DEFAULT 'Pendiente',"
                                + "usuario_gerente VARCHAR(100) NULL,"
                                + "fecha_decision DATETIME NULL,"
                                + "INDEX idx_notif_funcion (id_funcion),"
                                + "INDEX idx_notif_pelicula (id_pelicula),"
                                + "INDEX idx_notif_estado (estado_reembolso),"
                                + "INDEX idx_notif_fecha (fecha_cambio)"
                                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
                );

                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS reembolsos ("
                                + "id_reembolso INT AUTO_INCREMENT PRIMARY KEY,"
                                + "id_venta INT NOT NULL,"
                                + "id_cliente INT NOT NULL,"
                                + "id_funcion INT NOT NULL,"
                                + "id_notificacion INT NULL,"
                                + "usuario_taquillero VARCHAR(100) NOT NULL,"
                                + "fecha_reembolso DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                                + "metodo_reembolso VARCHAR(30) NOT NULL DEFAULT 'Efectivo',"
                                + "monto_total DECIMAL(10,2) NOT NULL DEFAULT 0,"
                                + "estado_reembolso VARCHAR(20) NOT NULL,"
                                + "motivo VARCHAR(500) NULL,"
                                + "INDEX idx_reembolso_venta (id_venta),"
                                + "INDEX idx_reembolso_funcion (id_funcion),"
                                + "INDEX idx_reembolso_fecha (fecha_reembolso)"
                                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
                );

                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS detalle_reembolsos ("
                                + "id_detalle INT AUTO_INCREMENT PRIMARY KEY,"
                                + "id_reembolso INT NOT NULL,"
                                + "id_entrada INT NOT NULL,"
                                + "id_asiento INT NULL,"
                                + "asiento VARCHAR(20) NOT NULL,"
                                + "tipo_entrada VARCHAR(100) NULL,"
                                + "precio_original DECIMAL(10,2) NOT NULL DEFAULT 0,"
                                + "monto_reembolsado DECIMAL(10,2) NOT NULL DEFAULT 0,"
                                + "UNIQUE KEY uk_detalle_entrada (id_entrada),"
                                + "INDEX idx_detalle_reembolso (id_reembolso)"
                                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
                );

                /*
                 * La base anterior podía tener entradas.estado como ENUM
                 * sin el valor Reembolsada. Eso producía:
                 * Data truncated for column 'estado'.
                 *
                 * La migración se ejecuta automáticamente una sola vez
                 * por inicio de la aplicación.
                 */
                asegurarEstadoEntradaReembolsable(con);

                estructuraLista = true;

            } catch (SQLException e) {
                throw new RuntimeException(
                        "No se pudo preparar el módulo de reembolsos en Railway.",
                        e
                );
            }
        }
    }

    private static void asegurarEstadoEntradaReembolsable(
            Connection con
    ) throws SQLException {
        String tipoDato = "";
        long longitud = 0;

        String consulta =
                "SELECT DATA_TYPE, "
                        + "IFNULL(CHARACTER_MAXIMUM_LENGTH, 0) "
                        + "AS longitud "
                        + "FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() "
                        + "AND TABLE_NAME = 'entradas' "
                        + "AND COLUMN_NAME = 'estado' "
                        + "LIMIT 1";

        try (PreparedStatement ps =
                     con.prepareStatement(consulta);
             ResultSet rs = ps.executeQuery()) {

            if (!rs.next()) {
                throw new SQLException(
                        "La tabla entradas no contiene "
                                + "la columna estado."
                );
            }

            tipoDato = rs.getString("DATA_TYPE");
            longitud = rs.getLong("longitud");
        }

        boolean esTextoFlexible =
                "varchar".equalsIgnoreCase(tipoDato)
                        && longitud >= 30;

        if (!esTextoFlexible) {
            try (Statement st = con.createStatement()) {
                st.executeUpdate(
                        "ALTER TABLE entradas "
                                + "MODIFY COLUMN estado "
                                + "VARCHAR(30) NULL "
                                + "DEFAULT 'Emitida'"
                );
            }
        }

        /*
         * Sincroniza reembolsos anteriores que ya estén
         * registrados en detalle_reembolsos.
         */
        try (Statement st = con.createStatement()) {
            st.executeUpdate(
                    "UPDATE entradas e "
                            + "INNER JOIN detalle_reembolsos dr "
                            + "ON dr.id_entrada = e.id_entrada "
                            + "SET e.estado = 'Reembolsada' "
                            + "WHERE e.estado IS NULL "
                            + "OR e.estado <> 'Reembolsada'"
            );
        }
    }

}
