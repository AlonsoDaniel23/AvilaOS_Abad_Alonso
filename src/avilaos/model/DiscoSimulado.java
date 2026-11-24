package avilaos.model;

import avilaos.datastructures.ColaProcesos;
import avilaos.enums.DireccionCabezal;
import avilaos.interfaces.IRelojListener;
import avilaos.scheduling.PlanificadorDisco;
import avilaos.scheduling.PlanificadorDiscoFIFO;
import avilaos.services.EventLog;
import avilaos.services.MetricasService;
import avilaos.synchronization.Semaforo;

/**
 * Simula el Disco Local (SD) de AvilaOS como un componente de hardware sincronizado
 * con el reloj global mediante IRelojListener. Administra el mapa de bloques numerados,
 * la reserva del area Swap, la asignacion encadenada, el movimiento del cabezal y
 * la cola unica de solicitudes protegida mediante Semaforo.
 * 
 * @author Gustavo Abad
 */
public class DiscoSimulado implements IRelojListener {

    private static DiscoSimulado instancia; // Instancia Singleton del disco local

    private final int totalBloques;         // Capacidad total del disco en bloques
    private final int bloquesSwap;          // Cantidad de bloques reservados para Swap
    private final BloqueDisco[] bloques;    // Arreglo fisico de bloques del disco

    private int posicionCabezal;            // Posicion actual (bloque) del cabezal
    private DireccionCabezal direccionCabezal; // Sentido actual del movimiento del cabezal
    private int velocidadCabezal;           // Posiciones que recorre el cabezal por ciclo de reloj
    private String nombrePoliticaActual;    // Nombre de la politica de planificacion activa

    private PlanificadorDisco planificadorDisco; // Estrategia activa para ordenar la cola de E/S
    private final ColaProcesos colaSolicitudes;  // Cola unica de solicitudes pendientes de E/S
    private SolicitudDisco solicitudEnCurso;     // Solicitud que esta siendo atendida actualmente
    private int ciclosRestantesServicio;         // Ciclos faltantes de lectura/escritura de bloques

    private final Semaforo mutexDisco;      // Semaforo de exclusion mutua para estructuras del disco
    private MetricasService metricasService;// Servicio para registrar desplazamientos y tiempos de espera

    /**
     * Constructor que inicializa el disco simulado con su capacidad, area Swap y cabezal inicial.
     * 
     * @param totalBloques Numero total de bloques del disco (ej. 200).
     * @param bloquesSwap Numero de bloques reservados al final del disco para intercambio (Swap).
     * @param cabezalInicial Posicion inicial del cabezal de lectura/escritura.
     */
    public DiscoSimulado(int totalBloques, int bloquesSwap, int cabezalInicial) {
        this.totalBloques = Math.max(10, totalBloques);
        this.bloquesSwap = Math.max(0, Math.min(bloquesSwap, this.totalBloques - 1));
        this.bloques = new BloqueDisco[this.totalBloques];

        // Reservar los ultimos 'bloquesSwap' bloques del disco para el area de intercambio
        int inicioSwap = this.totalBloques - this.bloquesSwap;
        for (int i = 0; i < this.totalBloques; i++) {
            boolean esSwap = (i >= inicioSwap);
            this.bloques[i] = new BloqueDisco(i, esSwap);
        }

        // Configurar estado inicial del cabezal y planificador por defecto (FIFO)
        this.posicionCabezal = Math.max(0, Math.min(cabezalInicial, this.totalBloques - 1));
        this.direccionCabezal = DireccionCabezal.ASCENDENTE;
        this.velocidadCabezal = 1;
        this.planificadorDisco = new PlanificadorDiscoFIFO();
        this.nombrePoliticaActual = "FIFO";

        this.colaSolicitudes = new ColaProcesos();
        this.solicitudEnCurso = null;
        this.ciclosRestantesServicio = 0;

        this.mutexDisco = new Semaforo(1);
        instancia = this;
    }

