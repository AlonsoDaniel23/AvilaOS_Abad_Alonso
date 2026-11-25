package avilaos.model;

import avilaos.enums.TipoOperacionES;

/**
 * Modela una solicitud de Entrada/Salida (E/S) generada por un proceso hacia el Disco Simulado.
 * Almacena la operacion solicitada, la ruta del recurso, el bloque objetivo y el ciclo de llegada
 * para el calculo de metricas de espera en cola.
 * 
 * @author Gustavo Abad
 */
public class SolicitudDisco {

    private final Proceso proceso;              // Proceso que origino la solicitud de E/S
    private final TipoOperacionES operacion;    // Tipo de operacion (CREAR, LEER, ACTUALIZAR, ELIMINAR, SUSPENDER, REACTIVAR)
    private final String rutaArchivo;           // Ruta del archivo o directorio involucrado
    private final int bloqueObjetivo;           // Bloque inicial al que debe desplazarse el cabezal
    private final int cantidadBloques;          // Cantidad de bloques a leer o escribir (1 ciclo por bloque)
    private final int cicloLlegada;             // Ciclo del reloj global en el que la solicitud entro a la cola

    /**
     * Constructor que inicializa una nueva solicitud de disco.
     * 
     * @param proceso Proceso solicitante.
     * @param operacion Tipo de operacion de E/S a ejecutar.
     * @param rutaArchivo Ruta del archivo en el sistema de archivos.
     * @param bloqueObjetivo Bloque fisico destino en el disco.
     * @param cantidadBloques Cantidad de bloques afectados por la transferencia.
     * @param cicloLlegada Ciclo actual del reloj al momento de encolar.
     */
    public SolicitudDisco(Proceso proceso, TipoOperacionES operacion, String rutaArchivo,
            int bloqueObjetivo, int cantidadBloques, int cicloLlegada) {
        this.proceso = proceso;
        this.operacion = operacion;
        this.rutaArchivo = (rutaArchivo != null) ? rutaArchivo : "";
        this.bloqueObjetivo = bloqueObjetivo;
        this.cantidadBloques = Math.max(1, cantidadBloques);
        this.cicloLlegada = cicloLlegada;
    }

    // ==================== GETTERS ====================

    public Proceso getProceso() {
        return proceso;
    }

    public TipoOperacionES getOperacion() {
        return operacion;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public int getBloqueObjetivo() {
        return bloqueObjetivo;
    }

    public int getCantidadBloques() {
        return cantidadBloques;
    }

    public int getCicloLlegada() {
        return cicloLlegada;
    }
}