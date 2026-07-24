package control;

import entidad.ClienteCINEX;
import entidad.ComprobanteCINEX;
import entidad.EntradaCINEX;
import entidad.FuncionCINEX;
import entidad.SalaCINEX;
import entidad.PagoCINEX;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ControlEmitirComprobanteCINEX {

    public static ComprobanteCINEX recuperarInformacionVenta(
            String usuario,
            String pelicula,
            String funcion,
            List<String> asientos,
            double totalPagado,
            String metodoPago
    ) {
        return recuperarInformacionVenta(usuario, pelicula, funcion, asientos, null, totalPagado, metodoPago);
    }

    public static ComprobanteCINEX recuperarInformacionVenta(
            String usuario,
            String pelicula,
            String funcion,
            List<String> asientos,
            List<String> tiposEntrada,
            double totalPagado,
            String metodoPago
    ) {
        return recuperarInformacionVenta(
                "",
                usuario,
                pelicula,
                funcion,
                asientos,
                tiposEntrada,
                totalPagado,
                metodoPago
        );
    }

    public static ComprobanteCINEX recuperarInformacionVenta(
            String numeroVentaRegistrada,
            String usuario,
            String pelicula,
            String funcion,
            List<String> asientos,
            List<String> tiposEntrada,
            double totalPagado,
            String metodoPago
    ) {
        ComprobanteCINEX comprobante = new ComprobanteCINEX();

        String numeroVenta = valorSeguro(numeroVentaRegistrada);

        if (numeroVenta.isEmpty()) {
            numeroVenta = obtenerUltimoNumeroVentaPorUsuario(usuario);
        }

        if (numeroVenta.isEmpty()) {
            numeroVenta = generarNumeroVentaLocal();
        }

        FuncionCINEX funcionEntidad =
                ControlVerificarDisponibilidadCINEX.consultarFuncionSeleccionada(pelicula, funcion);
        SalaCINEX salaEntidad =
                ControlConsultarPlanoAsientosCINEX.obtenerSalaAsociada(funcionEntidad);
        String sala = salaEntidad == null ? "Sala" : valorSeguro(salaEntidad.getNombre());
        if (sala.isEmpty()) {
            sala = "Sala";
        }

        PagoCINEX pago = consultarMonto(totalPagado, metodoPago);
        ClienteCINEX cliente = consultarDatosCliente(usuario);
        ArrayList<EntradaCINEX> entradas = consultarEntradasAsociadas(numeroVenta, pelicula, funcion, sala, asientos, tiposEntrada, pago);

        comprobante.setNumeroVenta(numeroVenta);
        comprobante.setNumeroComprobante("COMP-" + numeroVenta);
        comprobante.setFechaEmision(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        comprobante.setUsuario(valorSeguro(usuario));
        comprobante.setPelicula(valorSeguro(pelicula));
        comprobante.setFuncion(valorSeguro(funcion));
        comprobante.setSala(sala);
        comprobante.setCliente(cliente);
        comprobante.setPago(pago);
        comprobante.setEntradas(entradas);
        comprobante.setQrGenerado(false);
        comprobante.setRegistrado(false);

        return comprobante;
        }

    public static ArrayList<EntradaCINEX> consultarEntradasAsociadas(
            String numeroVenta,
            String pelicula,
            String funcion,
            String sala,
            List<String> asientos,
            PagoCINEX pago
    ) {
        return consultarEntradasAsociadas(numeroVenta, pelicula, funcion, sala, asientos, null, pago);
    }

    public static ArrayList<EntradaCINEX> consultarEntradasAsociadas(
            String numeroVenta,
            String pelicula,
            String funcion,
            String sala,
            List<String> asientos,
            List<String> tiposEntrada,
            PagoCINEX pago
    ) {
        ArrayList<EntradaCINEX> entradas = new ArrayList<>();

        if (asientos == null) {
            return entradas;
        }

        FuncionCINEX funcionEntidad =
                ControlVerificarDisponibilidadCINEX.consultarFuncionSeleccionada(pelicula, funcion);
        SalaCINEX salaEntidad =
                ControlConsultarPlanoAsientosCINEX.obtenerSalaAsociada(funcionEntidad);
        String tipoSalaFuncion = salaEntidad == null ? "" : valorSeguro(salaEntidad.getTipo());

        for (int i = 0; i < asientos.size(); i++) {
            String asiento = asientos.get(i);
            String codigo = valorSeguro(asiento);
            if (codigo.isEmpty()) {
                continue;
            }

            EntradaCINEX entrada = new EntradaCINEX();
            entrada.setNumeroVenta(numeroVenta);
            entrada.setFechaCompra(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            entrada.setFechaCompraCorta(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM")));
            entrada.setPelicula(pelicula);
            entrada.setFuncion(funcion);
            entrada.setSala(sala);
            String tipo = obtenerTipoEntrada(tiposEntrada, i);
            entrada.setAsientos(codigo);
            entrada.setTipoEntrada(tipo);
            entrada.setPrecioUnitario(
                    ControlGestionarPagoCINEX.obtenerMontoPorTipo(
                            tipo,
                            tipoSalaFuncion
                    )
            );
            entrada.setCantidadEntradas(1);
            entrada.setEstado("Pagada");
            entrada.setPago(pago);
            entradas.add(entrada);
        }

        return entradas;
    }

    public static ClienteCINEX consultarDatosCliente(String usuario) {
        ClienteCINEX cliente = new ClienteCINEX();
        cliente.setNombre(valorSeguro(usuario).isEmpty() ? "Cliente CINEX" : valorSeguro(usuario));
        cliente.setTipoDocumento("DNI");
        cliente.setNumeroDocumento("");
        return cliente;
    }

    public static PagoCINEX consultarMonto(double totalPagado, String metodoPago) {
        PagoCINEX pago = new PagoCINEX();
        pago.setMetodoPago(valorSeguro(metodoPago));
        pago.setTotal(Math.max(0, totalPagado));
        pago.setEstado(totalPagado > 0 ? "Pagado" : "Pendiente");
        return pago;
    }

    public static boolean ventaEstaPagada(ComprobanteCINEX comprobante) {
        if (comprobante == null) {
            return false;
        }

        PagoCINEX pago = comprobante.getPago();

        return comprobante.getPelicula() != null && !comprobante.getPelicula().trim().isEmpty()
                && comprobante.getFuncion() != null && !comprobante.getFuncion().trim().isEmpty()
                && comprobante.getEntradas() != null && !comprobante.getEntradas().isEmpty()
                && pago != null
                && pago.getTotal() > 0
                && pago.getMetodoPago() != null && !pago.getMetodoPago().trim().isEmpty()
                && pago.estaPagado();
    }

    public static boolean registrarComprobante(ComprobanteCINEX comprobante, String rutaQR) {
        if (!ventaEstaPagada(comprobante)) {
            return false;
        }

        comprobante.setRutaQR(rutaQR);
        comprobante.setQrGenerado(true);

        boolean registrado = false;

        try {
            registrado = registrarComprobanteVentaExistente(
                    comprobante.getNumeroVenta(),
                    rutaQR,
                    comprobante.getMontoPagado()
            );
        } catch (Exception e) {
            System.out.println("ControlEmitirComprobanteCINEX: comprobante pendiente -> " + e.getMessage());
            registrado = false;
        }

        comprobante.setRegistrado(registrado);
        return registrado;
    }

    public static boolean confirmarRegistroComprobante(ComprobanteCINEX comprobante) {
        return comprobante != null && comprobante.isQrGenerado();
    }

    private static String obtenerTipoEntrada(List<String> tiposEntrada, int indice) {
        if (tiposEntrada == null || indice < 0 || indice >= tiposEntrada.size()) {
            return ControlGestionarPagoCINEX.obtenerTipoEntradaPrincipal();
        }

        String tipo = tiposEntrada.get(indice);
        return valorSeguro(tipo).isEmpty() ? ControlGestionarPagoCINEX.obtenerTipoEntradaPrincipal() : tipo.trim();
    }

    private static String generarNumeroVentaLocal() {
        return "VTA-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private static String obtenerUltimoNumeroVentaPorUsuario(String usuario) {
        String sql = "SELECT v.numero_venta FROM ventas v "
                + "INNER JOIN usuarios u ON v.id_usuario = u.id_usuario "
                + "WHERE u.usuario = ? ORDER BY v.id_venta DESC LIMIT 1";

        try (Connection con = BDCINEX.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, valorSeguro(usuario));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? valorSeguro(rs.getString("numero_venta")) : "";
            }
        } catch (SQLException e) {
            System.out.println("Error al recuperar última venta: " + e.getMessage());
            return "";
        }
    }

    private static boolean registrarComprobanteVentaExistente(
            String numeroVenta,
            String codigoQR,
            double total
    ) {
        if (valorSeguro(numeroVenta).isEmpty()) {
            return false;
        }

        Connection con = null;
        try {
            con = BDCINEX.conectar();
            con.setAutoCommit(false);

            int idVenta = obtenerIdVentaPorNumero(con, numeroVenta.trim());
            if (idVenta <= 0) {
                con.rollback();
                return false;
            }

            if (columnaExiste(con, "ventas", "qr_entrada")) {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE ventas SET qr_entrada = ? WHERE id_venta = ?")) {
                    ps.setString(1, valorSeguro(codigoQR));
                    ps.setInt(2, idVenta);
                    ps.executeUpdate();
                }
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM comprobantes WHERE id_venta = ?")) {
                ps.setInt(1, idVenta);
                ps.executeUpdate();
            }

            insertarComprobante(con, idVenta, valorSeguro(codigoQR), total, numeroVenta.trim());
            con.commit();
            return true;
        } catch (Exception e) {
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException ignored) {
            }
            System.out.println("Error al registrar comprobante: " + e.getMessage());
            return false;
        } finally {
            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException ignored) {
            }
        }
    }

    private static int obtenerIdVentaPorNumero(Connection con, String numeroVenta) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id_venta FROM ventas WHERE numero_venta = ? LIMIT 1")) {
            ps.setString(1, numeroVenta);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id_venta") : -1;
            }
        }
    }

    private static void insertarComprobante(
            Connection con,
            int idVenta,
            String codigoQR,
            double total,
            String numeroVenta
    ) throws SQLException {
        boolean tieneCodigoQR = columnaExiste(con, "comprobantes", "codigo_qr");
        boolean tieneTotal = columnaExiste(con, "comprobantes", "total");
        boolean tieneTipo = columnaExiste(con, "comprobantes", "tipo_comprobante");
        boolean tieneNumero = columnaExiste(con, "comprobantes", "numero_comprobante");
        boolean tieneFecha = columnaExiste(con, "comprobantes", "fecha_emision");

        if (tieneCodigoQR && tieneTotal) {
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO comprobantes(id_venta, codigo_qr, total) VALUES (?, ?, ?)")) {
                ps.setInt(1, idVenta);
                ps.setString(2, codigoQR);
                ps.setDouble(3, total);
                ps.executeUpdate();
            }
            return;
        }

        if (tieneTipo && tieneNumero && tieneFecha) {
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO comprobantes(id_venta, tipo_comprobante, numero_comprobante, fecha_emision) "
                            + "VALUES (?, 'Boleta', ?, NOW())")) {
                ps.setInt(1, idVenta);
                ps.setString(2, numeroVenta);
                ps.executeUpdate();
            }
            return;
        }

        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO comprobantes(id_venta) VALUES (?)")) {
            ps.setInt(1, idVenta);
            ps.executeUpdate();
        }
    }

    private static boolean columnaExiste(Connection con, String tabla, String columna) {
        try {
            DatabaseMetaData meta = con.getMetaData();
            String catalogo = con.getCatalog();
            try (ResultSet rs = meta.getColumns(catalogo, null, tabla, columna)) {
                if (rs.next()) return true;
            }
            try (ResultSet rs = meta.getColumns(catalogo, null, tabla.toLowerCase(), columna)) {
                if (rs.next()) return true;
            }
            try (ResultSet rs = meta.getColumns(catalogo, null, tabla.toUpperCase(), columna)) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private static String valorSeguro(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
