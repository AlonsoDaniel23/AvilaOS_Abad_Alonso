package avilaos.model;

import avilaos.enums.EstadoProceso;
import avilaos.enums.PermisoArchivo;
import avilaos.enums.TipoOperacionES;
import avilaos.enums.TipoProceso;

/**
 * Representa el Bloque de Control de Procesos (PCB) dentro de AvilaOS.
 * Almacena identidad, jerarquia, estado, tabla de paginas y parametros de E/S.
 * 
 * @author Equipo AvilaOS
 * @version 1.7
 */
public class Proceso {

    private int id;
    private int idPadre;
    private String nombre;
    private EstadoProceso estado;
    private TipoProceso tipo;
    private TipoOperacionES operacionES;
    private String rutaArchivo;
    private String motivoBloqueo;
    private int prioridad;
    private int deadline;
    private int tiempoLlegada;
    private int tiempoCpuConsumido;
    private int tiempoEjecucion;
    private int ciclosBloqueado;
    private boolean solicitudDiscoEnviada;
    private boolean operacionESCompletada;
    private EntradaTablaPaginas[] tablaPaginas;

    // Metadatos opcionales para operaciones CRUD de E/S
    private boolean esDirectorioTarget;
    private int bloquesSolicitadosES = 1;
    private String nuevoNombreES = "";
    private String contenidoES = "";
    private PermisoArchivo permisoES = PermisoArchivo.PUBLICO;

    /**
     * Constructor base para procesos CPU_BOUND por defecto.
     */
    public Proceso(int id, String nombre, int tiempoEjecucion) {
        this(id, 0, nombre, 1, tiempoEjecucion, 4, TipoProceso.CPU_BOUND, null, "");
    }

    /**
     * Constructor estandar con jerarquia, prioridad y cantidad de paginas.
     */
    public Proceso(int id, int idPadre, String nombre, int prioridad, int tiempoEjecucion, int totalPaginas) {
        this(id, idPadre, nombre, prioridad, tiempoEjecucion, totalPaginas, TipoProceso.CPU_BOUND, null, "");
    }

    /**
     * Constructor completo con soporte para operaciones de E/S y rutas del sistema de archivos.
     */
    public Proceso(int id, int idPadre, String nombre, int prioridad, int tiempoEjecucion, int totalPaginas,
            TipoProceso tipo, TipoOperacionES operacionES, String rutaArchivo) {
        this.id = id;
        this.idPadre = idPadre;
        this.nombre = nombre;
        this.estado = EstadoProceso.NUEVO;
        this.prioridad = prioridad;
        this.deadline = 0;
        this.tiempoLlegada = 0;
        this.tiempoCpuConsumido = 0;
        this.tiempoEjecucion = tiempoEjecucion;
        this.ciclosBloqueado = 0;
        this.tipo = (tipo != null) ? tipo : TipoProceso.CPU_BOUND;
        this.operacionES = operacionES;
        this.rutaArchivo = (rutaArchivo != null) ? rutaArchivo : "";
        this.motivoBloqueo = "";
        this.solicitudDiscoEnviada = false;
        this.operacionESCompletada = false;

        this.tablaPaginas = new EntradaTablaPaginas[totalPaginas];
        for (int i = 0; i < totalPaginas; i++) {
            this.tablaPaginas[i] = new EntradaTablaPaginas(i);
        }
    }

    /**
     * Incrementa en un ciclo el tiempo de CPU consumido por el proceso.
     */
    public void ejecutarCiclo() {
        this.tiempoCpuConsumido++;
    }

    /**
     * Incrementa en un ciclo el contador de tiempo que el proceso lleva bloqueado.
     */
    public void incrementarCiclosBloqueado() {
        this.ciclosBloqueado++;
    }

    /**
     * Indica si el proceso completo todos sus ciclos de ejecucion requeridos.
     */
    public boolean esFinalizado() {
        return tiempoCpuConsumido >= tiempoEjecucion;
    }

    // Getters y Setters principales
    public int getId() { return id; }
    public int getIdPadre() { return idPadre; }
    public String getNombre() { return nombre; }
    public EstadoProceso getEstado() { return estado; }
    public void setEstado(EstadoProceso estado) { this.estado = estado; }
    public TipoProceso getTipo() { return tipo; }
    public void setTipo(TipoProceso tipo) { this.tipo = tipo; }
    public TipoOperacionES getOperacionES() { return operacionES; }
    public void setOperacionES(TipoOperacionES operacionES) { this.operacionES = operacionES; }
    public String getRutaArchivo() { return rutaArchivo; }
    public void setRutaArchivo(String rutaArchivo) { this.rutaArchivo = rutaArchivo; }
    public String getMotivoBloqueo() { return motivoBloqueo; }
    public void setMotivoBloqueo(String motivoBloqueo) { this.motivoBloqueo = motivoBloqueo; }
    public int getPrioridad() { return prioridad; }
    public void setPrioridad(int prioridad) { this.prioridad = prioridad; }
    public int getDeadline() { return deadline; }
    public void setDeadline(int deadline) { this.deadline = deadline; }
    public int getTiempoLlegada() { return tiempoLlegada; }
    public void setTiempoLlegada(int tiempoLlegada) { this.tiempoLlegada = tiempoLlegada; }
    public int getTiempoCpuConsumido() { return tiempoCpuConsumido; }
    public int getTiempoEjecucion() { return tiempoEjecucion; }
    public void setTiempoEjecucion(int tiempoEjecucion) { this.tiempoEjecucion = tiempoEjecucion; }
    public int getCiclosBloqueado() { return ciclosBloqueado; }
    public void setCiclosBloqueado(int ciclosBloqueado) { this.ciclosBloqueado = ciclosBloqueado; }
    public boolean isSolicitudDiscoEnviada() { return solicitudDiscoEnviada; }
    public void setSolicitudDiscoEnviada(boolean solicitudDiscoEnviada) { this.solicitudDiscoEnviada = solicitudDiscoEnviada; }
    public boolean isOperacionESCompletada() { return operacionESCompletada; }
    public void setOperacionESCompletada(boolean operacionESCompletada) { this.operacionESCompletada = operacionESCompletada; }
    public EntradaTablaPaginas[] getTablaPaginas() { return tablaPaginas; }

    // Getters y Setters para operaciones CRUD de E/S
    public boolean isEsDirectorioTarget() { return esDirectorioTarget; }
    public void setEsDirectorioTarget(boolean esDirectorioTarget) { this.esDirectorioTarget = esDirectorioTarget; }
    public int getBloquesSolicitadosES() { return bloquesSolicitadosES; }
    public void setBloquesSolicitadosES(int bloquesSolicitadosES) { this.bloquesSolicitadosES = Math.max(1, bloquesSolicitadosES); }
    public String getNuevoNombreES() { return nuevoNombreES; }
    public void setNuevoNombreES(String nuevoNombreES) { this.nuevoNombreES = nuevoNombreES; }
    public String getContenidoES() { return contenidoES; }
    public void setContenidoES(String contenidoES) { this.contenidoES = contenidoES; }
    public PermisoArchivo getPermisoES() { return permisoES; }
    public void setPermisoES(PermisoArchivo permisoES) { this.permisoES = permisoES; }

    @Override
    public String toString() {
        return "Proceso{" + "id=" + id + ", nombre='" + nombre + '\'' + ", tipo=" + tipo + ", estado=" + estado + ", tiempo=" + tiempoEjecucion + '}';
    }
}