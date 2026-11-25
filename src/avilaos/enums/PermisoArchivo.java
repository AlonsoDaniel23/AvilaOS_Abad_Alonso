package avilaos.enums;

/**
 * Define los niveles de visibilidad y acceso para archivos y directorios
 * dentro del sistema de archivos jerarquico de AvilaOS.
 * 
 * @author Gustavo Abad
 */
public enum PermisoArchivo {
    PUBLICO,  // Cualquier usuario puede leer o acceder al recurso
    PRIVADO   // Solo el propietario del recurso o un ADMINISTRADOR puede acceder o modificarlo
}