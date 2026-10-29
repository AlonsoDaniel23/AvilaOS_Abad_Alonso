/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.model;

import avilaos.enums.ModoEjecucion;

/**
 * Representa la Unidad Central de Procesamiento (CPU) del simulador.
 * Mantiene el estado del procesador, el contador de programa (PC)
 * y el modo de ejecucion actual.
 * 
 * @author Daniel Alonso
 */
public class CPU {
    
private final int idCpu;
    private Proceso procesoActual;
    private int programCounter;
    private ModoEjecucion modoActual;
    private boolean ocupado;

    public CPU(int idCpu) {
        this.idCpu = idCpu;
        this.procesoActual = null;
        this.programCounter = 0;
        this.modoActual = ModoEjecucion.USUARIO;
        this.ocupado = false;
    }

    /**
     * Asigna un proceso a la CPU para iniciar o reanudar su ejecucion.
     * @param proceso Proceso a despachar.
     */
    public void cargarProceso(Proceso proceso) {
        this.procesoActual = proceso;
        this.programCounter = 0;
        this.ocupado = (proceso != null);
        this.modoActual = ModoEjecucion.USUARIO;
    }

    /**
     * Libera la CPU al finalizar o desalojar un proceso.
     */
    public void liberar() {
        this.procesoActual = null;
        this.programCounter = 0;
        this.ocupado = false;
        this.modoActual = ModoEjecucion.USUARIO;
    }

    /**
     * Incrementa el Contador de Programa (PC) en un ciclo de instruccion.
     */
    public void incrementarPC() {
        this.programCounter++;
    }

    // Getters y Setters
    public int getIdCpu() {
        return idCpu;
    }

    public Proceso getProcesoActual() {
        return procesoActual;
    }

    public int getProgramCounter() {
        return programCounter;
    }

    public void setProgramCounter(int programCounter) {
        this.programCounter = programCounter;
    }

    public ModoEjecucion getModoActual() {
        return modoActual;
    }

    public void setModoActual(ModoEjecucion modoActual) {
        this.modoActual = modoActual;
    }

    public boolean isOcupado() {
        return ocupado;
    }
}
