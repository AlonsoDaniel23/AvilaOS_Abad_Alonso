/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.services;

import avilaos.synchronization.RelojGlobal;

/**
 * Registro centralizado de eventos del simulador AvilaOS.
 * Almacena mensajes de auditoria y decisiones del sistema usando un buffer circular
 * sobre arreglos nativos, garantizando compatibilidad sin usar java.util.
 * 
 * @author Daniel Alonso
 */
public class EventLog {
    
    private static EventLog instancia;
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
     * Acceso Singleton a la instancia global de auditoría.
     */
    public static synchronized EventLog getInstancia() {
        if (instancia == null) {
            instancia = new EventLog(100); // 100 eventos por defecto
        }
        return instancia;
    }

    /**
     * Registra un nuevo evento especificando manualmente el ciclo del reloj.
     */
    public synchronized void registrar(int cicloActual, String mensaje) {
        String formato = "[Ciclo " + cicloActual + "] " + mensaje;
        if (cantidad < capacidad) {
            eventos[cantidad] = formato;
            cantidad++;
        } else {
            // Sobreescribe el evento más antiguo cuando el buffer se llena
            eventos[cabeza] = formato;
            cabeza = (cabeza + 1) % capacidad;
        }
    }

    /**
     * Sobrecarga conveniente para registrar tomando el ciclo actual del RelojGlobal.
     */
    public synchronized void registrar(String mensaje) {
        int cicloActual = (RelojGlobal.getInstancia() != null) ? RelojGlobal.getInstancia().getCicloActual() : 0;
        registrar(cicloActual, mensaje);
    }

    /**
     * Obtiene todos los eventos ordenados cronológicamente.
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