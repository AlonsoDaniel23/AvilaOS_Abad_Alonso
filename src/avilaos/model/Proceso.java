package avilaos.model;

/**
 * Clase que representa un Proceso dentro del sistema operativo.
 * Almacena la información del Bloque de Control de Procesos (PCB).
 */
public class Proceso {
    private int id;
    private String nombre;
    private String estado; // Posibles estados: "Listo", "Ejecución", "Terminado"
    private int tiempoEjecucion; // Tiempo o ráfaga de CPU que necesita para terminar

    /**
     * Constructor para inicializar un nuevo proceso.
     * @param id Identificador único del proceso.
     * @param nombre Nombre descriptivo (ej. "Proceso A").
     * @param tiempoEjecucion Cantidad de ciclos que requiere en el procesador.
     */
    public Proceso(int id, String nombre, int tiempoEjecucion) {
        this.id = id;
        this.nombre = nombre;
        this.estado = "Listo"; // Todo proceso nuevo entra en estado "Listo"
        this.tiempoEjecucion = tiempoEjecucion;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public int getTiempoEjecucion() {
        return tiempoEjecucion;
    }

    public void setTiempoEjecucion(int tiempoEjecucion) {
        this.tiempoEjecucion = tiempoEjecucion;
    }
    
    @Override
    public String toString() {
        return "Proceso{" + "id=" + id + ", nombre='" + nombre + '\'' + ", estado='" + estado + '\'' + ", tiempo=" + tiempoEjecucion + '}';
    }
}