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

    private static SistemaArchivos instancia;

    private Directorio raiz;
    private DiscoSimulado disco;
    private String usuarioActual;
    private ModoUsuario modoUsuarioActual;

    public SistemaArchivos(DiscoSimulado disco) {
        this.disco = (disco != null) ? disco : DiscoSimulado.getInstancia();
        this.raiz = new Directorio("/", "admin", PermisoArchivo.PUBLICO, null);
        this.usuarioActual = "admin";
        this.modoUsuarioActual = ModoUsuario.ADMINISTRADOR;
        instancia = this;
    }

    public static synchronized SistemaArchivos getInstancia() {
        if (instancia == null) {
            instancia = new SistemaArchivos(DiscoSimulado.getInstancia());
        }
        return instancia;
    }

    // ==================== NAVEGACION DE RUTAS ====================

    public EntradaSistemaArchivos buscarPorRuta(String ruta) {
        if (ruta == null || ruta.trim().isEmpty() || "/".equals(ruta.trim())) {
            return raiz;
        }

        String[] segmentos = ruta.trim().split("/");
        EntradaSistemaArchivos actual = raiz;

        for (int i = 0; i < segmentos.length; i++) {
            String seg = segmentos[i].trim();
            if (seg.isEmpty()) {
                continue;
            }
            if (!actual.esDirectorio()) {
                return null;
            }
            actual = ((Directorio) actual).buscarHijo(seg);
            if (actual == null) {
                return null;
            }
        }
        return actual;
    }

    // ==================== OPERACIONES CRUD ====================

    public boolean crearDirectorio(String rutaPadre, String nombreDir, PermisoArchivo permiso) {
        EntradaSistemaArchivos destino = buscarPorRuta(rutaPadre);
        if (destino == null || !destino.esDirectorio()) {
            return false;
        }

        Directorio dirPadre = (Directorio) destino;

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

    public boolean crearArchivo(String rutaPadre, String nombreArchivo, int tamanoBloques,
            PermisoArchivo permiso, String contenido) {
        return crearArchivo(rutaPadre, nombreArchivo, tamanoBloques, permiso, contenido, "Sistema");
    }

    public boolean crearArchivo(String rutaPadre, String nombreArchivo, int tamanoBloques,
            PermisoArchivo permiso, String contenido, String procesoCreador) {
        EntradaSistemaArchivos destino = buscarPorRuta(rutaPadre);
        if (destino == null || !destino.esDirectorio()) {
            return false;
        }

        Directorio dirPadre = (Directorio) destino;

        if (!dirPadre.puedeEscribir(usuarioActual, modoUsuarioActual)) {
            EventLog.getInstancia().registrar("Acceso denegado: '" + usuarioActual
                    + "' no puede crear archivos en " + dirPadre.getRutaCompleta());
            return false;
        }

        if (dirPadre.buscarHijo(nombreArchivo) != null) {
            return false;
        }

        int bloquesRequeridos = Math.max(1, tamanoBloques);
        int primerBloque = disco.asignarBloquesEncadenados(nombreArchivo, bloquesRequeridos);
        if (primerBloque == -1) {
            EventLog.getInstancia().registrar("Error de almacenamiento: Espacio insuficiente en disco para '"
                    + nombreArchivo + "' (" + bloquesRequeridos + " bloques).");
            return false;
        }

        Archivo nuevoArchivo = new Archivo(nombreArchivo, usuarioActual, permiso,
                dirPadre, bloquesRequeridos, primerBloque, procesoCreador);
        nuevoArchivo.setContenido(contenido);
        disco.asignarColorACadena(primerBloque, nuevoArchivo.getColorDistintivo());
        dirPadre.agregarHijo(nuevoArchivo);

        EventLog.getInstancia().registrar("Archivo creado: " + nuevoArchivo.getRutaCompleta()
                + " (" + bloquesRequeridos + " bloques, inicio: #" + primerBloque
                + ", creador: " + nuevoArchivo.getProcesoCreador() + ")");
        return true;
    }

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

        if (entrada.getPadre().buscarHijo(nuevoNombre) != null) {
            return false;
        }

        String rutaAnterior = entrada.getRutaCompleta();
        entrada.setNombre(nuevoNombre);
        EventLog.getInstancia().registrar("Renombrado: " + rutaAnterior + " -> " + entrada.getRutaCompleta());
        return true;
    }

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

    public boolean eliminarEntrada(String rutaEntrada) {
        if (rutaEntrada == null || "/".equals(rutaEntrada.trim())) {
            return false;
        }

        EntradaSistemaArchivos entrada = buscarPorRuta(rutaEntrada);
        if (entrada == null || entrada.getPadre() == null) {
            return false;
        }

        if (!entrada.puedeEscribir(usuarioActual, modoUsuarioActual)) {
            EventLog.getInstancia().registrar("Acceso denegado (DELETE): '" + usuarioActual
                    + "' no tiene permiso para eliminar " + rutaEntrada);
            return false;
        }

        liberarBloquesRecursivo(entrada);

        boolean eliminado = entrada.getPadre().eliminarHijo(entrada.getNombre());
        if (eliminado) {
            EventLog.getInstancia().registrar("Eliminado del sistema de archivos: " + rutaEntrada);
        }
        return eliminado;
    }

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

    public void iniciarSesion(String usuario, ModoUsuario modo) {
        this.usuarioActual = (usuario != null && !usuario.trim().isEmpty()) ? usuario.trim() : "usuario";
        this.modoUsuarioActual = (modo != null) ? modo : ModoUsuario.USUARIO;
    }

    public void reiniciarArbol() {
        this.raiz = new Directorio("/", "admin", PermisoArchivo.PUBLICO, null);
    }

    // ==================== GETTERS Y SETTERS ====================

    public Directorio getRaiz() { return raiz; }
    public DiscoSimulado getDisco() { return disco; }
    public String getUsuarioActual() { return usuarioActual; }
    public ModoUsuario getModoUsuarioActual() { return modoUsuarioActual; }
}