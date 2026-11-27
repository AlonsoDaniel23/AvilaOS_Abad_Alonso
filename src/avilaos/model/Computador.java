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
import avilaos.synchronization.RelojGlobal;

/**
 * Representa el computador unico del simulador AvilaOS.
 * Integra CPU, RAM, DiscoSwap, colas de 7 estados y planificadores
 * a corto y mediano plazo.
 * 
 * @author Daniel Alonso
 */
public class Computador implements IRelojListener {

    private static Computador instancia;

    private final int idComputador;
    private final String nombre;
    private final CPU cpu;
    private final MemoriaRAM ram;
    private final DiscoSwap discoSwap;
    private final ColaProcesos colaNuevos;
    private final ColaProcesos colaProcesos;
    private final ColaProcesos colaBloqueados;
    private final ColaProcesos colaSuspendidosListos;
    private final ColaProcesos colaSuspendidosBloqueados;
    private final ColaProcesos colaTerminados;
    private Planificador planificador;
    private final PlanificadorMedianoPlazo planificadorMedianoPlazo;
    private String nombrePoliticaActual;

    public Computador(int idComputador, String nombre, int tamanioRamKB, int tamanioSwapKB) {
        this.idComputador = idComputador;
        this.nombre = nombre;
        this.cpu = new CPU(idComputador);
        this.ram = new MemoriaRAM(tamanioRamKB, 4);
        this.discoSwap = new DiscoSwap(tamanioSwapKB, 4);
        this.colaNuevos = new ColaProcesos();
        this.colaProcesos = new ColaProcesos();
        this.colaBloqueados = new ColaProcesos();
        this.colaSuspendidosListos = new ColaProcesos();
        this.colaSuspendidosBloqueados = new ColaProcesos();
        this.colaTerminados = new ColaProcesos();

        this.planificador = new PlanificadorFCFS();
        this.planificadorMedianoPlazo = new PlanificadorMedianoPlazo();
        this.nombrePoliticaActual = "FCFS";

        instancia = this;
    }

    /**
     * Retorna la instancia unica del computador en el sistema.
     */
    public static synchronized Computador getInstancia() {
        if (instancia == null) {
            instancia = new Computador(1, "AvilaOS-PC", 256, 512);
        }
        return instancia;
    }

    /**
     * Encola un nuevo proceso en estado NUEVO e intenta asignarle memoria RAM.
     */
    public void encolarProceso(Proceso proceso) {
        if (proceso != null) {
            proceso.setEstado(EstadoProceso.NUEVO);
            this.colaNuevos.encolar(proceso);
            revisarColaNuevos();
        }
    }

    /**
     * Admite procesos nuevos en RAM; si no hay espacio suficiente, activa Swap-Out.
     */
    public void revisarColaNuevos() {
        int cicloActual = RelojGlobal.getInstancia().getCicloActual();
        while (!colaNuevos.estaVacia()) {
            Proceso p = (Proceso) colaNuevos.desencolar();
            if (p != null) {
                int paginas = (p.getTablaPaginas() != null) ? p.getTablaPaginas().length : 4;

                if (!ram.tieneEspacio(paginas)) {
                    cpu.activarModoKernel();
                    planificadorMedianoPlazo.intentarLiberarRAM(
                            ram, discoSwap, colaBloqueados, colaSuspendidosBloqueados, paginas, cicloActual);
                    if (!cpu.isOcupado()) {
                        cpu.activarModoUsuario();
                    }
                }

                if (ram.tieneEspacio(paginas)) {
                    ram.asignarMemoriaProceso(p.getId(), paginas);
                    p.setEstado(EstadoProceso.LISTO);
                    agregarAListos(p);
                } else {
                    colaNuevos.encolar(p);
                    break;
                }
            }
        }
    }

