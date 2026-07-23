package control;

import entidad.UsuarioCINEX;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class ControlGestionarUsuariosCINEX {

    private static final SecureRandom RANDOM = new SecureRandom();

    public ArrayList<UsuarioCINEX> listarUsuarios() {
        ArrayList<UsuarioCINEX> usuarios = new ArrayList<>();
        String sql = "SELECT id_usuario, nombre, usuario, rol, estado "
                + "FROM usuarios ORDER BY id_usuario DESC";

        try (Connection con = BDCINEX.conectar()) {
            asegurarColumnaCambioContrasena(con);

            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    usuarios.add(new UsuarioCINEX(
                            rs.getInt("id_usuario"),
                            rs.getString("nombre"),
                            rs.getString("usuario"),
                            rs.getString("rol"),
                            rs.getString("estado")
                    ));
                }
            }

        } catch (SQLException e) {
            System.out.println("[CINEX] Error al listar usuarios: " + e.getMessage());
        }

        return usuarios;
    }

    public ArrayList<UsuarioCINEX> buscarUsuarios(String filtro) {
        ArrayList<UsuarioCINEX> usuarios = new ArrayList<>();
        String texto = filtro == null ? "" : filtro.trim();

        if (texto.isEmpty()) {
            return listarUsuarios();
        }

        String sql = "SELECT id_usuario, nombre, usuario, rol, estado "
                + "FROM usuarios "
                + "WHERE LOWER(nombre) LIKE LOWER(?) "
                + "OR LOWER(usuario) LIKE LOWER(?) "
                + "OR LOWER(rol) LIKE LOWER(?) "
                + "OR LOWER(estado) LIKE LOWER(?) "
                + "ORDER BY id_usuario DESC";

        try (Connection con = BDCINEX.conectar()) {
            asegurarColumnaCambioContrasena(con);

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                String like = "%" + texto + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
                ps.setString(4, like);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        usuarios.add(new UsuarioCINEX(
                                rs.getInt("id_usuario"),
                                rs.getString("nombre"),
                                rs.getString("usuario"),
                                rs.getString("rol"),
                                rs.getString("estado")
                        ));
                    }
                }
            }

        } catch (SQLException e) {
            System.out.println("[CINEX] Error al buscar usuarios: " + e.getMessage());
        }

        return usuarios;
    }

    public ResultadoUsuario registrarUsuario(UsuarioCINEX usuario) {
        if (usuario == null
                || !usuario.datosObligatoriosCompletos()
                || usuario.getContrasena().trim().isEmpty()) {
            return ResultadoUsuario.DATOS_INCOMPLETOS;
        }

        if (usuarioYaRegistrado(usuario.getUsuario())) {
            return ResultadoUsuario.USUARIO_EXISTE;
        }

        String sql = "INSERT INTO usuarios(" 
                + "nombre, usuario, contrasena, rol, estado, debe_cambiar_contrasena" 
                + ") VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = BDCINEX.conectar()) {
            asegurarColumnaCambioContrasena(con);

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, usuario.getNombre().trim());
                ps.setString(2, usuario.getUsuario().trim());
                ps.setString(3, usuario.getContrasena().trim());
                ps.setString(4, usuario.getRol().trim());
                ps.setString(5, usuario.getEstado().trim());
                ps.setInt(6, esGerente(usuario.getRol()) ? 1 : 0);

                return ps.executeUpdate() > 0
                        ? ResultadoUsuario.REGISTRADO
                        : ResultadoUsuario.ERROR;
            }

        } catch (SQLException e) {
            System.out.println("[CINEX] Error al registrar usuario: " + e.getMessage());
            return ResultadoUsuario.ERROR;
        }
    }

    public ResultadoUsuario actualizarUsuario(UsuarioCINEX usuario) {
        if (usuario == null
                || usuario.getIdUsuario() <= 0
                || !usuario.datosObligatoriosCompletos()) {
            return ResultadoUsuario.DATOS_INCOMPLETOS;
        }

        if (usuarioYaRegistradoEnOtroId(
                usuario.getUsuario(),
                usuario.getIdUsuario()
        )) {
            return ResultadoUsuario.USUARIO_EXISTE;
        }

        try (Connection con = BDCINEX.conectar()) {
            asegurarColumnaCambioContrasena(con);

            String rolAnterior = obtenerRolUsuario(con, usuario.getIdUsuario());
            if (rolAnterior == null) {
                return ResultadoUsuario.NO_ENCONTRADO;
            }

            boolean eraGerente = esGerente(rolAnterior);
            boolean seraGerente = esGerente(usuario.getRol());

            /*
             * La contraseña de un gerente nunca se cambia desde la edición
             * normal del administrador. Para eso existe RESTABLECER CONTRASEÑA.
             */
            boolean cambiarContrasena = !eraGerente
                    && !seraGerente
                    && !usuario.getContrasena().trim().isEmpty();

            StringBuilder sql = new StringBuilder(
                    "UPDATE usuarios SET nombre = ?, usuario = ?, rol = ?, estado = ?"
            );

            if (cambiarContrasena) {
                sql.append(", contrasena = ?");
            }

            if (!eraGerente && seraGerente) {
                sql.append(", debe_cambiar_contrasena = 1");
            } else if (eraGerente && !seraGerente) {
                sql.append(", debe_cambiar_contrasena = 0");
            }

            sql.append(" WHERE id_usuario = ?");

            try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
                int i = 1;
                ps.setString(i++, usuario.getNombre().trim());
                ps.setString(i++, usuario.getUsuario().trim());
                ps.setString(i++, usuario.getRol().trim());
                ps.setString(i++, usuario.getEstado().trim());

                if (cambiarContrasena) {
                    ps.setString(i++, usuario.getContrasena().trim());
                }

                ps.setInt(i, usuario.getIdUsuario());

                return ps.executeUpdate() > 0
                        ? ResultadoUsuario.ACTUALIZADO
                        : ResultadoUsuario.ERROR;
            }

        } catch (SQLException e) {
            System.out.println("[CINEX] Error al actualizar usuario: " + e.getMessage());
            return ResultadoUsuario.ERROR;
        }
    }

    public RestablecimientoContrasena restablecerContrasenaGerente(int idUsuario) {
        if (idUsuario <= 0) {
            return new RestablecimientoContrasena(
                    false,
                    "Seleccione un gerente.",
                    ""
            );
        }

        try (Connection con = BDCINEX.conectar()) {
            asegurarColumnaCambioContrasena(con);

            String rol = obtenerRolUsuario(con, idUsuario);
            if (rol == null) {
                return new RestablecimientoContrasena(
                        false,
                        "El usuario ya no existe.",
                        ""
                );
            }

            if (!esGerente(rol)) {
                return new RestablecimientoContrasena(
                        false,
                        "Solo se puede restablecer la contraseña de un gerente.",
                        ""
                );
            }

            String temporal = generarContrasenaTemporal();
            String sql = "UPDATE usuarios SET contrasena = ?, "
                    + "debe_cambiar_contrasena = 1 "
                    + "WHERE id_usuario = ? AND LOWER(TRIM(rol)) = 'gerente'";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, temporal);
                ps.setInt(2, idUsuario);

                if (ps.executeUpdate() > 0) {
                    return new RestablecimientoContrasena(
                            true,
                            "Contraseña restablecida. El gerente deberá crear una contraseña personal al iniciar sesión.",
                            temporal
                    );
                }
            }

        } catch (SQLException e) {
            System.out.println("[CINEX] Error al restablecer contraseña: " + e.getMessage());
        }

        return new RestablecimientoContrasena(
                false,
                "No se pudo restablecer la contraseña.",
                ""
        );
    }

    public ResultadoCambioContrasena cambiarContrasenaPersonalGerente(
            int idUsuario,
            String nombreUsuario,
            String nuevaContrasena
    ) {
        String validacion = validarContrasenaSegura(
                nombreUsuario,
                nuevaContrasena
        );

        if (!validacion.isEmpty()) {
            return new ResultadoCambioContrasena(false, validacion);
        }

        try (Connection con = BDCINEX.conectar()) {
            asegurarColumnaCambioContrasena(con);

            String sql = "UPDATE usuarios SET contrasena = ?, "
                    + "debe_cambiar_contrasena = 0 "
                    + "WHERE id_usuario = ? "
                    + "AND LOWER(TRIM(rol)) = 'gerente'";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, nuevaContrasena.trim());
                ps.setInt(2, idUsuario);

                if (ps.executeUpdate() > 0) {
                    return new ResultadoCambioContrasena(
                            true,
                            "Contraseña personal registrada correctamente."
                    );
                }
            }

        } catch (SQLException e) {
            System.out.println("[CINEX] Error al cambiar contraseña del gerente: " + e.getMessage());
        }

        return new ResultadoCambioContrasena(
                false,
                "No se pudo guardar la nueva contraseña."
        );
    }

    public String validarContrasenaSegura(
            String nombreUsuario,
            String nuevaContrasena
    ) {
        String clave = nuevaContrasena == null ? "" : nuevaContrasena.trim();
        String usuario = nombreUsuario == null ? "" : nombreUsuario.trim();

        if (clave.length() < 8) {
            return "La contraseña debe tener como mínimo 8 caracteres.";
        }
        if (!clave.matches(".*[A-Z].*")) {
            return "La contraseña debe incluir una letra mayúscula.";
        }
        if (!clave.matches(".*[a-z].*")) {
            return "La contraseña debe incluir una letra minúscula.";
        }
        if (!clave.matches(".*\\d.*")) {
            return "La contraseña debe incluir un número.";
        }
        if (!clave.matches(".*[^A-Za-z0-9].*")) {
            return "La contraseña debe incluir un carácter especial.";
        }
        if (!usuario.isEmpty() && clave.equalsIgnoreCase(usuario)) {
            return "La contraseña no puede ser igual al usuario.";
        }

        String simple = clave.toLowerCase();
        String[] comunes = {
                "password", "contraseña", "contrasena", "gerente123",
                "admin123", "cinex123", "12345678", "qwerty123"
        };

        for (String comun : comunes) {
            if (simple.equals(comun)) {
                return "Elija una contraseña personal que no sea básica ni predecible.";
            }
        }

        return "";
    }

    public ResultadoUsuario cambiarEstadoUsuario(int idUsuario, String nuevoEstado) {
        if (idUsuario <= 0
                || nuevoEstado == null
                || nuevoEstado.trim().isEmpty()) {
            return ResultadoUsuario.DATOS_INCOMPLETOS;
        }

        String sql = "UPDATE usuarios SET estado = ? WHERE id_usuario = ?";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nuevoEstado.trim());
            ps.setInt(2, idUsuario);

            return ps.executeUpdate() > 0
                    ? ResultadoUsuario.ESTADO_CAMBIADO
                    : ResultadoUsuario.ERROR;

        } catch (SQLException e) {
            System.out.println("[CINEX] Error al cambiar estado de usuario: " + e.getMessage());
            return ResultadoUsuario.ERROR;
        }
    }

    public ResultadoUsuario eliminarUsuarioSeguro(int idUsuario) {
        if (idUsuario <= 0) {
            return ResultadoUsuario.DATOS_INCOMPLETOS;
        }

        try (Connection con = BDCINEX.conectar()) {
            if (!existeUsuario(con, idUsuario)) {
                return ResultadoUsuario.NO_ENCONTRADO;
            }

            if (usuarioTieneVentas(con, idUsuario)) {
                String sqlInactivar = "UPDATE usuarios SET estado = 'Inactivo' WHERE id_usuario = ?";
                try (PreparedStatement ps = con.prepareStatement(sqlInactivar)) {
                    ps.setInt(1, idUsuario);
                    return ps.executeUpdate() > 0
                            ? ResultadoUsuario.USUARIO_CON_VENTAS_INACTIVADO
                            : ResultadoUsuario.ERROR;
                }
            }

            String sqlEliminar = "DELETE FROM usuarios WHERE id_usuario = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlEliminar)) {
                ps.setInt(1, idUsuario);
                return ps.executeUpdate() > 0
                        ? ResultadoUsuario.ELIMINADO
                        : ResultadoUsuario.ERROR;
            }

        } catch (SQLException e) {
            System.out.println("[CINEX] Error al eliminar usuario: " + e.getMessage());
            return ResultadoUsuario.ERROR;
        }
    }

    public boolean usuarioYaRegistrado(String usuario) {
        String sql = "SELECT COUNT(*) AS total FROM usuarios "
                + "WHERE LOWER(TRIM(usuario)) = LOWER(TRIM(?))";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, usuario == null ? "" : usuario.trim());

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("total") > 0;
            }

        } catch (SQLException e) {
            System.out.println("[CINEX] Error al verificar usuario: " + e.getMessage());
            return false;
        }
    }

    private boolean usuarioYaRegistradoEnOtroId(String usuario, int idUsuario) {
        String sql = "SELECT COUNT(*) AS total FROM usuarios "
                + "WHERE LOWER(TRIM(usuario)) = LOWER(TRIM(?)) "
                + "AND id_usuario <> ?";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, usuario == null ? "" : usuario.trim());
            ps.setInt(2, idUsuario);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("total") > 0;
            }

        } catch (SQLException e) {
            System.out.println("[CINEX] Error al verificar usuario existente: " + e.getMessage());
            return false;
        }
    }

    private String obtenerRolUsuario(Connection con, int idUsuario) throws SQLException {
        String sql = "SELECT rol FROM usuarios WHERE id_usuario = ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("rol") : null;
            }
        }
    }

    private boolean existeUsuario(Connection con, int idUsuario) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM usuarios WHERE id_usuario = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("total") > 0;
            }
        }
    }

    private boolean usuarioTieneVentas(Connection con, int idUsuario) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM ventas WHERE id_usuario = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("total") > 0;
            }
        }
    }

    private void asegurarColumnaCambioContrasena(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.executeUpdate(
                    "ALTER TABLE usuarios "
                            + "ADD COLUMN debe_cambiar_contrasena "
                            + "TINYINT(1) NOT NULL DEFAULT 0"
            );
        } catch (SQLException e) {
            if (e.getErrorCode() != 1060
                    && !String.valueOf(e.getMessage()).toLowerCase()
                    .contains("duplicate column")) {
                throw e;
            }
        }
    }

    private String generarContrasenaTemporal() {
        int numero = 1000 + RANDOM.nextInt(9000);
        return "Cinex@" + numero;
    }

    private boolean esGerente(String rol) {
        return rol != null && "gerente".equalsIgnoreCase(rol.trim());
    }

    public enum ResultadoUsuario {
        REGISTRADO,
        ACTUALIZADO,
        ESTADO_CAMBIADO,
        ELIMINADO,
        USUARIO_CON_VENTAS_INACTIVADO,
        NO_ENCONTRADO,
        USUARIO_EXISTE,
        DATOS_INCOMPLETOS,
        ERROR
    }

    public static class RestablecimientoContrasena {
        private final boolean exitoso;
        private final String mensaje;
        private final String contrasenaTemporal;

        public RestablecimientoContrasena(
                boolean exitoso,
                String mensaje,
                String contrasenaTemporal
        ) {
            this.exitoso = exitoso;
            this.mensaje = mensaje == null ? "" : mensaje;
            this.contrasenaTemporal = contrasenaTemporal == null
                    ? ""
                    : contrasenaTemporal;
        }

        public boolean fueExitoso() {
            return exitoso;
        }

        public String getMensaje() {
            return mensaje;
        }

        public String getContrasenaTemporal() {
            return contrasenaTemporal;
        }
    }

    public static class ResultadoCambioContrasena {
        private final boolean exitoso;
        private final String mensaje;

        public ResultadoCambioContrasena(boolean exitoso, String mensaje) {
            this.exitoso = exitoso;
            this.mensaje = mensaje == null ? "" : mensaje;
        }

        public boolean fueExitoso() {
            return exitoso;
        }

        public String getMensaje() {
            return mensaje;
        }
    }
}
