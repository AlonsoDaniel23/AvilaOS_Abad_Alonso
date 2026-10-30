package avilaos.scheduling;

import avilaos.datastructures.ColaProcesos;
import avilaos.model.Proceso;

/**
 * Implementa la politica First-Come, First-Served (FCFS).
 * Atiende los procesos en estricto orden de llegada.
 * 
 * @author Gustavo Abad
 */
public class PlanificadorFCFS implements Planificador {
    
    private ColaProcesos colaListos;
    
    public PlanificadorFCFS() {
        this.colaListos = new ColaProcesos();
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
    
    @Override
    public int getCantidadProcesos() {
        return (colaListos != null) ? colaListos.getTamaño() : 0;
    }
    
    @Override
    public ColaProcesos getCola() {
        return this.colaListos;
    }
}