    /**
     * Revisa la Cola de Bloqueados y promueve a Listos los procesos cuya E/S ya finalizo.
     */
    public void revisarColaBloqueados() {
        int tamanioInicial = colaBloqueados.getTamaño();
        for (int i = 0; i < tamanioInicial; i++) {
            Proceso p = (Proceso) colaBloqueados.desencolar();
            if (p != null) {
                if (p.isOperacionESCompletada()) {
                    p.setMotivoBloqueo("");
                    p.setEstado(EstadoProceso.LISTO);
                    agregarAListos(p);
                    EventLog.getInstancia().registrar("[Ciclo " + RelojGlobal.getInstancia().getCicloActual()
                            + "] Proceso " + p.getNombre() + " completo su E/S y paso a Cola de Listos.");
                } else {
                    p.incrementarCiclosBloqueado();
                    colaBloqueados.encolar(p);
                }
            }
        }
    }

    /**
     * Incorpora un proceso reactivado desde Swap hacia la cola de listos.
     */
    public void admitirProcesoReactivado(Proceso p) {
        if (p != null) {
            agregarAListos(p);
        }
    }

    /**
     * Inserta un proceso en el planificador activo o en la cola de listos por defecto.
     */
    private void agregarAListos(Proceso p) {
        if (planificador != null) {
            planificador.agregarProceso(p);
        } else {
            colaProcesos.encolar(p);
        }
    }

    /**
     * Cambia la politica de planificacion activa y migra los procesos pendientes.
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
     * Extrae el siguiente proceso a ejecutar segun la politica de planificacion activa.
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
     * Retorna la cantidad de procesos en espera dentro de la Cola de Listos.
     */
    public int getCantidadProcesosListos() {
        if (planificador != null) {
            return planificador.getCantidadProcesos();
        }
        return (colaProcesos != null) ? colaProcesos.getTamaño() : 0;
    }

    /**
     * Indica si existen procesos listos pendientes por ejecutar.
     */
    public boolean tieneProcesosEnEspera() {
        if (planificador != null) {
            return planificador.hayProcesos();
        }
        return !colaProcesos.estaVacia();
    }

    /**
     * Retorna la cola de listos activa para renderizado en la interfaz grafica.
     */
    public ColaProcesos getColaListosEfectiva() {
        if (planificador != null && planificador.getCola() != null) {
            return planificador.getCola();
        }
        return colaProcesos;
    }