    /**
     * Retorna la instancia unica del Disco Simulado en el sistema.
     * 
     * @return Instancia actual de DiscoSimulado.
     */
    public static synchronized DiscoSimulado getInstancia() {
        if (instancia == null) {
            instancia = new DiscoSimulado(200, 50, 50);
        }
        return instancia;
    }

    /**
     * Inserta una nueva solicitud de E/S en la cola del disco protegiendo la seccion critica.
     * 
     * @param solicitud Solicitud de disco a encolar.
     */
    public void encolarSolicitud(SolicitudDisco solicitud) {
        if (solicitud == null) {
            return;
        }
        mutexDisco.waitSemaforo();
        try {
            colaSolicitudes.encolar(solicitud);
        } finally {
            mutexDisco.signalSemaforo();
        }
    }

    /**
     * Ejecuta exactamente un ciclo de reloj del disco.
     * Despacha nuevas solicitudes segun la politica activa, desplaza el cabezal y
     * desbloquea procesos cuando finaliza su transferencia de bloques.
     * 
     * @param cicloActual Numero de ciclo actual emitido por el RelojGlobal.
     */
    @Override
    public void tick(int cicloActual) {
        mutexDisco.waitSemaforo();
        try {
            // 1. Si el disco esta libre y hay solicitudes pendientes, seleccionar la siguiente
            if (solicitudEnCurso == null && !colaSolicitudes.estaVacia()) {
                solicitudEnCurso = planificadorDisco.seleccionarSiguiente(
                        colaSolicitudes, posicionCabezal, direccionCabezal, totalBloques);

                if (solicitudEnCurso != null) {
                    // Calcular tiempo que espero la solicitud en cola antes de ser atendida
                    int tiempoEspera = Math.max(0, cicloActual - solicitudEnCurso.getCicloLlegada());
                    if (metricasService != null) {
                        metricasService.registrarSolicitudESAtendida(nombrePoliticaActual, tiempoEspera);
                    }

                    // Cada bloque leido o escrito consume 1 ciclo de reloj
                    ciclosRestantesServicio = solicitudEnCurso.getCantidadBloques();

                    String nombreProc = (solicitudEnCurso.getProceso() != null)
                            ? solicitudEnCurso.getProceso().getNombre() : "Sistema";
                    EventLog.getInstancia().registrar(cicloActual,
                            "El disco atiende la solicitud de " + nombreProc
                            + " en el bloque " + solicitudEnCurso.getBloqueObjetivo()
                            + " (" + nombrePoliticaActual + ")");
                }
            }

            // 2. Si hay una solicitud activa, mover el cabezal o procesar la transferencia
            if (solicitudEnCurso != null) {
                int objetivo = solicitudEnCurso.getBloqueObjetivo();

                if (posicionCabezal != objetivo) {
                    // El cabezal aun no llega al bloque destino: avanzar segun velocidad
                    moverCabezalHacia(objetivo, cicloActual);
                } else {
                    // El cabezal ya esta sobre el bloque: ejecutar lectura/escritura
                    ciclosRestantesServicio--;
                    if (ciclosRestantesServicio <= 0) {
                        finalizarSolicitudActual();
                    }
                }
            }
        } finally {
            mutexDisco.signalSemaforo();
        }
    }

    /**
     * Desplaza el cabezal hacia el bloque objetivo actualizando su direccion y metricas.
     * 
     * @param objetivo Numero de bloque destino.
     * @param cicloActual Ciclo actual de simulacion.
     */
    private void moverCabezalHacia(int objetivo, int cicloActual) {
        int distancia = objetivo - posicionCabezal;
        int paso = Math.min(Math.abs(distancia), velocidadCabezal);

        if (distancia > 0) {
            direccionCabezal = DireccionCabezal.ASCENDENTE;
            posicionCabezal += paso;
        } else {
            direccionCabezal = DireccionCabezal.DESCENDENTE;
            posicionCabezal -= paso;
        }

        if (metricasService != null) {
            metricasService.registrarMovimientoCabezal(cicloActual, paso, posicionCabezal);
        }
    }

