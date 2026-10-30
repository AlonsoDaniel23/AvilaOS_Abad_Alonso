package avilaos.model;

import avilaos.enums.EstadoProceso;

/**
 * Clase que representa un Proceso dentro del sistema operativo.
 * Almacena la informacion del Bloque de Control de Procesos (PCB),
 * jerarquia, estado de Stallings, deadline y Tabla de Paginas para Memoria Virtual.
 * 
 * @author Equipo AvilaOS
 * @version 1.3
 */
public class Proceso {
    
    private int id;
    private int idPadre;
    private String nombre;
    private EstadoProceso estado;
    private int prioridad;
    private int deadline; // Agregado para soporte del algoritmo EDF
    private int tiempoLlegada; // Ciclo de reloj en el que ingresó el proceso
    private int tiempoCpuConsumido;
    private int tiempoEjecucion; // Tiempo total requerido en CPU
    private EntradaTablaPaginas[] tablaPaginas;

    /**
     * Constructor base.
     */
    public Proceso(int id, String nombre, int tiempoEjecucion) {
        this(id, 0, nombre, 1, tiempoEjecucion, 4); // 4 paginas por defecto
    }

    /**
     * Constructor completo para el simulador con soporte de Memoria Virtual y Jerarquia.
     */
    public Proceso(int id, int idPadre, String nombre, int prioridad, int tiempoEjecucion, int totalPaginas) {
        this.id = id;
        this.idPadre = idPadre;
        this.nombre = nombre;
        this.estado = EstadoProceso.NUEVO;
        this.prioridad = prioridad;
        this.deadline = 0; // Valor por defecto
        this.tiempoLlegada = 0; // Ciclo 0 por defecto
        this.tiempoCpuConsumido = 0;
        this.tiempoEjecucion = tiempoEjecucion;

        // Inicializacion estatica de la Tabla de Paginas del proceso
        this.tablaPaginas = new EntradaTablaPaginas[totalPaginas];
        for (int i = 0; i < totalPaginas; i++) {
            this.tablaPaginas[i] = new EntradaTablaPaginas(i);
        }
    }

    /**
     * Incrementa en un ciclo la ejecucion consumida por el proceso en CPU.
     */
    public void ejecutarCiclo() {
        this.tiempoCpuConsumido++;
    }

    /**
     * Evalua si el proceso completo todos sus ciclos requeridos.
     * @return true si finalizo, false si aun requiere tiempo de CPU.
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

    @Override
    public String toString() {
        return "Proceso{" + "id=" + id + ", nombre='" + nombre + '\'' + ", estado=" + estado + ", tiempo=" + tiempoEjecucion + '}';
    }
}