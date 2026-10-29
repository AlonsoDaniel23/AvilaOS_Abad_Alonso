/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package avilaos.interfaces;

/**
 * Interfaz obligatoria para los componentes que reaccionan
 * a cada ciclo emitido por el Reloj Global del sistema.
 * @author Daniel Alonso
 */
public interface IRelojListener {
    /**
     * Avanza un ciclo de simulacion.
     * @param cicloActual Numero de ciclo global transcurrido.
     */
    void tick(int cicloActual);
}
