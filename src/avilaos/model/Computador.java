/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.model;

import avilaos.datastructures.ColaProcesos;
import avilaos.enums.EstadoProceso;
import avilaos.interfaces.IRelojListener;

/**
 * Representa un nodo de computo individual dentro del cluster simulado.
 * Encapsula la CPU, Memoria RAM, Disco Swap y la cola de procesos,
 * respondiendo a las senales del RelojGlobal.
 * 
 * @author Daniel Alonso
 */
public class Computador implements IRelojListener {
    
    private final int idComputador;
    private final String nombre;
    private final CPU cpu;
    private final MemoriaRAM ram;
    private final DiscoSwap discoSwap;
    private final ColaProcesos colaProcesos;

    public Computador(int idComputador, String nombre, int tamanioRamKB, int tamanioSwapKB) {
        this.idComputador = idComputador;
        this.nombre = nombre;
        this.cpu = new CPU(idComputador);
        this.ram = new MemoriaRAM(tamanioRamKB, 4); // Marcos de 4KB por defecto
        this.discoSwap = new DiscoSwap(tamanioSwapKB, 4); // Bloques de 4KB por defecto
        this.colaProcesos = new ColaProcesos();
    }

    /**
     * Reacciona a cada pulso emitido por el RelojGlobal.
     * Avanza un ciclo de instruccion en la CPU si hay un proceso cargado.
     * @param cicloActual Numero de ciclo emitido por el reloj.
     */
    @Override
    public void tick(int cicloActual) {
        if (cpu.isOcupado() && cpu.getProcesoActual() != null) {
            Proceso procesoActual = cpu.getProcesoActual();
            procesoActual.ejecutarCiclo();
            cpu.incrementarPC();

            // Si el proceso completo su tiempo requerido, se libera la memoria y la CPU
            if (procesoActual.esFinalizado()) {
                procesoActual.setEstado(EstadoProceso.TERMINADO);
                ram.liberarMemoriaProceso(procesoActual.getId());
                cpu.liberar();
            }
        }
    }

    /**
     * Encola un nuevo proceso preparado para ejecucion en este nodo.
     * @param proceso Proceso a agregar.
     */
    public void encolarProceso(Proceso proceso) {
        if (proceso != null) {
            proceso.setEstado(EstadoProceso.LISTO);
            this.colaProcesos.encolar(proceso);
        }
    }

    // Getters
    public int getIdComputador() {
        return idComputador;
    }

    public String getNombre() {
        return nombre;
    }

    public CPU getCpu() {
        return cpu;
    }

    public MemoriaRAM getRam() {
        return ram;
    }

    public DiscoSwap getDiscoSwap() {
        return discoSwap;
    }

    public ColaProcesos getColaProcesos() {
        return colaProcesos;
    }
}
