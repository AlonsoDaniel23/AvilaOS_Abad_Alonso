package avilaos.scheduling;

import avilaos.datastructures.ListaEnlazada;
import avilaos.model.Proceso;

/**
 * Implementa la politica Earliest Deadline First (EDF).
 * Despacha el proceso con la fecha limite (deadline) mas cercana.
 * 
 * @author Gustavo Abad
 */
public class PlanificadorEDF implements Planificador {
    
    private ListaEnlazada listaProcesos;
    
    public PlanificadorEDF() {
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
        
        int menorIndice = 0;
        Proceso candidato = (Proceso) listaProcesos.obtener(0);
        
        for (int i = 1; i < listaProcesos.getTamaño(); i++) {
            Proceso actual = (Proceso) listaProcesos.obtener(i);
            if (actual != null && candidato != null && actual.getDeadline() < candidato.getDeadline()) {
                candidato = actual;
                menorIndice = i;
            }
        }
        
        ListaEnlazada nuevaLista = new ListaEnlazada();
        for (int i = 0; i < listaProcesos.getTamaño(); i++) {
            if (i != menorIndice) {
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
}