/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.services;

/**
 * Registro centralizado de eventos del simulador AvilaOS.
 * Almacena mensajes de auditoria y decisiones del sistema usando un buffer circular
 * sobre arreglos nativos, garantizando compatibilidad sin usar java.util.
 * 
 * @author Daniel Alonso
 */
public class EventLog {
    
    private final String[] eventos;
    private int cabeza;
    private int cantidad;
    private final int capacidad;

    public EventLog(int capacidadMaxima) {
        this.capacidad = capacidadMaxima;
        this.eventos = new String[capacidadMaxima];
        this.cabeza = 0;
        this.cantidad = 0;
    }

    /**
     * Registra un nuevo evento en la consola circular de logs.
     * @param cicloActual Ciclo del reloj global.
     * @param mensaje Descripcion del evento.
     */
    public synchronized void registrar(int cicloActual, String mensaje) {
        String formato = "[Ciclo " + cicloActual + "] " + mensaje;
        if (cantidad < capacidad) {
            eventos[cantidad] = formato;
            cantidad++;
        } else {
            // Sobreescribe el evento mas antiguo cuando el buffer se llena
            eventos[cabeza] = formato;
            cabeza = (cabeza + 1) % capacidad;
        }
    }

    /**
     * Obtiene todos los eventos ordenados cronologicamente.
     * @return Arreglo nativo de cadenas con los eventos.
     */
    public String[] obtenerEventos() {
        String[] resultado = new String[cantidad];
        if (cantidad < capacidad) {
            for (int i = 0; i < cantidad; i++) {
                resultado[i] = eventos[i];
            }
        } else {
            int idx = 0;
            for (int i = cabeza; i < capacidad; i++) {
                resultado[idx++] = eventos[i];
            }
            for (int i = 0; i < cabeza; i++) {
                resultado[idx++] = eventos[i];
            }
        }
        return resultado;
    }

    public int getCantidad() {
        return cantidad;
    }
}
