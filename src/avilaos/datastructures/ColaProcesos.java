package avilaos.datastructures;

/**
 * Clase que representa una Cola (Queue) basada en nodos.
 * Sigue el principio FIFO (First In, First Out), ideal para gestionar procesos.
 * 
 * @author Gustavo Abad
 */
public class ColaProcesos {
    private Nodo frente; // Apunta al primer elemento de la cola (el próximo a salir)
    private Nodo fin;    // Apunta al último elemento de la cola (el último que entró)
    private int tamaño;  // Cantidad de procesos en la cola

    /**
     * Constructor que inicializa una cola vacía.
     */
    public ColaProcesos() {
        this.frente = null;
        this.fin = null;
        this.tamaño = 0;
    }

    /**
     * Encola (inserta) un nuevo proceso al final de la cola.
     * @param proceso El proceso o dato a encolar.
     */
    public void encolar(Object proceso) {
        Nodo nuevoNodo = new Nodo(proceso);
        if (estaVacia()) {
            frente = nuevoNodo; // Si está vacía, el frente y el fin son el mismo nodo
        } else {
            fin.setSiguiente(nuevoNodo); // El último nodo actual ahora apunta al nuevo
        }
        fin = nuevoNodo; // El nuevo nodo se convierte en el fin definitivo
        tamaño++;
    }

    /**
     * Desencola (elimina y devuelve) el proceso que está al frente de la cola.
     * @return El proceso desencolado, o null si la cola está vacía.
     */
    public Object desencolar() {
        if (estaVacia()) {
            return null; // No hay nada que desencolar
        }
        Object procesoDesencolado = frente.getDato(); // Guardamos el dato a devolver
        frente = frente.getSiguiente(); // El frente pasa a ser el siguiente nodo
        
        if (frente == null) {
            fin = null; // Si sacamos el último elemento, el fin también queda vacío
        }
        tamaño--;
        return procesoDesencolado;
    }

    /**
     * Verifica si la cola está vacía.
     * @return true si no hay elementos, false en caso contrario.
     */
    public boolean estaVacia() {
        return frente == null;
    }

    /**
     * Obtiene la cantidad de elementos en la cola.
     * @return El tamaño actual de la cola.
     */
    public int getTamaño() {
        return tamaño;
    }
}