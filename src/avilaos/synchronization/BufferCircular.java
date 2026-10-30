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
    private int idComputadorAnfitrion; // <--- ID del nodo anfitrión donde reside el buffer
    
    private Semaforo mutex;
    private Semaforo items;
    private Semaforo espacios;
    private MetricasService metricas;

    /**
     * Constructor completo especificando nodo anfitrión.
     */
    public BufferCircular(String nombre, int capacidad, int idComputadorAnfitrion, MetricasService metricas) {
        this.nombre = (nombre != null && !nombre.isEmpty()) ? nombre : "B1";
        this.capacidad = capacidad;
        this.idComputadorAnfitrion = idComputadorAnfitrion;
        this.buffer = new Proceso[capacidad];
        this.entrada = 0;
        this.salida = 0;
        this.metricas = metricas;
        
        this.mutex = new Semaforo(1);
        this.items = new Semaforo(0);
        this.espacios = new Semaforo(capacidad);
    }

    /**
     * Constructor con nombre explícito del buffer (asigna Nodo 1 por defecto).
     */
    public BufferCircular(String nombre, int capacidad, MetricasService metricas) {
        this(nombre, capacidad, 1, metricas);
    }

    /**
     * Constructor base de compatibilidad.
     */
    public BufferCircular(int capacidad, MetricasService metricas) {
        this("B1", capacidad, 1, metricas);
    }

    public boolean producir(Proceso proceso) {
        if (espacios.getValor() <= 0) {
            return false;
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

    public boolean consumir(Proceso proceso) {
        if (items.getValor() <= 0) {
            return false;
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
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public int getCapacidad() { return capacidad; }
    public int getIdComputadorAnfitrion() { return idComputadorAnfitrion; }
    public void setIdComputadorAnfitrion(int idComputadorAnfitrion) { this.idComputadorAnfitrion = idComputadorAnfitrion; }
    public Semaforo getMutex() { return mutex; }
    public Semaforo getItems() { return items; }
    public Semaforo getEspacios() { return espacios; }
}