    /**
     * Finaliza la solicitud en curso y notifica al Computador para despertar al proceso.
     */
    private void finalizarSolicitudActual() {
        Proceso p = solicitudEnCurso.getProceso();
        if (p != null && Computador.getInstancia() != null) {
            Computador.getInstancia().desbloquearProceso(p);
        }
        solicitudEnCurso = null;
        ciclosRestantesServicio = 0;
    }

    /**
     * Cambia en tiempo real la estrategia de planificacion del disco.
     * 
     * @param nuevoPlanificador Implementacion de PlanificadorDisco a utilizar.
     */
    public void cambiarPlanificador(PlanificadorDisco nuevoPlanificador) {
        if (nuevoPlanificador == null) {
            return;
        }
        mutexDisco.waitSemaforo();
        try {
            this.planificadorDisco = nuevoPlanificador;
            this.nombrePoliticaActual = nuevoPlanificador.getNombrePolitica();
        } finally {
            mutexDisco.signalSemaforo();
        }
    }

    // ==================== GESTION DE BLOQUES Y ASIGNACION ENCADENADA ====================

    /**
     * Cuenta cuantos bloques libres estan disponibles para archivos (excluyendo el area Swap).
     * 
     * @return Cantidad de bloques libres para almacenamiento de archivos.
     */
    public int getBloquesLibresArchivos() {
        int libres = 0;
        for (int i = 0; i < totalBloques; i++) {
            if (!bloques[i].isEsAreaSwap() && !bloques[i].isOcupado()) {
                libres++;
            }
        }
        return libres;
    }

    /**
     * Asigna 'cantidad' bloques libres mediante asignacion encadenada para un archivo nuevo.
     * 
     * @param nombreArchivo Nombre del archivo propietario de los bloques.
     * @param cantidad Cantidad de bloques requeridos.
     * @return Indice del primer bloque de la lista enlazada, o -1 si no hay espacio suficiente.
     */
    public int asignarBloquesEncadenados(String nombreArchivo, int cantidad) {
        if (cantidad <= 0 || getBloquesLibresArchivos() < cantidad) {
            return -1;
        }

        int primerBloque = -1;
        int bloqueAnterior = -1;
        int asignados = 0;

        // Buscar bloques libres y enlazar cada bloque con el siguiente
        for (int i = 0; i < totalBloques && asignados < cantidad; i++) {
            if (!bloques[i].isEsAreaSwap() && !bloques[i].isOcupado()) {
                bloques[i].asignarArchivo(nombreArchivo, -1);
                if (primerBloque == -1) {
                    primerBloque = i;
                }
                if (bloqueAnterior != -1) {
                    bloques[bloqueAnterior].setSiguienteBloque(i);
                }
                bloqueAnterior = i;
                asignados++;
            }
        }
        return primerBloque;
    }

    /**
     * Asigna un rango contiguo especifico de bloques (usado al importar el JSON inicial).
     * 
     * @param nombreArchivo Nombre del archivo propietario.
     * @param bloqueInicio Bloque inicial especificado en el JSON.
     * @param cantidad Cantidad de bloques del archivo.
     * @return true si los bloques estaban libres y pudieron asignarse, false en caso contrario.
     */
    public boolean asignarBloquesEspecificos(String nombreArchivo, int bloqueInicio, int cantidad) {
        if (bloqueInicio < 0 || bloqueInicio + cantidad > totalBloques) {
            return false;
        }
        // Verificar disponibilidad completa antes de asignar
        for (int i = bloqueInicio; i < bloqueInicio + cantidad; i++) {
            if (bloques[i].isEsAreaSwap() || bloques[i].isOcupado()) {
                return false;
            }
        }
        // Asignar y encadenar cada bloque con su consecutivo
        for (int i = bloqueInicio; i < bloqueInicio + cantidad; i++) {
            int siguiente = (i < bloqueInicio + cantidad - 1) ? (i + 1) : -1;
            bloques[i].asignarArchivo(nombreArchivo, siguiente);
        }
        return true;
    }

