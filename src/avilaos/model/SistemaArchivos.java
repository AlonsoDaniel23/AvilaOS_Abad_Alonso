package avilaos.model;

import avilaos.datastructures.ListaEnlazada;
import avilaos.enums.ModoUsuario;
import avilaos.enums.PermisoArchivo;
import avilaos.services.EventLog;

/**
 * Administra el Sistema de Archivos Jerarquico de AvilaOS.
 * Controla el arbol de directorios y archivos sin colecciones de java.util,
 * ejecuta operaciones CRUD conectadas con la asignacion encadenada de bloques en
 * DiscoSimulado y valida permisos de acceso segun el propietario y ModoUsuario.
 * 
 * @author Gustavo Abad
 */
public class SistemaArchivos {

    private static SistemaArchivos instancia; // Instancia Singleton del sistema de archivos

    private Directorio raiz;                  // Directorio raiz "/" del arbol jerarquico
    private DiscoSimulado disco;              // Referencia al disco fisico donde se asignan los bloques
    private String usuarioActual;             // Nombre del usuario activo en la sesion actual
    private ModoUsuario modoUsuarioActual;    // Nivel de privilegio actual (ADMINISTRADOR o USUARIO)

    /**
     * Constructor que inicializa el sistema de archivos con su directorio raiz "/"
     * y lo vincula con el DiscoSimulado.
     * 
     * @param disco Instancia del DiscoSimulado donde se almacenaran los bloques.
     */
    public SistemaArchivos(DiscoSimulado disco) {
        this.disco = (disco != null) ? disco : DiscoSimulado.getInstancia();
        this.raiz = new Directorio("/", "admin", PermisoArchivo.PUBLICO, null);
        this.usuarioActual = "admin";
        this.modoUsuarioActual = ModoUsuario.ADMINISTRADOR;
        instancia = this;
    }

    /**
     * Retorna la instancia unica del Sistema de Archivos en AvilaOS.
     * 
     * @return Instancia actual de SistemaArchivos.
     */
    public static synchronized SistemaArchivos getInstancia() {
        if (instancia == null) {
            instancia = new SistemaArchivos(DiscoSimulado.getInstancia());
        }
        return instancia;
    }

    // ==================== NAVEGACION DE RUTAS ====================

    /**
     * Navega el arbol desde la raiz para localizar una entrada (archivo o directorio)
     * dada su ruta absoluta (ej. "/documentos/informe.pdf").
     * 
     * @param ruta Ruta absoluta separada por '/'.
     * @return La EntradaSistemaArchivos correspondiente, o null si no existe.
     */
    public EntradaSistemaArchivos buscarPorRuta(String ruta) {
        if (ruta == null || ruta.trim().isEmpty() || "/".equals(ruta.trim())) {
            return raiz;
        }

        // Separar los segmentos de la ruta usando arreglo nativo String[]
        String[] segmentos = ruta.trim().split("/");
        EntradaSistemaArchivos actual = raiz;

        for (int i = 0; i < segmentos.length; i++) {
            String seg = segmentos[i].trim();
            if (seg.isEmpty()) {
                continue;
            }
            if (!actual.esDirectorio()) {
                return null; // No se puede navegar dentro de un archivo
            }
            actual = ((Directorio) actual).buscarHijo(seg);
            if (actual == null) {
                return null;
            }
        }
        return actual;
    }

    // ==================== OPERACIONES CRUD ====================

    /**
     * CREATE (Directorio): Crea un nuevo subdirectorio dentro de la ruta padre indicada.
     * 
     * @param rutaPadre Ruta del directorio contenedor (ej. "/").
     * @param nombreDir Nombre del nuevo directorio.
     * @param permiso Nivel de acceso (PUBLICO o PRIVADO).
     * @return true si se creo exitosamente, false si hubo error de ruta, permisos o duplicado.
     */
    public boolean crearDirectorio(String rutaPadre, String nombreDir, PermisoArchivo permiso) {
        EntradaSistemaArchivos destino = buscarPorRuta(rutaPadre);
        if (destino == null || !destino.esDirectorio()) {
            return false;
        }

        Directorio dirPadre = (Directorio) destino;

        // Validar permiso de escritura en el directorio padre
        if (!dirPadre.puedeEscribir(usuarioActual, modoUsuarioActual)) {
            EventLog.getInstancia().registrar("Acceso denegado: '" + usuarioActual
                    + "' no tiene permiso para crear directorios en " + dirPadre.getRutaCompleta());
            return false;
        }

        Directorio nuevoDir = new Directorio(nombreDir, usuarioActual, permiso, dirPadre);
        boolean creado = dirPadre.agregarHijo(nuevoDir);

        if (creado) {
            EventLog.getInstancia().registrar("Directorio creado: " + nuevoDir.getRutaCompleta()
                    + " [Owner: " + usuarioActual + ", Permiso: " + nuevoDir.getPermiso() + "]");
        }
        return creado;
    }

