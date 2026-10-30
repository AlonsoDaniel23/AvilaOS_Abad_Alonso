/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.model;

import avilaos.datastructures.ListaEnlazada;
import avilaos.synchronization.BufferCircular;

/**
 * Administra la red de nodos de computo (cluster) del simulador.
 * Controla el arreglo de objetos Computador y proporciona metodos para
 * balanceo de carga y monitoreo global del hardware sin usar java.util.
 * 
 * @author Daniel Alonso
 */
public class ClusterManager {
    
    private static ClusterManager instancia;
    private final Computador[] computadores;
    private int cantidadActual;
    private final ListaEnlazada listaBuffers;

    public ClusterManager(int capacidadMaxima) {
        this.computadores = new Computador[capacidadMaxima];
        this.cantidadActual = 0;
        this.listaBuffers = new ListaEnlazada();
        instancia = this; // Guarda la referencia Singleton al instanciar
    }

    /**
     * Acceso Singleton global al ClusterManager.
     */
    public static synchronized ClusterManager getInstancia() {
        if (instancia == null) {
            instancia = new ClusterManager(4); // Capacidad por defecto de 4 nodos
        }
        return instancia;
    }

    /**
     * Registra un nuevo BufferCircular en el clúster.
     */
    public void registrarBuffer(BufferCircular buffer) {
        if (buffer != null) {
            this.listaBuffers.insertarFinal(buffer);
        }
    }

    /**
     * Busca un BufferCircular registrado en el clúster por su nombre.
     * @param nombreBuffer Nombre del buffer (ej: "B1").
     * @return Objeto BufferCircular o null si no se encuentra.
     */
    public BufferCircular buscarBuffer(String nombreBuffer) {
        if (nombreBuffer == null || listaBuffers == null) {
            return null;
        }
        for (int i = 0; i < listaBuffers.getTamaño(); i++) {
            BufferCircular b = (BufferCircular) listaBuffers.obtener(i);
            if (b != null && nombreBuffer.equalsIgnoreCase(b.getNombre())) {
                return b;
            }
        }
        return null;
    }

    /**
     * Agrega un nuevo nodo Computador al clúster.
     * @param computador Instancia del nodo a registrar.
     * @return true si se agregó con éxito, false si el clúster está lleno.
     */
    public boolean agregarComputador(Computador computador) {
        if (computador != null && cantidadActual < computadores.length) {
            computadores[cantidadActual] = computador;
            cantidadActual++;
            return true;
        }
        return false;
    }

    /**
     * Selecciona el nodo con menor cantidad de procesos en cola
     * para implementar una estrategia básica de balanceo de carga.
     * @return El Computador menos cargado o null si no hay nodos.
     */
    public Computador obtenerNodoMenosCargado() {
        if (cantidadActual == 0) {
            return null;
        }

        Computador menosCargado = computadores[0];
        int menorTamanioCola = menosCargado.getColaProcesos().getTamaño();

        for (int i = 1; i < cantidadActual; i++) {
            int tamanioActual = computadores[i].getColaProcesos().getTamaño();
            if (tamanioActual < menorTamanioCola) {
                menorTamanioCola = tamanioActual;
                menosCargado = computadores[i];
            }
        }
        return menosCargado;
    }

    /**
     * Busca un nodo por su identificador único.
     * @param idComputador ID del nodo a buscar.
     * @return El objeto Computador coincidente o null si no existe.
     */
    public Computador buscarPorId(int idComputador) {
        for (int i = 0; i < cantidadActual; i++) {
            if (computadores[i].getIdComputador() == idComputador) {
                return computadores[i];
            }
        }
        return null;
    }

    // Getters
    public Computador[] getComputadores() {
        return computadores;
    }

    public int getCantidadActual() {
        return cantidadActual;
    }

    public int getCapacidadMaxima() {
        return computadores.length;
    }

    public ListaEnlazada getListaBuffers() {
        return listaBuffers;
    }
}