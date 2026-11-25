package avilaos.model;

import avilaos.enums.PermisoArchivo;

/**
 * Representa un archivo dentro del sistema de archivos jerarquico de AvilaOS.
 * Almacena su tamano en bloques, el indice del primer bloque asignado en el
 * DiscoSimulado (para asignacion encadenada) y su contenido textual.
 * 
 * @author Gustavo Abad
 */
public class Archivo extends EntradaSistemaArchivos {

    private int tamanoBloques;     // Cantidad de bloques fisicos que ocupa el archivo en disco
    private int primerBloque;      // Indice del bloque inicial en DiscoSimulado (-1 si no tiene bloques)
    private String contenido;      // Contenido de datos o texto almacenado en el archivo

    /**
     * Constructor que crea un archivo con sus metadatos y bloque inicial asignado.
     * 
     * @param nombre Nombre del archivo (ej. "notas.txt").
     * @param propietario Usuario propietario del archivo.
     * @param permiso Nivel de acceso (PUBLICO o PRIVADO).
     * @param padre Directorio donde se aloja el archivo.
     * @param tamanoBloques Cantidad de bloques que ocupa en disco.
     * @param primerBloque Indice del primer bloque en la cadena del DiscoSimulado.
     */
    public Archivo(String nombre, String propietario, PermisoArchivo permiso,
            Directorio padre, int tamanoBloques, int primerBloque) {
        super(nombre, propietario, permiso, padre);
        this.tamanoBloques = Math.max(1, tamanoBloques);
        this.primerBloque = primerBloque;
        this.contenido = "";
    }

    @Override
    public boolean esDirectorio() {
        return false;
    }

    @Override
    public int getTamanoBloques() {
        return tamanoBloques;
    }

    // ==================== GETTERS Y SETTERS ====================

    public void setTamanoBloques(int tamanoBloques) {
        this.tamanoBloques = Math.max(1, tamanoBloques);
    }

    public int getPrimerBloque() {
        return primerBloque;
    }

    public void setPrimerBloque(int primerBloque) {
        this.primerBloque = primerBloque;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = (contenido != null) ? contenido : "";
    }
}