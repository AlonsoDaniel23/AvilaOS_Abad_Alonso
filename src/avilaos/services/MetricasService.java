/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.services;

/**
 * Servicio encargado de recopilar y calcular métricas de rendimiento globales
 * (Throughput, utilizacion de procesadores, tasa de cumplimiento de deadlines y tiempos de respuesta).
 * Cumple con el Requerimiento 8 del proyecto sin librerías de java.util.
 * 
 * @author Daniel Alonso
 */
public class MetricasService {
    
    private int procesosCompletadosTotales;
    private int deadlinesIncumplidosTotales;
    private int deadlinesCumplidosTotales;
    private long acumuladoTiempoRespuesta;
    private int elementosProducidosTotales;
    private int elementosConsumidosTotales;

    // Matriz de historial de uso de CPU: [idNodo][ciclo] = porcentajeUso (0 a 100)
    private static final int MAX_NODOS = 10;
    private static final int MAX_CICLOS = 2000;
    private final double[][] historialCpuNodos;
    private int maxCicloRegistrado;

    public MetricasService() {
        this.procesosCompletadosTotales = 0;
        this.deadlinesIncumplidosTotales = 0;
        this.deadlinesCumplidosTotales = 0;
        this.acumuladoTiempoRespuesta = 0;
        this.elementosProducidosTotales = 0;
        this.elementosConsumidosTotales = 0;
        this.historialCpuNodos = new double[MAX_NODOS][MAX_CICLOS];
        this.maxCicloRegistrado = 0;
    }

    public synchronized void registrarProcesoTerminado(int tiempoRespuesta, boolean cumplioDeadline) {
        this.procesosCompletadosTotales++;
        this.acumuladoTiempoRespuesta += tiempoRespuesta;
        if (cumplioDeadline) {
            this.deadlinesCumplidosTotales++;
        } else {
            this.deadlinesIncumplidosTotales++;
        }
    }

    public synchronized void registrarProduccion() {
        this.elementosProducidosTotales++;
    }

    public synchronized void registrarConsumo() {
        this.elementosConsumidosTotales++;
    }

    /**
     * Registra el uso instantáneo de la CPU de un nodo en un ciclo específico.
     */
    public synchronized void registrarUsoCpuNodo(int idNodo, int ciclo, double porcentajeUso) {
        if (idNodo >= 1 && idNodo < MAX_NODOS && ciclo >= 0 && ciclo < MAX_CICLOS) {
            historialCpuNodos[idNodo][ciclo] = porcentajeUso;
            if (ciclo > maxCicloRegistrado) {
                maxCicloRegistrado = ciclo;
            }
        }
    }

    /**
     * Obtiene el historial de utilización de CPU de un nodo particular.
     */
    public synchronized double[] getHistorialCpuNodo(int idNodo) {
        if (idNodo < 1 || idNodo >= MAX_NODOS) {
            return new double[0];
        }
        int tamanio = maxCicloRegistrado + 1;
        double[] copia = new double[tamanio];
        for (int c = 0; c < tamanio; c++) {
            copia[c] = historialCpuNodos[idNodo][c];
        }
        return copia;
    }

    public double calcularThroughput(int cicloActual) {
        if (cicloActual <= 0) return 0.0;
        return (double) procesosCompletadosTotales / cicloActual;
    }

    public double calcularTasaCumplimientoDeadlines() {
        int total = deadlinesCumplidosTotales + deadlinesIncumplidosTotales;
        if (total == 0) return 100.0;
        return ((double) deadlinesCumplidosTotales / total) * 100.0;
    }

    public double calcularTiempoRespuestaPromedio() {
        if (procesosCompletadosTotales == 0) return 0.0;
        return (double) acumuladoTiempoRespuesta / procesosCompletadosTotales;
    }

    // Getters
    public int getProcesosCompletadosTotales() { return procesosCompletadosTotales; }
    public int getDeadlinesIncumplidosTotales() { return deadlinesIncumplidosTotales; }
    public int getDeadlinesCumplidosTotales() { return deadlinesCumplidosTotales; }
    public int getElementosProducidosTotales() { return elementosProducidosTotales; }
    public int getElementosConsumidosTotales() { return elementosConsumidosTotales; }
    public int getMaxCicloRegistrado() { return maxCicloRegistrado; }
}