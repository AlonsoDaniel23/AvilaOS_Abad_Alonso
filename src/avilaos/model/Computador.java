/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.model;

import avilaos.datastructures.ColaProcesos;
import avilaos.enums.EstadoProceso;
import avilaos.interfaces.IRelojListener;
import avilaos.scheduling.*;

/**
 * Representa un nodo de computo individual dentro del cluster simulado.
 * Integra el patrón Strategy con la interfaz Planificador para alternar
 * algoritmos de planificación (FCFS, RR, EDF, Prioridades) dinámicamente.
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
    private Planificador planificador;
    private String nombrePoliticaActual;

    public Computador(int idComputador, String nombre, int tamanioRamKB, int tamanioSwapKB) {
        this.idComputador = idComputador;
        this.nombre = nombre;
        this.cpu = new CPU(idComputador);
        this.ram = new MemoriaRAM(tamanioRamKB, 4); // Marcos de 4KB por defecto
        this.discoSwap = new DiscoSwap(tamanioSwapKB, 4); // Bloques de 4KB por defecto
        this.colaProcesos = new ColaProcesos();
        
        // Política de planificación por defecto (FCFS)
        this.planificador = new PlanificadorFCFS();
        this.nombrePoliticaActual = "FCFS";
    }

    /**
     * Cambia la política de planificación activa y reordena los procesos pendientes.
     * @param nuevaPolitica Nombre de la política seleccionada en la interfaz.
     */
    public void cambiarPolitica(String nuevaPolitica) {
        if (nuevaPolitica == null || nuevaPolitica.equalsIgnoreCase(nombrePoliticaActual)) {
            return;
        }

        Planificador nuevoPlanificador;
        switch (nuevaPolitica) {
            case "Round Robin":
                nuevoPlanificador = new PlanificadorRR();
                break;
            case "EDF":
                nuevoPlanificador = new PlanificadorEDF();
                break;
            case "Prioridades":
                nuevoPlanificador = new PlanificadorPrioridades();
                break;
            case "FCFS":
            default:
                nuevoPlanificador = new PlanificadorFCFS();
                break;
        }

        // Migrar procesos existentes de la cola actual al nuevo planificador
        while (tieneProcesosEnEspera()) {
            Proceso p = despacharSiguiente();
            if (p != null) {
                nuevoPlanificador.agregarProceso(p);
            }
        }

        this.planificador = nuevoPlanificador;
        this.nombrePoliticaActual = nuevaPolitica;
    }

    /**
     * Encola un nuevo proceso preparado para ejecución en este nodo.
     * @param proceso Proceso a agregar.
     */
    public void encolarProceso(Proceso proceso) {
        if (proceso != null) {
            proceso.setEstado(EstadoProceso.LISTO);
            if (planificador != null) {
                planificador.agregarProceso(proceso);
            } else {
                this.colaProcesos.encolar(proceso);
            }
        }
    }

    /**
     * Obtiene y remueve el siguiente proceso a ejecutar según la política activa.
     */
    public Proceso despacharSiguiente() {
        if (planificador != null && planificador.hayProcesos()) {
            return planificador.despacharSiguiente();
        } else if (!colaProcesos.estaVacia()) {
            return (Proceso) colaProcesos.desencolar();
        }
        return null;
    }

    /**
     * Evalúa si hay procesos pendientes por ejecutar.
     */
    public boolean tieneProcesosEnEspera() {
        if (planificador != null) {
            return planificador.hayProcesos();
        }
        return !colaProcesos.estaVacia();
    }

    @Override
    public void tick(int cicloActual) {
        if (cpu.isOcupado() && cpu.getProcesoActual() != null) {
            Proceso procesoActual = cpu.getProcesoActual();
            procesoActual.ejecutarCiclo();
            cpu.incrementarPC();

            // Si el proceso completó su tiempo requerido, se libera RAM y CPU
            if (procesoActual.esFinalizado()) {
                procesoActual.setEstado(EstadoProceso.TERMINADO);
                ram.liberarMemoriaProceso(procesoActual.getId());
                cpu.liberar();
            }
        }
    }

    // Getters
    public int getIdComputador() { return idComputador; }
    public String getNombre() { return nombre; }
    public CPU getCpu() { return cpu; }
    public MemoriaRAM getRam() { return ram; }
    public DiscoSwap getDiscoSwap() { return discoSwap; }
    public ColaProcesos getColaProcesos() { return colaProcesos; }
    public Planificador getPlanificador() { return planificador; }
    public String getNombrePoliticaActual() { return nombrePoliticaActual; }
}