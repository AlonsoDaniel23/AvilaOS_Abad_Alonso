package avilaos.synchronization;

/**
 * Implementacion de un Semaforo contador para el control de concurrencia y exclusion mutua.
 * 
 * @author Gustavo Abad
 */
public class Semaforo {
    
    private int valor;

    /**
     * Inicializa el semaforo con un numero determinado de permisos.
     * 
     * @param valorInicial Cantidad de permisos iniciales.
     */
    public Semaforo(int valorInicial) {
        this.valor = valorInicial;
    }

    /**
     * Operacion wait (P). Decrementa el contador o bloquea el hilo si no hay permisos disponibles.
     */
    public synchronized void waitSemaforo() {
        while (this.valor <= 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        this.valor--;
    }

    /**
     * Operacion signal (V). Incremente el contador de permisos y notifica a los hilos en espera.
     */
    public synchronized void signalSemaforo() {
        this.valor++;
        notify();
    }

    /**
     * Consulta el valor actual del contador del semaforo.
     * 
     * @return Numero de permisos disponibles.
     */
    public synchronized int getValor() {
        return valor;
    }
}