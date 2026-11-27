package avilaos.model;

import avilaos.enums.ModoUsuario;
import avilaos.enums.PermisoArchivo;

/**
 * Clase base abstracta para los elementos del arbol del sistema de archivos
 * (Patron Composite). Define los atributos comunes a archivos y directorios:
 * nombre, propietario, nivel de permisos (publico/privado) y referencia al
 * directorio padre.
 * 
 * @author Gustavo Abad
 */
public abstract class EntradaSistemaArchivos {

    protected String nombre;              // Nombre del archivo o directorio
    protected String propietario;         // Nombre del usuario dueno del recurso
    protected PermisoArchivo permiso;     // Nivel de acceso (PUBLICO o PRIVADO)
    protected Directorio padre;           // Referencia al directorio contenedor (null si es la raiz)

    /**
     * Constructor base para inicializar una entrada en el arbol de directorios.
     * 
     * @param nombre Nombre identificador del archivo o directorio.
     * @param propietario Usuario creador o dueno del recurso.
     * @param permiso Nivel de acceso (PUBLICO o PRIVADO).
     * @param padre Directorio padre que contiene esta entrada.
     */
    public EntradaSistemaArchivos(String nombre, String propietario, PermisoArchivo permiso, Directorio padre) {
        this.nombre = (nombre != null && !nombre.trim().isEmpty()) ? nombre.trim() : "sin_nombre";
        this.propietario = (propietario != null && !propietario.trim().isEmpty()) ? propietario.trim() : "admin";
        this.permiso = (permiso != null) ? permiso : PermisoArchivo.PUBLICO;
        this.padre = padre;
    }

    /**
     * Valida si un usuario tiene permiso de lectura sobre esta entrada.
     * Los recursos PUBLICO pueden ser leidos por cualquiera; los PRIVADO solo por su dueno o ADMINISTRADOR.
     * 
     * @param usuarioActual Nombre del usuario que solicita la lectura.
     * @param modoActual Modo de sesion (ADMINISTRADOR o USUARIO).
     * @return true si tiene permiso de lectura, false en caso contrario.
     */
    public boolean puedeLeer(String usuarioActual, ModoUsuario modoActual) {
        if (modoActual == ModoUsuario.ADMINISTRADOR) {
            return true;
        }
        if (this.permiso == PermisoArchivo.PUBLICO) {
            return true;
        }
        return this.propietario.equalsIgnoreCase(usuarioActual);
    }

    /**
     * Valida si un usuario tiene permiso de escritura, modificacion o eliminacion.
     * Solo el propietario del recurso o un ADMINISTRADOR pueden modificarlo.
     * 
     * @param usuarioActual Nombre del usuario que solicita la modificacion.
     * @param modoActual Modo de sesion (ADMINISTRADOR o USUARIO).
     * @return true si tiene permiso de escritura/eliminacion, false en caso contrario.
     */
    public boolean puedeEscribir(String usuarioActual, ModoUsuario modoActual) {
        if (modoActual == ModoUsuario.ADMINISTRADOR) {
            return true;
        }
        return this.propietario.equalsIgnoreCase(usuarioActual);
    }

    /**
     * Construye y retorna la ruta absoluta desde la raiz hasta esta entrada (ej. "/docs/tarea.txt").
     * 
     * @return Cadena con la ruta completa en el sistema de archivos.
     */
    public String getRutaCompleta() {
        if (padre == null) {
            return "/";
        }
        String rutaPadre = padre.getRutaCompleta();
        if ("/".equals(rutaPadre)) {
            return "/" + nombre;
        }
        return rutaPadre + "/" + nombre;
    }

    /**
     * Indica si la entrada corresponde a un directorio (true) o a un archivo (false).
     * 
     * @return true si es un directorio, false si es un archivo.
     */
    public abstract boolean esDirectorio();

    /**
     * Retorna el tamano total en bloques ocupado por esta entrada (o la suma de sus hijos si es directorio).
     * 
     * @return Cantidad de bloques ocupados en disco.
     */
    public abstract int getTamanoBloques();

    // ==================== GETTERS Y SETTERS ====================

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        if (nombre != null && !nombre.trim().isEmpty()) {
            this.nombre = nombre.trim();
        }
    }

    public String getPropietario() {
        return propietario;
    }

    public void setPropietario(String propietario) {
        this.propietario = propietario;
    }

    public PermisoArchivo getPermiso() {
        return permiso;
    }

    public void setPermiso(PermisoArchivo permiso) {
        this.permiso = permiso;
    }

    public Directorio getPadre() {
        return padre;
    }

    public void setPadre(Directorio padre) {
        this.padre = padre;
    }
    
    @Override
    public String toString() {
        return (nombre != null && !nombre.isEmpty()) ? nombre : "/";
    }
}