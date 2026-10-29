/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.model;

/**
 * Administra la red de nodos de computo (cluster) del simulador.
 * Controla el arreglo de objetos Computador y proporciona metodos para
 * balanceo de carga y monitoreo global del hardware sin usar java.util.
 * 
 * @author Daniel Alonso
 */
public class ClusterManager {
    
    private final Computador[] computadores;
    private int cantidadActual;

    public ClusterManager(int capacidadMaxima) {
        this.computadores = new Computador[capacidadMaxima];
        this.cantidadActual = 0;
    }

    /**
     * Agrega un nuevo nodo Computador al cluster.
     * @param computador Instancia del nodo a registrar.
     * @return true si se agrego con exito, false si el cluster esta lleno.
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
     * para implementar una estrategia basica de balanceo de carga.
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
     * Busca un nodo por su identificador unico.
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
}
