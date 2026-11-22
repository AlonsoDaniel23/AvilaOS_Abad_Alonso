/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.services;

/**
 * Servicio encargado de recopilar y calcular metricas de rendimiento del sistema:
 * CPU, Throughput, deadlines, E/S de disco (desplazamiento del cabezal y espera)
 * e intercambio de memoria (Swap-Out / Swap-In), usando arreglos nativos sin java.util.
 * 
 * @author Daniel Alonso
 */
public class MetricasService {

    private static final int MAX_CICLOS = 2000;

    // Indices de politicas de disco: 0=FIFO, 1=SSTF, 2=SCAN, 3=C_SCAN
    public static final int IDX_FIFO = 0;
    public static final int IDX_SSTF = 1;
    public static final int IDX_SCAN = 2;
    public static final int IDX_CSCAN = 3;

    // Metricas de CPU y Procesos
    private int procesosCompletadosTotales;
    private int deadlinesCumplidosTotales;
    private int deadlinesIncumplidosTotales;
    private long acumuladoTiempoRespuesta;
    private final double[] historialUsoCpu;

    // Metricas de Disco y Cabezal
    private int desplazamientoTotalCabezal;
    private int posicionActualCabezal;
    private final int[] historialDesplazamientoCabezal;
    private final int[] historialPosicionCabezal;
    private int solicitudesESCompletadas;
    private long acumuladoTiempoEsperaES;
    private final int[] solicitudesPorPolitica;

    // Metricas de Swap (Mediano Plazo)
    private int totalSuspensiones;
    private int totalReactivaciones;

    private int maxCicloRegistrado;

    public MetricasService() {
        this.historialUsoCpu = new double[MAX_CICLOS];
        this.historialDesplazamientoCabezal = new int[MAX_CICLOS];
        this.historialPosicionCabezal = new int[MAX_CICLOS];
        this.solicitudesPorPolitica = new int[4];
        reiniciarMetricas();
    }

    /**
     * Reinicia todos los contadores e historiales al comenzar una nueva simulacion.
     */
    public synchronized void reiniciarMetricas() {
        this.procesosCompletadosTotales = 0;
        this.deadlinesCumplidosTotales = 0;
        this.deadlinesIncumplidosTotales = 0;
        this.acumuladoTiempoRespuesta = 0;

        this.desplazamientoTotalCabezal = 0;
        this.posicionActualCabezal = 0;
        this.solicitudesESCompletadas = 0;
        this.acumuladoTiempoEsperaES = 0;

        this.totalSuspensiones = 0;
        this.totalReactivaciones = 0;
        this.maxCicloRegistrado = 0;

        for (int i = 0; i < MAX_CICLOS; i++) {
            historialUsoCpu[i] = 0.0;
            historialDesplazamientoCabezal[i] = 0;
            historialPosicionCabezal[i] = 0;
        }
        for (int i = 0; i < solicitudesPorPolitica.length; i++) {
            solicitudesPorPolitica[i] = 0;
        }
    }

    /**
     * Registra las estadisticas de un proceso al culminar su ejecucion en CPU.
     */
    public synchronized void registrarProcesoTerminado(int tiempoRespuesta, boolean cumplioDeadline) {
        this.procesosCompletadosTotales++;
        this.acumuladoTiempoRespuesta += Math.max(0, tiempoRespuesta);
        if (cumplioDeadline) {
            this.deadlinesCumplidosTotales++;
        } else {
            this.deadlinesIncumplidosTotales++;
        }
    }

    /**
     * Registra el estado instantaneo del sistema en un ciclo de reloj para las graficas.
     */
    public synchronized void registrarEstadoCiclo(int ciclo, double porcentajeUsoCpu,
            int posicionCabezal, int desplazamientoAcumulado) {
        if (ciclo >= 0 && ciclo < MAX_CICLOS) {
            this.historialUsoCpu[ciclo] = porcentajeUsoCpu;
            this.posicionActualCabezal = posicionCabezal;
            this.desplazamientoTotalCabezal = desplazamientoAcumulado;
            this.historialPosicionCabezal[ciclo] = posicionCabezal;
            this.historialDesplazamientoCabezal[ciclo] = desplazamientoAcumulado;

            if (ciclo > maxCicloRegistrado) {
                maxCicloRegistrado = ciclo;
            }
        }
    }

    /**
     * Registra el desplazamiento incremental del cabezal en un ciclo especifico.
     */
    public synchronized void registrarMovimientoCabezal(int ciclo, int bloquesRecorridos, int nuevaPosicion) {
        if (bloquesRecorridos > 0) {
            this.desplazamientoTotalCabezal += bloquesRecorridos;
        }
        this.posicionActualCabezal = nuevaPosicion;

        if (ciclo >= 0 && ciclo < MAX_CICLOS) {
            this.historialDesplazamientoCabezal[ciclo] = this.desplazamientoTotalCabezal;
            this.historialPosicionCabezal[ciclo] = nuevaPosicion;
            if (ciclo > maxCicloRegistrado) {
                maxCicloRegistrado = ciclo;
            }
        }
    }

