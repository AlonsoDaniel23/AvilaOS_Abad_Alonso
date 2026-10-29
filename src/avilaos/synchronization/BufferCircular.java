package avilaos.synchronization;

import avilaos.model.Proceso;
import avilaos.services.MetricasService;

/**
 * Buffer circular acotado que implementa el patron Productor-Consumidor
 * utilizando semaforos e integrando las metricas globales del sistema.
 * 
 * @author Gustavo Abad
 */
public class BufferCircular {
    
    private Proceso[] buffer;
    private int entrada;
    private int salida;
    private int capacidad;
    
    private Semaforo mutex;
    private Semaforo items;
    private Semaforo espacios;
    private MetricasService metricas;

    /**
     * Constructor para inicializar el buffer acotado con sincronizacion por semaforos.
     * 
     * @param capacidad Tamaño maximo del buffer.
     * @param metricas Instancia global de MetricasService para auditoria.
     */
    public BufferCircular(int capacidad, MetricasService metricas) {
        this.capacidad = capacidad;
        this.buffer = new Proceso[capacidad];
        this.entrada = 0;
        this.salida = 0;
        this.metricas = metricas;
        
        this.mutex = new Semaforo(1);
        this.items = new Semaforo(0);
        this.espacios = new Semaforo(capacidad);
    }

    /**
     * Inserta un nuevo proceso producido en el buffer circular.
     * 
     * @param proceso Proceso a encolar.
     */
    public void producir(Proceso proceso) {
        espacios.waitSemaforo();
        mutex.waitSemaforo();

        buffer[entrada] = proceso;
        entrada = (entrada + 1) % capacidad;

        if (metricas != null) {
            metricas.registrarProduccion();
        }

        mutex.signalSemaforo();
        items.signalSemaforo();
    }

    /**
     * Extrae un proceso consumido del buffer circular.
     * 
     * @return El proceso consumido.
     */
    public Proceso consumir() {
        items.waitSemaforo();
        mutex.waitSemaforo();

        Proceso proceso = buffer[salida];
        buffer[salida] = null;
        salida = (salida + 1) % capacidad;

        if (metricas != null) {
            metricas.registrarConsumo();
        }

        mutex.signalSemaforo();
        espacios.signalSemaforo();

        return proceso;
    }

    public int getCapacidad() {
        return capacidad;
    }
}