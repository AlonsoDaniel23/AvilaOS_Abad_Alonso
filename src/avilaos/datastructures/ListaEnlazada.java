package avilaos.datastructures;

/**
 * Clase que representa una lista enlazada simple.
 * Permite almacenar y gestionar una secuencia de nodos.
 */
public class ListaEnlazada {
    private Nodo cabeza; // Referencia al primer nodo de la lista
    private int tamaño; // Contador de elementos en la lista

    /**
     * Constructor que inicializa una lista enlazada vacía.
     */
    public ListaEnlazada() {
        this.cabeza = null;
        this.tamaño = 0;
    }

    /**
     * Inserta un nuevo elemento al final de la lista.
     * @param dato El objeto a insertar.
     */
    public void insertarFinal(Object dato) {
        Nodo nuevoNodo = new Nodo(dato);
        if (cabeza == null) {
            cabeza = nuevoNodo; // Si está vacía, el nuevo nodo es la cabeza
        } else {
            Nodo actual = cabeza;
            // Recorrer hasta el último nodo
            while (actual.getSiguiente() != null) {
                actual = actual.getSiguiente();
            }
            actual.setSiguiente(nuevoNodo); // Enlazar el último nodo con el nuevo
        }
        tamaño++;
    }

    /**
     * Verifica si la lista está vacía.
     * @return true si está vacía, false en caso contrario.
     */
    public boolean estaVacia() {
        return cabeza == null;
    }

    /**
     * Obtiene el tamaño actual de la lista.
     * @return El número de nodos en la lista.
     */
    public int getTamaño() {
        return tamaño;
    }
}