    /**
     * Contabiliza una solicitud de E/S atendida por el planificador de disco activo.
     */
    public synchronized void registrarSolicitudESAtendida(String politicaDisco, int tiempoEsperaEnCola) {
        this.solicitudesESCompletadas++;
        this.acumuladoTiempoEsperaES += Math.max(0, tiempoEsperaEnCola);

        int idx = mapearIndicePolitica(politicaDisco);
        if (idx >= 0 && idx < solicitudesPorPolitica.length) {
            solicitudesPorPolitica[idx]++;
        }
    }

    /**
     * Registra una operacion de expulsion de memoria RAM hacia DiscoSwap (Swap-Out).
     */
    public synchronized void registrarSuspension() {
        this.totalSuspensiones++;
    }

    /**
     * Registra una operacion de reingreso desde DiscoSwap hacia memoria RAM (Swap-In).
     */
    public synchronized void registrarReactivacion() {
        this.totalReactivaciones++;
    }

    /**
     * Sincroniza los contadores de swapping con el PlanificadorMedianoPlazo.
     */
    public synchronized void sincronizarMetricasSwap(int suspensiones, int reactivaciones) {
        this.totalSuspensiones = Math.max(0, suspensiones);
        this.totalReactivaciones = Math.max(0, reactivaciones);
    }

    private int mapearIndicePolitica(String politica) {
        if (politica == null) {
            return IDX_FIFO;
        }
        String p = politica.trim().toUpperCase();
        if (p.contains("SSTF")) {
            return IDX_SSTF;
        } else if (p.contains("C-SCAN") || p.contains("CSCAN") || p.contains("C_SCAN")) {
            return IDX_CSCAN;
        } else if (p.contains("SCAN")) {
            return IDX_SCAN;
        }
        return IDX_FIFO;
    }

    // Calculos agregados
    public double calcularThroughput(int cicloActual) {
        if (cicloActual <= 0) {
            return 0.0;
        }
        return (double) procesosCompletadosTotales / cicloActual;
    }

    public double calcularTasaCumplimientoDeadlines() {
        int total = deadlinesCumplidosTotales + deadlinesIncumplidosTotales;
        if (total == 0) {
            return 100.0;
        }
        return ((double) deadlinesCumplidosTotales / total) * 100.0;
    }

    public double calcularTiempoRespuestaPromedio() {
        if (procesosCompletadosTotales == 0) {
            return 0.0;
        }
        return (double) acumuladoTiempoRespuesta / procesosCompletadosTotales;
    }

    public double calcularTiempoEsperaPromedioES() {
        if (solicitudesESCompletadas == 0) {
            return 0.0;
        }
        return (double) acumuladoTiempoEsperaES / solicitudesESCompletadas;
    }

    /**
     * Retorna una copia del historial de desplazamiento acumulado del cabezal hasta el ultimo ciclo.
     */
    public synchronized int[] getHistorialDesplazamientoCabezal() {
        int tamanio = maxCicloRegistrado + 1;
        int[] copia = new int[tamanio];
        for (int i = 0; i < tamanio; i++) {
            copia[i] = historialDesplazamientoCabezal[i];
        }
        return copia;
    }

    /**
     * Retorna una copia del historial de posicion (bloque) del cabezal hasta el ultimo ciclo.
     */
    public synchronized int[] getHistorialPosicionCabezal() {
        int tamanio = maxCicloRegistrado + 1;
        int[] copia = new int[tamanio];
        for (int i = 0; i < tamanio; i++) {
            copia[i] = historialPosicionCabezal[i];
        }
        return copia;
    }

    /**
     * Retorna una copia del historial de utilizacion de CPU hasta el ultimo ciclo.
     */
    public synchronized double[] getHistorialUsoCpu() {
        int tamanio = maxCicloRegistrado + 1;
        double[] copia = new double[tamanio];
        for (int i = 0; i < tamanio; i++) {
            copia[i] = historialUsoCpu[i];
        }
        return copia;
    }

    public synchronized int getSolicitudesPorPolitica(String politica) {
        int idx = mapearIndicePolitica(politica);
        return solicitudesPorPolitica[idx];
    }

    // Getters
    public int getProcesosCompletadosTotales() { return procesosCompletadosTotales; }
    public int getDeadlinesCumplidosTotales() { return deadlinesCumplidosTotales; }
    public int getDeadlinesIncumplidosTotales() { return deadlinesIncumplidosTotales; }
    public int getDesplazamientoTotalCabezal() { return desplazamientoTotalCabezal; }
    public int getPosicionActualCabezal() { return posicionActualCabezal; }
    public int getSolicitudesESCompletadas() { return solicitudesESCompletadas; }
    public int getTotalSuspensiones() { return totalSuspensiones; }
    public int getTotalReactivaciones() { return totalReactivaciones; }
    public int getMaxCicloRegistrado() { return maxCicloRegistrado; }
}