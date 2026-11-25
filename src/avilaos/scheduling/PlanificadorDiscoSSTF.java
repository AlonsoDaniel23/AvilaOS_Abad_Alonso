package avilaos.scheduling;

import avilaos.datastructures.ColaProcesos;
import avilaos.enums.DireccionCabezal;
import avilaos.model.SolicitudDisco;

/**
 * Implementa la politica Shortest Seek Time First (SSTF) para planificacion de disco.
 * Selecciona la solicitud cuyo bloque objetivo se encuentre a la menor distancia
 * absoluta respecto a la posicion actual del cabezal.
 * 
 * @author Gustavo Abad
 */
public class PlanificadorDiscoSSTF implements PlanificadorDisco {

    @Override
    public SolicitudDisco seleccionarSiguiente(ColaProcesos colaSolicitudes, int posicionCabezal,
            DireccionCabezal direccionActual, int totalBloques) {
        if (colaSolicitudes == null || colaSolicitudes.estaVacia()) {
            return null;
        }

        int n = colaSolicitudes.getTamaño();
        SolicitudDisco[] buffer = new SolicitudDisco[n];
        int indiceMejor = 0;
        int menorDistancia = Integer.MAX_VALUE;

        // Extraer elementos temporalmente a un arreglo nativo para evaluar distancias
        for (int i = 0; i < n; i++) {
            buffer[i] = (SolicitudDisco) colaSolicitudes.desencolar();
            if (buffer[i] != null) {
                int distancia = Math.abs(buffer[i].getBloqueObjetivo() - posicionCabezal);
                if (distancia < menorDistancia) {
                    menorDistancia = distancia;
                    indiceMejor = i;
                }
            }
        }

        // Reencolar todas las solicitudes excepto la seleccionada
        SolicitudDisco seleccionada = buffer[indiceMejor];
        for (int i = 0; i < n; i++) {
            if (i != indiceMejor && buffer[i] != null) {
                colaSolicitudes.encolar(buffer[i]);
            }
        }

        return seleccionada;
    }

    @Override
    public String getNombrePolitica() {
        return "SSTF";
    }
}