    /**
     * CREATE (Archivo): Crea un nuevo archivo y reserva sus bloques encadenados en DiscoSimulado.
     * 
     * @param rutaPadre Ruta del directorio donde se creara el archivo.
     * @param nombreArchivo Nombre del archivo nuevo.
     * @param tamanoBloques Cantidad de bloques requeridos en disco.
     * @param permiso Nivel de acceso (PUBLICO o PRIVADO).
     * @param contenido Contenido inicial del archivo.
     * @return true si se creo y asigno bloques correctamente, false en caso contrario.
     */
    public boolean crearArchivo(String rutaPadre, String nombreArchivo, int tamanoBloques,
            PermisoArchivo permiso, String contenido) {
        EntradaSistemaArchivos destino = buscarPorRuta(rutaPadre);
        if (destino == null || !destino.esDirectorio()) {
            return false;
        }

        Directorio dirPadre = (Directorio) destino;

        // 1. Validar permiso de escritura sobre el directorio contenedor
        if (!dirPadre.puedeEscribir(usuarioActual, modoUsuarioActual)) {
            EventLog.getInstancia().registrar("Acceso denegado: '" + usuarioActual
                    + "' no puede crear archivos en " + dirPadre.getRutaCompleta());
            return false;
        }

        // 2. Evitar nombres duplicados antes de reservar bloques en disco
        if (dirPadre.buscarHijo(nombreArchivo) != null) {
            return false;
        }

        // 3. Solicitar asignacion encadenada de bloques en el DiscoSimulado
        int bloquesRequeridos = Math.max(1, tamanoBloques);
        int primerBloque = disco.asignarBloquesEncadenados(nombreArchivo, bloquesRequeridos);
        if (primerBloque == -1) {
            EventLog.getInstancia().registrar("Error de almacenamiento: Espacio insuficiente en disco para '"
                    + nombreArchivo + "' (" + bloquesRequeridos + " bloques).");
            return false;
        }

        // 4. Crear nodo Archivo e insertarlo en el arbol jerarquico
        Archivo nuevoArchivo = new Archivo(nombreArchivo, usuarioActual, permiso,
                dirPadre, bloquesRequeridos, primerBloque);
        nuevoArchivo.setContenido(contenido);
        dirPadre.agregarHijo(nuevoArchivo);

        EventLog.getInstancia().registrar("Archivo creado: " + nuevoArchivo.getRutaCompleta()
                + " (" + bloquesRequeridos + " bloques, inicio: #" + primerBloque + ")");
        return true;
    }

    /**
     * READ: Lee y retorna un archivo si el usuario actual posee permisos de lectura.
     * 
     * @param rutaArchivo Ruta completa del archivo a consultar.
     * @return El objeto Archivo si existe y tiene permiso, o null si no existe o fue denegado.
     */
    public Archivo leerArchivo(String rutaArchivo) {
        EntradaSistemaArchivos entrada = buscarPorRuta(rutaArchivo);
        if (entrada == null || entrada.esDirectorio()) {
            return null;
        }

        if (!entrada.puedeLeer(usuarioActual, modoUsuarioActual)) {
            EventLog.getInstancia().registrar("Acceso denegado (READ): '" + usuarioActual
                    + "' intento leer archivo privado " + rutaArchivo);
            return null;
        }

        return (Archivo) entrada;
    }

    /**
     * UPDATE (Renombrar): Cambia el nombre de un archivo o directorio validando permisos.
     * 
     * @param rutaEntrada Ruta actual del archivo o directorio.
     * @param nuevoNombre Nuevo nombre a asignar.
     * @return true si se renombro con exito, false si no existe, no hay permisos o el nombre ya existe.
     */
    public boolean renombrarEntrada(String rutaEntrada, String nuevoNombre) {
        if (nuevoNombre == null || nuevoNombre.trim().isEmpty() || "/".equals(rutaEntrada)) {
            return false;
        }

        EntradaSistemaArchivos entrada = buscarPorRuta(rutaEntrada);
        if (entrada == null || entrada.getPadre() == null) {
            return false;
        }

        if (!entrada.puedeEscribir(usuarioActual, modoUsuarioActual)) {
            EventLog.getInstancia().registrar("Acceso denegado (UPDATE): '" + usuarioActual
                    + "' no puede renombrar " + rutaEntrada);
            return false;
        }

        // Verificar que no exista otra entrada con el nuevo nombre en el mismo directorio
        if (entrada.getPadre().buscarHijo(nuevoNombre) != null) {
            return false;
        }

        String rutaAnterior = entrada.getRutaCompleta();
        entrada.setNombre(nuevoNombre);
        EventLog.getInstancia().registrar("Renombrado: " + rutaAnterior + " -> " + entrada.getRutaCompleta());
        return true;
    }

