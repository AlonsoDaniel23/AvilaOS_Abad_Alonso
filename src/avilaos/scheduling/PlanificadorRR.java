package avilaos.scheduling;

import avilaos.datastructures.ColaProcesos;
import avilaos.model.Proceso;

/**
 * Implementa la politica Round Robin (RR).
 * Asigna un quantum de tiempo de CPU a cada proceso en la cola.
 * 
 * @author Gustavo Abad
 */
public class PlanificadorRR implements Planificador {
    
    private ColaProcesos colaListos;
    private int quantum;
    
    public PlanificadorRR(int quantum) {
        this.colaListos = new ColaProcesos();
        this.quantum = quantum;
    }
    
    public PlanificadorRR() {
        this(2); // Quantum de 2 ciclos por defecto
    }
    
    @Override
    public void agregarProceso(Proceso proceso) {
        if (proceso != null) {
            colaListos.encolar(proceso);
        }
    }
    
    @Override
    public Proceso despacharSiguiente() {
        if (!hayProcesos()) {
            return null;
        }
        return (Proceso) colaListos.desencolar();
    }
    
    @Override
    public boolean hayProcesos() {
        return !colaListos.estaVacia();
    }

    public int getQuantum() {
        return quantum;
    }
}