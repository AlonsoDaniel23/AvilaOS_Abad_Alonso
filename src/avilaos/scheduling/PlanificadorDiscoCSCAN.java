package avilaos.scheduling;

import avilaos.datastructures.ColaProcesos;
import avilaos.enums.DireccionCabezal;
import avilaos.model.SolicitudDisco;

/**
 * Implementa la politica Circular SCAN (C-SCAN) para planificacion de disco.
 * Atiende solicitudes en un unico sentido (ej. ascendente hacia el final del disco)
 * y, al no quedar peticiones adelante, salta al extremo opuesto (bloque 0) para
 * continuar barriendo en la misma direccion.
 * 
 * @author Gustavo Abad
 */
public class PlanificadorDiscoCSCAN implements PlanificadorDisco {

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
            // 1. Buscar la solicitud mas cercana hacia adelante (>= posicionCabezal)
            int menorDist = Integer.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (buffer[i] != null && buffer[i].getBloqueObjetivo() >= posicionCabezal) {
                    int dist = buffer[i].getBloqueObjetivo() - posicionCabezal;
                    if (dist < menorDist) {
                        menorDist = dist;
                        indiceSeleccionado = i;
                    }
                }
            }
            // 2. Si no hay mas solicitudes adelante, saltar al inicio del disco (menor bloque)
            if (indiceSeleccionado == -1) {
                int menorBloque = Integer.MAX_VALUE;
                for (int i = 0; i < n; i++) {
                    if (buffer[i] != null && buffer[i].getBloqueObjetivo() < menorBloque) {
                        menorBloque = buffer[i].getBloqueObjetivo();
                        indiceSeleccionado = i;
                    }
                }
            }
        } else {
            // 1. Buscar la solicitud mas cercana hacia atras (<= posicionCabezal)
            int menorDist = Integer.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (buffer[i] != null && buffer[i].getBloqueObjetivo() <= posicionCabezal) {
                    int dist = posicionCabezal - buffer[i].getBloqueObjetivo();
                    if (dist < menorDist) {
                        menorDist = dist;
                        indiceSeleccionado = i;
                    }
                }
            }
            // 2. Si no hay mas solicitudes atras, saltar al final del disco (mayor bloque)
            if (indiceSeleccionado == -1) {
                int mayorBloque = -1;
                for (int i = 0; i < n; i++) {
                    if (buffer[i] != null && buffer[i].getBloqueObjetivo() > mayorBloque) {
                        mayorBloque = buffer[i].getBloqueObjetivo();
                        indiceSeleccionado = i;
                    }
                }
            }
        }

        // Reencolar el resto de solicitudes pendientes
        SolicitudDisco seleccionada = (indiceSeleccionado != -1) ? buffer[indiceSeleccionado] : null;
        for (int i = 0; i < n; i++) {
            if (i != indiceSeleccionado && buffer[i] != null) {
                colaSolicitudes.encolar(buffer[i]);
            }
        }

        return seleccionada;
    }

    @Override
    public String getNombrePolitica() {
        return "C-SCAN";
    }
}