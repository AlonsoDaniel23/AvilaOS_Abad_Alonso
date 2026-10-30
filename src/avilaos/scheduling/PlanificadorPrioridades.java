package avilaos.scheduling;

import avilaos.datastructures.ListaEnlazada;
import avilaos.model.Proceso;

/**
 * Implementa la politica de Planificacion por Prioridades.
 * Despacha el proceso con el mayor numero de prioridad.
 * 
 * @author Gustavo Abad
 */
public class PlanificadorPrioridades implements Planificador {
    
    private ListaEnlazada listaProcesos;
    
    public PlanificadorPrioridades() {
        this.listaProcesos = new ListaEnlazada();
    }
    
    @Override
    public void agregarProceso(Proceso proceso) {
        if (proceso != null) {
            listaProcesos.insertarFinal(proceso);
        }
    }
    
    @Override
    public Proceso despacharSiguiente() {
        if (!hayProcesos()) {
            return null;
        }
        
        int indiceMayorPrioridad = 0;
        Proceso candidato = (Proceso) listaProcesos.obtener(0);
        
        for (int i = 1; i < listaProcesos.getTamaño(); i++) {
            Proceso actual = (Proceso) listaProcesos.obtener(i);
            if (actual != null && candidato != null && actual.getPrioridad() > candidato.getPrioridad()) {
                candidato = actual;
                indiceMayorPrioridad = i;
            }
        }
        
        ListaEnlazada nuevaLista = new ListaEnlazada();
        for (int i = 0; i < listaProcesos.getTamaño(); i++) {
            if (i != indiceMayorPrioridad) {
                nuevaLista.insertarFinal(listaProcesos.obtener(i));
            }
        }
        this.listaProcesos = nuevaLista;
        
        return candidato;
    }
    
    @Override
    public boolean hayProcesos() {
        return !listaProcesos.estaVacia();
    }
    
    @Override
    public int getCantidadProcesos() {
        return (listaProcesos != null) ? listaProcesos.getTamaño() : 0;
    }
}