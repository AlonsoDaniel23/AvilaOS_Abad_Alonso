/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.scheduling;

import avilaos.datastructures.ColaProcesos;
import avilaos.enums.EstadoProceso;
import avilaos.enums.TipoOperacionES;
import avilaos.model.Computador;
import avilaos.model.DiscoSwap;
import avilaos.model.MemoriaRAM;
import avilaos.model.Proceso;
import avilaos.services.EventLog;

/**
 * Gestiona el intercambio de procesos entre RAM y Swap (Swap-Out / Swap-In)
 * y controla las transiciones de los estados suspendidos del modelo de 7 estados.
 * 
 * @author Daniel Alonso
 */
public class PlanificadorMedianoPlazo {

    private int totalSuspensiones;
    private int totalReactivaciones;

    public PlanificadorMedianoPlazo() {
        this.totalSuspensiones = 0;
        this.totalReactivaciones = 0;
    }

    /**
     * Expulsa procesos bloqueados hacia DiscoSwap (Swap-Out) hasta liberar
     * la cantidad de marcos de RAM solicitada.
     */
    public boolean intentarLiberarRAM(MemoriaRAM ram, DiscoSwap discoSwap, ColaProcesos colaBloqueados,
            ColaProcesos colaSuspendidosBloqueados, int marcosNecesarios, int cicloActual) {

        while (!ram.tieneEspacio(marcosNecesarios) && !colaBloqueados.estaVacia()) {
            Proceso victima = extraerVictimaBloqueada(colaBloqueados);
            if (victima == null) {
                break;
            }

            int paginasVictima = (victima.getTablaPaginas() != null) ? victima.getTablaPaginas().length : 4;

            if (discoSwap.asignarSwapProceso(victima.getId(), paginasVictima)) {
                ram.liberarMemoriaProceso(victima.getId());
                victima.setOperacionES(TipoOperacionES.SUSPENDER);
                victima.setEstado(EstadoProceso.SUSPENDIDO_BLOQUEADO);
                victima.setMotivoBloqueo("Suspendido en Swap (RAM llena)");
                colaSuspendidosBloqueados.encolar(victima);

                totalSuspensiones++;
                EventLog.getInstancia().registrar("[Ciclo " + cicloActual + "] [KERNEL] [SWAP-OUT] Proceso "
                        + victima.getNombre() + " expulsado de RAM a Swap (" + paginasVictima
                        + " bloques). Estado: SUSPENDIDO_BLOQUEADO.");
            } else {
                // Swap lleno: devuelve la victima a bloqueados y aborta expulsion
                colaBloqueados.encolar(victima);
                break;
            }
        }

        return ram.tieneEspacio(marcosNecesarios);
    }

    /**
     * Selecciona y extrae de la cola de bloqueados el proceso candidato a suspender
     * (menor prioridad y mayor tiempo acumulado en espera).
     */
    private Proceso extraerVictimaBloqueada(ColaProcesos colaBloqueados) {
        int n = colaBloqueados.getTamaño();
        if (n == 0) {
            return null;
        }

        Proceso[] buffer = new Proceso[n];
        int indiceVictima = 0;

        for (int i = 0; i < n; i++) {
            buffer[i] = (Proceso) colaBloqueados.desencolar();
            if (buffer[i] != null && buffer[indiceVictima] != null) {
                if (buffer[i].getPrioridad() < buffer[indiceVictima].getPrioridad()
                        || (buffer[i].getPrioridad() == buffer[indiceVictima].getPrioridad()
                        && buffer[i].getCiclosBloqueado() > buffer[indiceVictima].getCiclosBloqueado())) {
                    indiceVictima = i;
                }
            }
        }

        Proceso victima = buffer[indiceVictima];
        for (int i = 0; i < n; i++) {
            if (i != indiceVictima && buffer[i] != null) {
                colaBloqueados.encolar(buffer[i]);
            }
        }
        return victima;
    }

    /**
     * Promueve a SUSPENDIDO_LISTO los procesos suspendidos cuya E/S ya culmino.
     */
    public void actualizarSuspendidosBloqueados(ColaProcesos colaSuspendidosBloqueados,
            ColaProcesos colaSuspendidosListos, int cicloActual) {
        int n = colaSuspendidosBloqueados.getTamaño();
        for (int i = 0; i < n; i++) {
            Proceso p = (Proceso) colaSuspendidosBloqueados.desencolar();
            if (p != null) {
                if (p.isOperacionESCompletada()) {
                    p.setEstado(EstadoProceso.SUSPENDIDO_LISTO);
                    p.setMotivoBloqueo("Listo en Swap (Esperando RAM)");
                    colaSuspendidosListos.encolar(p);

                    EventLog.getInstancia().registrar("[Ciclo " + cicloActual + "] [KERNEL] Proceso "
                            + p.getNombre() + " completo su E/S en Swap. Pasa a SUSPENDIDO_LISTO.");
                } else {
                    p.incrementarCiclosBloqueado();
                    colaSuspendidosBloqueados.encolar(p);
                }
            }
        }
    }

    /**
     * Reingresa procesos desde SUSPENDIDO_LISTO hacia RAM (Swap-In) cuando haya espacio libre.
     */
    public void reactivarProcesos(MemoriaRAM ram, DiscoSwap discoSwap, ColaProcesos colaSuspendidosListos,
            Computador computador, int cicloActual) {
        int n = colaSuspendidosListos.getTamaño();
        for (int i = 0; i < n; i++) {
            Proceso p = (Proceso) colaSuspendidosListos.desencolar();
            if (p != null) {
                int paginas = (p.getTablaPaginas() != null) ? p.getTablaPaginas().length : 4;
                if (ram.tieneEspacio(paginas)) {
                    discoSwap.liberarSwapProceso(p.getId());
                    ram.asignarMemoriaProceso(p.getId(), paginas);
                    p.setOperacionES(TipoOperacionES.REACTIVAR);
                    p.setMotivoBloqueo("");
                    p.setEstado(EstadoProceso.LISTO);
                    computador.admitirProcesoReactivado(p);

                    totalReactivaciones++;
                    EventLog.getInstancia().registrar("[Ciclo " + cicloActual + "] [KERNEL] [SWAP-IN] Proceso "
                            + p.getNombre() + " reactivado desde Swap a RAM (" + paginas
                            + " marcos). Estado: LISTO.");
                } else {
                    colaSuspendidosListos.encolar(p);
                }
            }
        }
    }

    // Getters de metricas
    public int getTotalSuspensiones() { return totalSuspensiones; }
    public int getTotalReactivaciones() { return totalReactivaciones; }
}