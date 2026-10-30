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
    private int latenciaRed; // <--- Parámetro configurable de latencia en ciclos de reloj

    public ClusterManager(int capacidadMaxima) {
        this.computadores = new Computador[capacidadMaxima];
        this.cantidadActual = 0;
        this.listaBuffers = new ListaEnlazada();
        this.latenciaRed = 2; // 2 ciclos de latencia de red por defecto
        instancia = this;
    }

    public static synchronized ClusterManager getInstancia() {
        if (instancia == null) {
            instancia = new ClusterManager(4);
        }
        return instancia;
    }

    public void registrarBuffer(BufferCircular buffer) {
        if (buffer != null) {
            this.listaBuffers.insertarFinal(buffer);
        }
    }

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

    public boolean agregarComputador(Computador computador) {
        if (computador != null && cantidadActual < computadores.length) {
            computadores[cantidadActual] = computador;
            cantidadActual++;
            return true;
        }
        return false;
    }

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

    public Computador buscarPorId(int idComputador) {
        for (int i = 0; i < cantidadActual; i++) {
            if (computadores[i].getIdComputador() == idComputador) {
                return computadores[i];
            }
        }
        return null;
    }

    // Getters y Setters
    public Computador[] getComputadores() { return computadores; }
    public int getCantidadActual() { return cantidadActual; }
    public int getCapacidadMaxima() { return computadores.length; }
    public ListaEnlazada getListaBuffers() { return listaBuffers; }
    public int getLatenciaRed() { return latenciaRed; }
    public void setLatenciaRed(int latenciaRed) { this.latenciaRed = latenciaRed; }
}