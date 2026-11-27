package avilaos.scheduling;

import avilaos.datastructures.ColaProcesos;
import avilaos.enums.DireccionCabezal;
import avilaos.model.SolicitudDisco;

/**
 * Implementa la politica SCAN (algoritmo del elevador) para planificacion de disco.
 * El cabezal recorre el disco en su direccion actual atendiendo todas las solicitudes
 * en su camino hasta llegar al extremo, donde invierte su sentido de movimiento.
 * 
 * @author Gustavo Abad
 */
public class PlanificadorDiscoSCAN implements PlanificadorDisco {

    @Override
    public SolicitudDisco seleccionarSiguiente(ColaProcesos colaSolicitudes, int posicionCabezal,
            DireccionCabezal direccionActual, int totalBloques) {
        if (colaSolicitudes == null || colaSolicitudes.estaVacia()) {
            return null;
        }

        int n = colaSolicitudes.getTamaño();
        SolicitudDisco[] buffer = new SolicitudDisco[n];
        for (int i = 0; i < n; i++) {
            buffer[i] = (SolicitudDisco) colaSolicitudes.desencolar();
        }

        int indiceSeleccionado = -1;

        if (direccionActual == DireccionCabezal.ASCENDENTE) {
            // Buscar el bloque mas cercano >= posicionCabezal
            indiceSeleccionado = buscarMasCercanoAscendente(buffer, posicionCabezal);
            // Si no hay solicitudes hacia arriba, invertir direccion y buscar hacia abajo
            if (indiceSeleccionado == -1) {
                indiceSeleccionado = buscarMasCercanoDescendente(buffer, posicionCabezal);
            }
        } else {
            // Buscar el bloque mas cercano <= posicionCabezal
            indiceSeleccionado = buscarMasCercanoDescendente(buffer, posicionCabezal);
            // Si no hay solicitudes hacia abajo, invertir direccion y buscar hacia arriba
            if (indiceSeleccionado == -1) {
                indiceSeleccionado = buscarMasCercanoAscendente(buffer, posicionCabezal);
            }
        }

        // Reencolar el resto de solicitudes manteniendo su orden relativo
        SolicitudDisco seleccionada = (indiceSeleccionado != -1) ? buffer[indiceSeleccionado] : null;
        for (int i = 0; i < n; i++) {
            if (i != indiceSeleccionado && buffer[i] != null) {
                colaSolicitudes.encolar(buffer[i]);
            }
        }

        return seleccionada;
    }

    /**
     * Busca la solicitud con bloque >= posicionCabezal mas cercana al cabezal.
     */
    private int buscarMasCercanoAscendente(SolicitudDisco[] buffer, int posicionCabezal) {
        int indiceMejor = -1;
        int menorDistancia = Integer.MAX_VALUE;
        for (int i = 0; i < buffer.length; i++) {
            if (buffer[i] != null && buffer[i].getBloqueObjetivo() >= posicionCabezal) {
                int dist = buffer[i].getBloqueObjetivo() - posicionCabezal;
                if (dist < menorDistancia) {
                    menorDistancia = dist;
                    indiceMejor = i;
                }
            }
        }
        return indiceMejor;
    }

    /**
     * Busca la solicitud con bloque <= posicionCabezal mas cercana al cabezal.
     */
    private int buscarMasCercanoDescendente(SolicitudDisco[] buffer, int posicionCabezal) {
        int indiceMejor = -1;
        int menorDistancia = Integer.MAX_VALUE;
        for (int i = 0; i < buffer.length; i++) {
            if (buffer[i] != null && buffer[i].getBloqueObjetivo() <= posicionCabezal) {
                int dist = posicionCabezal - buffer[i].getBloqueObjetivo();
                if (dist < menorDistancia) {
                    menorDistancia = dist;
                    indiceMejor = i;
                }
            }
        }
        return indiceMejor;
    }

    @Override
    public String getNombrePolitica() {
        return "SCAN";
    }
}