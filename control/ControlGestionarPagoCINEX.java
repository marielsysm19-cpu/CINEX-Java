package control;

import entidad.EntradaCINEX;
import entidad.PagoCINEX;
import entidad.PrecioCINEX;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ControlGestionarPagoCINEX {

    public static ArrayList<PrecioCINEX> solicitarPreciosActivos() {
        return BDCINEX.listarPreciosEntradaActivos();
    }

    public static ArrayList<PrecioCINEX> solicitarPreciosParaFuncion(String tipoSalaFuncion) {
        ArrayList<PrecioCINEX> preciosBD = BDCINEX.listarPreciosEntradaActivos();
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
        return BDCINEX.obtenerTipoEntradaPredeterminado();
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

        return BDCINEX.obtenerPrecioEntradaPorTipo(tipoBuscado);
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

        return BDCINEX.registrarVentaPagadaCompleta(
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
            return BDCINEX.obtenerTipoEntradaPredeterminado();
        }

        String tipo = tiposEntrada.get(indice);
        return limpiar(tipo).isEmpty() ? BDCINEX.obtenerTipoEntradaPredeterminado() : tipo.trim();
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
}
