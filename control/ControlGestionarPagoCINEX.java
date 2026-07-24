package control;

import entidad.ClienteCINEX;
import entidad.EntradaCINEX;
import entidad.ReferenciaFuncionCINEX;
import entidad.PagoCINEX;
import entidad.PrecioCINEX;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalTime;
import java.util.Locale;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ControlGestionarPagoCINEX {

    public static ArrayList<PrecioCINEX> solicitarPreciosActivos() {
        ArrayList<PrecioCINEX> precios = new ArrayList<>();

        try (Connection con = BDCINEX.conectar()) {
            String columnaPrecio = columnaExiste(con, "precios", "precio") ? "precio" : "monto";
            String estadoSelect = columnaExiste(con, "precios", "estado") ? ", estado" : "";
            String sql = "SELECT id_precio, tipo_entrada, " + columnaPrecio + " AS monto" + estadoSelect
                    + " FROM precios ORDER BY id_precio ASC";

            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String estado = estadoSelect.isEmpty() ? "Activo" : rs.getString("estado");
                    precios.add(new PrecioCINEX(
                            rs.getInt("id_precio"),
                            rs.getString("tipo_entrada"),
                            rs.getDouble("monto"),
                            estado == null || estado.trim().isEmpty() ? "Activo" : estado
                    ));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al listar precios de entrada: " + e.getMessage());
        }

        if (precios.isEmpty()) {
            precios.add(new PrecioCINEX(0, "Entrada General", 32.00, "Activo"));
        }
        return precios;
    }

    public static ArrayList<PrecioCINEX> solicitarPreciosParaFuncion(String tipoSalaFuncion) {
        ArrayList<PrecioCINEX> preciosBD = solicitarPreciosActivos();
        ArrayList<PrecioCINEX> preciosFinales = new ArrayList<>();

        if (preciosBD == null || preciosBD.isEmpty()) {
            preciosBD = new ArrayList<>();
            preciosBD.add(new PrecioCINEX(1, "Entrada General", 32.00, "Activo"));
            preciosBD.add(new PrecioCINEX(2, "Entrada Niño", 22.00, "Activo"));
            preciosBD.add(new PrecioCINEX(3, "Adulto Mayor", 24.00, "Activo"));
            preciosBD.add(new PrecioCINEX(4, "Sala 3D", 38.00, "Activo"));
        }

        boolean funcion3D = esFuncion3D(tipoSalaFuncion);
        double montoGeneral = obtenerMontoLista(preciosBD, "Entrada General", 32.00);
        double montoSala3D = obtenerMontoLista(preciosBD, "Sala 3D", montoGeneral);

        for (PrecioCINEX precio : preciosBD) {
            if (precio == null || !precio.estaActivo()) {
                continue;
            }

            String tipo = limpiar(precio.getTipoEntrada());

            // Sala 3D no debe aparecer como entrada seleccionable. Se aplica automáticamente
            // cuando la función elegida pertenece a una sala/formato 3D.
            if (esPrecioSala3D(tipo)) {
                continue;
            }

            double montoFinal = precio.getMonto();

            if (funcion3D) {
                if (esEntradaGeneral(tipo)) {
                    montoFinal = montoSala3D;
                } else {
                    double descuentoRespectoGeneral = Math.max(0, montoGeneral - precio.getMonto());
                    montoFinal = Math.max(0, montoSala3D - descuentoRespectoGeneral);
                }
            }

            preciosFinales.add(new PrecioCINEX(
                    precio.getIdPrecio(),
                    tipo,
                    montoFinal,
                    precio.getEstado()
            ));
        }

        if (preciosFinales.isEmpty()) {
            preciosFinales.add(new PrecioCINEX(1, "Entrada General", funcion3D ? montoSala3D : montoGeneral, "Activo"));
        }

        return preciosFinales;
    }

    public static String obtenerTipoEntradaPrincipal() {
        ArrayList<PrecioCINEX> precios = solicitarPreciosActivos();
        if (precios.isEmpty() || limpiar(precios.get(0).getTipoEntrada()).isEmpty()) {
            return "Entrada General";
        }
        return precios.get(0).getTipoEntrada().trim();
    }

    public static double obtenerMontoPorTipo(String tipoEntrada) {
        return obtenerMontoPorTipo(tipoEntrada, "");
    }

    public static double obtenerMontoPorTipo(String tipoEntrada, String tipoSalaFuncion) {
        String tipoBuscado = limpiar(tipoEntrada);
        ArrayList<PrecioCINEX> precios = solicitarPreciosParaFuncion(tipoSalaFuncion);

        for (PrecioCINEX precio : precios) {
            if (precio != null && precio.getTipoEntrada().equalsIgnoreCase(tipoBuscado)) {
                return precio.getMonto();
            }
        }

        return consultarMontoRegistrado(tipoBuscado);
    }

    public static boolean existeVentaGenerada(String pelicula, String funcion, List<String> asientos, double total) {
        return pelicula != null && !pelicula.trim().isEmpty()
                && funcion != null && !funcion.trim().isEmpty()
                && asientos != null && !asientos.isEmpty()
                && total > 0;
    }

    public static ArrayList<EntradaCINEX> solicitarResumenCompra(
            String pelicula,
            String funcion,
            List<String> asientos,
            double total,
            String metodoPago
    ) {
        return solicitarResumenCompra(pelicula, funcion, asientos, null, "", total, metodoPago);
    }

    public static ArrayList<EntradaCINEX> solicitarResumenCompra(
            String pelicula,
            String funcion,
            List<String> asientos,
            List<String> tiposEntrada,
            double total,
            String metodoPago
    ) {
        return solicitarResumenCompra(pelicula, funcion, asientos, tiposEntrada, "", total, metodoPago);
    }

    public static ArrayList<EntradaCINEX> solicitarResumenCompra(
            String pelicula,
            String funcion,
            List<String> asientos,
            List<String> tiposEntrada,
            String tipoSalaFuncion,
            double total,
            String metodoPago
    ) {
        ArrayList<EntradaCINEX> entradas = new ArrayList<>();

        if (!existeVentaGenerada(pelicula, funcion, asientos, total)) {
            return entradas;
        }

        PagoCINEX pago = registrarMetodoPago(metodoPago, total);

        for (int i = 0; i < asientos.size(); i++) {
            String asiento = asientos.get(i);
            String tipo = obtenerTipoEntrada(tiposEntrada, i);
            EntradaCINEX entrada = new EntradaCINEX();
            entrada.setPelicula(pelicula);
            entrada.setFuncion(funcion);
            entrada.setAsientos(asiento);
            entrada.setTipoEntrada(tipo);
            entrada.setPrecioUnitario(obtenerMontoPorTipo(tipo, tipoSalaFuncion));
            entrada.setCantidadEntradas(1);
            entrada.setEstado("Seleccionada");
            entrada.setPago(pago);
            entradas.add(entrada);
        }

        return entradas;
    }

    public static PagoCINEX registrarMetodoPago(String metodoPago, double total) {
        PagoCINEX pago = new PagoCINEX();
        pago.setMetodoPago(limpiar(metodoPago).isEmpty() ? "Efectivo" : metodoPago);
        pago.setTotal(total);
        pago.setEstado("Pendiente");
        return pago;
    }

    public static String validarPagoEfectivo(double total, String montoTexto) {
        if (montoTexto == null || montoTexto.trim().isEmpty()) {
            return "Ingrese el monto recibido.";
        }

        try {
            double recibido = Double.parseDouble(montoTexto.trim());

            if (recibido < total) {
                double faltante = total - recibido;
                return "Pago insuficiente. Falta completar S/ " + String.format("%.2f", faltante) + ".";
            }

            return "";

        } catch (NumberFormatException e) {
            return "Pago rechazado. Ingrese un monto válido.";
        }
    }

    public static PagoCINEX registrarPago(String metodoPago, double total) {
        PagoCINEX pago = new PagoCINEX();
        pago.setMetodoPago(limpiar(metodoPago).isEmpty() ? "Efectivo" : metodoPago);
        pago.setTotal(total);
        pago.setEstado("Pagado");
        return pago;
    }

    public static boolean confirmarPago(PagoCINEX pago) {
        return pago != null
                && pago.getMetodoPago() != null
                && !pago.getMetodoPago().trim().isEmpty()
                && "Pagado".equalsIgnoreCase(pago.getEstado())
                && pago.getTotal() > 0;
    }

    public static ArrayList<EntradaCINEX> marcarEntradasPagadas(
            String pelicula,
            String funcion,
            List<String> asientos,
            PagoCINEX pago
    ) {
        return marcarEntradasPagadas(pelicula, funcion, asientos, null, "", pago);
    }

    public static ArrayList<EntradaCINEX> marcarEntradasPagadas(
            String pelicula,
            String funcion,
            List<String> asientos,
            List<String> tiposEntrada,
            PagoCINEX pago
    ) {
        return marcarEntradasPagadas(pelicula, funcion, asientos, tiposEntrada, "", pago);
    }

    public static ArrayList<EntradaCINEX> marcarEntradasPagadas(
            String pelicula,
            String funcion,
            List<String> asientos,
            List<String> tiposEntrada,
            String tipoSalaFuncion,
            PagoCINEX pago
    ) {
        ArrayList<EntradaCINEX> entradas = new ArrayList<>();

        if (!confirmarPago(pago) || asientos == null || asientos.isEmpty()) {
            return entradas;
        }

        for (int i = 0; i < asientos.size(); i++) {
            String asiento = asientos.get(i);
            String tipo = obtenerTipoEntrada(tiposEntrada, i);
            EntradaCINEX entrada = new EntradaCINEX();
            entrada.setPelicula(pelicula);
            entrada.setFuncion(funcion);
            entrada.setAsientos(asiento);
            entrada.setTipoEntrada(tipo);
            entrada.setPrecioUnitario(obtenerMontoPorTipo(tipo, tipoSalaFuncion));
            entrada.setCantidadEntradas(1);
            entrada.setEstado("Pagada");
            entrada.setPago(pago);
            entradas.add(entrada);
        }

        return entradas;
    }

    /**
     * Genera el número definitivo antes de guardar la venta.
     * La venta ya no depende de la generación del comprobante.
     */
    public static String generarNumeroVenta() {
        return "VTA-" + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
        );
    }

    /**
     * Registra definitivamente la venta cuando el pago fue aprobado.
     * Guarda venta, pago y entradas; no crea todavía el comprobante.
     */
    public static boolean registrarVentaPagada(
            String numeroVenta,
            String usuarioVendedor,
            String pelicula,
            String funcion,
            List<String> asientos,
            List<String> tiposEntrada,
            PagoCINEX pago,
            List<EntradaCINEX> entradasPagadas
    ) {
        if (!confirmarPago(pago)
                || limpiar(numeroVenta).isEmpty()
                || limpiar(usuarioVendedor).isEmpty()
                || limpiar(pelicula).isEmpty()
                || limpiar(funcion).isEmpty()
                || asientos == null
                || asientos.isEmpty()) {
            return false;
        }

        ArrayList<Double> preciosUnitarios = new ArrayList<>();

        if (entradasPagadas != null) {
            for (EntradaCINEX entrada : entradasPagadas) {
                preciosUnitarios.add(
                        entrada == null
                                ? 0.0
                                : entrada.getPrecioUnitario()
                );
            }
        }

        return registrarVentaPagadaCompleta(
                numeroVenta,
                usuarioVendedor,
                pelicula,
                funcion,
                asientos,
                tiposEntrada,
                preciosUnitarios,
                pago.getTotal(),
                pago.getMetodoPago()
        );
    }

    private static String obtenerTipoEntrada(List<String> tiposEntrada, int indice) {
        if (tiposEntrada == null || indice < 0 || indice >= tiposEntrada.size()) {
            return obtenerTipoEntradaPrincipal();
        }

        String tipo = tiposEntrada.get(indice);
        return limpiar(tipo).isEmpty() ? obtenerTipoEntradaPrincipal() : tipo.trim();
    }

    private static double obtenerMontoLista(ArrayList<PrecioCINEX> precios, String tipoEntrada, double respaldo) {
        if (precios == null) {
            return respaldo;
        }

        for (PrecioCINEX precio : precios) {
            if (precio != null && limpiar(precio.getTipoEntrada()).equalsIgnoreCase(tipoEntrada)) {
                return precio.getMonto();
            }
        }

        return respaldo;
    }

    private static boolean esFuncion3D(String tipoSalaFuncion) {
        return tipoSalaFuncion != null && tipoSalaFuncion.toUpperCase().contains("3D");
    }

    private static boolean esPrecioSala3D(String tipoEntrada) {
        return tipoEntrada != null && tipoEntrada.toUpperCase().contains("SALA 3D");
    }

    private static boolean esEntradaGeneral(String tipoEntrada) {
        return tipoEntrada != null && tipoEntrada.equalsIgnoreCase("Entrada General");
    }

    private static String limpiar(String texto) {
        return texto == null ? "" : texto.trim();
    }

    private static double consultarMontoRegistrado(String tipoEntrada) {
        String tipo = normalizarTipoEntradaInterno(tipoEntrada);
        try (Connection con = BDCINEX.conectar()) {
            return obtenerPrecioUnitarioPorTipo(con, tipo, 32.00);
        } catch (SQLException e) {
            System.out.println("Error al consultar precio: " + e.getMessage());
            return 32.00;
        }
    }

    private static boolean registrarVentaPagadaCompleta(
            String numeroVenta,
            String usuarioVendedor,
            String pelicula,
            String funcion,
            List<String> asientos,
            List<String> tiposEntrada,
            List<Double> preciosUnitarios,
            double total,
            String metodoPago
    ) {
        Connection con = null;

        try {
            con = BDCINEX.conectar();
            con.setAutoCommit(false);

            int idUsuario = obtenerIdUsuario(con, usuarioVendedor);
            int idCliente = obtenerOCrearClienteVenta(con);
            int idFuncion = obtenerIdFuncionPorPeliculaHora(con, pelicula, funcion);

            if (idUsuario <= 0 || idFuncion <= 0) {
                con.rollback();
                return false;
            }

            int idSala = obtenerIdSalaPorFuncion(con, idFuncion);
            int idPrecioGeneral = obtenerIdPrecioGeneral(con);

            if (idSala <= 0 || idPrecioGeneral <= 0) {
                con.rollback();
                return false;
            }

            int idVenta = obtenerIdVentaPorNumero(con, numeroVenta);

            if (idVenta <= 0) {
                String columnaFecha = columnaExiste(con, "ventas", "fecha_venta")
                        ? "fecha_venta" : "fecha_hora";
                String sqlVenta = "INSERT INTO ventas(numero_venta, id_cliente, id_usuario, id_funcion, "
                        + columnaFecha + ", total, qr_entrada, estado) "
                        + "VALUES (?, ?, ?, ?, NOW(), ?, '', 'Registrada')";

                try (PreparedStatement ps = con.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, numeroVenta);
                    ps.setInt(2, idCliente);
                    ps.setInt(3, idUsuario);
                    ps.setInt(4, idFuncion);
                    ps.setDouble(5, total);
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            idVenta = rs.getInt(1);
                        }
                    }
                }
            } else {
                String sqlUpdate = "UPDATE ventas SET id_cliente=?, id_usuario=?, id_funcion=?, total=?, "
                        + "qr_entrada='', estado='Registrada' WHERE id_venta=?";
                try (PreparedStatement ps = con.prepareStatement(sqlUpdate)) {
                    ps.setInt(1, idCliente);
                    ps.setInt(2, idUsuario);
                    ps.setInt(3, idFuncion);
                    ps.setDouble(4, total);
                    ps.setInt(5, idVenta);
                    ps.executeUpdate();
                }
                borrarDetalleVenta(con, idVenta);
            }

            if (idVenta <= 0) {
                con.rollback();
                return false;
            }

            registrarPagoBD(con, idVenta, metodoPago, total, numeroVenta);
            registrarEntradasBD(
                    con, idVenta, idFuncion, idSala, idPrecioGeneral,
                    numeroVenta, asientos, tiposEntrada, preciosUnitarios, total
            );

            con.commit();
            ControlValidarClienteCINEX.limpiarClientePreparadoParaVenta();
            return true;

        } catch (Exception e) {
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException ignored) {
            }
            System.out.println("Error al registrar venta pagada: " + e.getMessage());
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

    private static int obtenerOCrearClienteVenta(Connection con) throws SQLException {
        ClienteCINEX cliente = ControlValidarClienteCINEX.obtenerClientePreparadoParaVenta();
        if (cliente == null
                || limpiar(cliente.getNumeroDocumento()).isEmpty()
                || limpiar(cliente.getNombre()).isEmpty()) {
            return obtenerIdClienteGeneral(con);
        }

        String documento = cliente.getNumeroDocumento().trim();
        String nombre = cliente.getNombre().trim();
        String sqlBuscar = "SELECT id_cliente FROM clientes WHERE dni = ? LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sqlBuscar)) {
            ps.setString(1, documento);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int idCliente = rs.getInt("id_cliente");
                    try (PreparedStatement up = con.prepareStatement(
                            "UPDATE clientes SET nombre = ? WHERE id_cliente = ?")) {
                        up.setString(1, nombre);
                        up.setInt(2, idCliente);
                        up.executeUpdate();
                    }
                    return idCliente;
                }
            }
        }

        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO clientes(nombre, dni) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.setString(2, documento);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return obtenerIdClienteGeneral(con);
    }

    private static int obtenerIdClienteGeneral(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id_cliente FROM clientes WHERE dni = '00000000' LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("id_cliente");
            }
        }

        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO clientes(nombre, dni) VALUES ('Cliente General', '00000000')",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 1;
    }

    private static int obtenerIdUsuario(Connection con, String usuario) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id_usuario FROM usuarios WHERE usuario = ? LIMIT 1")) {
            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id_usuario") : -1;
            }
        }
    }

    private static int obtenerIdFuncionPorPeliculaHora(
            Connection con,
            String pelicula,
            String funcion
    ) throws SQLException {
        int idDirecto = ReferenciaFuncionCINEX.obtenerId(funcion);
        if (idDirecto <= 0) {
            idDirecto = extraerIdFuncion(funcion);
        }

        String titulo = limpiar(pelicula);
        if (idDirecto > 0) {
            String sql = "SELECT f.id_funcion FROM funciones f "
                    + "INNER JOIN peliculas p ON f.id_pelicula=p.id_pelicula "
                    + "WHERE f.id_funcion=? AND f.estado='Activa' "
                    + "AND (?='' OR LOWER(TRIM(p.titulo))=LOWER(TRIM(?))) LIMIT 1";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idDirecto);
                ps.setString(2, titulo);
                ps.setString(3, titulo);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt("id_funcion") : -1;
                }
            }
        }

        int idPelicula = obtenerIdPeliculaPorTitulo(con, titulo);
        if (idPelicula <= 0) {
            return -1;
        }

        String visible = ReferenciaFuncionCINEX.mostrar(funcion);
        String hora24 = normalizarHora(visible);
        String sql = "SELECT id_funcion FROM funciones WHERE id_pelicula=? AND estado='Activa' "
                + "AND fecha>=CURDATE() AND fecha<DATE_ADD(CURDATE(), INTERVAL 2 DAY) "
                + "AND (TIME_FORMAT(hora,'%H:%i')=? OR UPPER(TIME_FORMAT(hora,'%h:%i %p'))=? "
                + "OR UPPER(TIME_FORMAT(hora,'%l:%i %p'))=?) ORDER BY fecha,hora LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPelicula);
            ps.setString(2, hora24);
            ps.setString(3, visible.toUpperCase());
            ps.setString(4, visible.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id_funcion") : -1;
            }
        }
    }

    private static int obtenerIdPeliculaPorTitulo(Connection con, String titulo) throws SQLException {
        if (limpiar(titulo).isEmpty()) {
            return -1;
        }
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id_pelicula FROM peliculas WHERE LOWER(TRIM(titulo))=LOWER(TRIM(?)) LIMIT 1")) {
            ps.setString(1, titulo.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id_pelicula") : -1;
            }
        }
    }

    private static int obtenerIdSalaPorFuncion(Connection con, int idFuncion) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id_sala FROM funciones WHERE id_funcion=? LIMIT 1")) {
            ps.setInt(1, idFuncion);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id_sala") : -1;
            }
        }
    }

    private static int obtenerIdPrecioGeneral(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id_precio FROM precios ORDER BY id_precio LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt("id_precio") : -1;
        }
    }

    private static int obtenerIdPrecioPorTipo(Connection con, String tipoEntrada) throws SQLException {
        String tipo = normalizarTipoEntradaInterno(tipoEntrada);
        if (columnaExiste(con, "precios", "tipo_entrada")) {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT id_precio FROM precios WHERE LOWER(tipo_entrada)=LOWER(?) ORDER BY id_precio LIMIT 1")) {
                ps.setString(1, tipo);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("id_precio");
                    }
                }
            }
        }
        return obtenerIdPrecioGeneral(con);
    }

    private static int obtenerIdAsiento(Connection con, int idSala, String codigo) throws SQLException {
        String limpio = limpiar(codigo).toUpperCase();
        String fila = limpio.replaceAll("[0-9]", "");
        String numeroTexto = limpio.replaceAll("[^0-9]", "");
        if (fila.isEmpty() || numeroTexto.isEmpty()) {
            return -1;
        }
        int numero = Integer.parseInt(numeroTexto);

        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id_asiento FROM asientos WHERE id_sala=? AND fila=? AND numero=? LIMIT 1")) {
            ps.setInt(1, idSala);
            ps.setString(2, fila);
            ps.setInt(3, numero);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_asiento");
                }
            }
        }

        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO asientos(id_sala,fila,numero,estado) VALUES (?,?,?,'Disponible')",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idSala);
            ps.setString(2, fila);
            ps.setInt(3, numero);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    private static int obtenerIdVentaPorNumero(Connection con, String numeroVenta) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id_venta FROM ventas WHERE numero_venta=? LIMIT 1")) {
            ps.setString(1, numeroVenta);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id_venta") : -1;
            }
        }
    }

    private static void borrarDetalleVenta(Connection con, int idVenta) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM entradas WHERE id_venta=?")) {
            ps.setInt(1, idVenta);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM pagos WHERE id_venta=?")) {
            ps.setInt(1, idVenta);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM comprobantes WHERE id_venta=?")) {
            ps.setInt(1, idVenta);
            ps.executeUpdate();
        }
    }

    private static void registrarPagoBD(
            Connection con,
            int idVenta,
            String metodoPago,
            double total,
            String numeroVenta
    ) throws SQLException {
        boolean tieneMetodo = columnaExiste(con, "pagos", "metodo_pago");
        boolean tieneMonto = columnaExiste(con, "pagos", "monto");
        boolean tieneCodigo = columnaExiste(con, "pagos", "codigo_operacion");
        boolean tieneEstadoPago = columnaExiste(con, "pagos", "estado_pago");
        boolean tieneEstado = columnaExiste(con, "pagos", "estado");
        boolean tieneFecha = columnaExiste(con, "pagos", "fecha_pago");

        StringBuilder columnas = new StringBuilder("id_venta");
        StringBuilder valores = new StringBuilder("?");
        ArrayList<Object> params = new ArrayList<>();
        params.add(idVenta);

        if (tieneMetodo) { columnas.append(", metodo_pago"); valores.append(", ?"); params.add(normalizarMetodo(metodoPago)); }
        if (tieneMonto) { columnas.append(", monto"); valores.append(", ?"); params.add(total); }
        if (tieneCodigo) { columnas.append(", codigo_operacion"); valores.append(", ?"); params.add(numeroVenta); }
        if (tieneEstadoPago) { columnas.append(", estado_pago"); valores.append(", ?"); params.add("Pagado"); }
        else if (tieneEstado) { columnas.append(", estado"); valores.append(", ?"); params.add("Aprobado"); }
        if (tieneFecha) { columnas.append(", fecha_pago"); valores.append(", NOW()"); }

        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO pagos(" + columnas + ") VALUES (" + valores + ")")) {
            for (int i = 0; i < params.size(); i++) {
                Object valor = params.get(i);
                if (valor instanceof Integer) ps.setInt(i + 1, (Integer) valor);
                else if (valor instanceof Double) ps.setDouble(i + 1, (Double) valor);
                else ps.setString(i + 1, String.valueOf(valor));
            }
            ps.executeUpdate();
        }
    }

    private static void registrarEntradasBD(
            Connection con,
            int idVenta,
            int idFuncion,
            int idSala,
            int idPrecioGeneral,
            String numeroVenta,
            List<String> asientos,
            List<String> tiposEntrada,
            List<Double> preciosUnitarios,
            double total
    ) throws SQLException {
        if (asientos == null || asientos.isEmpty()) {
            return;
        }

        double unitarioGeneral = total / asientos.size();
        boolean tieneIdFuncion = columnaExiste(con, "entradas", "id_funcion");
        boolean tieneIdPrecio = columnaExiste(con, "entradas", "id_precio");
        boolean tieneCodigo = columnaExiste(con, "entradas", "codigo_entrada");
        boolean tienePrecio = columnaExiste(con, "entradas", "precio_unitario");
        boolean tieneTipo = columnaExiste(con, "entradas", "tipo_entrada");
        boolean tieneEstado = columnaExiste(con, "entradas", "estado");

        StringBuilder columnas = new StringBuilder("id_venta");
        StringBuilder valores = new StringBuilder("?");
        if (tieneIdFuncion) { columnas.append(", id_funcion"); valores.append(", ?"); }
        columnas.append(", id_asiento"); valores.append(", ?");
        if (tieneIdPrecio) { columnas.append(", id_precio"); valores.append(", ?"); }
        if (tieneCodigo) { columnas.append(", codigo_entrada"); valores.append(", ?"); }
        if (tienePrecio) { columnas.append(", precio_unitario"); valores.append(", ?"); }
        if (tieneTipo) { columnas.append(", tipo_entrada"); valores.append(", ?"); }
        if (tieneEstado) { columnas.append(", estado"); valores.append(", ?"); }

        String sqlVerificar = "SELECT COUNT(*) AS total FROM entradas WHERE id_funcion=? AND id_asiento=? "
                + "AND (estado IS NULL OR estado NOT IN ('Anulada','Reembolsada'))";
        String sqlInsert = "INSERT INTO entradas(" + columnas + ") VALUES (" + valores + ")";

        try (PreparedStatement verificar = con.prepareStatement(sqlVerificar);
             PreparedStatement insertar = con.prepareStatement(sqlInsert)) {
            for (int indice = 0; indice < asientos.size(); indice++) {
                String asiento = asientos.get(indice);
                String tipo = obtenerTipoEntrada(tiposEntrada, indice);
                int idPrecio = obtenerIdPrecioPorTipo(con, tipo);
                double respaldo = obtenerPrecioUnitarioPorTipo(con, tipo, unitarioGeneral);
                double precio = respaldo;
                if (preciosUnitarios != null && indice < preciosUnitarios.size()
                        && preciosUnitarios.get(indice) != null && preciosUnitarios.get(indice) > 0) {
                    precio = preciosUnitarios.get(indice);
                }

                int idAsiento = obtenerIdAsiento(con, idSala, asiento);
                if (idAsiento <= 0) {
                    throw new SQLException("No se pudo obtener el asiento " + asiento);
                }

                verificar.setInt(1, idFuncion);
                verificar.setInt(2, idAsiento);
                try (ResultSet rs = verificar.executeQuery()) {
                    if (rs.next() && rs.getInt("total") > 0) {
                        throw new SQLException("El asiento " + asiento + " ya no se encuentra disponible.");
                    }
                }

                int i = 1;
                insertar.setInt(i++, idVenta);
                if (tieneIdFuncion) insertar.setInt(i++, idFuncion);
                insertar.setInt(i++, idAsiento);
                if (tieneIdPrecio) insertar.setInt(i++, idPrecio > 0 ? idPrecio : idPrecioGeneral);
                if (tieneCodigo) insertar.setString(i++, numeroVenta + "-" + asiento);
                if (tienePrecio) insertar.setDouble(i++, precio > 0 ? precio : unitarioGeneral);
                if (tieneTipo) insertar.setString(i++, tipo);
                if (tieneEstado) insertar.setString(i++, "Emitida");
                insertar.addBatch();
            }
            insertar.executeBatch();
        }
    }

    private static double obtenerPrecioUnitarioPorTipo(
            Connection con,
            String tipoEntrada,
            double respaldo
    ) throws SQLException {
        String tipo = normalizarTipoEntradaInterno(tipoEntrada);
        String columnaPrecio = columnaExiste(con, "precios", "precio") ? "precio" : "monto";
        if (columnaExiste(con, "precios", "tipo_entrada")) {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT " + columnaPrecio + " AS precio FROM precios "
                            + "WHERE LOWER(tipo_entrada)=LOWER(?) ORDER BY id_precio LIMIT 1")) {
                ps.setString(1, tipo);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble("precio");
                    }
                }
            }
        }
        return respaldo > 0 ? respaldo : 32.00;
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

    private static String normalizarTipoEntradaInterno(String tipoEntrada) {
        String tipo = limpiar(tipoEntrada);
        if (tipo.isEmpty()) {
            return obtenerTipoEntradaPrincipal();
        }
        if (tipo.equalsIgnoreCase("Adulto")) return "Entrada General";
        if (tipo.equalsIgnoreCase("Niño") || tipo.equalsIgnoreCase("Nino")) return "Entrada Niño";
        return tipo;
    }

    private static String normalizarMetodo(String metodo) {
        String valor = limpiar(metodo);
        if (valor.equalsIgnoreCase("Yape")) return "Yape";
        if (valor.equalsIgnoreCase("Plin")) return "Plin";
        if (valor.equalsIgnoreCase("Efectivo")) return "Efectivo";
        if (valor.equalsIgnoreCase("Tarjeta Débito") || valor.equalsIgnoreCase("Tarjeta Debito")) return "Tarjeta Débito";
        if (valor.equalsIgnoreCase("Tarjeta Crédito") || valor.equalsIgnoreCase("Tarjeta Credito")
                || valor.equalsIgnoreCase("Tarjeta")) return "Tarjeta Crédito";
        return "Efectivo";
    }

    private static int extraerIdFuncion(String texto) {
        if (texto == null) return -1;
        try {
            String limpio = texto.trim();
            if (limpio.matches("^\\d+$")) return Integer.parseInt(limpio);
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(?i)(?:ID\\s*:?\\s*|FUNCION\\s*:?\\s*|FUNCIÓN\\s*:?\\s*)(\\d+)")
                    .matcher(limpio);
            return m.find() ? Integer.parseInt(m.group(1)) : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private static String normalizarHora(String texto) {
        String hora = limpiar(texto).toUpperCase()
                .replace("A. M.", "AM").replace("P. M.", "PM")
                .replace("A.M.", "AM").replace("P.M.", "PM");
        try {
            LocalTime valor = LocalTime.parse(
                    hora,
                    DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
            );
            return valor.format(DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            return hora;
        }
    }

}
