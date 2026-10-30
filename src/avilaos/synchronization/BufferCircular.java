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
    
    private String nombre;
    private Proceso[] buffer;
    private int entrada;
    private int salida;
    private int capacidad;
    
    private Semaforo mutex;
    private Semaforo items;
    private Semaforo espacios;
    private MetricasService metricas;

    /**
     * Constructor con nombre explícito del buffer.
     */
    public BufferCircular(String nombre, int capacidad, MetricasService metricas) {
        this.nombre = (nombre != null && !nombre.isEmpty()) ? nombre : "B1";
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
     * Constructor base de compatibilidad.
     */
    public BufferCircular(int capacidad, MetricasService metricas) {
        this("B1", capacidad, metricas);
    }

    /**
     * Inserta un nuevo proceso producido en el buffer circular.
     * @return true si produjo exitosamente; false si el semáforo 'espacios' bloqueó el proceso.
     */
    public boolean producir(Proceso proceso) {
        // Verifica si hay espacio libre en el semáforo de espacios
        if (espacios.getValor() <= 0) {
            return false; // El buffer está lleno: el proceso debe pasar a Cola Bloqueados
        }

        espacios.waitSemaforo();
        mutex.waitSemaforo();

        buffer[entrada] = proceso;
        entrada = (entrada + 1) % capacidad;

        if (metricas != null) {
            metricas.registrarProduccion();
        }

        mutex.signalSemaforo();
        items.signalSemaforo();
        
        return true;
    }

    /**
     * Extrae un proceso consumido del buffer circular pasando el proceso consumidor.
     * @return true si consumió exitosamente; false si el semáforo 'items' bloqueó el proceso.
     */
    public boolean consumir(Proceso proceso) {
        // Verifica si hay elementos disponibles en el semáforo de ítems
        if (items.getValor() <= 0) {
            return false; // El buffer está vacío: el proceso debe pasar a Cola Bloqueados
        }

        items.waitSemaforo();
        mutex.waitSemaforo();

        Proceso pExtraido = buffer[salida];
        buffer[salida] = null;
        salida = (salida + 1) % capacidad;

        if (metricas != null) {
            metricas.registrarConsumo();
        }

        mutex.signalSemaforo();
        espacios.signalSemaforo();

        return true;
    }

    /**
     * Sobrecarga de extracción directa.
     */
    public Proceso consumir() {
        if (items.getValor() <= 0) {
            return null;
        }
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

    // Getters y Setters
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public Semaforo getMutex() {
        return mutex;
    }

    public Semaforo getItems() {
        return items;
    }

    public Semaforo getEspacios() {
        return espacios;
    }
}