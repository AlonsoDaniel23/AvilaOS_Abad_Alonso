package avilaos.model;

import avilaos.enums.EstadoProceso;
import avilaos.enums.TipoProceso;

/**
 * Clase que representa un Proceso dentro del sistema operativo.
 * Almacena la informacion del Bloque de Control de Procesos (PCB),
 * jerarquia, estado de Stallings, deadline y Tabla de Paginas para Memoria Virtual.
 * 
 * @author Equipo AvilaOS
 * @version 1.4
 */
public class Proceso {
    
    private int id;
    private int idPadre;
    private String nombre;
    private EstadoProceso estado;
    private TipoProceso tipo;        // Tipo de proceso (CPU_BOUND, IO_BOUND, PRODUCTOR, CONSUMIDOR)
    private String nombreBuffer;     // Nombre del buffer si es Productor o Consumidor
    private int prioridad;
    private int deadline;            // Agregado para soporte del algoritmo EDF
    private int tiempoLlegada;       // Ciclo de reloj en el que ingresó el proceso
    private int tiempoCpuConsumido;
    private int tiempoEjecucion;     // Tiempo total requerido en CPU
    private EntradaTablaPaginas[] tablaPaginas;
    
    // Bandera para asegurar que la interacción con semáforos/buffers ocurra una sola vez por ejecución
    private boolean operacionBufferRealizada;

    /**
     * Constructor base.
     */
    public Proceso(int id, String nombre, int tiempoEjecucion) {
        this(id, 0, nombre, 1, tiempoEjecucion, 4, TipoProceso.CPU_BOUND, "");
    }

    /**
     * Constructor estándar para procesos CPU Bound o I/O Bound.
     */
    public Proceso(int id, int idPadre, String nombre, int prioridad, int tiempoEjecucion, int totalPaginas) {
        this(id, idPadre, nombre, prioridad, tiempoEjecucion, totalPaginas, TipoProceso.CPU_BOUND, "");
    }

    /**
     * Constructor completo para el simulador con soporte de Productor/Consumidor, Memoria y Jerarquía.
     */
    public Proceso(int id, int idPadre, String nombre, int prioridad, int tiempoEjecucion, int totalPaginas, TipoProceso tipo, String nombreBuffer) {
        this.id = id;
        this.idPadre = idPadre;
        this.nombre = nombre;
        this.estado = EstadoProceso.NUEVO;
        this.prioridad = prioridad;
        this.deadline = 0;
        this.tiempoLlegada = 0;
        this.tiempoCpuConsumido = 0;
        this.tiempoEjecucion = tiempoEjecucion;
        this.tipo = (tipo != null) ? tipo : TipoProceso.CPU_BOUND;
        this.nombreBuffer = (nombreBuffer != null) ? nombreBuffer : "";
        this.operacionBufferRealizada = false;

        // Inicialización estática de la Tabla de Páginas del proceso
        this.tablaPaginas = new EntradaTablaPaginas[totalPaginas];
        for (int i = 0; i < totalPaginas; i++) {
            this.tablaPaginas[i] = new EntradaTablaPaginas(i);
        }
    }

    /**
     * Incrementa en un ciclo la ejecución consumida por el proceso en CPU.
     */
    public void ejecutarCiclo() {
        this.tiempoCpuConsumido++;
    }

    /**
     * Evalúa si el proceso completó todos sus ciclos requeridos.
     * @return true si finalizó, false si aún requiere tiempo de CPU.
     */
    public boolean esFinalizado() {
        return tiempoCpuConsumido >= tiempoEjecucion;
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public int getIdPadre() {
        return idPadre;
    }

    public String getNombre() {
        return nombre;
    }

    public EstadoProceso getEstado() {
        return estado;
    }

    public void setEstado(EstadoProceso estado) {
        this.estado = estado;
    }

    public TipoProceso getTipo() {
        return tipo;
    }

    public void setTipo(TipoProceso tipo) {
        this.tipo = tipo;
    }

    public String getNombreBuffer() {
        return nombreBuffer;
    }

    public void setNombreBuffer(String nombreBuffer) {
        this.nombreBuffer = nombreBuffer;
    }

    public int getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(int prioridad) {
        this.prioridad = prioridad;
    }

    public int getDeadline() {
        return deadline;
    }

    public void setDeadline(int deadline) {
        this.deadline = deadline;
    }

    public int getTiempoLlegada() {
        return tiempoLlegada;
    }

    public void setTiempoLlegada(int tiempoLlegada) {
        this.tiempoLlegada = tiempoLlegada;
    }

    public int getTiempoCpuConsumido() {
        return tiempoCpuConsumido;
    }

    public int getTiempoEjecucion() {
        return tiempoEjecucion;
    }

    public void setTiempoEjecucion(int tiempoEjecucion) {
        this.tiempoEjecucion = tiempoEjecucion;
    }

    public EntradaTablaPaginas[] getTablaPaginas() {
        return tablaPaginas;
    }

    public boolean isOperacionBufferRealizada() {
        return operacionBufferRealizada;
    }

    public void setOperacionBufferRealizada(boolean operacionBufferRealizada) {
        this.operacionBufferRealizada = operacionBufferRealizada;
    }

    @Override
    public String toString() {
        return "Proceso{" + "id=" + id + ", nombre='" + nombre + '\'' + ", tipo=" + tipo + ", estado=" + estado + ", tiempo=" + tiempoEjecucion + '}';
    }
}