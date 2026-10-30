/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.model;

import avilaos.datastructures.ColaProcesos;
import avilaos.enums.EstadoProceso;
import avilaos.enums.TipoProceso;
import avilaos.interfaces.IRelojListener;
import avilaos.scheduling.*;
import avilaos.services.EventLog;
import avilaos.synchronization.BufferCircular;
import avilaos.synchronization.RelojGlobal;
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
    private final ColaProcesos colaNuevos;
    private final ColaProcesos colaProcesos; // Cola de Listos por defecto
    private final ColaProcesos colaBloqueados;
    private final ColaProcesos colaTerminados;
    private Planificador planificador;
    private String nombrePoliticaActual;

    public Computador(int idComputador, String nombre, int tamanioRamKB, int tamanioSwapKB) {
        this.idComputador = idComputador;
        this.nombre = nombre;
        this.cpu = new CPU(idComputador);
        this.ram = new MemoriaRAM(tamanioRamKB, 4); // Marcos de 4KB por defecto
        this.discoSwap = new DiscoSwap(tamanioSwapKB, 4); // Bloques de 4KB por defecto
        this.colaNuevos = new ColaProcesos();
        this.colaProcesos = new ColaProcesos();
        this.colaBloqueados = new ColaProcesos();
        this.colaTerminados = new ColaProcesos();
        
        // Política de planificación por defecto (FCFS)
        this.planificador = new PlanificadorFCFS();
        this.nombrePoliticaActual = "FCFS";
    }

    /**
     * Encola un nuevo proceso. Ingresa a la Cola de Nuevos e intenta asignar RAM.
     * @param proceso Proceso a agregar.
     */
    public void encolarProceso(Proceso proceso) {
        if (proceso != null) {
            proceso.setEstado(EstadoProceso.NUEVO);
            this.colaNuevos.encolar(proceso);
            revisarColaNuevos(); // Asigna RAM si hay espacio disponible
        }
    }

    /**
     * Revisa la Cola de Nuevos y traslada a la Cola de Listos aquellos procesos
     * que dispongan de suficiente memoria RAM libre.
     */
    public void revisarColaNuevos() {
        while (!colaNuevos.estaVacia()) {
            Proceso p = (Proceso) colaNuevos.desencolar();
            if (p != null) {
                int paginas = (p.getTablaPaginas() != null) ? p.getTablaPaginas().length : 4;
                if (ram.getMarcosLibres() >= paginas) {
                    ram.asignarMemoriaProceso(p.getId(), paginas);
                    p.setEstado(EstadoProceso.LISTO);
                    if (planificador != null) {
                        planificador.agregarProceso(p);
                    } else {
                        colaProcesos.encolar(p);
                    }
                } else {
                    // No hay espacio suficiente en RAM: regresa a Cola Nuevos y detiene la admisión
                    colaNuevos.encolar(p);
                    break;
                }
            }
        }
    }

    /**
     * Revisa la Cola de Bloqueados y promueve a la Cola de Listos a los procesos
     * cuyo semáforo o buffer asociado ya dispone de recursos.
     */
    public void revisarColaBloqueados() {
        int tamanioInicial = colaBloqueados.getTamaño();
        for (int i = 0; i < tamanioInicial; i++) {
            Proceso p = (Proceso) colaBloqueados.desencolar();
            if (p != null) {
                BufferCircular buffer = obtenerBufferParaProceso(p);
                boolean seDesbloqueo = false;

                if (buffer != null) {
                    if (p.getTipo() == TipoProceso.PRODUCTOR && buffer.getEspacios().getValor() > 0) {
                        seDesbloqueo = true;
                    } else if (p.getTipo() == TipoProceso.CONSUMIDOR && buffer.getItems().getValor() > 0) {
                        seDesbloqueo = true;
                    }
                }

                if (seDesbloqueo) {
                    p.setEstado(EstadoProceso.LISTO);
                    if (planificador != null) {
                        planificador.agregarProceso(p);
                    } else {
                        colaProcesos.encolar(p);
                    }
                    EventLog.getInstancia().registrar("[Ciclo " + RelojGlobal.getInstancia().getCicloActual() + 
                        "] Proceso " + p.getNombre() + " se desbloqueó y pasó a Cola de Listos.");
                } else {
                    colaBloqueados.encolar(p);
                }
            }
        }
    }

    /**
     * Obtiene el buffer asociado al proceso. Si el nombre no está especificado o no existe,
     * lo busca/asigna automáticamente desde el Clúster ("B1" o el primer buffer disponible).
     */
    private BufferCircular obtenerBufferParaProceso(Proceso p) {
        if (p == null) {
            return null;
        }

        String nombreBuf = p.getNombreBuffer();
        BufferCircular buffer = null;

        // 1. Intenta buscarlo por el nombre indicado en el proceso
        if (nombreBuf != null && !nombreBuf.trim().isEmpty()) {
            buffer = ClusterManager.getInstancia().buscarBuffer(nombreBuf);
        }

        // 2. Si es nulo o vacío, realiza la asignación/búsqueda automática
        if (buffer == null) {
            buffer = ClusterManager.getInstancia().buscarBuffer("B1");
            if (buffer == null && ClusterManager.getInstancia().getListaBuffers() != null 
                    && ClusterManager.getInstancia().getListaBuffers().getTamaño() > 0) {
                buffer = (BufferCircular) ClusterManager.getInstancia().getListaBuffers().obtener(0);
            }
            
            // Asigna automáticamente el nombre del buffer encontrado al proceso
            if (buffer != null) {
                p.setNombreBuffer(buffer.getNombre());
            }
        }

        return buffer;
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

        // Migrar procesos existentes al nuevo planificador
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
     * Retorna el número de procesos en espera dentro de la Cola de Listos o el Planificador activo.
     */
    public int getCantidadProcesosListos() {
        if (planificador != null) {
            return planificador.getCantidadProcesos();
        }
        return (colaProcesos != null) ? colaProcesos.getTamaño() : 0;
    }

    /**
     * Evalúa si hay procesos pendientes por ejecutar en estado LISTO.
     */
    public boolean tieneProcesosEnEspera() {
        if (planificador != null) {
            return planificador.hayProcesos();
        }
        return !colaProcesos.estaVacia();
    }

    /**
     * Obtiene la cola activa de procesos listos para renderizado en la interfaz.
     */
    public ColaProcesos getColaListosEfectiva() {
        if (planificador != null && planificador.getCola() != null) {
            return planificador.getCola();
        }
        return colaProcesos;
    }

    @Override
    public void tick(int cicloActual) {
        // 0. Evaluar si algún proceso bloqueado puede desbloquearse
        revisarColaBloqueados();

        // 1. Si la CPU está inactiva y hay procesos listos, despachar uno a la CPU
        if (!cpu.isOcupado() && tieneProcesosEnEspera()) {
            Proceso siguiente = despacharSiguiente();
            if (siguiente != null) {
                siguiente.setEstado(EstadoProceso.EJECUCION);
                cpu.cargarProceso(siguiente);
            }
        }

        // 2. Si hay un proceso activo en la CPU
        if (cpu.isOcupado() && cpu.getProcesoActual() != null) {
            Proceso procesoActual = cpu.getProcesoActual();

            // Evaluar semáforo SOLO si el proceso aún no ha realizado la operación en el buffer
            boolean seBloqueo = false;
            if (!procesoActual.isOperacionBufferRealizada()) {
                seBloqueo = evaluarSincronizacionBuffer(procesoActual);
            }

            if (seBloqueo) {
                procesoActual.setEstado(EstadoProceso.BLOQUEADO);
                colaBloqueados.encolar(procesoActual);
                
                cpu.liberar();

                if (tieneProcesosEnEspera()) {
                    Proceso siguiente = despacharSiguiente();
                    if (siguiente != null) {
                        siguiente.setEstado(EstadoProceso.EJECUCION);
                        cpu.cargarProceso(siguiente);
                    }
                }
                return;
            }

            // Si no se bloqueó, ejecuta la instrucción normal
            procesoActual.ejecutarCiclo();
            cpu.incrementarPC();

            // Si el proceso completó su tiempo de ejecución
            if (procesoActual.esFinalizado()) {
                procesoActual.setEstado(EstadoProceso.TERMINADO);
                colaTerminados.encolar(procesoActual);
                
                ram.liberarMemoriaProceso(procesoActual.getId());
                cpu.liberar();
                
                revisarColaNuevos(); // Evalúa admisión de procesos pendientes tras liberar memoria
                
                if (tieneProcesosEnEspera()) {
                    Proceso siguiente = despacharSiguiente();
                    if (siguiente != null) {
                        siguiente.setEstado(EstadoProceso.EJECUCION);
                        cpu.cargarProceso(siguiente);
                    }
                }
            }
        }
    }

    /**
     * Gestiona el acceso al Buffer Circular mediante semáforos si el proceso es Productor/Consumidor.
     * @return true si el proceso se bloqueó por semáforo, false en caso contrario.
     */
    private boolean evaluarSincronizacionBuffer(Proceso p) {
        if (p == null || p.getTipo() == null) {
            return false;
        }

        BufferCircular buffer = obtenerBufferParaProceso(p);
        if (buffer == null) {
            return false;
        }

        if (p.getTipo() == TipoProceso.PRODUCTOR) {
            boolean exito = buffer.producir(p);
            if (!exito) {
                EventLog.getInstancia().registrar("[Ciclo " + RelojGlobal.getInstancia().getCicloActual() + 
                    "] Proceso " + p.getNombre() + " se bloqueó en el semáforo 'espacios' del Buffer " + buffer.getNombre());
                return true;
            }
            p.setOperacionBufferRealizada(true);
        } else if (p.getTipo() == TipoProceso.CONSUMIDOR) {
            boolean exito = buffer.consumir(p);
            if (!exito) {
                EventLog.getInstancia().registrar("[Ciclo " + RelojGlobal.getInstancia().getCicloActual() + 
                    "] Proceso " + p.getNombre() + " se bloqueó en el semáforo 'items' del Buffer " + buffer.getNombre());
                return true;
            }
            p.setOperacionBufferRealizada(true);
        }

        return false;
    }

    /**
     * Libera un proceso de la Cola de Bloqueados y lo reingresa al Planificador/Cola de Listos.
     */
    public void desbloquearProceso(Proceso p) {
        if (p != null) {
            p.setEstado(EstadoProceso.LISTO);
            if (planificador != null) {
                planificador.agregarProceso(p);
            } else {
                colaProcesos.encolar(p);
            }
        }
    }

    // Getters
    public int getIdComputador() { return idComputador; }
    public String getNombre() { return nombre; }
    public CPU getCpu() { return cpu; }
    public MemoriaRAM getRam() { return ram; }
    public DiscoSwap getDiscoSwap() { return discoSwap; }
    public ColaProcesos getColaNuevos() { return colaNuevos; }
    public ColaProcesos getColaProcesos() { return colaProcesos; }
    public ColaProcesos getColaBloqueados() { return colaBloqueados; }
    public ColaProcesos getColaTerminados() { return colaTerminados; }
    public Planificador getPlanificador() { return planificador; }
    public String getNombrePoliticaActual() { return nombrePoliticaActual; }
}