/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.gui;

import avilaos.datastructures.ColaProcesos;
import avilaos.datastructures.ListaEnlazada;
import avilaos.enums.EstadoProceso;
import avilaos.model.ClusterManager;
import avilaos.model.Computador;
import avilaos.model.MemoriaRAM;
import avilaos.model.Proceso;
import avilaos.services.EventLog;
import avilaos.services.MetricasService;
import avilaos.synchronization.BufferCircular;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Vista 2: Panel principal de monitoreo en tiempo real del cluster ÁvilaOS (Requerimiento 6).
 * Permite visualizar el reloj global, CPU, colas de procesos, ocupacion de RAM y selector
 * dinamico de politicas de planificacion por computador.
 * Incluye temporizador Swing para avance de reloj en tiempo real e integración de eventos.
 * Sincronizado con las firmas reales de Proceso.java, MemoriaRAM.java y ClusterManager.java.
 * 
 * @author Daniel Alonso
 * @version 1.8
 */
public class Vista2Monitoreo extends JFrame {
    
    private ClusterManager clusterManager;
    private EventLog eventLog;
    private MetricasService metricasService;
    private int cicloRelojGlobal;
    private boolean simulacionEnEjecucion;

    // Motor de Simulación
    private Timer timerSimulacion;
    private int velocidadCicloMs = 1000;

    // Componentes de Control Global
    private JLabel lblCicloReloj;
    private JButton btnPausarReanudar;
    private JButton btnAvanzarCiclo;
    private JButton btnVerMetricas;

    // Contenedores Principales
    private JPanel panelNodosContainer;
    private JTextArea txtLogEventos;

    // Colores del Tema
    private static final Color COLOR_HEADER = new Color(15, 23, 42);
    private static final Color COLOR_BG = new Color(248, 250, 252);
    private static final Color COLOR_CARD_BG = Color.WHITE;
    private static final Color COLOR_TEXT_HEADER = new Color(30, 41, 59);
    private static final Color COLOR_BORDER = new Color(226, 232, 240);
    private static final Color COLOR_PRIMARY = new Color(37, 99, 235);
    private static final Color COLOR_SUCCESS = new Color(16, 185, 129);
    private static final Color COLOR_DANGER = new Color(239, 68, 68);
    private static final Color COLOR_WARNING = new Color(245, 158, 11);
    private static final Color COLOR_SECONDARY = new Color(71, 85, 105);

    public Vista2Monitoreo(ClusterManager clusterManager, EventLog eventLog, MetricasService metricasService) {
        super("ÁvilaOS V2 - Panel de Monitoreo de Clúster y Procesos");
        this.clusterManager = (clusterManager != null) ? clusterManager : crearClusterEjemplo();
        this.eventLog = (eventLog != null) ? eventLog : new EventLog(100);
        this.metricasService = (metricasService != null) ? metricasService : new MetricasService();
        this.cicloRelojGlobal = 0;
        this.simulacionEnEjecucion = false;

        aplicarLookAndFeel();
        configurarVentana();
        construirInterfaz();
        inicializarTimerSimulacion();
    }

    public Vista2Monitoreo() {
        this(null, null, null);
    }

    private ClusterManager crearClusterEjemplo() {
        ClusterManager cm = new ClusterManager(2);
        cm.agregarComputador(new Computador(1, "Computador 1", 1024, 2048));
        cm.agregarComputador(new Computador(2, "Computador 2", 1024, 2048));
        return cm;
    }

    private void aplicarLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    private void configurarVentana() {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(1240, 800);
        this.setLocationRelativeTo(null);
        this.getContentPane().setBackground(COLOR_BG);
        this.setLayout(new BorderLayout(0, 0));
    }

