/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.gui;

import avilaos.services.MetricasService;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 * Componente grafico nativo en Swing (Graphics2D) que dibuja la curva de
 * desplazamiento acumulado del cabezal del disco en funcion del tiempo (ciclos).
 * 
 * @author Daniel Alonso
 */
public class PanelGraficaCabezal extends JPanel {

    private MetricasService metricasService;

    private static final Color COLOR_FONDO = Color.WHITE;
    private static final Color COLOR_CUADRICULA = new Color(226, 232, 240);
    private static final Color COLOR_EJES = new Color(100, 116, 139);
    private static final Color COLOR_TEXTO = new Color(30, 41, 59);
    private static final Color COLOR_LINEA = new Color(37, 99, 235);
    private static final Color COLOR_AREA = new Color(37, 99, 235, 38);
    private static final Color COLOR_PUNTO = new Color(16, 185, 129);

    public PanelGraficaCabezal(MetricasService metricasService) {
        this.metricasService = metricasService;
        setPreferredSize(new Dimension(540, 260));
        setBackground(COLOR_FONDO);
    }

    public void setMetricasService(MetricasService metricasService) {
        this.metricasService = metricasService;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int ancho = getWidth();
        int alto = getHeight();

        int margenIzq = 62;
        int margenDer = 24;
        int margenSup = 38;
        int margenInf = 42;

        int areaAncho = Math.max(10, ancho - margenIzq - margenDer);
        int areaAlto = Math.max(10, alto - margenSup - margenInf);

        // Titulo superior
        g2.setColor(COLOR_TEXTO);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.drawString("Desplazamiento Acumulado del Cabezal vs. Tiempo (Ciclos)", margenIzq, 22);

        int[] historial = (metricasService != null)
                ? metricasService.getHistorialDesplazamientoCabezal()
                : new int[0];

        if (historial.length <= 1) {
            dibujarEjesVacios(g2, margenIzq, margenSup, areaAncho, areaAlto);
            g2.setColor(COLOR_EJES);
            g2.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            String msg = "Sin datos suficientes: avanza ciclos de simulacion para visualizar la curva.";
            FontMetrics fm = g2.getFontMetrics();
            int msgX = margenIzq + Math.max(0, (areaAncho - fm.stringWidth(msg)) / 2);
            int msgY = margenSup + (areaAlto / 2);
            g2.drawString(msg, msgX, msgY);
            g2.dispose();
            return;
        }

        // Calcular escala maxima del eje Y
        int maxDesplazamiento = 10;
        for (int i = 0; i < historial.length; i++) {
            if (historial[i] > maxDesplazamiento) {
                maxDesplazamiento = historial[i];
            }
        }
        int techoY = ((maxDesplazamiento / 10) + 1) * 10;

        // Cuadricula horizontal y etiquetas Eje Y
        int divisionesY = 5;
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        for (int i = 0; i <= divisionesY; i++) {
            int y = margenSup + areaAlto - (i * areaAlto / divisionesY);
            int valorY = (i * techoY) / divisionesY;

            g2.setColor(COLOR_CUADRICULA);
            g2.drawLine(margenIzq, y, margenIzq + areaAncho, y);

            g2.setColor(COLOR_EJES);
            String etiqueta = String.valueOf(valorY);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(etiqueta, margenIzq - fm.stringWidth(etiqueta) - 8, y + 4);
        }

        // Cuadricula vertical y etiquetas Eje X (Ciclos)
        int totalCiclos = historial.length - 1;
        int divisionesX = Math.min(totalCiclos, 8);
        for (int i = 0; i <= divisionesX; i++) {
            int cicloIdx = (i * totalCiclos) / divisionesX;
            int x = margenIzq + (int) (((double) cicloIdx / totalCiclos) * areaAncho);

            g2.setColor(COLOR_CUADRICULA);
            g2.drawLine(x, margenSup, x, margenSup + areaAlto);

            g2.setColor(COLOR_EJES);
            String etiqueta = "C" + cicloIdx;
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(etiqueta, x - (fm.stringWidth(etiqueta) / 2), margenSup + areaAlto + 16);
        }

        // Ejes principales
        g2.setColor(COLOR_EJES);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(margenIzq, margenSup, margenIzq, margenSup + areaAlto);
        g2.drawLine(margenIzq, margenSup + areaAlto, margenIzq + areaAncho, margenSup + areaAlto);

        // Titulos de ejes
        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        g2.drawString("Bloques", 10, margenSup + 10);
        g2.drawString("Tiempo (Ciclos de Reloj)", margenIzq + (areaAncho / 2) - 55, alto - 8);

        // Construir coordenadas de la polilinea y area sombreada
        int[] puntosX = new int[historial.length];
        int[] puntosY = new int[historial.length];

        for (int i = 0; i < historial.length; i++) {
            puntosX[i] = margenIzq + (int) (((double) i / totalCiclos) * areaAncho);
            double proporcion = (double) historial[i] / techoY;
            puntosY[i] = margenSup + areaAlto - (int) (proporcion * areaAlto);
        }

        Polygon areaBajoCurva = new Polygon();
        areaBajoCurva.addPoint(puntosX[0], margenSup + areaAlto);
        for (int i = 0; i < historial.length; i++) {
            areaBajoCurva.addPoint(puntosX[i], puntosY[i]);
        }
        areaBajoCurva.addPoint(puntosX[historial.length - 1], margenSup + areaAlto);

        g2.setColor(COLOR_AREA);
        g2.fillPolygon(areaBajoCurva);

        // Dibujar linea de desplazamiento acumulado
        g2.setColor(COLOR_LINEA);
        g2.setStroke(new BasicStroke(2.2f));
        for (int i = 0; i < historial.length - 1; i++) {
            g2.drawLine(puntosX[i], puntosY[i], puntosX[i + 1], puntosY[i + 1]);
        }

        // Punto destacado en el ciclo actual
        int ultimoX = puntosX[historial.length - 1];
        int ultimoY = puntosY[historial.length - 1];
        int valorFinal = historial[historial.length - 1];

        g2.setColor(COLOR_PUNTO);
        g2.fillOval(ultimoX - 4, ultimoY - 4, 8, 8);

        g2.setColor(COLOR_TEXTO);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        String textoValor = valorFinal + " bloq.";
        FontMetrics fmVal = g2.getFontMetrics();
        int labelX = Math.min(ultimoX + 6, ancho - fmVal.stringWidth(textoValor) - 6);
        int labelY = Math.max(margenSup + 12, ultimoY - 6);
        g2.drawString(textoValor, labelX, labelY);

        g2.dispose();
    }

    private void dibujarEjesVacios(Graphics2D g2, int margenIzq, int margenSup, int areaAncho, int areaAlto) {
        g2.setColor(COLOR_CUADRICULA);
        g2.drawRect(margenIzq, margenSup, areaAncho, areaAlto);
        g2.setColor(COLOR_EJES);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(margenIzq, margenSup, margenIzq, margenSup + areaAlto);
        g2.drawLine(margenIzq, margenSup + areaAlto, margenIzq + areaAncho, margenSup + areaAlto);
    }
}