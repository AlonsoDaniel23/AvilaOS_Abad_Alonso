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
 * la cola unica de solicitudes protegida mediante Semaforo e integrada con el patron
 * Strategy (PlanificadorDisco).
 * 
 * @author Gustavo Abad
 */
public class DiscoSimulado implements IRelojListener {

    private static DiscoSimulado instancia; // Instancia Singleton del disco local

    private final int totalBloques;         // Capacidad total del disco en bloques (0 a totalBloques - 1)
    private final int bloquesSwap;          // Cantidad de bloques reservados para Swap
    private final BloqueDisco[] bloques;    // Arreglo fisico de bloques del disco

    private int posicionCabezal;            // Posicion actual (bloque) del cabezal
    private DireccionCabezal direccionCabezal; // Sentido actual del movimiento del cabezal
    private int velocidadCabezal;           // Posiciones que recorre el cabezal por ciclo de reloj
    private int desplazamientoTotal;        // Contador acumulado de bloques desplazados por el cabezal
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
        this.desplazamientoTotal = 0;
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
                int posicionAntes = posicionCabezal;
                solicitudEnCurso = planificadorDisco.seleccionarSiguiente(
                        colaSolicitudes, posicionCabezal, direccionCabezal, totalBloques);

                if (solicitudEnCurso != null) {
                    // Verificar si la politica aplico recorrido de borde (SCAN / C-SCAN)
                    ajustarBordeSiAplica(posicionAntes, solicitudEnCurso.getBloqueObjetivo(), cicloActual);

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

            // 2. Si hay una solicitud en curso, mover el cabezal o procesar la transferencia
            if (solicitudEnCurso != null) {
                int objetivo = solicitudEnCurso.getBloqueObjetivo();

                if (posicionCabezal != objetivo) {
                    moverCabezalHacia(objetivo, cicloActual);
                } else {
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
     * Gestiona el desplazamiento hasta el extremo del disco (0 o totalBloques - 1)
     * cuando SCAN invierte sentido o C-SCAN realiza el retorno circular al inicio.
     */
    private void ajustarBordeSiAplica(int posActual, int siguienteObjetivo, int cicloActual) {
        int ultimoBloque = totalBloques - 1;

        if ("SCAN".equalsIgnoreCase(nombrePoliticaActual)) {
            // Si iba subiendo y el siguiente objetivo es menor, primero toca el extremo superior
            if (direccionCabezal == DireccionCabezal.ASCENDENTE && siguienteObjetivo < posActual) {
                int distBorde = ultimoBloque - posActual;
                if (distBorde > 0) {
                    desplazamientoTotal += distBorde;
                    posicionCabezal = ultimoBloque;
                    if (metricasService != null) {
                        metricasService.registrarMovimientoCabezal(cicloActual, distBorde, posicionCabezal);
                    }
                }
                direccionCabezal = DireccionCabezal.DESCENDENTE;
            } else if (direccionCabezal == DireccionCabezal.DESCENDENTE && siguienteObjetivo > posActual) {
                if (posActual > 0) {
                    desplazamientoTotal += posActual;
                    posicionCabezal = 0;
                    if (metricasService != null) {
                        metricasService.registrarMovimientoCabezal(cicloActual, posActual, posicionCabezal);
                    }
                }
                direccionCabezal = DireccionCabezal.ASCENDENTE;
            }
        } else if ("C-SCAN".equalsIgnoreCase(nombrePoliticaActual)) {
            // Si iba subiendo y ya no quedan peticiones adelante, llega a 199 y salta a 0 sin sumar retorno
            if (direccionCabezal == DireccionCabezal.ASCENDENTE && siguienteObjetivo < posActual) {
                int distBorde = ultimoBloque - posActual;
                if (distBorde > 0) {
                    desplazamientoTotal += distBorde;
                    if (metricasService != null) {
                        metricasService.registrarMovimientoCabezal(cicloActual, distBorde, ultimoBloque);
                    }
                }
                posicionCabezal = 0; // Salto circular al bloque 0
            } else if (direccionCabezal == DireccionCabezal.DESCENDENTE && siguienteObjetivo > posActual) {
                if (posActual > 0) {
                    desplazamientoTotal += posActual;
                    if (metricasService != null) {
                        metricasService.registrarMovimientoCabezal(cicloActual, posActual, 0);
                    }
                }
                posicionCabezal = ultimoBloque;
            }
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

        desplazamientoTotal += paso;

        if (metricasService != null) {
            metricasService.registrarMovimientoCabezal(cicloActual, paso, posicionCabezal);
        }
    }

    /**
     * Finaliza la solicitud en curso y notifica al Computador para despertar al proceso.
     */
    private void finalizarSolicitudActual() {
        Proceso p = solicitudEnCurso.getProceso();
        if (p != null) {
            ejecutarOperacionCRUDEnSistemaArchivos(p);
            if (Computador.getInstancia() != null) {
                Computador.getInstancia().desbloquearProceso(p);
            }
        }
        solicitudEnCurso = null;
        ciclosRestantesServicio = 0;
    }

    private void ejecutarOperacionCRUDEnSistemaArchivos(Proceso p) {
        if (p.getOperacionES() == null) {
            return;
        }
        SistemaArchivos fs = SistemaArchivos.getInstancia();
        String ruta = p.getRutaArchivo();

        switch (p.getOperacionES()) {
            case CREAR:
                int idx = ruta.lastIndexOf('/');
                String rutaPadre = (idx <= 0) ? "/" : ruta.substring(0, idx);
                String nombreNuevo = (idx >= 0) ? ruta.substring(idx + 1) : ruta;
                if (p.isEsDirectorioTarget()) {
                    fs.crearDirectorio(rutaPadre, nombreNuevo, p.getPermisoES());
                } else {
                    fs.crearArchivo(rutaPadre, nombreNuevo, p.getBloquesSolicitadosES(),
                        p.getPermisoES(), p.getContenidoES(), p.getNombre());
                }
                break;
            case ACTUALIZAR:
                if (p.getNuevoNombreES() != null && !p.getNuevoNombreES().isEmpty()) {
                    fs.renombrarEntrada(ruta, p.getNuevoNombreES());
                } else if (p.getContenidoES() != null && !p.getContenidoES().isEmpty()) {
                    fs.actualizarContenidoArchivo(ruta, p.getContenidoES());
                }
                break;
            case ELIMINAR:
                fs.eliminarEntrada(ruta);
                break;
            case LEER:
            default:
                fs.leerArchivo(ruta);
                break;
        }
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

    public int getBloquesLibresArchivos() {
        int libres = 0;
        for (int i = 0; i < totalBloques; i++) {
            if (!bloques[i].isEsAreaSwap() && !bloques[i].isOcupado()) {
                libres++;
            }
        }
        return libres;
    }

    public int asignarBloquesEncadenados(String nombreArchivo, int cantidad) {
        if (cantidad <= 0 || getBloquesLibresArchivos() < cantidad) {
            return -1;
        }

        int primerBloque = -1;
        int bloqueAnterior = -1;
        int asignados = 0;

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

    public boolean asignarBloquesEspecificos(String nombreArchivo, int bloqueInicio, int cantidad) {
        if (bloqueInicio < 0 || bloqueInicio + cantidad > totalBloques) {
            return false;
        }
        for (int i = bloqueInicio; i < bloqueInicio + cantidad; i++) {
            if (bloques[i].isEsAreaSwap() || bloques[i].isOcupado()) {
                return false;
            }
        }
        for (int i = bloqueInicio; i < bloqueInicio + cantidad; i++) {
            int siguiente = (i < bloqueInicio + cantidad - 1) ? (i + 1) : -1;
            bloques[i].asignarArchivo(nombreArchivo, siguiente);
        }
        return true;
    }

    public void liberarCadenaBloques(int primerBloque) {
        int actual = primerBloque;
        while (actual >= 0 && actual < totalBloques) {
            int siguiente = bloques[actual].getSiguienteBloque();
            bloques[actual].liberar();
            actual = siguiente;
        }
    }
    
    public void asignarColorACadena(int primerBloque, java.awt.Color color) {
    int actual = primerBloque;
    int seguridad = 0;
    while (actual >= 0 && actual < totalBloques && seguridad < totalBloques) {
        bloques[actual].setColorArchivo(color);
        actual = bloques[actual].getSiguienteBloque();
        seguridad++;
    }
}

    // ==================== GESTION DEL AREA SWAP EN DISCO ====================

    public int getBloquesLibresSwap() {
        int libres = 0;
        for (int i = 0; i < totalBloques; i++) {
            if (bloques[i].isEsAreaSwap() && !bloques[i].isOcupado()) {
                libres++;
            }
        }
        return libres;
    }

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
    public int getDesplazamientoTotal() { return desplazamientoTotal; }

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