    private void construirInterfaz() {
        this.add(crearPanelEncabezadoReloj(), BorderLayout.NORTH);

        panelNodosContainer = new JPanel(new BorderLayout(0, 10));
        panelNodosContainer.setOpaque(false);
        
        panelNodosContainer.add(crearPanelBuffersGlobales(), BorderLayout.NORTH);
        panelNodosContainer.add(crearPanelNodosCluster(), BorderLayout.CENTER);
        
        this.add(panelNodosContainer, BorderLayout.CENTER);
        this.add(crearPanelInferiorLogs(), BorderLayout.SOUTH);
    }

    private void inicializarTimerSimulacion() {
        this.timerSimulacion = new Timer(velocidadCicloMs, (ActionEvent e) -> {
            ejecutarCicloSimulacion();
        });
    }

    private void ejecutarCicloSimulacion() {
        cicloRelojGlobal++;
        lblCicloReloj.setText("Reloj Global del Clúster: Ciclo " + cicloRelojGlobal);

        Computador[] nodos = clusterManager.getComputadores();
        for (int i = 0; i < clusterManager.getCantidadActual(); i++) {
            Computador nodo = nodos[i];
            if (nodo != null) {
                Proceso activoPrevio = nodo.getCpu().getProcesoActual();
                int terminadosPrevio = (nodo.getColaTerminados() != null) ? nodo.getColaTerminados().getTamaño() : 0;

                nodo.tick(cicloRelojGlobal);

                // Registrar utilización instantánea de CPU para la gráfica vs tiempo (Requerimiento 8)
                double usoCpu = nodo.getCpu().isOcupado() ? 100.0 : 0.0;
                metricasService.registrarUsoCpuNodo(nodo.getIdComputador(), cicloRelojGlobal, usoCpu);

                Proceso activoActual = nodo.getCpu().getProcesoActual();
                int terminadosActual = (nodo.getColaTerminados() != null) ? nodo.getColaTerminados().getTamaño() : 0;

                if (activoPrevio != activoActual && activoActual != null) {
                    eventLog.registrar(cicloRelojGlobal, nodo.getNombre() + ": Proceso " + activoActual.getNombre() + 
                        " en ejecución en CPU bajo política (" + nodo.getNombrePoliticaActual() + ").");
                }

                if (terminadosActual > terminadosPrevio) {
                    Proceso ultimoTerminado = (Proceso) nodo.getColaTerminados().obtener(terminadosActual - 1);
                    if (ultimoTerminado != null) {
                        int tiempoRespuesta = cicloRelojGlobal - ultimoTerminado.getTiempoLlegada();
                        boolean cumplioDeadline = (ultimoTerminado.getDeadline() <= 0) || (cicloRelojGlobal <= ultimoTerminado.getDeadline());
                        metricasService.registrarProcesoTerminado(tiempoRespuesta, cumplioDeadline);
                        eventLog.registrar(cicloRelojGlobal, nodo.getNombre() + ": Proceso " + ultimoTerminado.getNombre() + " ha FINALIZADO su ejecución.");
                    }
                }
            }
        }

        // Refresco dinámico de paneles
        panelNodosContainer.removeAll();
        panelNodosContainer.add(crearPanelBuffersGlobales(), BorderLayout.NORTH);
        panelNodosContainer.add(crearPanelNodosCluster(), BorderLayout.CENTER);
        panelNodosContainer.revalidate();
        panelNodosContainer.repaint();

        actualizarVisorLogs();
    }

