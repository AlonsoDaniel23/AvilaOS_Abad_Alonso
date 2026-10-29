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

    public MetricasService() {
        this.procesosCompletadosTotales = 0;
        this.deadlinesIncumplidosTotales = 0;
        this.deadlinesCumplidosTotales = 0;
        this.acumuladoTiempoRespuesta = 0;
        this.elementosProducidosTotales = 0;
        this.elementosConsumidosTotales = 0;
    }

    /**
     * Registra las estadisticas finales de un proceso culminado.
     * @param tiempoRespuesta Tiempo de respuesta acumulado.
     * @param cumplioDeadline Indica si el proceso finalizo dentro de su tiempo limite.
     */
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
     * Tasa de procesos completados por ciclo de reloj (Throughput global).
     * @param cicloActual Numero de ciclo actual emitido por el reloj.
     * @return El valor del throughput procesado.
     */
    public double calcularThroughput(int cicloActual) {
        if (cicloActual <= 0) return 0.0;
        return (double) procesosCompletadosTotales / cicloActual;
    }

    /**
     * Porcentaje de cumplimiento de deadlines (0-100%).
     * @return El porcentaje de cumplimiento de tiempos limite.
     */
    public double calcularTasaCumplimientoDeadlines() {
        int total = deadlinesCumplidosTotales + deadlinesIncumplidosTotales;
        if (total == 0) return 100.0;
        return ((double) deadlinesCumplidosTotales / total) * 100.0;
    }

    /**
     * Tiempo promedio de respuesta de los procesos en ciclos de reloj.
     * @return El tiempo promedio de respuesta calculado en ciclos.
     */
    public double calcularTiempoRespuestaPromedio() {
        if (procesosCompletadosTotales == 0) return 0.0;
        return (double) acumuladoTiempoRespuesta / procesosCompletadosTotales;
    }

    // Getters
    public int getProcesosCompletadosTotales() {
        return procesosCompletadosTotales;
    }

    public int getDeadlinesIncumplidosTotales() {
        return deadlinesIncumplidosTotales;
    }

    public int getDeadlinesCumplidosTotales() {
        return deadlinesCumplidosTotales;
    }

    public int getElementosProducidosTotales() {
        return elementosProducidosTotales;
    }

    public int getElementosConsumidosTotales() {
        return elementosConsumidosTotales;
    }
}
