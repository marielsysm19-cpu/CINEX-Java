package control;

import entidad.ClienteCINEX;
import entidad.ComprobanteCINEX;
import entidad.EntradaCINEX;
import entidad.PagoCINEX;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
            numeroVenta = invocarStringBDCINEX(
                    "obtenerUltimoNumeroVentaPorUsuario",
                    usuario
            );
        }

        if (numeroVenta.isEmpty()) {
            numeroVenta = generarNumeroVentaLocal();
        }

        String sala = invocarStringBDCINEX("obtenerNombreSalaFuncion", pelicula, funcion);
        if (sala.isEmpty()) {
            sala = invocarStringBDCINEX("obtenerNombreSalaPorPeliculaYFuncion", pelicula, funcion);
        }
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

        String tipoSalaFuncion =
                BDCINEX.obtenerTipoSalaFuncion(
                        pelicula,
                        funcion
                );

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
            registrado = BDCINEX.registrarComprobanteVentaExistente(
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
            return BDCINEX.obtenerTipoEntradaPredeterminado();
        }

        String tipo = tiposEntrada.get(indice);
        return valorSeguro(tipo).isEmpty() ? BDCINEX.obtenerTipoEntradaPredeterminado() : tipo.trim();
    }

    private static String generarNumeroVentaLocal() {
        return "VTA-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private static String invocarStringBDCINEX(String metodo, Object... parametros) {
        String[] clases = {"control.BDCINEX", "BDCINEX"};

        for (String nombreClase : clases) {
            try {
                Class<?> clase = Class.forName(nombreClase);

                for (Method m : clase.getDeclaredMethods()) {
                    if (!m.getName().equals(metodo)) {
                        continue;
                    }

                    if (m.getParameterCount() != parametros.length) {
                        continue;
                    }

                    m.setAccessible(true);
                    Object respuesta = m.invoke(null, parametros);

                    if (respuesta != null) {
                        return String.valueOf(respuesta).trim();
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return "";
    }

    private static String valorSeguro(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