    /**
     * Recorre la lista enlazada de bloques desde 'primerBloque' y libera cada uno.
     * 
     * @param primerBloque Indice del primer bloque asignado al archivo eliminado.
     */
    public void liberarCadenaBloques(int primerBloque) {
        int actual = primerBloque;
        while (actual >= 0 && actual < totalBloques) {
            int siguiente = bloques[actual].getSiguienteBloque();
            bloques[actual].liberar();
            actual = siguiente;
        }
    }

    // ==================== GESTION DEL AREA SWAP EN DISCO ====================

    /**
     * Calcula cuantos bloques libres quedan dentro de la particion Swap.
     * 
     * @return Numero de bloques libres en el area Swap.
     */
    public int getBloquesLibresSwap() {
        int libres = 0;
        for (int i = 0; i < totalBloques; i++) {
            if (bloques[i].isEsAreaSwap() && !bloques[i].isOcupado()) {
                libres++;
            }
        }
        return libres;
    }

    /**
     * Reserva bloques en el area Swap del disco para alojar un proceso suspendido.
     * 
     * @param idProceso ID del proceso suspendido.
     * @param nombreProceso Nombre del proceso suspendido.
     * @param cantidadBloques Cantidad de bloques (equivalentes a marcos de RAM) requeridos.
     * @return true si habia espacio en Swap y se asigno correctamente.
     */
    public boolean asignarSwapProceso(int idProceso, String nombreProceso, int cantidadBloques) {
        if (getBloquesLibresSwap() < cantidadBloques) {
            return false;
        }
        int asignados = 0;
        for (int i = 0; i < totalBloques && asignados < cantidadBloques; i++) {
            if (bloques[i].isEsAreaSwap() && !bloques[i].isOcupado()) {
                bloques[i].asignarSwap(idProceso, nombreProceso);
                asignados++;
            }
        }
        return true;
    }

    /**
     * Libera los bloques de Swap ocupados por un proceso al ser reactivado o terminado.
     * 
     * @param idProceso ID del proceso cuyos bloques Swap se liberaran.
     */
    public void liberarSwapProceso(int idProceso) {
        for (int i = 0; i < totalBloques; i++) {
            if (bloques[i].isEsAreaSwap() && bloques[i].isOcupado() && bloques[i].getIdProcesoSwap() == idProceso) {
                bloques[i].liberar();
            }
        }
    }

    // ==================== GETTERS Y SETTERS ====================

    public int getTotalBloques() { return totalBloques; }
    public int getBloquesSwap() { return bloquesSwap; }
    public BloqueDisco[] getBloques() { return bloques; }
    public int getPosicionCabezal() { return posicionCabezal; }

    public void setPosicionCabezal(int posicionCabezal) {
        if (posicionCabezal >= 0 && posicionCabezal < totalBloques) {
            this.posicionCabezal = posicionCabezal;
        }
    }

    public DireccionCabezal getDireccionCabezal() { return direccionCabezal; }
    public void setDireccionCabezal(DireccionCabezal direccionCabezal) { this.direccionCabezal = direccionCabezal; }
    public int getVelocidadCabezal() { return velocidadCabezal; }
    public void setVelocidadCabezal(int velocidadCabezal) { this.velocidadCabezal = Math.max(1, velocidadCabezal); }
    public String getNombrePoliticaActual() { return nombrePoliticaActual; }
    public PlanificadorDisco getPlanificadorDisco() { return planificadorDisco; }
    public ColaProcesos getColaSolicitudes() { return colaSolicitudes; }
    public SolicitudDisco getSolicitudEnCurso() { return solicitudEnCurso; }
    public void setMetricasService(MetricasService metricasService) { this.metricasService = metricasService; }
}