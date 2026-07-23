package control;
import entidad.PrecioCINEX;
import java.sql.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class BDCINEX {

    private static final String URL =
            "jdbc:mysql://tokaido.proxy.rlwy.net:25706/railway"
                    + "?useSSL=false"
                    + "&allowPublicKeyRetrieval=true"
                    + "&serverTimezone=America/Lima"
                    + "&useUnicode=true"
                    + "&characterEncoding=UTF-8";

    private static final String USUARIO = "root";

    private static final String CLAVE =
            "EFTxbLnITmSGuFRadKWWiCAyPoaSrmJU";

    
    // ======================================================
    // CONEXIÓN GLOBAL OPTIMIZADA PARA RAILWAY
    // Este pool evita abrir/cerrar una conexión física en cada consulta.
    // Las interfaces siguen usando BDCINEX.conectar() igual que antes.
    // ======================================================

    private static final int MAX_POOL_SIZE = 6;
    private static final int WAIT_CONNECTION_SECONDS = 8;
    private static final ArrayBlockingQueue<Connection> POOL_CONEXIONES = new ArrayBlockingQueue<>(MAX_POOL_SIZE);
    private static final AtomicInteger CONEXIONES_CREADAS = new AtomicInteger(0);
    private static volatile boolean driverCargado = false;
    private static final Map<String, Boolean> CACHE_COLUMNAS_EXISTENTES = new ConcurrentHashMap<>();

// ======================================================
    // CLIENTE TEMPORAL DE LA VENTA ACTUAL
    // Se usa para NO guardar clientes solo por buscarlos.
    // Se guarda cuando el pago es aprobado; el comprobante se genera después si el cliente lo solicita.
    // ======================================================

    private static String ventaTipoDocumento = "";
    private static String ventaNumeroDocumento = "";
    private static String ventaNombreCliente = "";

    public static void prepararClienteParaVenta(String tipoDocumento, String numeroDocumento, String nombreCliente) {
        ventaTipoDocumento = tipoDocumento == null ? "" : tipoDocumento.trim();
        ventaNumeroDocumento = numeroDocumento == null ? "" : numeroDocumento.trim();
        ventaNombreCliente = nombreCliente == null ? "" : nombreCliente.trim();

        System.out.println("CLIENTE PREPARADO PARA VENTA");
        System.out.println("Tipo: " + ventaTipoDocumento);
        System.out.println("Documento: " + ventaNumeroDocumento);
        System.out.println("Nombre: " + ventaNombreCliente);
    }

    private static boolean hayClientePreparadoParaVenta() {
        return ventaNumeroDocumento != null &&
                !ventaNumeroDocumento.trim().isEmpty() &&
                ventaNombreCliente != null &&
                !ventaNombreCliente.trim().isEmpty();
    }

    private static void limpiarClientePreparadoParaVenta() {
        ventaTipoDocumento = "";
        ventaNumeroDocumento = "";
        ventaNombreCliente = "";
    }

    // ======================================================
    // CONEXIÓN Y LOGIN
    // ======================================================

    public static Connection conectar() throws SQLException {
        cargarDriverMySQL();

        Connection conexionFisica = obtenerConexionFisica();
        return crearConexionProxy(conexionFisica);
    }

    private static void cargarDriverMySQL() throws SQLException {
        if (driverCargado) {
            return;
        }

        synchronized (BDCINEX.class) {
            if (driverCargado) {
                return;
            }

            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                driverCargado = true;
            } catch (ClassNotFoundException e1) {
                try {
                    Class.forName("com.mysql.jdbc.Driver");
                    driverCargado = true;
                } catch (ClassNotFoundException e2) {
                    throw new SQLException("No se encontró el driver MySQL Connector/J en la librería del proyecto.", e2);
                }
            }
        }
    }

    private static Connection obtenerConexionFisica() throws SQLException {
        Connection con = POOL_CONEXIONES.poll();

        while (con != null) {
            if (conexionValida(con)) {
                configurarConexionCINEX(con);
                return con;
            }

            cerrarConexionFisica(con);
            con = POOL_CONEXIONES.poll();
        }

        while (true) {
            int creadas = CONEXIONES_CREADAS.get();

            if (creadas < MAX_POOL_SIZE) {
                if (CONEXIONES_CREADAS.compareAndSet(creadas, creadas + 1)) {
                    try {
                        Connection nuevaConexion = DriverManager.getConnection(URL, USUARIO, CLAVE);
                        configurarConexionCINEX(nuevaConexion);
                        return nuevaConexion;
                    } catch (SQLException e) {
                        CONEXIONES_CREADAS.decrementAndGet();
                        throw e;
                    }
                }
            } else {
                try {
                    con = POOL_CONEXIONES.poll(WAIT_CONNECTION_SECONDS, TimeUnit.SECONDS);

                    if (con != null && conexionValida(con)) {
                        configurarConexionCINEX(con);
                        return con;
                    }

                    if (con != null) {
                        cerrarConexionFisica(con);
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Tiempo de espera interrumpido al obtener conexión Railway.", e);
                }

                throw new SQLException("No hay conexiones disponibles para Railway. Intente nuevamente.");
            }
        }
    }


    private static void configurarConexionCINEX(Connection con) throws SQLException {
        if (con == null || con.isClosed()) {
            return;
        }

        /*
         * Railway/MySQL trabaja normalmente en UTC.
         * CINEX se usa en Perú, por eso cada sesión de BD debe trabajar en UTC-05:00.
         * Así NOW() y CURRENT_TIMESTAMP guardan la hora real de Perú en ventas, pagos y comprobantes.
         */
        try (Statement st = con.createStatement()) {
            st.execute("SET time_zone = '-05:00'");
        }
    }

    private static Connection crearConexionProxy(Connection conexionFisica) {
        InvocationHandler handler = new InvocationHandler() {
            private boolean devueltaAlPool = false;

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String nombreMetodo = method.getName();

                if ("close".equals(nombreMetodo)) {
                    if (!devueltaAlPool) {
                        devueltaAlPool = true;
                        devolverConexionAlPool(conexionFisica);
                    }
                    return null;
                }

                if ("isClosed".equals(nombreMetodo)) {
                    return devueltaAlPool || conexionFisica.isClosed();
                }

                if ("unwrap".equals(nombreMetodo)) {
                    Class<?> clase = (Class<?>) args[0];
                    if (clase.isInstance(conexionFisica)) {
                        return conexionFisica;
                    }
                }

                if ("isWrapperFor".equals(nombreMetodo)) {
                    Class<?> clase = (Class<?>) args[0];
                    return clase.isInstance(conexionFisica);
                }

                return method.invoke(conexionFisica, args);
            }
        };

        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class[]{Connection.class},
                handler
        );
    }

    private static void devolverConexionAlPool(Connection con) {
        if (con == null) {
            return;
        }

        try {
            if (!conexionValida(con)) {
                cerrarConexionFisica(con);
                return;
            }

            if (!con.getAutoCommit()) {
                try {
                    con.rollback();
                } catch (SQLException ignored) {
                }
                con.setAutoCommit(true);
            }

            con.clearWarnings();

            if (!POOL_CONEXIONES.offer(con)) {
                cerrarConexionFisica(con);
            }

        } catch (SQLException e) {
            cerrarConexionFisica(con);
        }
    }

    private static boolean conexionValida(Connection con) {
        try {
            return con != null && !con.isClosed() && con.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    private static void cerrarConexionFisica(Connection con) {
        try {
            if (con != null && !con.isClosed()) {
                con.close();
            }
        } catch (SQLException ignored) {
        } finally {
            CONEXIONES_CREADAS.updateAndGet(valor -> Math.max(0, valor - 1));
        }
    }

    public static boolean probarConexion() {
        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement("SELECT 1");
             ResultSet rs = ps.executeQuery()) {
            return rs.next();
        } catch (SQLException e) {
            JOptionPaneBD.mostrarError("Error al probar conexión Railway", e);
            return false;
        }
    }

    public static void cerrarPoolConexiones() {
        Connection con;

        while ((con = POOL_CONEXIONES.poll()) != null) {
            cerrarConexionFisica(con);
        }
    }

    public static String validarLogin(String usuario, String contrasena) {
        String sql = "SELECT rol FROM usuarios WHERE usuario = ? AND contrasena = ? AND estado = 'Activo' LIMIT 1";

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, usuario);
            ps.setString(2, contrasena);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("rol");
                }
            }

        } catch (SQLException e) {
            JOptionPaneBD.mostrarError("Error al validar login", e);
        }

        return null;
    }

        private static boolean columnaExiste(Connection con, String tabla, String columna) {
        try {
            String catalogo = con.getCatalog();
            String claveCache = (catalogo + "." + tabla + "." + columna).toLowerCase();

            Boolean valorCache = CACHE_COLUMNAS_EXISTENTES.get(claveCache);
            if (valorCache != null) {
                return valorCache;
            }

            DatabaseMetaData meta = con.getMetaData();
            boolean existe = false;

            try (ResultSet rs = meta.getColumns(catalogo, null, tabla, columna)) {
                if (rs.next()) {
                    existe = true;
                }
            }

            if (!existe) {
                try (ResultSet rs = meta.getColumns(catalogo, null, tabla.toLowerCase(), columna)) {
                    if (rs.next()) {
                        existe = true;
                    }
                }
            }

            if (!existe) {
                try (ResultSet rs = meta.getColumns(catalogo, null, tabla.toUpperCase(), columna)) {
                    if (rs.next()) {
                        existe = true;
                    }
                }
            }

            CACHE_COLUMNAS_EXISTENTES.put(claveCache, existe);
            return existe;

        } catch (SQLException e) {
            return false;
        }
    }

    // ======================================================
    // CLIENTES
    // ======================================================

    private static int obtenerOCrearClienteVenta(Connection con) throws SQLException {
        if (!hayClientePreparadoParaVenta()) {
            System.out.println("No hay cliente preparado. Se usará Cliente General.");
            return obtenerIdClienteGeneral(con);
        }

        String documento = ventaNumeroDocumento.trim();
        String nombre = ventaNombreCliente.trim();

        String sqlBuscar = "SELECT id_cliente FROM clientes WHERE dni = ? LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sqlBuscar)) {
            ps.setString(1, documento);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int idCliente = rs.getInt("id_cliente");

                    String sqlActualizar = "UPDATE clientes SET nombre = ? WHERE id_cliente = ?";

                    try (PreparedStatement psUpdate = con.prepareStatement(sqlActualizar)) {
                        psUpdate.setString(1, nombre);
                        psUpdate.setInt(2, idCliente);
                        psUpdate.executeUpdate();
                    }

                    System.out.println("Cliente existente usado en venta. ID: " + idCliente);
                    return idCliente;
                }
            }
        }

        String sqlInsertar = "INSERT INTO clientes(nombre, dni) VALUES (?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sqlInsertar, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.setString(2, documento);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int idNuevo = rs.getInt(1);
                    System.out.println("Cliente creado recién al comprar. ID: " + idNuevo);
                    return idNuevo;
                }
            }
        }

        return obtenerIdClienteGeneral(con);
    }

    public static int obtenerIdClienteGeneral(Connection con) throws SQLException {
        String sqlBuscar = "SELECT id_cliente FROM clientes WHERE dni = '00000000' LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sqlBuscar);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("id_cliente");
            }
        }

        String sqlInsertar = "INSERT INTO clientes(nombre, dni) VALUES (?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sqlInsertar, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "Cliente General");
            ps.setString(2, "00000000");
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 1;
    }

    public static String buscarClientePorDocumento(String tipoDocumento, String numeroDocumento) {
        String sql = "SELECT nombre FROM clientes WHERE dni = ? LIMIT 1";

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, numeroDocumento);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("nombre");
                }
            }

        } catch (SQLException e) {
            JOptionPaneBD.mostrarError("Error al buscar cliente", e);
        }

        return null;
    }

    /*
     * Este método se deja por compatibilidad, pero NO debe usarse en VentaEntradasCINEXGUI
     * antes de completar la compra.
     */
    public static boolean guardarClienteDocumento(String tipoDocumento, String numeroDocumento, String nombreCompleto) {
        String sqlExiste = "SELECT id_cliente FROM clientes WHERE dni = ? LIMIT 1";

        try (Connection con = conectar()) {
            int idCliente = -1;

            try (PreparedStatement ps = con.prepareStatement(sqlExiste)) {
                ps.setString(1, numeroDocumento);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        idCliente = rs.getInt("id_cliente");
                    }
                }
            }

            if (idCliente == -1) {
                String sqlInsert = "INSERT INTO clientes(nombre, dni) VALUES (?, ?)";

                try (PreparedStatement ps = con.prepareStatement(sqlInsert)) {
                    ps.setString(1, nombreCompleto);
                    ps.setString(2, numeroDocumento);
                    return ps.executeUpdate() > 0;
                }

            } else {
                String sqlUpdate = "UPDATE clientes SET nombre = ? WHERE id_cliente = ?";

                try (PreparedStatement ps = con.prepareStatement(sqlUpdate)) {
                    ps.setString(1, nombreCompleto);
                    ps.setInt(2, idCliente);
                    return ps.executeUpdate() > 0;
                }
            }

        } catch (SQLException e) {
            JOptionPaneBD.mostrarError("Error al guardar cliente", e);
            return false;
        }
    }

    public static ArrayList<Object[]> listarClientesCompradores() {
        ArrayList<Object[]> lista = new ArrayList<>();

        String sql = "SELECT DISTINCT c.dni, c.nombre " +
                "FROM clientes c " +
                "INNER JOIN ventas v ON c.id_cliente = v.id_cliente " +
                "WHERE c.dni IS NOT NULL AND c.dni <> '' " +
                "AND c.dni <> '00000000' " +
                "AND c.nombre IS NOT NULL AND c.nombre <> '' " +
                "ORDER BY c.nombre ASC";

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(new Object[]{
                        rs.getString("dni"),
                        rs.getString("nombre")
                });
            }

        } catch (SQLException e) {
            JOptionPaneBD.mostrarError("Error al listar clientes compradores", e);
        }

        return lista;
    }

    public static ArrayList<Object[]> listarClientes() {
        ArrayList<Object[]> lista = new ArrayList<>();

        String sql = "SELECT id_cliente, dni, nombre FROM clientes ORDER BY id_cliente DESC";

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(new Object[]{
                        rs.getInt("id_cliente"),
                        rs.getString("dni"),
                        rs.getString("nombre")
                });
            }

        } catch (SQLException e) {
            JOptionPaneBD.mostrarError("Error al listar clientes", e);
        }

        return lista;
    }

    // ======================================================
    // PELÍCULAS
    // ======================================================

    public static ArrayList<Object[]> listarPeliculasCartelera() {
        ArrayList<Object[]> lista = new ArrayList<>();

        String sql = "SELECT id_pelicula, titulo, genero, duracion, clasificacion, imagen " +
                "FROM peliculas WHERE estado = 'Activa' AND en_cartelera = 1 ORDER BY id_pelicula DESC";

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(new Object[]{
                        rs.getInt("id_pelicula"),
                        rs.getString("titulo"),
                        rs.getString("genero"),
                        rs.getInt("duracion"),
                        rs.getString("clasificacion"),
                        rs.getString("imagen")
                });
            }

        } catch (SQLException e) {
            JOptionPaneBD.mostrarError("Error al listar películas en cartelera", e);
        }

        return lista;
    }

    public static ArrayList<Object[]> listarPeliculas() {
        ArrayList<Object[]> lista = new ArrayList<>();

        String sql = "SELECT id_pelicula, titulo, genero, duracion, clasificacion, imagen, estado, en_cartelera " +
                "FROM peliculas ORDER BY id_pelicula DESC";

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(new Object[]{
                        rs.getInt("id_pelicula"),
                        rs.getString("titulo"),
                        rs.getString("genero"),
                        rs.getInt("duracion"),
                        rs.getString("clasificacion"),
                        rs.getString("imagen"),
                        rs.getString("estado"),
                        rs.getInt("en_cartelera") == 1 ? "Sí" : "No"
                });
            }

        } catch (SQLException e) {
            JOptionPaneBD.mostrarError("Error al listar películas", e);
        }

        return lista;
    }

    public static boolean guardarPelicula(String titulo, String genero, int duracion, String clasificacion, String imagen, String estado, int enCartelera) {
        String sql = "INSERT INTO peliculas(titulo, genero, duracion, clasificacion, imagen, estado, en_cartelera) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, titulo);
            ps.setString(2, genero);
            ps.setInt(3, duracion);
            ps.setString(4, clasificacion);
            ps.setString(5, imagen);
            ps.setString(6, estado);
            ps.setInt(7, enCartelera);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            JOptionPaneBD.mostrarError("Error al guardar película", e);
            return false;
        }
    }

    public static boolean actualizarPelicula(int idPelicula, String titulo, String genero, int duracion, String clasificacion, String imagen, String estado, int enCartelera) {
        String sql = "UPDATE peliculas SET titulo=?, genero=?, duracion=?, clasificacion=?, imagen=?, estado=?, en_cartelera=? " +
                "WHERE id_pelicula=?";

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, titulo);
            ps.setString(2, genero);
            ps.setInt(3, duracion);
            ps.setString(4, clasificacion);
            ps.setString(5, imagen);
            ps.setString(6, estado);
            ps.setInt(7, enCartelera);
            ps.setInt(8, idPelicula);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            JOptionPaneBD.mostrarError("Error al actualizar película", e);
            return false;
        }
    }

    public static boolean actualizarEstadoPelicula(int idPelicula, String estado, int enCartelera) {
        String sql = "UPDATE peliculas SET estado=?, en_cartelera=? WHERE id_pelicula=?";

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, estado);
            ps.setInt(2, enCartelera);
            ps.setInt(3, idPelicula);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            JOptionPaneBD.mostrarError("Error al cambiar estado de película", e);
            return false;
        }
    }

    public static int obtenerIdPeliculaPorTitulo(
            Connection con,
            String titulo
    ) throws SQLException {
        String tituloLimpio = titulo == null
                ? ""
                : titulo.replace("\\n", " ")
                        .replace("\n", " ")
                        .trim();

        if (tituloLimpio.isEmpty()) {
            return -1;
        }

        String sql = "SELECT id_pelicula "
                + "FROM peliculas "
                + "WHERE LOWER(TRIM(titulo)) = LOWER(TRIM(?)) "
                + "LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tituloLimpio);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_pelicula");
                }
            }
        }

        return -1;
    }

    // ======================================================
    // FUNCIONES / SALAS / ASIENTOS / PRECIOS
    // ======================================================

        public static int obtenerIdFuncionPorPeliculaHora(
            Connection con,
            String pelicula,
            String funcion
    ) throws SQLException {
        int idFuncionDirecto = extraerIdFuncion(funcion);

        if (idFuncionDirecto > 0) {
            String titulo = pelicula == null ? "" : pelicula.trim();

            String sqlDirecto = "SELECT f.id_funcion "
                    + "FROM funciones f "
                    + "INNER JOIN peliculas p "
                    + "ON f.id_pelicula = p.id_pelicula "
                    + "WHERE f.id_funcion = ? "
                    + "AND f.estado = 'Activa' "
                    + "AND (? = '' "
                    + "OR LOWER(TRIM(p.titulo)) "
                    + "= LOWER(TRIM(?))) "
                    + "LIMIT 1";

            try (PreparedStatement ps = con.prepareStatement(sqlDirecto)) {
                ps.setInt(1, idFuncionDirecto);
                ps.setString(2, titulo);
                ps.setString(3, titulo);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("id_funcion");
                    }
                }
            }

            return -1;
        }

        int idPelicula = obtenerIdPeliculaPorTitulo(con, pelicula);

        if (idPelicula == -1) {
            return -1;
        }

        String hora24 = normalizarHora(funcion);
        String funcionTexto = funcion == null
                ? ""
                : funcion.trim().toUpperCase();

        /*
         * Compatibilidad con pantallas antiguas que todavía envían
         * solamente la hora. Se limita estrictamente a hoy y mañana.
         * El flujo actual siempre envía el ID real de la función.
         */
        String sql = "SELECT id_funcion "
                + "FROM funciones "
                + "WHERE id_pelicula = ? "
                + "AND estado = 'Activa' "
                + "AND fecha >= CURDATE() "
                + "AND fecha < DATE_ADD(CURDATE(), INTERVAL 2 DAY) "
                + "AND (TIME_FORMAT(hora, '%H:%i') = ? "
                + "OR UPPER(TIME_FORMAT(hora, '%h:%i %p')) = ? "
                + "OR UPPER(TIME_FORMAT(hora, '%l:%i %p')) = ?) "
                + "ORDER BY fecha, hora "
                + "LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPelicula);
            ps.setString(2, hora24);
            ps.setString(3, funcionTexto);
            ps.setString(4, funcionTexto);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_funcion");
                }
            }
        }

        return -1;
    }

    // Alias por si alguna interfaz lo llama con este nombre.
        public static int obtenerIdFuncionPorPeliculaYHora(
            Connection con,
            int idPelicula,
            String funcion
    ) throws SQLException {
        int idFuncionDirecto = extraerIdFuncion(funcion);

        if (idFuncionDirecto > 0) {
            String sqlDirecto = "SELECT id_funcion "
                    + "FROM funciones "
                    + "WHERE id_funcion = ? "
                    + "AND id_pelicula = ? "
                    + "AND estado = 'Activa' "
                    + "LIMIT 1";

            try (PreparedStatement ps = con.prepareStatement(sqlDirecto)) {
                ps.setInt(1, idFuncionDirecto);
                ps.setInt(2, idPelicula);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("id_funcion");
                    }
                }
            }

            return -1;
        }

        String hora24 = normalizarHora(funcion);
        String funcionTexto = funcion == null
                ? ""
                : funcion.trim().toUpperCase();

        String sql = "SELECT id_funcion "
                + "FROM funciones "
                + "WHERE id_pelicula = ? "
                + "AND estado = 'Activa' "
                + "AND fecha >= CURDATE() "
                + "AND fecha < DATE_ADD(CURDATE(), INTERVAL 2 DAY) "
                + "AND (TIME_FORMAT(hora, '%H:%i') = ? "
                + "OR UPPER(TIME_FORMAT(hora, '%h:%i %p')) = ? "
                + "OR UPPER(TIME_FORMAT(hora, '%l:%i %p')) = ?) "
                + "ORDER BY fecha, hora "
                + "LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPelicula);
            ps.setString(2, hora24);
            ps.setString(3, funcionTexto);
            ps.setString(4, funcionTexto);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_funcion");
                }
            }
        }

        return -1;
    }


    private static int extraerIdFuncion(String texto) {
        if (texto == null) {
            return -1;
        }

        String limpio = texto.trim();

        try {
            if (limpio.matches("^\\d+$")) {
                return Integer.parseInt(limpio);
            }

            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("(?i)(?:ID\\s*:?\\s*|FUNCION\\s*:?\\s*|FUNCIÓN\\s*:?\\s*)(\\d+)")
                    .matcher(limpio);

            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }

        } catch (Exception e) {
            return -1;
        }

        return -1;
    }

    public static String formatearHora12(java.sql.Time hora) {
        if (hora == null) {
            return "";
        }

        return hora.toLocalTime()
                .format(java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.ENGLISH));
    }

    public static String formatearFecha(java.sql.Date fecha) {
        if (fecha == null) {
            return "";
        }

        return fecha.toLocalDate()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private static String normalizarHora(String hora) {
        try {
            String h = hora.trim()
                    .toUpperCase()
                    .replace("A. M.", "AM")
                    .replace("P. M.", "PM")
                    .replace("A.M.", "AM")
                    .replace("P.M.", "PM");

            java.time.format.DateTimeFormatter entrada =
                    java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.ENGLISH);

            return java.time.LocalTime.parse(h, entrada)
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

        } catch (Exception e) {
            return hora == null ? "" : hora.trim();
        }
    }

    public static int obtenerIdSalaPorFuncion(Connection con, int idFuncion) throws SQLException {
        String sql = "SELECT id_sala FROM funciones WHERE id_funcion = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idFuncion);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_sala");
                }
            }
        }

        return -1;
    }

    public static int obtenerIdAsiento(Connection con, int idSala, String codigo) throws SQLException {
        String fila = codigo.replaceAll("[0-9]", "");
        int numero = Integer.parseInt(codigo.replaceAll("[^0-9]", ""));

        String sqlBuscar = "SELECT id_asiento FROM asientos WHERE id_sala = ? AND fila = ? AND numero = ? LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sqlBuscar)) {
            ps.setInt(1, idSala);
            ps.setString(2, fila);
            ps.setInt(3, numero);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_asiento");
                }
            }
        }

        String sqlInsertar = "INSERT INTO asientos(id_sala, fila, numero, estado) VALUES (?, ?, ?, 'Disponible')";

        try (PreparedStatement ps = con.prepareStatement(sqlInsertar, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idSala);
            ps.setString(2, fila);
            ps.setInt(3, numero);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return -1;
    }

    // Alias por si alguna versión anterior lo llama así.
    public static int obtenerIdAsientoPorTexto(Connection con, String asientoTexto) throws SQLException {
        int idSala = 1;
        return obtenerIdAsiento(con, idSala, asientoTexto);
    }

    public static int obtenerIdPrecioGeneral(Connection con) throws SQLException {
        String sql = "SELECT id_precio FROM precios ORDER BY id_precio LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("id_precio");
            }
        }

        String sqlCualquiera = "SELECT id_precio FROM precios ORDER BY id_precio LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sqlCualquiera);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("id_precio");
            }
        }

        return -1;
    }



    public static ArrayList<PrecioCINEX> listarPreciosEntradaActivos() {
        ArrayList<PrecioCINEX> precios = new ArrayList<>();

        try (Connection con = conectar()) {
            String columnaPrecio = columnaExiste(con, "precios", "precio") ? "precio" : "monto";
            String sql = "SELECT id_precio, tipo_entrada, "
                    + columnaPrecio + " AS monto "
                    + "FROM precios ORDER BY id_precio ASC";

            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    precios.add(new PrecioCINEX(
                            rs.getInt("id_precio"),
                            rs.getString("tipo_entrada"),
                            rs.getDouble("monto"),
                            "Activo"
                    ));
                }
            }

        } catch (SQLException e) {
            JOptionPaneBD.mostrarError("Error al listar precios de entrada", e);
        }

        if (precios.isEmpty()) {
            precios.add(new PrecioCINEX(0, "Entrada General", 32.00, "Activo"));
        }

        return precios;
    }

    public static String obtenerTipoEntradaPredeterminado() {
        ArrayList<PrecioCINEX> precios = listarPreciosEntradaActivos();
        if (precios == null || precios.isEmpty()) {
            return "Entrada General";
        }

        String tipo = precios.get(0).getTipoEntrada();
        return tipo == null || tipo.trim().isEmpty() ? "Entrada General" : tipo.trim();
    }

    private static String normalizarTipoEntradaInterno(String tipoEntrada) {
        if (tipoEntrada == null || tipoEntrada.trim().isEmpty()) {
            return obtenerTipoEntradaPredeterminado();
        }

        String tipo = tipoEntrada.trim();
        if (tipo.equalsIgnoreCase("Adulto")) {
            return "Entrada General";
        }
        if (tipo.equalsIgnoreCase("Niño") || tipo.equalsIgnoreCase("Nino")) {
            return "Entrada Niño";
        }
        return tipo;
    }

    public static int obtenerIdPrecioPorTipo(Connection con, String tipoEntrada) throws SQLException {
        String tipo = normalizarTipoEntradaInterno(tipoEntrada);

        if (columnaExiste(con, "precios", "tipo_entrada")) {
            String sql = "SELECT id_precio FROM precios "
                    + "WHERE LOWER(tipo_entrada) = LOWER(?) "
                    + "ORDER BY id_precio LIMIT 1";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
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

    public static double obtenerPrecioEntradaPorTipo(String tipoEntrada) {
        String tipo = normalizarTipoEntradaInterno(tipoEntrada);

        try (Connection con = conectar()) {
            return obtenerPrecioUnitarioPorTipo(con, tipo, obtenerPrecioEntradaGeneral());
        } catch (SQLException e) {
            JOptionPaneBD.mostrarError("Error al obtener precio por tipo de entrada", e);
        }

        return 32.00;
    }

    public static double obtenerPrecioEntradaGeneral() {
        try (Connection con = conectar()) {
            String columnaPrecio = columnaExiste(con, "precios", "precio") ? "precio" : "monto";
            String sql = "SELECT " + columnaPrecio + " AS precio FROM precios ORDER BY id_precio LIMIT 1";

            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return rs.getDouble("precio");
                }
            }

        } catch (SQLException e) {
            JOptionPaneBD.mostrarError("Error al obtener precio", e);
        }

        return 10.00;
    }

        public static ArrayList<String> listarAsientosOcupados(
            String pelicula,
            String funcion
    ) {
        try (Connection con = conectar()) {
            int idFuncion = obtenerIdFuncionPorPeliculaHora(
                    con,
                    pelicula,
                    funcion
            );

            return listarAsientosOcupados(con, idFuncion);

        } catch (SQLException e) {
            JOptionPaneBD.mostrarError(
                    "Error al cargar asientos ocupados",
                    e
            );
            return new ArrayList<>();
        }
    }

    public static ArrayList<String> listarAsientosOcupados(
            int idFuncion
    ) {
        try (Connection con = conectar()) {
            return listarAsientosOcupados(con, idFuncion);
        } catch (SQLException e) {
            JOptionPaneBD.mostrarError(
                    "Error al cargar asientos ocupados",
                    e
            );
            return new ArrayList<>();
        }
    }

    private static ArrayList<String> listarAsientosOcupados(
            Connection con,
            int idFuncion
    ) throws SQLException {
        ArrayList<String> ocupados = new ArrayList<>();

        if (idFuncion <= 0) {
            return ocupados;
        }

        String sql = "SELECT DISTINCT "
                + "CONCAT(a.fila, a.numero) AS asiento "
                + "FROM entradas e "
                + "INNER JOIN asientos a "
                + "ON e.id_asiento = a.id_asiento "
                + "WHERE e.id_funcion = ? "
                + "AND (e.estado IS NULL "
                + "OR e.estado NOT IN ('Anulada', 'Reembolsada'))";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idFuncion);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ocupados.add(rs.getString("asiento"));
                }
            }
        }

        return ocupados;
    }

    public static boolean asientoDisponible(String pelicula, String funcion, String asiento) {
        try (Connection con = conectar()) {
            int idFuncion = obtenerIdFuncionPorPeliculaHora(con, pelicula, funcion);

            if (idFuncion == -1) {
                return false;
            }

            int idSala = obtenerIdSalaPorFuncion(con, idFuncion);

            if (idSala == -1) {
                return false;
            }

            int idAsiento = obtenerIdAsiento(con, idSala, asiento);

            String sql = "SELECT COUNT(*) AS total " +
                    "FROM entradas " +
                    "WHERE id_funcion = ? AND id_asiento = ? " +
                    "AND (estado IS NULL OR estado NOT IN ('Anulada', 'Reembolsada'))";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idFuncion);
                ps.setInt(2, idAsiento);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("total") == 0;
                    }
                }
            }

        } catch (SQLException e) {
            JOptionPaneBD.mostrarError("Error al verificar disponibilidad del asiento", e);
        }

        return false;
    }

    public static boolean asientosDisponibles(String pelicula, String funcion, List<String> asientos) {
        if (asientos == null || asientos.isEmpty()) {
            return false;
        }

        for (String asiento : asientos) {
            if (!asientoDisponible(pelicula, funcion, asiento)) {
                return false;
            }
        }

        return true;
    }

    public static int contarAsientosVendidosFuncion(String pelicula, String funcion) {
        try (Connection con = conectar()) {
            int idFuncion = obtenerIdFuncionPorPeliculaHora(con, pelicula, funcion);

            if (idFuncion == -1) {
                return 0;
            }

            String sql = "SELECT COUNT(*) AS total FROM entradas " +
                    "WHERE id_funcion = ? AND (estado IS NULL OR estado NOT IN ('Anulada', 'Reembolsada'))";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idFuncion);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("total");
                    }
                }
            }

        } catch (SQLException e) {
            JOptionPaneBD.mostrarError("Error al contar asientos vendidos", e);
        }

        return 0;
    }

    public static int obtenerCapacidadSalaFuncion(
            String pelicula,
            String funcion
    ) {
        try (Connection con = conectar()) {
            int idFuncion = obtenerIdFuncionPorPeliculaHora(
                    con,
                    pelicula,
                    funcion
            );

            return obtenerCapacidadSalaFuncion(con, idFuncion);

        } catch (SQLException e) {
            JOptionPaneBD.mostrarError(
                    "Error al obtener capacidad de sala",
                    e
            );
            return 0;
        }
    }

    public static int obtenerCapacidadSalaFuncion(int idFuncion) {
        try (Connection con = conectar()) {
            return obtenerCapacidadSalaFuncion(con, idFuncion);
        } catch (SQLException e) {
            JOptionPaneBD.mostrarError(
                    "Error al obtener capacidad de sala",
                    e
            );
            return 0;
        }
    }

    private static int obtenerCapacidadSalaFuncion(
            Connection con,
            int idFuncion
    ) throws SQLException {
        if (idFuncion <= 0) {
            return 0;
        }

        String sql = "SELECT s.capacidad "
                + "FROM funciones f "
                + "INNER JOIN salas s "
                + "ON f.id_sala = s.id_sala "
                + "WHERE f.id_funcion = ? "
                + "LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idFuncion);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("capacidad") : 0;
            }
        }
    }

    public static String obtenerNombreSalaFuncion(
            String pelicula,
            String funcion
    ) {
        try (Connection con = conectar()) {
            int idFuncion = obtenerIdFuncionPorPeliculaHora(
                    con,
                    pelicula,
                    funcion
            );

            return obtenerNombreSalaFuncion(con, idFuncion);

        } catch (SQLException e) {
            JOptionPaneBD.mostrarError(
                    "Error al obtener sala de la función",
                    e
            );
            return "";
        }
    }

    public static String obtenerNombreSalaFuncion(int idFuncion) {
        try (Connection con = conectar()) {
            return obtenerNombreSalaFuncion(con, idFuncion);
        } catch (SQLException e) {
            JOptionPaneBD.mostrarError(
                    "Error al obtener sala de la función",
                    e
            );
            return "";
        }
    }

    private static String obtenerNombreSalaFuncion(
            Connection con,
            int idFuncion
    ) throws SQLException {
        if (idFuncion <= 0) {
            return "";
        }

        String sql = "SELECT s.nombre "
                + "FROM funciones f "
                + "INNER JOIN salas s "
                + "ON f.id_sala = s.id_sala "
                + "WHERE f.id_funcion = ? "
                + "LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idFuncion);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("nombre") : "";
            }
        }
    }

    public static String obtenerTipoSalaFuncion(
            String pelicula,
            String funcion
    ) {
        try (Connection con = conectar()) {
            int idFuncion = obtenerIdFuncionPorPeliculaHora(
                    con,
                    pelicula,
                    funcion
            );

            if (idFuncion <= 0) {
                return "";
            }

            String sql = "SELECT s.tipo "
                    + "FROM funciones f "
                    + "INNER JOIN salas s "
                    + "ON f.id_sala = s.id_sala "
                    + "WHERE f.id_funcion = ? "
                    + "LIMIT 1";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idFuncion);

                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString("tipo") : "";
                }
            }

        } catch (SQLException e) {
            JOptionPaneBD.mostrarError(
                    "Error al obtener tipo de sala",
                    e
            );
            return "";
        }
    }

    public static int contarAsientosDisponiblesFuncion(String pelicula, String funcion) {
        int capacidad = obtenerCapacidadSalaFuncion(pelicula, funcion);
        int vendidos = contarAsientosVendidosFuncion(pelicula, funcion);

        return Math.max(0, capacidad - vendidos);
    }

    public static ArrayList<Object[]> listarFuncionesPorPelicula(
            String pelicula
    ) {
        ArrayList<Object[]> lista = new ArrayList<>();

        String peliculaLimpia = pelicula == null
                ? ""
                : pelicula.replace("\\n", " ")
                        .replace("\n", " ")
                        .trim();

        /*
         * Si el título llega vacío no se ejecuta LIKE '%%',
         * porque eso devolvería las funciones de cualquier película.
         */
        if (peliculaLimpia.isEmpty()) {
            return lista;
        }

        String sql = "SELECT f.id_funcion, p.titulo, p.genero, "
                + "p.duracion, p.clasificacion, p.imagen, "
                + "s.nombre AS sala, s.capacidad, s.tipo, "
                + "f.fecha, f.hora, f.estado, "
                + "IFNULL(vendidos.total, 0) AS vendidos "
                + "FROM funciones f "
                + "INNER JOIN peliculas p "
                + "ON f.id_pelicula = p.id_pelicula "
                + "INNER JOIN salas s "
                + "ON f.id_sala = s.id_sala "
                + "LEFT JOIN ( "
                + "   SELECT id_funcion, COUNT(*) AS total "
                + "   FROM entradas "
                + "   WHERE estado IS NULL "
                + "   OR estado NOT IN ('Anulada', 'Reembolsada') "
                + "   GROUP BY id_funcion "
                + ") vendidos ON vendidos.id_funcion = f.id_funcion "
                + "WHERE LOWER(TRIM(p.titulo)) "
                + "= LOWER(TRIM(?)) "
                + "AND f.estado = 'Activa' "
                + "AND f.fecha >= CURDATE() "
                + "AND f.fecha < DATE_ADD("
                + "CURDATE(), INTERVAL 2 DAY) "
                + "AND (f.fecha > CURDATE() "
                + "OR TIMESTAMP(f.fecha, f.hora) "
                + ">= DATE_SUB(NOW(), INTERVAL 10 MINUTE)) "
                + "ORDER BY f.fecha ASC, f.hora ASC";

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, peliculaLimpia);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String hora12 =
                            formatearHora12(rs.getTime("hora"));
                    String fecha =
                            formatearFecha(rs.getDate("fecha"));
                    int capacidad = rs.getInt("capacidad");
                    int vendidos = rs.getInt("vendidos");
                    int disponibles = Math.max(
                            0,
                            capacidad - vendidos
                    );

                    lista.add(new Object[]{
                            rs.getInt("id_funcion"),
                            rs.getString("titulo"),
                            rs.getString("genero"),
                            rs.getInt("duracion"),
                            rs.getString("clasificacion"),
                            rs.getString("imagen"),
                            rs.getString("sala"),
                            capacidad,
                            rs.getString("tipo"),
                            fecha,
                            hora12,
                            disponibles
                    });
                }
            }

        } catch (SQLException e) {
            JOptionPaneBD.mostrarError(
                    "Error al listar funciones de la película",
                    e
            );
        }

        return lista;
    }

    public static ArrayList<Object[]> listarFuncionesProgramadasTaquillero() {
        ArrayList<Object[]> lista = new ArrayList<>();

        String sql = "SELECT f.id_funcion, p.titulo, p.genero, p.duracion, p.clasificacion, p.imagen, " +
                "s.nombre AS sala, s.capacidad, s.tipo, f.fecha, f.hora, f.estado, " +
                "IFNULL(vendidos.total, 0) AS vendidos " +
                "FROM funciones f " +
                "INNER JOIN peliculas p ON f.id_pelicula = p.id_pelicula " +
                "INNER JOIN salas s ON f.id_sala = s.id_sala " +
                "LEFT JOIN ( " +
                "   SELECT id_funcion, COUNT(*) AS total " +
                "   FROM entradas " +
                "   WHERE estado IS NULL OR estado NOT IN ('Anulada', 'Reembolsada') " +
                "   GROUP BY id_funcion " +
                ") vendidos ON vendidos.id_funcion = f.id_funcion " +
                "WHERE f.estado = 'Activa' " +
                "ORDER BY f.fecha ASC, f.hora ASC";

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String hora12 = formatearHora12(rs.getTime("hora"));
                String fecha = formatearFecha(rs.getDate("fecha"));
                int capacidad = rs.getInt("capacidad");
                int vendidos = rs.getInt("vendidos");
                int disponibles = Math.max(0, capacidad - vendidos);

                lista.add(new Object[]{
                        rs.getInt("id_funcion"),
                        rs.getString("titulo"),
                        rs.getString("genero"),
                        rs.getInt("duracion"),
                        rs.getString("clasificacion"),
                        rs.getString("imagen"),
                        rs.getString("sala"),
                        capacidad,
                        rs.getString("tipo"),
                        fecha,
                        hora12,
                        disponibles
                });
            }

        } catch (SQLException e) {
            JOptionPaneBD.mostrarError("Error al listar funciones programadas", e);
        }

        return lista;
    }

    public static ArrayList<Object[]> listarSalas() {
    ArrayList<Object[]> lista = new ArrayList<>();

    String sql = "SELECT id_sala, nombre, capacidad, tipo, estado FROM salas ORDER BY id_sala ASC";

    try (Connection con = conectar();
         PreparedStatement ps = con.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

        while (rs.next()) {
            lista.add(new Object[]{
                    rs.getInt("id_sala"),
                    rs.getString("nombre"),
                    rs.getInt("capacidad"),
                    rs.getString("tipo"),
                    rs.getString("estado")
            });
        }

    } catch (SQLException e) {
        JOptionPaneBD.mostrarError("Error al listar salas", e);
    }

    return lista;
}

    public static ArrayList<Object[]> listarPrecios() {
        ArrayList<Object[]> lista = new ArrayList<>();

        try (Connection con = conectar()) {
            String columnaPrecio = columnaExiste(con, "precios", "precio") ? "precio" : "monto";
            String sql = "SELECT id_precio, tipo_entrada, " + columnaPrecio + " AS precio, estado FROM precios ORDER BY id_precio ASC";

            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    lista.add(new Object[]{
                            rs.getInt("id_precio"),
                            rs.getString("tipo_entrada"),
                            rs.getDouble("precio"),
                            rs.getString("estado")
                    });
                }
            }

        } catch (SQLException e) {
            JOptionPaneBD.mostrarError("Error al listar precios", e);
        }

        return lista;
    }

    public static boolean actualizarPrecio(int idPrecio, double precio, String estado) {
        try (Connection con = conectar()) {
            String columnaPrecio = columnaExiste(con, "precios", "precio") ? "precio" : "monto";
            String sql = "UPDATE precios SET " + columnaPrecio + " = ? WHERE id_precio = ?";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setDouble(1, precio);
                ps.setInt(2, idPrecio);

                return ps.executeUpdate() > 0;
            }

        } catch (SQLException e) {
            JOptionPaneBD.mostrarError("Error al actualizar precio", e);
            return false;
        }
    }

    // ======================================================
    // VENTAS / QR / ENTRADAS
    // ======================================================

    public static String obtenerUltimoNumeroVentaPorUsuario(String usuario) {
        /*
         * Para evitar reutilizar números antiguos, devolvemos null.
         * ConfirmacionCINEXGUI generará un nuevo número de venta.
         */
        return null;
    }

    public static boolean registrarVentaConQRCompleta(
            String numeroVenta,
            String usuarioVendedor,
            String pelicula,
            String funcion,
            List<String> asientos,
            double total,
            String metodoPago,
            String qrEntrada
    ) {
        return registrarVentaConQRCompleta(
                numeroVenta,
                usuarioVendedor,
                pelicula,
                funcion,
                asientos,
                null,
                total,
                metodoPago,
                qrEntrada
        );
    }

    public static boolean registrarVentaConQRCompleta(
            String numeroVenta,
            String usuarioVendedor,
            String pelicula,
            String funcion,
            List<String> asientos,
            List<String> tiposEntrada,
            double total,
            String metodoPago,
            String qrEntrada
    ) {
        return registrarVentaConQRCompleta(
                numeroVenta,
                usuarioVendedor,
                pelicula,
                funcion,
                asientos,
                tiposEntrada,
                null,
                total,
                metodoPago,
                qrEntrada
        );
    }

    public static boolean registrarVentaConQRCompleta(
            String numeroVenta,
            String usuarioVendedor,
            String pelicula,
            String funcion,
            List<String> asientos,
            List<String> tiposEntrada,
            List<Double> preciosUnitarios,
            double total,
            String metodoPago,
            String qrEntrada
    ) {
        return registrarVentaCompleta(
                numeroVenta,
                usuarioVendedor,
                pelicula,
                funcion,
                asientos,
                tiposEntrada,
                preciosUnitarios,
                total,
                metodoPago,
                qrEntrada,
                true
        );
    }

    public static boolean registrarVentaPagadaCompleta(
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
        return registrarVentaCompleta(
                numeroVenta,
                usuarioVendedor,
                pelicula,
                funcion,
                asientos,
                tiposEntrada,
                preciosUnitarios,
                total,
                metodoPago,
                "",
                false
        );
    }

    private static boolean registrarVentaCompleta(
            String numeroVenta,
            String usuarioVendedor,
            String pelicula,
            String funcion,
            List<String> asientos,
            List<String> tiposEntrada,
            List<Double> preciosUnitarios,
            double total,
            String metodoPago,
            String qrEntrada,
            boolean incluirComprobante
    ) {
        Connection con = null;

        try {
            con = conectar();
            con.setAutoCommit(false);

            int idUsuario = obtenerIdUsuario(con, usuarioVendedor);
            int idCliente = obtenerOCrearClienteVenta(con);
            int idFuncion = obtenerIdFuncionPorPeliculaHora(con, pelicula, funcion);

            if (idUsuario == -1 || idFuncion == -1) {
                con.rollback();
                System.out.println("No se encontró usuario o función.");
                System.out.println("Usuario ID: " + idUsuario);
                System.out.println("Función ID: " + idFuncion);
                return false;
            }

            int idSala = obtenerIdSalaPorFuncion(con, idFuncion);
            int idPrecio = obtenerIdPrecioGeneral(con);

            if (idSala == -1 || idPrecio == -1) {
                con.rollback();
                System.out.println("No se encontró sala o precio.");
                return false;
            }

            int idVenta = obtenerIdVentaPorNumero(con, numeroVenta);

            if (idVenta == -1) {
                String columnaFechaVenta = columnaExiste(con, "ventas", "fecha_venta") ? "fecha_venta" : "fecha_hora";
                String sqlVenta = "INSERT INTO ventas(numero_venta, id_cliente, id_usuario, id_funcion, " + columnaFechaVenta + ", total, qr_entrada, estado) " +
                        "VALUES (?, ?, ?, ?, NOW(), ?, ?, 'Registrada')";

                try (PreparedStatement ps = con.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, numeroVenta);
                    ps.setInt(2, idCliente);
                    ps.setInt(3, idUsuario);
                    ps.setInt(4, idFuncion);
                    ps.setDouble(5, total);
                    ps.setString(6, qrEntrada);
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            idVenta = rs.getInt(1);
                        }
                    }
                }

            } else {
                String sqlUpdate = "UPDATE ventas SET id_cliente = ?, id_usuario = ?, id_funcion = ?, total = ?, qr_entrada = ?, estado = 'Registrada' " +
                        "WHERE id_venta = ?";

                try (PreparedStatement ps = con.prepareStatement(sqlUpdate)) {
                    ps.setInt(1, idCliente);
                    ps.setInt(2, idUsuario);
                    ps.setInt(3, idFuncion);
                    ps.setDouble(4, total);
                    ps.setString(5, qrEntrada);
                    ps.setInt(6, idVenta);
                    ps.executeUpdate();
                }

                borrarDetalleVenta(con, idVenta);
            }

            if (idVenta == -1) {
                con.rollback();
                System.out.println("No se pudo generar id_venta.");
                return false;
            }

            registrarPago(con, idVenta, metodoPago, total, numeroVenta);
            registrarEntradas(
                    con,
                    idVenta,
                    idFuncion,
                    idSala,
                    idPrecio,
                    numeroVenta,
                    asientos,
                    tiposEntrada,
                    preciosUnitarios,
                    total
            );
            if (incluirComprobante) {
                registrarComprobante(
                        con,
                        idVenta,
                        qrEntrada,
                        total,
                        numeroVenta
                );
            }

            con.commit();

            System.out.println("VENTA REGISTRADA CON CLIENTE REAL:");
            System.out.println("ID venta: " + idVenta);
            System.out.println("ID cliente: " + idCliente);
            System.out.println("Cliente: " + ventaNombreCliente);
            System.out.println("Documento: " + ventaNumeroDocumento);

            limpiarClientePreparadoParaVenta();
            return true;

        } catch (Exception e) {
            System.out.println("Error al registrar venta completa con QR.");
            System.out.println("[BD CINEX] " + e.getMessage());

            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException ex) {
                System.out.println("[BD CINEX rollback] " + ex.getMessage());
            }

            JOptionPaneBD.mostrarError("Error al registrar venta completa con QR", e);
            return false;

        } finally {
            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        }

    private static int obtenerIdUsuario(Connection con, String usuario) throws SQLException {
        String sql = "SELECT id_usuario FROM usuarios WHERE usuario = ? LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, usuario);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_usuario");
                }
            }
        }

        return -1;
    }

    public static int obtenerIdUsuario(String usuario) throws SQLException {
        try (Connection con = conectar()) {
            return obtenerIdUsuario(con, usuario);
        }
    }

    private static int obtenerIdVentaPorNumero(Connection con, String numeroVenta) throws SQLException {
        String sql = "SELECT id_venta FROM ventas WHERE numero_venta = ? LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, numeroVenta);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_venta");
                }
            }
        }

        return -1;
    }

    private static void borrarDetalleVenta(Connection con, int idVenta) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM entradas WHERE id_venta = ?")) {
            ps.setInt(1, idVenta);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = con.prepareStatement("DELETE FROM pagos WHERE id_venta = ?")) {
            ps.setInt(1, idVenta);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = con.prepareStatement("DELETE FROM comprobantes WHERE id_venta = ?")) {
            ps.setInt(1, idVenta);
            ps.executeUpdate();
        }
    }

    private static void registrarPago(Connection con, int idVenta, String metodoPago, double total, String numeroVenta) throws SQLException {
        boolean tieneMetodo = columnaExiste(con, "pagos", "metodo_pago");
        boolean tieneMonto = columnaExiste(con, "pagos", "monto");
        boolean tieneCodigoOperacion = columnaExiste(con, "pagos", "codigo_operacion");
        boolean tieneEstadoPago = columnaExiste(con, "pagos", "estado_pago");
        boolean tieneEstado = columnaExiste(con, "pagos", "estado");
        boolean tieneFechaPago = columnaExiste(con, "pagos", "fecha_pago");

        StringBuilder columnas = new StringBuilder("id_venta");
        StringBuilder valores = new StringBuilder("?");
        ArrayList<Object> params = new ArrayList<>();
        params.add(idVenta);

        if (tieneMetodo) {
            columnas.append(", metodo_pago");
            valores.append(", ?");
            params.add(normalizarMetodo(metodoPago));
        }

        if (tieneMonto) {
            columnas.append(", monto");
            valores.append(", ?");
            params.add(total);
        }

        if (tieneCodigoOperacion) {
            columnas.append(", codigo_operacion");
            valores.append(", ?");
            params.add(numeroVenta);
        }

        if (tieneEstadoPago) {
            columnas.append(", estado_pago");
            valores.append(", ?");
            params.add("Pagado");
        } else if (tieneEstado) {
            columnas.append(", estado");
            valores.append(", ?");
            params.add("Aprobado");
        }

        if (tieneFechaPago) {
            columnas.append(", fecha_pago");
            valores.append(", NOW()");
        }

        String sql = "INSERT INTO pagos(" + columnas + ") VALUES (" + valores + ")";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                Object valor = params.get(i);

                if (valor instanceof Integer) {
                    ps.setInt(i + 1, (Integer) valor);
                } else if (valor instanceof Double) {
                    ps.setDouble(i + 1, (Double) valor);
                } else {
                    ps.setString(i + 1, String.valueOf(valor));
                }
            }

            ps.executeUpdate();
        }
    }

        private static void registrarEntradas(
            Connection con,
            int idVenta,
            int idFuncion,
            int idSala,
            int idPrecio,
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
        boolean tieneCodigoEntrada = columnaExiste(con, "entradas", "codigo_entrada");
        boolean tienePrecioUnitario = columnaExiste(con, "entradas", "precio_unitario");
        boolean tieneTipoEntrada = columnaExiste(con, "entradas", "tipo_entrada");
        boolean tieneEstado = columnaExiste(con, "entradas", "estado");

        StringBuilder columnas = new StringBuilder("id_venta");
        StringBuilder valores = new StringBuilder("?");

        if (tieneIdFuncion) {
            columnas.append(", id_funcion");
            valores.append(", ?");
        }

        columnas.append(", id_asiento");
        valores.append(", ?");

        if (tieneIdPrecio) {
            columnas.append(", id_precio");
            valores.append(", ?");
        }

        if (tieneCodigoEntrada) {
            columnas.append(", codigo_entrada");
            valores.append(", ?");
        }

        if (tienePrecioUnitario) {
            columnas.append(", precio_unitario");
            valores.append(", ?");
        }

        if (tieneTipoEntrada) {
            columnas.append(", tipo_entrada");
            valores.append(", ?");
        }

        if (tieneEstado) {
            columnas.append(", estado");
            valores.append(", ?");
        }

        String sqlVerificar = "SELECT COUNT(*) AS total FROM entradas " +
                "WHERE id_funcion = ? AND id_asiento = ? " +
                "AND (estado IS NULL OR estado NOT IN ('Anulada', 'Reembolsada'))";

        String sqlEntrada = "INSERT INTO entradas(" + columnas + ") VALUES (" + valores + ")";

        try (PreparedStatement psVerificar = con.prepareStatement(sqlVerificar);
             PreparedStatement psInsertar = con.prepareStatement(sqlEntrada)) {

            for (int indice = 0; indice < asientos.size(); indice++) {
                String asiento = asientos.get(indice);
                String tipoEntrada = obtenerTipoEntradaPorIndice(tiposEntrada, indice);
                int idPrecioEntrada =
                        obtenerIdPrecioPorTipo(con, tipoEntrada);

                double precioUnitario =
                        obtenerPrecioUnitarioPorIndice(
                                preciosUnitarios,
                                indice,
                                obtenerPrecioUnitarioPorTipo(
                                        con,
                                        tipoEntrada,
                                        unitarioGeneral
                                )
                        );

                int idAsiento = obtenerIdAsiento(con, idSala, asiento);

                if (idAsiento == -1) {
                    throw new SQLException("No se pudo obtener el asiento " + asiento);
                }

                psVerificar.clearParameters();
                psVerificar.setInt(1, idFuncion);
                psVerificar.setInt(2, idAsiento);

                try (ResultSet rs = psVerificar.executeQuery()) {
                    if (rs.next() && rs.getInt("total") > 0) {
                        throw new SQLException("El asiento " + asiento + " ya no se encuentra disponible.");
                    }
                }

                int i = 1;
                psInsertar.setInt(i++, idVenta);

                if (tieneIdFuncion) {
                    psInsertar.setInt(i++, idFuncion);
                }

                psInsertar.setInt(i++, idAsiento);

                if (tieneIdPrecio) {
                    psInsertar.setInt(i++, idPrecioEntrada > 0 ? idPrecioEntrada : idPrecio);
                }

                if (tieneCodigoEntrada) {
                    psInsertar.setString(i++, numeroVenta + "-" + asiento);
                }

                if (tienePrecioUnitario) {
                    psInsertar.setDouble(i++, precioUnitario > 0 ? precioUnitario : unitarioGeneral);
                }

                if (tieneTipoEntrada) {
                    psInsertar.setString(i++, tipoEntrada);
                }

                if (tieneEstado) {
                    psInsertar.setString(i++, "Emitida");
                }

                psInsertar.addBatch();
            }

            psInsertar.executeBatch();
        }

        /*
         * No actualizamos asientos.estado = 'Ocupado' aquí.
         * La ocupación correcta depende de la función, no del asiento global.
         * El sistema ya consulta la ocupación mediante la tabla entradas.
         */
    }


    private static String obtenerTipoEntradaPorIndice(List<String> tiposEntrada, int indice) {
        if (tiposEntrada == null || indice < 0 || indice >= tiposEntrada.size()) {
            return obtenerTipoEntradaPredeterminado();
        }

        return normalizarTipoEntradaInterno(tiposEntrada.get(indice));
    }

    private static double obtenerPrecioUnitarioPorIndice(
            List<Double> preciosUnitarios,
            int indice,
            double respaldo
    ) {
        if (preciosUnitarios == null
                || indice < 0
                || indice >= preciosUnitarios.size()
                || preciosUnitarios.get(indice) == null
                || preciosUnitarios.get(indice) <= 0) {
            return respaldo;
        }

        return preciosUnitarios.get(indice);
    }

    private static double obtenerPrecioUnitarioPorTipo(Connection con, String tipoEntrada, double respaldo) throws SQLException {
        String tipo = normalizarTipoEntradaInterno(tipoEntrada);
        String columnaPrecio = columnaExiste(con, "precios", "precio") ? "precio" : "monto";

        if (columnaExiste(con, "precios", "tipo_entrada")) {
            String sql = "SELECT " + columnaPrecio + " AS precio FROM precios "
                    + "WHERE LOWER(tipo_entrada) = LOWER(?) "
                    + "ORDER BY id_precio LIMIT 1";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
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

    /**
     * Registra únicamente el comprobante de una venta que ya fue guardada
     * al aprobarse el pago. No vuelve a insertar venta, pago ni entradas.
     */
    public static boolean registrarComprobanteVentaExistente(
            String numeroVenta,
            String codigoQR,
            double total
    ) {
        if (numeroVenta == null || numeroVenta.trim().isEmpty()) {
            return false;
        }

        Connection con = null;

        try {
            con = conectar();
            con.setAutoCommit(false);

            int idVenta = obtenerIdVentaPorNumero(
                    con,
                    numeroVenta.trim()
            );

            if (idVenta <= 0) {
                con.rollback();
                System.out.println(
                        "No existe la venta pagada " + numeroVenta
                );
                return false;
            }

            if (columnaExiste(con, "ventas", "qr_entrada")) {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE ventas SET qr_entrada = ? WHERE id_venta = ?"
                )) {
                    ps.setString(1, codigoQR == null ? "" : codigoQR);
                    ps.setInt(2, idVenta);
                    ps.executeUpdate();
                }
            }

            // Evita comprobantes duplicados si se repite la operación.
            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM comprobantes WHERE id_venta = ?"
            )) {
                ps.setInt(1, idVenta);
                ps.executeUpdate();
            }

            registrarComprobante(
                    con,
                    idVenta,
                    codigoQR == null ? "" : codigoQR,
                    total,
                    numeroVenta.trim()
            );

            con.commit();
            return true;

        } catch (Exception e) {
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException ignored) {
            }

            JOptionPaneBD.mostrarError(
                    "Error al registrar el comprobante",
                    e
            );
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

    private static void registrarComprobante(Connection con, int idVenta, String codigoQR, double total, String numeroVenta) throws SQLException {
        boolean tieneCodigoQR = columnaExiste(con, "comprobantes", "codigo_qr");
        boolean tieneTotal = columnaExiste(con, "comprobantes", "total");
        boolean tieneTipo = columnaExiste(con, "comprobantes", "tipo_comprobante");
        boolean tieneNumero = columnaExiste(con, "comprobantes", "numero_comprobante");
        boolean tieneFecha = columnaExiste(con, "comprobantes", "fecha_emision");

        if (tieneCodigoQR && tieneTotal) {
            String sql = "INSERT INTO comprobantes(id_venta, codigo_qr, total) VALUES (?, ?, ?)";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idVenta);
                ps.setString(2, codigoQR);
                ps.setDouble(3, total);
                ps.executeUpdate();
            }
            return;
        }

        if (tieneTipo && tieneNumero && tieneFecha) {
            String sql = "INSERT INTO comprobantes(id_venta, tipo_comprobante, numero_comprobante, fecha_emision) VALUES (?, 'Boleta', ?, NOW())";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idVenta);
                ps.setString(2, numeroVenta);
                ps.executeUpdate();
            }
            return;
        }

        String sql = "INSERT INTO comprobantes(id_venta) VALUES (?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            ps.executeUpdate();
        }
    }

    private static String normalizarMetodo(String metodo) {
        if (metodo == null) {
            return "Efectivo";
        }

        String valor = metodo.trim();

        if (valor.equalsIgnoreCase("Yape")) {
            return "Yape";
        }

        if (valor.equalsIgnoreCase("Plin")) {
            return "Plin";
        }

        if (valor.equalsIgnoreCase("Efectivo")) {
            return "Efectivo";
        }

        if (valor.equalsIgnoreCase("Tarjeta Débito") || valor.equalsIgnoreCase("Tarjeta Debito")) {
            return "Tarjeta Débito";
        }

        if (valor.equalsIgnoreCase("Tarjeta Crédito") || valor.equalsIgnoreCase("Tarjeta Credito")) {
            return "Tarjeta Crédito";
        }

        /*
         * Si llega una venta antigua o una interfaz vieja con el texto genérico "Tarjeta",
         * se guarda como crédito para evitar volver a insertar el valor genérico en la BD.
         */
        if (valor.equalsIgnoreCase("Tarjeta")) {
            return "Tarjeta Crédito";
        }

        return "Efectivo";
    }

    public static boolean registrarVentaConQR(String numeroVenta, String usuarioVendedor, double total, String metodoPago, String qrEntrada) {
        ArrayList<String> sinAsientos = new ArrayList<>();
        return registrarVentaConQRCompleta(numeroVenta, usuarioVendedor, "", "", sinAsientos, total, metodoPago, qrEntrada);
    }

        public static ArrayList<Object[]> listarVentasGerente() {
        ArrayList<Object[]> lista = new ArrayList<>();

        try (Connection con = conectar()) {
            String columnaFechaVenta = columnaExiste(con, "ventas", "fecha_venta") ? "fecha_venta" : "fecha_hora";

            String sql = "SELECT v.id_venta, v.numero_venta, DATE(v." + columnaFechaVenta + ") AS fecha, TIME(v." + columnaFechaVenta + ") AS hora, " +
                    "v.total, IFNULL(p.metodo_pago, '-') AS metodo, u.usuario AS vendedor, v.qr_entrada " +
                    "FROM ventas v " +
                    "INNER JOIN usuarios u ON v.id_usuario = u.id_usuario " +
                    "LEFT JOIN pagos p ON p.id_venta = v.id_venta " +
                    "ORDER BY v.id_venta DESC";

            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    String numeroVenta = rs.getString("numero_venta");

                    if (numeroVenta == null || numeroVenta.trim().isEmpty()) {
                        numeroVenta = "VTA-" + rs.getInt("id_venta");
                    }

                    lista.add(new Object[]{
                            numeroVenta,
                            rs.getDate("fecha"),
                            rs.getTime("hora"),
                            rs.getDouble("total"),
                            rs.getString("metodo"),
                            rs.getString("vendedor"),
                            rs.getString("qr_entrada")
                    });
                }
            }

        } catch (SQLException e) {
            JOptionPaneBD.mostrarError("Error al listar ventas", e);
        }

        return lista;
    }


    // ======================================================
    // FUNCIONES ADMIN
    // ======================================================

    public static ArrayList<Object[]> listarFunciones() {
        ArrayList<Object[]> lista = new ArrayList<>();

        String sql = "SELECT f.id_funcion, p.titulo, s.nombre AS sala, f.fecha, f.hora, f.estado " +
                "FROM funciones f " +
                "INNER JOIN peliculas p ON f.id_pelicula = p.id_pelicula " +
                "INNER JOIN salas s ON f.id_sala = s.id_sala " +
                "ORDER BY f.fecha DESC, f.hora DESC";

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(new Object[]{
                        rs.getInt("id_funcion"),
                        rs.getString("titulo"),
                        rs.getString("sala"),
                        rs.getDate("fecha"),
                        rs.getTime("hora"),
                        rs.getString("estado")
                });
            }

        } catch (SQLException e) {
            JOptionPaneBD.mostrarError("Error al listar funciones", e);
        }

        return lista;
    }

    public static boolean cancelarFuncion(int idFuncion) {
        String sql = "UPDATE funciones SET estado = 'Cancelada' WHERE id_funcion = ?";

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idFuncion);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            JOptionPaneBD.mostrarError("Error al cancelar función", e);
            return false;
        }
    }

    // ======================================================
    // USUARIOS ADMIN
    // ======================================================

    public static ArrayList<Object[]> listarUsuarios() {
        ArrayList<Object[]> lista = new ArrayList<>();

        String sql = "SELECT id_usuario, usuario, rol, estado FROM usuarios ORDER BY id_usuario DESC";

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(new Object[]{
                        rs.getInt("id_usuario"),
                        rs.getString("usuario"),
                        rs.getString("rol"),
                        rs.getString("estado")
                });
            }

        } catch (SQLException e) {
            JOptionPaneBD.mostrarError("Error al listar usuarios", e);
        }

        return lista;
    }

    public static boolean cambiarEstadoUsuario(int idUsuario, String nuevoEstado) {
        String sql = "UPDATE usuarios SET estado = ? WHERE id_usuario = ?";

        try (Connection con = conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nuevoEstado);
            ps.setInt(2, idUsuario);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            JOptionPaneBD.mostrarError("Error al cambiar estado de usuario", e);
            return false;
        }
    }
}

class JOptionPaneBD {

    
    private static final boolean MOSTRAR_POPUPS_BD = false;

    public static void mostrarError(String titulo, Exception e) {
        String detalle = e == null ? "" : e.getMessage();

        if (detalle == null) {
            detalle = "";
        }

        System.out.println("[BD CINEX] " + titulo + ": " + detalle);

        if (MOSTRAR_POPUPS_BD) {
            javax.swing.JOptionPane.showMessageDialog(
                    null,
                    titulo + "" + detalle,
                    "Base de datos CINEX",
                    javax.swing.JOptionPane.ERROR_MESSAGE
            );
        }
    }
}

