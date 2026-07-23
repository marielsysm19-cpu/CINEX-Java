package control;

import entidad.UsuarioCINEX;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ControlIniciarSesionCINEX {

    public UsuarioCINEX iniciarSesion(String usuario, String contrasena) {
        if (textoVacio(usuario) || textoVacio(contrasena)) {
            return null;
        }

        try (Connection con = BDCINEX.conectar()) {
            asegurarColumnaCambioContrasena(con);

            String sql = "SELECT id_usuario, nombre, usuario, contrasena, rol, estado, "
                    + "COALESCE(debe_cambiar_contrasena, 0) AS debe_cambiar_contrasena "
                    + "FROM usuarios "
                    + "WHERE LOWER(TRIM(usuario)) = LOWER(TRIM(?)) "
                    + "AND contrasena = ? "
                    + "AND estado = 'Activo' LIMIT 1";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, usuario.trim());
                ps.setString(2, contrasena.trim());

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        UsuarioCINEX u = new UsuarioCINEX();
                        u.setIdUsuario(rs.getInt("id_usuario"));
                        u.setNombre(rs.getString("nombre"));
                        u.setUsuario(rs.getString("usuario"));
                        u.setContrasena(rs.getString("contrasena"));
                        u.setRol(rs.getString("rol"));
                        u.setEstado(rs.getString("estado"));
                        u.setDebeCambiarContrasena(
                                rs.getInt("debe_cambiar_contrasena") == 1
                        );
                        return u;
                    }
                }
            }

        } catch (SQLException e) {
            System.out.println("[LOGIN CINEX] " + e.getMessage());
        }

        return null;
    }

    public String validarLogin(String usuario, String contrasena) {
        UsuarioCINEX u = iniciarSesion(usuario, contrasena);
        return u == null ? null : u.getRol();
    }

    private void asegurarColumnaCambioContrasena(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.executeUpdate(
                    "ALTER TABLE usuarios "
                            + "ADD COLUMN debe_cambiar_contrasena "
                            + "TINYINT(1) NOT NULL DEFAULT 0"
            );
        } catch (SQLException e) {
            // MySQL 1060: columna duplicada. En ese caso ya está lista.
            if (e.getErrorCode() != 1060
                    && !String.valueOf(e.getMessage()).toLowerCase()
                    .contains("duplicate column")) {
                throw e;
            }
        }
    }

    private boolean textoVacio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }
}