    @Override
    public void tick(int cicloActual) {
        // 0. Gestion de estados suspendidos y bloqueados (Mediano y Corto Plazo)
        planificadorMedianoPlazo.actualizarSuspendidosBloqueados(
                colaSuspendidosBloqueados, colaSuspendidosListos, cicloActual);
        revisarColaBloqueados();
        planificadorMedianoPlazo.reactivarProcesos(
                ram, discoSwap, colaSuspendidosListos, this, cicloActual);
        revisarColaNuevos();

        // 1. Si la CPU esta libre, despachar el siguiente proceso listo
        if (!cpu.isOcupado() && tieneProcesosEnEspera()) {
            Proceso siguiente = despacharSiguiente();
            if (siguiente != null) {
                siguiente.setEstado(EstadoProceso.EJECUCION);
                cpu.cargarProceso(siguiente);
            }
        }

        // 2. Ejecutar ciclo del proceso actual en CPU
        if (cpu.isOcupado() && cpu.getProcesoActual() != null) {
            Proceso procesoActual = cpu.getProcesoActual();

            if (procesoActual.getTipo() == TipoProceso.IO_BOUND && !procesoActual.isSolicitudDiscoEnviada()) {
                procesarLlamadaSistemaES(procesoActual, cicloActual);
                return;
            }

            procesoActual.ejecutarCiclo();
            cpu.incrementarPC();

            if (procesoActual.esFinalizado()) {
                procesoActual.setEstado(EstadoProceso.TERMINADO);
                colaTerminados.encolar(procesoActual);

                ram.liberarMemoriaProceso(procesoActual.getId());
                cpu.liberar();

                // Al liberar RAM, priorizar reactivacion desde Swap y luego procesos nuevos
                planificadorMedianoPlazo.reactivarProcesos(
                        ram, discoSwap, colaSuspendidosListos, this, cicloActual);
                revisarColaNuevos();

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
     * Cambia la CPU a Modo Kernel (SISTEMA_OPERATIVO) para atender una solicitud de E/S
     * y encola la solicitud real en DiscoSimulado.
     */
    private void procesarLlamadaSistemaES(Proceso p, int cicloActual) {
        cpu.activarModoKernel();

        avilaos.enums.TipoOperacionES opEnum = (p.getOperacionES() != null)
                ? p.getOperacionES() : avilaos.enums.TipoOperacionES.LEER;
        String ruta = (p.getRutaArchivo() != null && !p.getRutaArchivo().isEmpty())
                ? p.getRutaArchivo() : "/tarea.txt";

        int bloqueObjetivo = 0;
        int cantidadBloques = Math.max(1, p.getBloquesSolicitadosES());
        EntradaSistemaArchivos entrada = SistemaArchivos.getInstancia().buscarPorRuta(ruta);
        if (entrada != null && !entrada.esDirectorio()) {
            Archivo arch = (Archivo) entrada;
            bloqueObjetivo = Math.max(0, arch.getPrimerBloque());
            cantidadBloques = Math.max(1, arch.getTamanoBloques());
        }

        SolicitudDisco solicitud = new SolicitudDisco(
                p, opEnum, ruta, bloqueObjetivo, cantidadBloques, cicloActual);
        DiscoSimulado.getInstancia().encolarSolicitud(solicitud);

        p.setSolicitudDiscoEnviada(true);
        p.setMotivoBloqueo("E/S Disco (" + opEnum.name() + ")");
        p.setEstado(EstadoProceso.BLOQUEADO);

        EventLog.getInstancia().registrar("[Ciclo " + cicloActual + "] [KERNEL] Proceso " + p.getNombre()
                + " solicita E/S (" + opEnum.name() + ") sobre '" + ruta
                + "' (Bloque #" + bloqueObjetivo + "). Pasa a BLOQUEADO.");

        colaBloqueados.encolar(p);

        cpu.liberar();
        if (tieneProcesosEnEspera()) {
            Proceso siguiente = despacharSiguiente();
            if (siguiente != null) {
                siguiente.setEstado(EstadoProceso.EJECUCION);
                cpu.cargarProceso(siguiente);
            }
        }
    }

    /**
     * Notifica que un proceso termino su operacion de E/S en el disco para despertarlo
     * y extraerlo de la cola de bloqueados sin duplicarlo.
     */
    public void desbloquearProceso(Proceso p) {
        if (p == null) {
            return;
        }
        p.setOperacionESCompletada(true);
        p.setMotivoBloqueo("");

        if (p.getEstado() == EstadoProceso.SUSPENDIDO_BLOQUEADO) {
            p.setEstado(EstadoProceso.SUSPENDIDO_LISTO);
            colaSuspendidosListos.encolar(p);
        } else if (p.getEstado() == EstadoProceso.BLOQUEADO) {
            // Extraerlo de colaBloqueados inmediatamente para evitar que revisarColaBloqueados() lo duplique
            int tam = colaBloqueados.getTamaño();
            for (int i = 0; i < tam; i++) {
                Proceso actual = (Proceso) colaBloqueados.desencolar();
                if (actual != null && actual.getId() != p.getId()) {
                    colaBloqueados.encolar(actual);
                }
            }
            p.setEstado(EstadoProceso.LISTO);
            agregarAListos(p);
            EventLog.getInstancia().registrar("[Ciclo " + RelojGlobal.getInstancia().getCicloActual()
                    + "] Proceso " + p.getNombre() + " completo su E/S y paso a Cola de Listos.");
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
    public ColaProcesos getColaSuspendidosListos() { return colaSuspendidosListos; }
    public ColaProcesos getColaSuspendidosBloqueados() { return colaSuspendidosBloqueados; }
    public ColaProcesos getColaTerminados() { return colaTerminados; }
    public Planificador getPlanificador() { return planificador; }
    public PlanificadorMedianoPlazo getPlanificadorMedianoPlazo() { return planificadorMedianoPlazo; }
    public String getNombrePoliticaActual() { return nombrePoliticaActual; }
}