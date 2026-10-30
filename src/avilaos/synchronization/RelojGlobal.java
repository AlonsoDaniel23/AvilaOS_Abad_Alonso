/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.synchronization;

import avilaos.datastructures.ListaEnlazada;
import avilaos.interfaces.IRelojListener;

/**
 * Emite los pulsos de tiempo que sincronizan a todos los computadores
 * del clúster en la simulación.
 * @author Daniel Alonso
 */
public class RelojGlobal implements Runnable {

    private static RelojGlobal instancia;

    private int cicloActual;
    private int duracionCicloMs;
    private volatile boolean enEjecucion;
    private volatile boolean pausado;
    
    private ListaEnlazada suscriptores; 
    private Thread hiloReloj;

    public RelojGlobal(int duracionCicloMs) {
        this.cicloActual = 0;
        this.duracionCicloMs = duracionCicloMs;
        this.enEjecucion = false;
        this.pausado = false;
        this.suscriptores = new ListaEnlazada();
        instancia = this; // Guarda la referencia global al instanciar
    }

    /**
     * Acceso Singleton a la instancia única del RelojGlobal.
     */
    public static synchronized RelojGlobal getInstancia() {
        if (instancia == null) {
            instancia = new RelojGlobal(1000); // 1000 ms por defecto
        }
        return instancia;
    }

    public synchronized void registrarSuscriptor(IRelojListener suscriptor) {
        this.suscriptores.insertarFinal(suscriptor);
    }

    public synchronized void iniciar() {
        if (!enEjecucion) {
            enEjecucion = true;
            pausado = false;
            hiloReloj = new Thread(this, "Hilo-RelojGlobal");
            hiloReloj.start();
        }
    }

    public void pausar() {
        this.pausado = true;
    }

    public void reanudar() {
        this.pausado = false;
    }

    public void detener() {
        this.enEjecucion = false;
    }

    @Override
    public void run() {
        while (enEjecucion) {
            if (!pausado) {
                cicloActual++;
                notificarSuscriptores();
            }

            try {
                Thread.sleep(duracionCicloMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void notificarSuscriptores() {
        for (int i = 0; i < suscriptores.getTamaño(); i++) {
            IRelojListener listener = (IRelojListener) suscriptores.obtener(i);
            if (listener != null) {
                listener.tick(cicloActual);
            }
        }
    }

    // Getters y Setters
    public int getCicloActual() {
        return cicloActual;
    }

    public int getDuracionCicloMs() {
        return duracionCicloMs;
    }

    public void setDuracionCicloMs(int duracionCicloMs) {
        this.duracionCicloMs = duracionCicloMs;
    }

    public boolean isPausado() {
        return pausado;
    }
}