    /**
     * UPDATE (Contenido): Actualiza el contenido textual de un archivo validando permisos de escritura.
     * 
     * @param rutaArchivo Ruta completa del archivo.
     * @param nuevoContenido Nuevo texto a guardar en el archivo.
     * @return true si se actualizo correctamente, false en caso contrario.
     */
    public boolean actualizarContenidoArchivo(String rutaArchivo, String nuevoContenido) {
        EntradaSistemaArchivos entrada = buscarPorRuta(rutaArchivo);
        if (entrada == null || entrada.esDirectorio()) {
            return false;
        }

        if (!entrada.puedeEscribir(usuarioActual, modoUsuarioActual)) {
            EventLog.getInstancia().registrar("Acceso denegado (UPDATE): '" + usuarioActual
                    + "' no puede modificar " + rutaArchivo);
            return false;
        }

        ((Archivo) entrada).setContenido(nuevoContenido);
        EventLog.getInstancia().registrar("Archivo actualizado: " + rutaArchivo);
        return true;
    }

    /**
     * DELETE: Elimina un archivo o directorio (recursivamente) y libera todos sus bloques
     * encadenados en el DiscoSimulado.
     * 
     * @param rutaEntrada Ruta completa del recurso a eliminar.
     * @return true si se elimino correctamente, false si es la raiz, no existe o no hay permisos.
     */
    public boolean eliminarEntrada(String rutaEntrada) {
        if (rutaEntrada == null || "/".equals(rutaEntrada.trim())) {
            return false; // No se permite eliminar el directorio raiz "/"
        }

        EntradaSistemaArchivos entrada = buscarPorRuta(rutaEntrada);
        if (entrada == null || entrada.getPadre() == null) {
            return false;
        }

        // Validar permiso de escritura/eliminacion
        if (!entrada.puedeEscribir(usuarioActual, modoUsuarioActual)) {
            EventLog.getInstancia().registrar("Acceso denegado (DELETE): '" + usuarioActual
                    + "' no tiene permiso para eliminar " + rutaEntrada);
            return false;
        }

        // Liberar bloques en disco (de forma recursiva si es un directorio)
        liberarBloquesRecursivo(entrada);

        // Desvincular del directorio padre
        boolean eliminado = entrada.getPadre().eliminarHijo(entrada.getNombre());
        if (eliminado) {
            EventLog.getInstancia().registrar("Eliminado del sistema de archivos: " + rutaEntrada);
        }
        return eliminado;
    }

    /**
     * Recorre una entrada y, si es directorio, libera recursivamente los bloques
     * de todos sus archivos y subdirectorios contenidos.
     * 
     * @param entrada Archivo o directorio cuyos bloques seran liberados.
     */
    private void liberarBloquesRecursivo(EntradaSistemaArchivos entrada) {
        if (entrada == null) {
            return;
        }
        if (!entrada.esDirectorio()) {
            Archivo arch = (Archivo) entrada;
            disco.liberarCadenaBloques(arch.getPrimerBloque());
        } else {
            Directorio dir = (Directorio) entrada;
            ListaEnlazada hijos = dir.getHijos();
            for (int i = 0; i < hijos.getTamaño(); i++) {
                EntradaSistemaArchivos hijo = (EntradaSistemaArchivos) hijos.obtener(i);
                liberarBloquesRecursivo(hijo);
            }
        }
    }

    /**
     * Lista las entradas visibles de un directorio para el usuario actual.
     * 
     * @param rutaDirectorio Ruta del directorio a listar.
     * @return ListaEnlazada con las EntradaSistemaArchivos que el usuario tiene permiso de ver.
     */
    public ListaEnlazada listarDirectorio(String rutaDirectorio) {
        ListaEnlazada visibles = new ListaEnlazada();
        EntradaSistemaArchivos entrada = buscarPorRuta(rutaDirectorio);
        if (entrada == null || !entrada.esDirectorio()) {
            return visibles;
        }

        Directorio dir = (Directorio) entrada;
        ListaEnlazada hijos = dir.getHijos();
        for (int i = 0; i < hijos.getTamaño(); i++) {
            EntradaSistemaArchivos hijo = (EntradaSistemaArchivos) hijos.obtener(i);
            if (hijo != null && hijo.puedeLeer(usuarioActual, modoUsuarioActual)) {
                visibles.insertarFinal(hijo);
            }
        }
        return visibles;
    }

    /**
     * Cambia la sesion activa (nombre de usuario y rol ADMINISTRADOR/USUARIO).
     * 
     * @param usuario Nombre del usuario que inicia sesion.
     * @param modo Nivel de privilegio del usuario.
     */
    public void iniciarSesion(String usuario, ModoUsuario modo) {
        this.usuarioActual = (usuario != null && !usuario.trim().isEmpty()) ? usuario.trim() : "usuario";
        this.modoUsuarioActual = (modo != null) ? modo : ModoUsuario.USUARIO;
    }

    /**
     * Reinicia el arbol de archivos a un directorio raiz vacio (util al cargar un JSON nuevo).
     */
    public void reiniciarArbol() {
        this.raiz = new Directorio("/", "admin", PermisoArchivo.PUBLICO, null);
    }

    // ==================== GETTERS Y SETTERS ====================

    public Directorio getRaiz() {
        return raiz;
    }

    public DiscoSimulado getDisco() {
        return disco;
    }

    public String getUsuarioActual() {
        return usuarioActual;
    }

    public ModoUsuario getModoUsuarioActual() {
        return modoUsuarioActual;
    }
}