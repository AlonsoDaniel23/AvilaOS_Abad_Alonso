package avilaos.model;

/**
 * Representa un bloque fisico individual dentro del Disco Simulado (SD).
 * Soporta asignacion encadenada mediante un puntero al siguiente bloque del archivo
 * y permite reservar bloques dedicados al area de intercambio (Swap).
 * 
 * @author Gustavo Abad
 */
public class BloqueDisco {

    private final int numeroBloque; // Indice o direccion fisica del bloque en el disco
    private boolean ocupado;        // Indica si el bloque contiene datos activos
    private boolean esAreaSwap;     // Indica si el bloque pertenece a la particion de Swap
    private String propietario;     // Nombre del archivo dueno o identificador del proceso en Swap
    private int idProcesoSwap;      // ID del proceso suspendido que ocupa el bloque (-1 si no aplica)
    private int siguienteBloque;    // Puntero al siguiente bloque en asignacion encadenada (-1 si es el fin)

    /**
     * Constructor que inicializa un bloque libre en el disco.
     * 
     * @param numeroBloque Posicion numerica del bloque dentro del disco.
     * @param esAreaSwap true si este bloque forma parte del area reservada para Swap.
     */
    public BloqueDisco(int numeroBloque, boolean esAreaSwap) {
        this.numeroBloque = numeroBloque;
        this.esAreaSwap = esAreaSwap;
        this.ocupado = false;
        this.propietario = "";
        this.idProcesoSwap = -1;
        this.siguienteBloque = -1;
    }

    /**
     * Marca el bloque como ocupado por un archivo y enlaza el siguiente bloque de la cadena.
     * 
     * @param nombreArchivo Nombre del archivo al que pertenece el bloque.
     * @param siguienteBloque Indice del siguiente bloque encadenado (-1 si es el ultimo).
     */
    public void asignarArchivo(String nombreArchivo, int siguienteBloque) {
        this.ocupado = true;
        this.propietario = (nombreArchivo != null) ? nombreArchivo : "Archivo";
        this.idProcesoSwap = -1;
        this.siguienteBloque = siguienteBloque;
    }

    /**
     * Marca el bloque como ocupado por un proceso suspendido en el area de Swap.
     * 
     * @param idProceso Identificador unico del proceso suspendido.
     * @param nombreProceso Nombre descriptivo del proceso suspendido.
     */
    public void asignarSwap(int idProceso, String nombreProceso) {
        this.ocupado = true;
        this.idProcesoSwap = idProceso;
        this.propietario = "SWAP:" + ((nombreProceso != null) ? nombreProceso : ("P" + idProceso));
        this.siguienteBloque = -1;
    }

    /**
     * Libera el bloque, limpiando su propietario y rompiendo cualquier enlace previo.
     */
    public void liberar() {
        this.ocupado = false;
        this.propietario = "";
        this.idProcesoSwap = -1;
        this.siguienteBloque = -1;
    }

    // ==================== GETTERS Y SETTERS ====================

    public int getNumeroBloque() {
        return numeroBloque;
    }

    public boolean isOcupado() {
        return ocupado;
    }

    public boolean isEsAreaSwap() {
        return esAreaSwap;
    }

    public void setEsAreaSwap(boolean esAreaSwap) {
        this.esAreaSwap = esAreaSwap;
    }

    public String getPropietario() {
        return propietario;
    }

    public int getIdProcesoSwap() {
        return idProcesoSwap;
    }

    public int getSiguienteBloque() {
        return siguienteBloque;
    }

    public void setSiguienteBloque(int siguienteBloque) {
        this.siguienteBloque = siguienteBloque;
    }
}