    private JPanel crearPanelEncabezadoReloj() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_HEADER);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));

        JPanel panelIzquierda = new JPanel(new GridLayout(2, 1, 0, 2));
        panelIzquierda.setOpaque(false);

        JLabel lblTitulo = new JLabel("ÁVILAOS: MONITOR DE HARDWARE Y CLÚSTER DISTRIBUIDO");
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 16));

        lblCicloReloj = new JLabel("Reloj Global del Clúster: Ciclo " + cicloRelojGlobal);
        lblCicloReloj.setForeground(COLOR_SUCCESS);
        lblCicloReloj.setFont(new Font("Segoe UI", Font.BOLD, 13));

        panelIzquierda.add(lblTitulo);
        panelIzquierda.add(lblCicloReloj);

        JPanel panelBotonera = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panelBotonera.setOpaque(false);

        btnVerMetricas = crearBotonFlat("📊 Métricas", COLOR_SECONDARY, Color.WHITE);
        btnAvanzarCiclo = crearBotonFlat("Siguiente Ciclo  →", COLOR_PRIMARY, Color.WHITE);
        btnPausarReanudar = crearBotonFlat("Iniciar Simulación", COLOR_SUCCESS, Color.WHITE);

        btnVerMetricas.addActionListener(e -> mostrarVentanaMetricas());
        btnAvanzarCiclo.addActionListener(this::accionAvanzarCiclo);
        btnPausarReanudar.addActionListener(this::accionPausarReanudar);

        panelBotonera.add(btnVerMetricas);
        panelBotonera.add(btnAvanzarCiclo);
        panelBotonera.add(btnPausarReanudar);

        panel.add(panelIzquierda, BorderLayout.WEST);
        panel.add(panelBotonera, BorderLayout.EAST);

        return panel;
    }

    /**
     * Panel Global para la visualización de Buffers Compartidos y Semáforos (Requerimiento 6).
     */
    private JPanel crearPanelBuffersGlobales() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(COLOR_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(10, 15, 0, 15),
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                "📦 ESTADO GLOBAL DE BUFFERS COMPARTIDOS Y SEMÁFOROS",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 11), COLOR_TEXT_HEADER)
        ));

        ListaEnlazada listaBuf = clusterManager.getListaBuffers();
        int totalBuffers = (listaBuf != null) ? listaBuf.getTamaño() : 0;

        if (totalBuffers == 0) {
            JLabel lblEmpty = new JLabel("No hay buffers registrados en la simulación.", SwingConstants.CENTER);
            lblEmpty.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            lblEmpty.setForeground(COLOR_SECONDARY);
            lblEmpty.setPreferredSize(new Dimension(300, 40));
            panel.add(lblEmpty, BorderLayout.CENTER);
            return panel;
        }

        JPanel gridBuffers = new JPanel(new GridLayout(1, Math.max(totalBuffers, 1), 10, 0));
        gridBuffers.setOpaque(false);

        for (int i = 0; i < totalBuffers; i++) {
            BufferCircular b = (BufferCircular) listaBuf.obtener(i);
            if (b != null) {
                gridBuffers.add(crearTarjetaBuffer(b));
            }
        }

        JScrollPane scroll = new JScrollPane(gridBuffers);
        scroll.setBorder(null);
        scroll.getHorizontalScrollBar().setUnitIncrement(12);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearTarjetaBuffer(BufferCircular b) {
        JPanel tarjeta = new JPanel(new BorderLayout(5, 4));
        tarjeta.setBackground(Color.WHITE);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_PRIMARY, 1, true),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        JLabel lblTitulo = new JLabel("Buffer: " + b.getNombre() + " (Anfitrión: Nodo " + b.getIdComputadorAnfitrion() + ")");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTitulo.setForeground(COLOR_PRIMARY);

        int elemAlmacenados = b.getItems().getValor();
        int cap = b.getCapacidad();
        int valMutex = b.getMutex().getValor();
        int valItems = b.getItems().getValor();
        int valEspacios = b.getEspacios().getValor();

        StringBuilder sbBloq = new StringBuilder();
        int contBloq = 0;
        Computador[] nodos = clusterManager.getComputadores();
        for (int i = 0; i < clusterManager.getCantidadActual(); i++) {
            Computador c = nodos[i];
            if (c != null && c.getColaBloqueados() != null) {
                for (int k = 0; k < c.getColaBloqueados().getTamaño(); k++) {
                    Proceso p = (Proceso) c.getColaBloqueados().obtener(k);
                    if (p != null && b.getNombre().equalsIgnoreCase(p.getNombreBuffer())) {
                        if (contBloq > 0) sbBloq.append(", ");
                        sbBloq.append(p.getNombre()).append(" (N").append(c.getIdComputador()).append(")");
                        contBloq++;
                    }
                }
            }
        }

        String txtBloq = (contBloq > 0) ? sbBloq.toString() : "Ninguno";

        JPanel panelDetalles = new JPanel(new GridLayout(3, 1, 2, 2));
        panelDetalles.setOpaque(false);

        panelDetalles.add(crearLabelEtiqueta("Ocupación: " + elemAlmacenados + " / " + cap + " elementos"));
        panelDetalles.add(crearLabelValor("Semáforos: [Mutex: " + valMutex + " | Items: " + valItems + " | Espacios: " + valEspacios + "]", COLOR_TEXT_HEADER));
        panelDetalles.add(crearLabelEtiqueta("Bloqueados (" + contBloq + "): " + txtBloq));

        tarjeta.add(lblTitulo, BorderLayout.NORTH);
        tarjeta.add(panelDetalles, BorderLayout.CENTER);

        return tarjeta;
    }

    /**
     * Muestra el diálogo modal de Métricas integrando la Gráfica de Utilización de CPU vs Tiempo (Requerimiento 8).
     */
    private void mostrarVentanaMetricas() {
        JDialog dialog = new JDialog(this, "Estadísticas y Métricas del Clúster", true);
        dialog.setSize(640, 540);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.getContentPane().setBackground(COLOR_BG);

        JPanel panelTitulo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelTitulo.setBackground(COLOR_HEADER);
        JLabel lblTitulo = new JLabel("📊 MÉTRICAS Y RENDIMIENTO DEL SISTEMA");
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panelTitulo.add(lblTitulo);

        // Panel numérico superior
        JPanel panelGrid = new JPanel(new GridLayout(3, 2, 10, 6));
        panelGrid.setBorder(BorderFactory.createEmptyBorder(10, 20, 5, 20));
        panelGrid.setOpaque(false);

        int completados = metricasService.getProcesosCompletadosTotales();
        double throughput = metricasService.calcularThroughput(cicloRelojGlobal);
        double tasaDeadlines = metricasService.calcularTasaCumplimientoDeadlines();
        double tiempoRespProm = metricasService.calcularTiempoRespuestaPromedio();
        int producidos = metricasService.getElementosProducidosTotales();
        int consumidos = metricasService.getElementosConsumidosTotales();

        panelGrid.add(crearLabelEtiqueta("Procesos Completados:"));
        panelGrid.add(crearLabelValor(completados + " proceso(s)", COLOR_PRIMARY));

        panelGrid.add(crearLabelEtiqueta("Throughput Global:"));
        panelGrid.add(crearLabelValor(String.format("%.3f proc/ciclo", throughput), COLOR_SUCCESS));

        panelGrid.add(crearLabelEtiqueta("Cumplimiento Deadlines:"));
        panelGrid.add(crearLabelValor(String.format("%.1f %%", tasaDeadlines), tasaDeadlines < 100 ? COLOR_WARNING : COLOR_SUCCESS));

        panelGrid.add(crearLabelEtiqueta("Tiempo Resp. Promedio:"));
        panelGrid.add(crearLabelValor(String.format("%.2f ciclos", tiempoRespProm), COLOR_TEXT_HEADER));

        panelGrid.add(crearLabelEtiqueta("Producción / Consumo:"));
        panelGrid.add(crearLabelValor(producidos + " / " + consumidos, COLOR_SECONDARY));

        // Panel central con la Gráfica de CPU vs Tiempo
        PanelGraficaCPU panelGrafica = new PanelGraficaCPU(clusterManager, metricasService);
        panelGrafica.setPreferredSize(new Dimension(580, 220));

        JPanel panelCentro = new JPanel(new BorderLayout(5, 5));
        panelCentro.setOpaque(false);
        panelCentro.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        panelCentro.add(panelGrid, BorderLayout.NORTH);
        panelCentro.add(panelGrafica, BorderLayout.CENTER);

        JButton btnCerrar = crearBotonFlat("Cerrar", COLOR_PRIMARY, Color.WHITE);
        btnCerrar.addActionListener(e -> dialog.dispose());

        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelBoton.setOpaque(false);
        panelBoton.add(btnCerrar);

        dialog.add(panelTitulo, BorderLayout.NORTH);
        dialog.add(panelCentro, BorderLayout.CENTER);
        dialog.add(panelBoton, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    /**
     * Componente Swing nativo para renderizar la Gráfica de Utilización de CPU vs Tiempo (Requerimiento 8).
     */
    private static class PanelGraficaCPU extends JPanel {
        private final ClusterManager clusterManager;
        private final MetricasService metricasService;
        private final Color[] coloresNodos = {COLOR_PRIMARY, COLOR_SUCCESS, COLOR_DANGER, COLOR_WARNING};

        public PanelGraficaCPU(ClusterManager cm, MetricasService ms) {
            this.clusterManager = cm;
            this.metricasService = ms;
            this.setBackground(new Color(15, 23, 42)); // Fondo oscuro estilo Dashboard
            this.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85)), "Utilización del Procesador vs. Tiempo (Ciclos)",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 11), Color.WHITE));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int paddingLeft = 45;
            int paddingRight = 20;
            int paddingTop = 30;
            int paddingBottom = 30;

            int graphW = width - paddingLeft - paddingRight;
            int graphH = height - paddingTop - paddingBottom;

            // Dibujar Ejes y Líneas de Cuadrícula (0%, 25%, 50%, 75%, 100%)
            g2.setColor(new Color(51, 65, 85));
            for (int i = 0; i <= 4; i++) {
                int y = paddingTop + (graphH * i / 4);
                g2.drawLine(paddingLeft, y, width - paddingRight, y);

                g2.setColor(new Color(148, 163, 184));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                int pct = 100 - (i * 25);
                g2.drawString(pct + "%", 12, y + 4);
                g2.setColor(new Color(51, 65, 85));
            }

            int maxCiclo = metricasService.getMaxCicloRegistrado();
            if (maxCiclo < 1) maxCiclo = 1;

            // Dibujar Línea de Tiempo de cada Computador
            Computador[] nodos = clusterManager.getComputadores();
            int cantNodos = clusterManager.getCantidadActual();

            for (int i = 0; i < cantNodos; i++) {
                Computador c = nodos[i];
                if (c == null) continue;

                double[] historial = metricasService.getHistorialCpuNodo(c.getIdComputador());
                if (historial.length < 2) continue;

                Color colorNodo = coloresNodos[i % coloresNodos.length];
                g2.setColor(colorNodo);
                g2.setStroke(new BasicStroke(2.0f));

                for (int ciclo = 0; ciclo < historial.length - 1; ciclo++) {
                    int x1 = paddingLeft + (ciclo * graphW / maxCiclo);
                    int y1 = paddingTop + graphH - (int) (historial[ciclo] * graphH / 100.0);

                    int x2 = paddingLeft + ((ciclo + 1) * graphW / maxCiclo);
                    int y2 = paddingTop + graphH - (int) (historial[ciclo + 1] * graphH / 100.0);

                    g2.drawLine(x1, y1, x2, y2);
                }

                // Leyenda del nodo en la parte superior derecha
                g2.fillRect(width - paddingRight - (100 * (cantNodos - i)), 12, 10, 10);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.drawString("Nodo " + c.getIdComputador(), width - paddingRight - (100 * (cantNodos - i)) + 14, 21);
            }

            // Etiqueta del eje X (Ciclos)
            g2.setColor(new Color(148, 163, 184));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.drawString("0 c.", paddingLeft - 5, height - 10);
            g2.drawString(maxCiclo + " ciclos", width - paddingRight - 30, height - 10);

            g2.dispose();
        }
    }

    private Component crearPanelNodosCluster() {
        int cantNodos = clusterManager.getCantidadActual();
        Computador[] nodos = clusterManager.getComputadores();

        JPanel panelGrid = new JPanel(new GridLayout(1, Math.max(cantNodos, 1), 15, 0));
        panelGrid.setBackground(COLOR_BG);
        panelGrid.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));

        for (int i = 0; i < cantNodos; i++) {
            Computador nodo = nodos[i];
            if (nodo != null) {
                JPanel tarjeta = crearTarjetaComputador(nodo, i);
                tarjeta.setPreferredSize(new Dimension(380, 450));
                panelGrid.add(tarjeta);
            }
        }

        JScrollPane scroll = new JScrollPane(panelGrid);
        scroll.setBorder(null);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        return scroll;
    }

    private JPanel crearTarjetaComputador(Computador nodo, int indiceNodo) {
        JPanel tarjeta = new JPanel(new BorderLayout(0, 10));
        tarjeta.setBackground(COLOR_CARD_BG);
        Border bordeLinea = BorderFactory.createLineBorder(COLOR_BORDER, 1, true);
        Border bordeInterno = BorderFactory.createEmptyBorder(12, 12, 12, 12);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(bordeLinea, bordeInterno));

        JPanel headerNodo = new JPanel(new BorderLayout());
        headerNodo.setOpaque(false);

        JLabel lblNombreNodo = new JLabel("🖥️ " + nodo.getNombre() + " (ID: " + nodo.getIdComputador() + ")");
        lblNombreNodo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblNombreNodo.setForeground(COLOR_TEXT_HEADER);

        JComboBox<String> comboPolitica = new JComboBox<>(new String[]{"FCFS", "Round Robin", "EDF", "Prioridades"});
        comboPolitica.setSelectedItem(nodo.getNombrePoliticaActual());
        comboPolitica.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        comboPolitica.setBackground(Color.WHITE);
        comboPolitica.addActionListener(e -> {
            String seleccion = (String) comboPolitica.getSelectedItem();
            nodo.cambiarPolitica(seleccion);
            eventLog.registrar(cicloRelojGlobal, nodo.getNombre() + ": Política de planificación cambiada activamente a " + seleccion);
            actualizarVisorLogs();
        });

        headerNodo.add(lblNombreNodo, BorderLayout.WEST);
        headerNodo.add(comboPolitica, BorderLayout.EAST);
        tarjeta.add(headerNodo, BorderLayout.NORTH);

        JPanel centroNodo = new JPanel();
        centroNodo.setLayout(new BoxLayout(centroNodo, BoxLayout.Y_AXIS));
        centroNodo.setOpaque(false);

        centroNodo.add(crearBloqueCPU(nodo));
        centroNodo.add(Box.createVerticalStrut(10));

        centroNodo.add(crearBloqueRAM(nodo));
        centroNodo.add(Box.createVerticalStrut(10));

        centroNodo.add(crearBloqueColas(nodo, indiceNodo));

        tarjeta.add(centroNodo, BorderLayout.CENTER);

        return tarjeta;
    }

    private JPanel crearBloqueCPU(Computador nodo) {
        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 4));
        panel.setBackground(new Color(241, 245, 249));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_BORDER), "Estado CPU", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 11), COLOR_TEXT_HEADER));

        Proceso activo = nodo.getCpu().getProcesoActual();
        String nombreProceso = (activo != null) ? activo.getNombre() + " (Ejecutando)" : "IDLE (Inactivo)";
        String pcValue = (activo != null) ? "PC: " + nodo.getCpu().getProgramCounter() + " / " + activo.getTiempoEjecucion() : "PC: 0";
        String modo = (nodo.getCpu().getModoActual() != null) ? nodo.getCpu().getModoActual().toString() : "USUARIO";
        String deadline = (activo != null) ? "DL: " + activo.getDeadline() + " c." : "N/A";

        panel.add(crearLabelEtiqueta("Proceso en CPU:"));
        panel.add(crearLabelValor(nombreProceso, (activo != null) ? COLOR_PRIMARY : COLOR_TEXT_HEADER));

        panel.add(crearLabelEtiqueta("Program Counter:"));
        panel.add(crearLabelValor(pcValue, COLOR_TEXT_HEADER));

        panel.add(crearLabelEtiqueta("Modo Ejecución:"));
        panel.add(crearLabelValor(modo, modo.contains("KERNEL") ? COLOR_DANGER : COLOR_SUCCESS));

        panel.add(crearLabelEtiqueta("Deadline Restante:"));
        panel.add(crearLabelValor(deadline, COLOR_WARNING));

        return panel;
    }

    private JPanel crearBloqueRAM(Computador nodo) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setOpaque(false);

        MemoriaRAM ram = nodo.getRam();
        int ramTotal = (ram != null) ? ram.getTamanioTotalKB() : 1024;
        int marcosOcupados = (ram != null) ? (ram.getTotalMarcos() - ram.getMarcosLibres()) : 0;
        int ramUsada = (ram != null) ? (marcosOcupados * ram.getTamanioMarcoKB()) : 0;
        int porcentaje = (ram != null) ? (int) ram.getPorcentajeUso() : 0;

        JLabel lblRam = new JLabel("Memoria RAM: " + ramUsada + " KB / " + ramTotal + " KB (" + porcentaje + "%)");
        lblRam.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblRam.setForeground(COLOR_TEXT_HEADER);

        JProgressBar barRam = new JProgressBar(0, 100);
        barRam.setValue(porcentaje);
        barRam.setStringPainted(true);
        barRam.setForeground(porcentaje > 80 ? COLOR_DANGER : COLOR_PRIMARY);

        panel.add(lblRam, BorderLayout.NORTH);
        panel.add(barRam, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel crearBloqueColas(Computador nodo, int indiceNodo) {
        JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        panel.setOpaque(false);

        // 1. Cola Nuevos
        ColaProcesos colaNuevos = nodo.getColaNuevos();
        int numNuevos = (colaNuevos != null) ? colaNuevos.getTamaño() : 0;
        StringBuilder sbNuevos = new StringBuilder();
        if (numNuevos > 0) {
            for (int k = 0; k < numNuevos; k++) {
                Proceso p = (Proceso) colaNuevos.obtener(k);
                if (p != null) {
                    if (k > 0) sbNuevos.append("\n");
                    sbNuevos.append("• ").append(p.getNombre());
                }
            }
        } else {
            sbNuevos.append("Sin procesos");
        }

        // 2. Cola Listos
        ColaProcesos colaListos = nodo.getColaListosEfectiva();
        int numListos = (colaListos != null) ? colaListos.getTamaño() : 0;
        StringBuilder sbListos = new StringBuilder();
        if (numListos > 0) {
            for (int k = 0; k < numListos; k++) {
                Proceso p = (Proceso) colaListos.obtener(k);
                if (p != null) {
                    if (k > 0) sbListos.append("\n");
                    sbListos.append("• ").append(p.getNombre());
                }
            }
        } else {
            sbListos.append("Sin procesos");
        }

        // 3. Cola Bloqueados
        ColaProcesos colaBloq = nodo.getColaBloqueados();
        int numBloqueados = (colaBloq != null) ? colaBloq.getTamaño() : 0;
        StringBuilder sbBloqueados = new StringBuilder();
        if (numBloqueados > 0) {
            for (int k = 0; k < numBloqueados; k++) {
                Proceso p = (Proceso) colaBloq.obtener(k);
                if (p != null) {
                    if (k > 0) sbBloqueados.append("\n");
                    sbBloqueados.append("• ").append(p.getNombre()).append(" [").append(p.getTipo()).append("]");
                }
            }
        } else {
            sbBloqueados.append("Sin bloqueos");
        }

        // 4. Cola Terminados
        ColaProcesos colaTerm = nodo.getColaTerminados();
        int numTerminados = (colaTerm != null) ? colaTerm.getTamaño() : 0;
        StringBuilder sbTerminados = new StringBuilder();
        if (numTerminados > 0) {
            for (int k = 0; k < numTerminados; k++) {
                Proceso p = (Proceso) colaTerm.obtener(k);
                if (p != null) {
                    if (k > 0) sbTerminados.append("\n");
                    sbTerminados.append("• ").append(p.getNombre());
                }
            }
        } else {
            sbTerminados.append("Ninguno");
        }

        panel.add(crearCajaCola("Cola Nuevos (" + numNuevos + ")", sbNuevos.toString(), COLOR_PRIMARY));
        panel.add(crearCajaCola("Cola Listos (" + numListos + ")", sbListos.toString(), COLOR_SUCCESS));
        panel.add(crearCajaCola("Cola Bloqueados (" + numBloqueados + ")", sbBloqueados.toString(), COLOR_DANGER));
        panel.add(crearCajaCola("Cola Terminados (" + numTerminados + ")", sbTerminados.toString(), COLOR_SECONDARY));

        return panel;
    }

    private JPanel crearCajaCola(String titulo, String contenidoTextual, Color colorBorde) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(colorBorde, 1), titulo, TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 10), colorBorde));

        JTextArea txt = new JTextArea((contenidoTextual == null || contenidoTextual.isEmpty()) ? "Sin procesos" : contenidoTextual);
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        txt.setEditable(false);
        txt.setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(txt);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(120, 65));

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelInferiorLogs() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(COLOR_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_BORDER),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        JLabel lblTituloLogs = new JLabel("📜 Registro de Auditoría de Eventos (EventLog)");
        lblTituloLogs.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTituloLogs.setForeground(COLOR_TEXT_HEADER);

        txtLogEventos = new JTextArea(5, 80);
        txtLogEventos.setFont(new Font("Consolas", Font.PLAIN, 11));
        txtLogEventos.setEditable(false);
        txtLogEventos.setBackground(COLOR_CARD_BG);

        JScrollPane scrollLog = new JScrollPane(txtLogEventos);
        scrollLog.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));

        panel.add(lblTituloLogs, BorderLayout.NORTH);
        panel.add(scrollLog, BorderLayout.CENTER);

        actualizarVisorLogs();

        return panel;
    }

    private JLabel crearLabelEtiqueta(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(new Color(100, 116, 139));
        return lbl;
    }

    private JLabel crearLabelValor(String texto, Color color) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(color);
        return lbl;
    }

    private JButton crearBotonFlat(String texto, Color bg, Color fg) {
        JButton btn = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(bg.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(bg.brighter());
                } else {
                    g2.setColor(bg);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(fg);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, 34));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));

        return btn;
    }

    private void accionAvanzarCiclo(ActionEvent e) {
        if (e == null) return;
        ejecutarCicloSimulacion();
    }

    private void accionPausarReanudar(ActionEvent e) {
        if (e == null) return;
        simulacionEnEjecucion = !simulacionEnEjecucion;
        if (simulacionEnEjecucion) {
            btnPausarReanudar.setText("Pausar Simulación");
            btnPausarReanudar.setBackground(COLOR_DANGER);
            timerSimulacion.start();
        } else {
            btnPausarReanudar.setText("Iniciar Simulación");
            btnPausarReanudar.setBackground(COLOR_SUCCESS);
            timerSimulacion.stop();
        }
    }

    private void actualizarVisorLogs() {
        if (eventLog != null && txtLogEventos != null) {
            StringBuilder sb = new StringBuilder();
            String[] eventos = eventLog.obtenerEventos();
            for (int i = 0; i < eventos.length; i++) {
                if (eventos[i] != null) {
                    sb.append(eventos[i]).append("\n");
                }
            }
            txtLogEventos.setText(sb.toString());
            txtLogEventos.setCaretPosition(txtLogEventos.getDocument().getLength());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Vista2Monitoreo vista = new Vista2Monitoreo();
            vista.setVisible(true);
        });
    }
}