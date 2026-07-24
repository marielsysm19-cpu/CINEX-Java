package control;

import entidad.ClienteCINEX;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class ControlValidarClienteCINEX {

    private static ClienteCINEX clienteVentaActual;

    public static String validarInformacion(String tipoDocumento, String numeroDocumento, String nombre) {
        String validacionDocumento = validarDatosParaConsulta(tipoDocumento, numeroDocumento);
        if (!validacionDocumento.isEmpty()) {
            return validacionDocumento;
        }

        if (nombre == null || nombre.trim().isEmpty()) {
            return "Complete la información obligatoria";
        }

        if (nombre.trim().length() < 3) {
            return "Complete la información obligatoria";
        }

        if (!nombre.trim().matches("^[A-Za-zÁÉÍÓÚáéíóúÑñÜü ]+$")) {
            return "Complete la información obligatoria";
        }

        return "";
    }

    public static String validarDatosParaConsulta(String tipoDocumento, String numeroDocumento) {
        String tipo = normalizarTipoDocumento(tipoDocumento);
        String numero = numeroDocumento == null ? "" : numeroDocumento.trim();

        if (numero.isEmpty()) {
            return "Complete la información obligatoria";
        }

        if (!numero.matches("\\d+")) {
            return "Complete la información obligatoria";
        }

        int longitud = obtenerLongitudDocumento(tipo);
        if (numero.length() != longitud) {
            return "Complete la información obligatoria";
        }

        return "";
    }

    /**
     * Busca un cliente utilizando la entidad de dominio y mantiene la BD encapsulada en control.
     */
    public static ClienteCINEX buscarClientePorDocumento(String tipoDocumento, String numeroDocumento) throws Exception {
        String validacion = validarDatosParaConsulta(tipoDocumento, numeroDocumento);
        if (!validacion.isEmpty()) {
            return null;
        }
        ClienteCINEX cliente = verificarCliente(numeroDocumento);
        if (cliente != null) {
            cliente.setTipoDocumento(normalizarTipoDocumento(tipoDocumento));
        }
        return cliente;
    }

    /**
     * Conserva la entidad cliente para la venta actual dentro del control.
     * BDCINEX no mantiene estado de negocio.
     */
    public static void prepararClienteParaVenta(ClienteCINEX cliente) {
        if (cliente == null) {
            clienteVentaActual = null;
            return;
        }

        ClienteCINEX copia = new ClienteCINEX();
        copia.setIdCliente(cliente.getIdCliente());
        copia.setTipoDocumento(cliente.getTipoDocumento());
        copia.setNumeroDocumento(cliente.getNumeroDocumento());
        copia.setNombre(cliente.getNombre());
        clienteVentaActual = copia;
    }

    public static ClienteCINEX obtenerClientePreparadoParaVenta() {
        return clienteVentaActual;
    }

    public static void limpiarClientePreparadoParaVenta() {
        clienteVentaActual = null;
    }

    public static ClienteCINEX verificarCliente(String numeroDocumento) throws Exception {
        String sql = "SELECT id_cliente, nombre, dni FROM clientes WHERE dni = ? LIMIT 1";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, limpiar(numeroDocumento));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ClienteCINEX cliente = new ClienteCINEX();
                    cliente.setIdCliente(rs.getInt("id_cliente"));
                    cliente.setNumeroDocumento(rs.getString("dni"));
                    cliente.setNombre(rs.getString("nombre"));
                    cliente.setTipoDocumento(obtenerTipoDesdeDocumento(rs.getString("dni")));
                    return cliente;
                }
            }
        }

        return null;
    }

    public static int registrarCliente(ClienteCINEX cliente) throws Exception {
        if (cliente == null) {
            return 0;
        }

        ClienteCINEX existente = verificarCliente(cliente.getNumeroDocumento());
        if (existente != null) {
            return -1;
        }

        String sql = "INSERT INTO clientes(nombre, dni) VALUES (?, ?)";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, limpiar(cliente.getNombre()));
            ps.setString(2, limpiar(cliente.getNumeroDocumento()));

            int filas = ps.executeUpdate();
            if (filas <= 0) {
                return 0;
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            return 1;
        }
    }

    public static int obtenerLongitudDocumento(String tipoDocumento) {
        String tipo = normalizarTipoDocumento(tipoDocumento);
        return "C.E.".equalsIgnoreCase(tipo) ? 9 : 8;
    }

    public static String normalizarTipoDocumento(String tipoDocumento) {
        if (tipoDocumento == null) {
            return "DNI";
        }

        String tipo = tipoDocumento.trim().toUpperCase();
        if (tipo.equals("C.E.") || tipo.equals("CE") || tipo.equals("CARNET EXTRANJERIA") || tipo.equals("CARNÉ DE EXTRANJERÍA")) {
            return "C.E.";
        }
        return "DNI";
    }

    public static String normalizarTipoDocumentoAPI(String tipoDocumento) {
        return "C.E.".equalsIgnoreCase(normalizarTipoDocumento(tipoDocumento)) ? "CE" : "DNI";
    }

    private static String obtenerTipoDesdeDocumento(String numeroDocumento) {
        if (numeroDocumento == null) {
            return "DNI";
        }
        return numeroDocumento.trim().length() == 9 ? "C.E." : "DNI";
    }

    private static String limpiar(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
