/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.gui;

import avilaos.datastructures.ColaProcesos;
import avilaos.datastructures.ListaEnlazada;
import avilaos.enums.ModoUsuario;
import avilaos.enums.PermisoArchivo;
import avilaos.enums.TipoOperacionES;
import avilaos.enums.TipoProceso;
import avilaos.model.Archivo;
import avilaos.model.BloqueDisco;
import avilaos.model.Computador;
import avilaos.model.Directorio;
import avilaos.model.DiscoSimulado;
import avilaos.model.EntradaSistemaArchivos;
import avilaos.model.MemoriaRAM;
import avilaos.model.Proceso;
import avilaos.model.SistemaArchivos;
import avilaos.model.SolicitudDisco;
import avilaos.persistence.GestorJSON;
import avilaos.scheduling.PlanificadorDiscoCSCAN;
import avilaos.scheduling.PlanificadorDiscoFIFO;
import avilaos.scheduling.PlanificadorDiscoSCAN;
import avilaos.scheduling.PlanificadorDiscoSSTF;
import avilaos.services.EventLog;
import avilaos.services.MetricasService;
import avilaos.synchronization.RelojGlobal;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Vista 2: Panel principal de monitoreo de AvilaOS (Proyecto 2 - Issue #8).
 * Integra CPU, RAM, colas de 7 estados (incluyendo suspendidos), Disco Simulado
 * (mapa de bloques coloreados por archivo, cabezal, cola E/S y politicas FIFO/SSTF/SCAN/C-SCAN),
 * JTree de archivos, JTable de asignacion encadenada con proceso creador y color,
 * y selector Modo Administrador / Modo Usuario.
 * 
 * @author Daniel Alonso
 * @version 3.2
 */
public class Vista2Monitoreo extends JFrame {

    private final Computador computador;
    private final DiscoSimulado disco;
    private final SistemaArchivos sistemaArchivos;
    private final EventLog eventLog;
    private final MetricasService metricasService;
    private final GestorJSON gestorJSON;

    private int cicloRelojGlobal;
    private int contadorProcesosGUI = 100;
    private boolean simulacionEnEjecucion;
    private Timer timerSimulacion;
    private final int velocidadCicloMs;

    private JLabel lblCicloReloj;
    private JButton btnPausarReanudar;
    private JButton btnAvanzarCiclo;
    private JComboBox<String> comboModoSesion;
    private JTextField txtUsuarioSesion;

    private JPanel panelComputadorContainer;

    private JTree treeSistemaArchivos;
    private DefaultTreeModel modeloArbolFS;
    private JTable tablaAsignacion;
    private DefaultTableModel modeloTablaAsignacion;
    private JTextArea txtContenidoArchivo;
    private JLabel lblRutaSeleccionada;
    private String rutaDirectorioActual = "/";

    private JComboBox<String> comboPoliticaDisco;
    private JLabel lblInfoCabezal;
    private JLabel lblColaESDisco;
    private PanelMapaBloques panelMapaBloques;

    private JTextArea txtLogEventos;

    private static final Color COLOR_HEADER = new Color(15, 23, 42);
    private static final Color COLOR_BG = new Color(248, 250, 252);
    private static final Color COLOR_CARD_BG = Color.WHITE;
    private static final Color COLOR_TEXT_HEADER = new Color(30, 41, 59);
    private static final Color COLOR_BORDER = new Color(226, 232, 240);
    private static final Color COLOR_PRIMARY = new Color(37, 99, 235);
    private static final Color COLOR_SUCCESS = new Color(16, 185, 129);
    private static final Color COLOR_DANGER = new Color(239, 68, 68);
    private static final Color COLOR_WARNING = new Color(245, 158, 11);
    private static final Color COLOR_PURPLE = new Color(124, 58, 237);
    private static final Color COLOR_SECONDARY = new Color(71, 85, 105);

    public Vista2Monitoreo(Computador computador, DiscoSimulado disco, SistemaArchivos sistemaArchivos,
                           EventLog eventLog, MetricasService metricasService, int velocidadCicloMs) {
        super("ÁvilaOS - Monitor de Procesos, Memoria, Disco y Sistema de Archivos");
        this.disco = (disco != null) ? disco : DiscoSimulado.getInstancia();
        this.sistemaArchivos = (sistemaArchivos != null) ? sistemaArchivos : new SistemaArchivos(this.disco);
        this.computador = (computador != null) ? computador : Computador.getInstancia();
        this.eventLog = (eventLog != null) ? eventLog : EventLog.getInstancia();
        this.metricasService = (metricasService != null) ? metricasService : new MetricasService();
        this.disco.setMetricasService(this.metricasService);
        this.gestorJSON = new GestorJSON();

        this.cicloRelojGlobal = 0;
        this.simulacionEnEjecucion = false;
        this.velocidadCicloMs = Math.max(100, velocidadCicloMs);

        aplicarLookAndFeel();
        configurarVentana();
        construirInterfaz();
        inicializarTimerSimulacion();
        refrescarVistaCompleta();
    }

    public Vista2Monitoreo() {
        this(null, null, null, null, null, 800);
    }

    private void aplicarLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    private void configurarVentana() {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(1380, 860);
        this.setLocationRelativeTo(null);
        this.getContentPane().setBackground(COLOR_BG);
        this.setLayout(new BorderLayout(0, 0));
    }

    private void construirInterfaz() {
        this.add(crearPanelEncabezado(), BorderLayout.NORTH);

        JSplitPane splitPrincipal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPrincipal.setResizeWeight(0.40);
        splitPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
        splitPrincipal.setBackground(COLOR_BG);

        panelComputadorContainer = new JPanel(new BorderLayout());
        panelComputadorContainer.setOpaque(false);
        panelComputadorContainer.add(crearPanelComputador(), BorderLayout.CENTER);

        splitPrincipal.setLeftComponent(panelComputadorContainer);
        splitPrincipal.setRightComponent(crearPanelDerechoAlmacenamiento());

        this.add(splitPrincipal, BorderLayout.CENTER);
        this.add(crearPanelInferiorLogs(), BorderLayout.SOUTH);
    }

    private void inicializarTimerSimulacion() {
        this.timerSimulacion = new Timer(velocidadCicloMs, (ActionEvent e) -> ejecutarCicloSimulacion());
    }

    private void ejecutarCicloSimulacion() {
        cicloRelojGlobal++;
        RelojGlobal.getInstancia().setCicloActual(cicloRelojGlobal);
        lblCicloReloj.setText("Reloj Global: Ciclo " + cicloRelojGlobal);

        Proceso activoPrevio = computador.getCpu().getProcesoActual();
        int terminadosPrevio = computador.getColaTerminados().getTamaño();

        computador.tick(cicloRelojGlobal);
        disco.tick(cicloRelojGlobal);

        double usoCpu = computador.getCpu().isOcupado() ? 100.0 : 0.0;
        metricasService.registrarEstadoCiclo(
                cicloRelojGlobal,
                usoCpu,
                disco.getPosicionCabezal(),
                disco.getDesplazamientoTotal()
        );
        metricasService.sincronizarMetricasSwap(
                computador.getPlanificadorMedianoPlazo().getTotalSuspensiones(),
                computador.getPlanificadorMedianoPlazo().getTotalReactivaciones()
        );

        Proceso activoActual = computador.getCpu().getProcesoActual();
        int terminadosActual = computador.getColaTerminados().getTamaño();

        if (activoPrevio != activoActual && activoActual != null) {
            eventLog.registrar(cicloRelojGlobal, "CPU despacha a " + activoActual.getNombre()
                    + " (" + computador.getNombrePoliticaActual() + ")");
        }

        if (terminadosActual > terminadosPrevio) {
            Proceso ultimoTerminado = (Proceso) computador.getColaTerminados().obtener(terminadosActual - 1);
            if (ultimoTerminado != null) {
                int tiempoRespuesta = cicloRelojGlobal - ultimoTerminado.getTiempoLlegada();
                boolean cumplioDL = (ultimoTerminado.getDeadline() <= 0) || (cicloRelojGlobal <= ultimoTerminado.getDeadline());
                metricasService.registrarProcesoTerminado(tiempoRespuesta, cumplioDL);
                eventLog.registrar(cicloRelojGlobal, "Proceso " + ultimoTerminado.getNombre() + " FINALIZADO.");
            }
        }

        refrescarVistaCompleta();
    }

    private void refrescarVistaCompleta() {
        panelComputadorContainer.removeAll();
        panelComputadorContainer.add(crearPanelComputador(), BorderLayout.CENTER);
        panelComputadorContainer.revalidate();
        panelComputadorContainer.repaint();

        actualizarInfoDisco();
        if (panelMapaBloques != null) {
            panelMapaBloques.repaint();
        }

        recargarArbolJTree();
        actualizarTablaAsignacion();
        actualizarVisorLogs();
    }

    // ==================== ENCABEZADO Y MODO ADMINISTRADOR / USUARIO ====================

 private JPanel crearPanelEncabezado() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(COLOR_HEADER);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JPanel panelIzq = new JPanel(new GridLayout(2, 1, 0, 2));
        panelIzq.setOpaque(false);

        JLabel lblTitulo = new JLabel("ÁVILAOS: MONITOR INTEGRAL DE PROCESOS, DISCO Y ARCHIVOS");
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 15));

        lblCicloReloj = new JLabel("Reloj Global: Ciclo 0");
        lblCicloReloj.setForeground(COLOR_SUCCESS);
        lblCicloReloj.setFont(new Font("Segoe UI", Font.BOLD, 13));

        panelIzq.add(lblTitulo);
        panelIzq.add(lblCicloReloj);

        JPanel panelSesion = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 2));
        panelSesion.setOpaque(false);

        JLabel lblSesion = new JLabel("Sesión Activa:");
        lblSesion.setForeground(new Color(203, 213, 225));
        lblSesion.setFont(new Font("Segoe UI", Font.BOLD, 11));

        txtUsuarioSesion = new JTextField("admin", 8);
        txtUsuarioSesion.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        comboModoSesion = new JComboBox<>(new String[]{"Modo Administrador", "Modo Usuario"});
        comboModoSesion.setFont(new Font("Segoe UI", Font.BOLD, 11));
        comboModoSesion.setBackground(Color.WHITE);

        JButton btnAplicarSesion = crearBotonFlat("Cambiar Modo", COLOR_PURPLE, Color.WHITE);
        btnAplicarSesion.addActionListener(e -> aplicarCambioModoSesion());

        panelSesion.add(lblSesion);
        panelSesion.add(txtUsuarioSesion);
        panelSesion.add(comboModoSesion);
        panelSesion.add(btnAplicarSesion);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panelBotones.setOpaque(false);

        JButton btnCargarJSON = crearBotonFlat("📂 Cargar JSON", COLOR_SECONDARY, Color.WHITE);
        JButton btnVerMetricas = crearBotonFlat("📊 Métricas", COLOR_SECONDARY, Color.WHITE);
        btnAvanzarCiclo = crearBotonFlat("+1 Ciclo →", COLOR_PRIMARY, Color.WHITE);
        btnPausarReanudar = crearBotonFlat("Iniciar", COLOR_SUCCESS, Color.WHITE);
        JButton btnReiniciar = crearBotonFlat("↺ Reiniciar", COLOR_DANGER, Color.WHITE);

        btnCargarJSON.addActionListener(e -> cargarJSONEnVivo());
        btnVerMetricas.addActionListener(e -> mostrarVentanaMetricas());
        btnAvanzarCiclo.addActionListener(e -> ejecutarCicloSimulacion());
        btnPausarReanudar.addActionListener(this::accionPausarReanudar);
        btnReiniciar.addActionListener(e -> accionReiniciarSimulador());

        panelBotones.add(btnCargarJSON);
        panelBotones.add(btnVerMetricas);
        panelBotones.add(btnAvanzarCiclo);
        panelBotones.add(btnPausarReanudar);
        panelBotones.add(btnReiniciar);

        panel.add(panelIzq, BorderLayout.WEST);
        panel.add(panelSesion, BorderLayout.CENTER);
        panel.add(panelBotones, BorderLayout.EAST);

        return panel;
    }
 
    private void accionReiniciarSimulador() {
        int conf = JOptionPane.showConfirmDialog(
                this,
                "¿Detener la simulación actual y volver a la pantalla de configuración?",
                "Reiniciar ÁvilaOS",
                JOptionPane.YES_NO_OPTION
        );
        if (conf != JOptionPane.YES_OPTION) {
            return;
        }

        if (timerSimulacion != null && timerSimulacion.isRunning()) {
            timerSimulacion.stop();
        }
        simulacionEnEjecucion = false;
        RelojGlobal.getInstancia().setCicloActual(0);

        this.dispose();
        SwingUtilities.invokeLater(() -> new Vista1Configuracion().setVisible(true));
    }

    private void aplicarCambioModoSesion() {
        String usuario = txtUsuarioSesion.getText().trim();
        if (usuario.isEmpty()) {
            usuario = "invitado";
            txtUsuarioSesion.setText(usuario);
        }
        boolean esAdmin = comboModoSesion.getSelectedIndex() == 0;
        ModoUsuario modo = esAdmin ? ModoUsuario.ADMINISTRADOR : ModoUsuario.USUARIO;
        sistemaArchivos.iniciarSesion(usuario, modo);

        eventLog.registrar(cicloRelojGlobal, "Sesión cambiada a usuario '" + usuario + "' [" + modo + "].");
        actualizarTablaAsignacion();
        actualizarVisorLogs();
    }

    private void cargarJSONEnVivo() {
        JFileChooser chooser = new JFileChooser(new File("."));
        chooser.setDialogTitle("Importar archivo JSON al simulador");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            ColaProcesos cargados = gestorJSON.cargarDesdeJSON(
                    chooser.getSelectedFile().getAbsolutePath(),
                    disco,
                    sistemaArchivos,
                    computador
            );
            if (cargados != null) {
                refrescarVistaCompleta();
            } else {
                JOptionPane.showMessageDialog(this, "Error al cargar el JSON seleccionado.", "Error JSON", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ==================== PANEL IZQUIERDO: CPU, RAM Y COLAS DE 7 ESTADOS ====================

    private JPanel crearPanelComputador() {
        JPanel tarjeta = new JPanel(new BorderLayout(0, 8));
        tarjeta.setBackground(COLOR_CARD_BG);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel lblNombre = new JLabel("🖥️ " + computador.getNombre() + " (CPU & Gestión de Procesos)");
        lblNombre.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblNombre.setForeground(COLOR_TEXT_HEADER);

        JComboBox<String> comboPoliticaCPU = new JComboBox<>(new String[]{"FCFS", "Round Robin", "EDF", "Prioridades"});
        comboPoliticaCPU.setSelectedItem(computador.getNombrePoliticaActual());
        comboPoliticaCPU.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        comboPoliticaCPU.addActionListener(e -> {
            String sel = (String) comboPoliticaCPU.getSelectedItem();
            computador.cambiarPolitica(sel);
            eventLog.registrar(cicloRelojGlobal, "Planificador de CPU cambiado a: " + sel);
            actualizarVisorLogs();
        });

        header.add(lblNombre, BorderLayout.WEST);
        header.add(comboPoliticaCPU, BorderLayout.EAST);
        tarjeta.add(header, BorderLayout.NORTH);

        JPanel centro = new JPanel();
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        centro.setOpaque(false);

        centro.add(crearBloqueCPU());
        centro.add(Box.createVerticalStrut(8));
        centro.add(crearBloqueRAM());
        centro.add(Box.createVerticalStrut(8));
        centro.add(crearBloqueColas7Estados());

        tarjeta.add(centro, BorderLayout.CENTER);
        return tarjeta;
    }

    private JPanel crearBloqueCPU() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 3));
        panel.setBackground(new Color(241, 245, 249));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_BORDER), "Estado del Procesador (CPU)",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 11), COLOR_TEXT_HEADER));

        Proceso activo = computador.getCpu().getProcesoActual();
        String nombreProceso = (activo != null) ? activo.getNombre() + " (Ejecutando)" : "IDLE (Inactivo)";
        String pcValue = (activo != null) ? "PC: " + computador.getCpu().getProgramCounter() + " / " + activo.getTiempoEjecucion() : "PC: 0";
        String modoCPU = (computador.getCpu().getModoActual() != null) ? computador.getCpu().getModoActual().toString() : "USUARIO";
        String deadline = (activo != null && activo.getDeadline() > 0) ? "DL: Ciclo " + activo.getDeadline() : "N/A";

        panel.add(crearLabelEtiqueta("Proceso en CPU:"));
        panel.add(crearLabelValor(nombreProceso, (activo != null) ? COLOR_PRIMARY : COLOR_TEXT_HEADER));

        panel.add(crearLabelEtiqueta("Program Counter:"));
        panel.add(crearLabelValor(pcValue, COLOR_TEXT_HEADER));

        panel.add(crearLabelEtiqueta("Modo Hardware CPU:"));
        panel.add(crearLabelValor(modoCPU, modoCPU.contains("SISTEMA") || modoCPU.contains("KERNEL") ? COLOR_DANGER : COLOR_SUCCESS));

        panel.add(crearLabelEtiqueta("Deadline Proceso:"));
        panel.add(crearLabelValor(deadline, COLOR_WARNING));

        return panel;
    }

    private JPanel crearBloqueRAM() {
        JPanel panel = new JPanel(new BorderLayout(5, 4));
        panel.setOpaque(false);

        MemoriaRAM ram = computador.getRam();
        int ramTotal = (ram != null) ? ram.getTamanioTotalKB() : 256;
        int marcosOcupados = (ram != null) ? (ram.getTotalMarcos() - ram.getMarcosLibres()) : 0;
        int ramUsada = (ram != null) ? (marcosOcupados * ram.getTamanioMarcoKB()) : 0;
        int porcentaje = (ram != null) ? (int) ram.getPorcentajeUso() : 0;

        JLabel lblRam = new JLabel("Memoria RAM: " + ramUsada + " KB / " + ramTotal + " KB (" + porcentaje + "%)"
                + "   |   Swap Disco libre: " + disco.getBloquesLibresSwap() + "/" + disco.getBloquesSwap() + " bloq.");
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

    private JPanel crearBloqueColas7Estados() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 6, 6));
        panel.setOpaque(false);

        panel.add(crearCajaCola("1. Nuevos", computador.getColaNuevos(), COLOR_PRIMARY));
        panel.add(crearCajaCola("2. Listos (RAM)", computador.getColaListosEfectiva(), COLOR_SUCCESS));
        panel.add(crearCajaCola("3. Bloqueados (E/S)", computador.getColaBloqueados(), COLOR_DANGER));
        panel.add(crearCajaCola("4. Listo Suspendido (Swap)", computador.getColaSuspendidosListos(), COLOR_WARNING));
        panel.add(crearCajaCola("5. Bloqueado Suspendido", computador.getColaSuspendidosBloqueados(), COLOR_PURPLE));
        panel.add(crearCajaCola("6. Terminados", computador.getColaTerminados(), COLOR_SECONDARY));

        return panel;
    }

    private JPanel crearCajaCola(String tituloBase, ColaProcesos cola, Color colorBorde) {
        int cantidad = (cola != null) ? cola.getTamaño() : 0;
        StringBuilder sb = new StringBuilder();
        if (cantidad > 0) {
            for (int i = 0; i < cantidad; i++) {
                Proceso p = (Proceso) cola.obtener(i);
                if (p != null) {
                    if (i > 0) sb.append("\n");
                    sb.append("• ").append(p.getNombre()).append(" (Prio:").append(p.getPrioridad()).append(")");
                }
            }
        } else {
            sb.append("Vacía");
        }

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(colorBorde, 1),
                tituloBase + " (" + cantidad + ")",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 10), colorBorde));

        JTextArea txt = new JTextArea(sb.toString());
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        txt.setEditable(false);
        txt.setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(txt);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(130, 58));

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ==================== PANEL DERECHO: SISTEMA DE ARCHIVOS (JTREE + JTABLE) Y DISCO ====================

    private JPanel crearPanelDerechoAlmacenamiento() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 10));
        panel.setOpaque(false);

        panel.add(crearPanelSistemaArchivos());
        panel.add(crearPanelDiscoSimulado());
        return panel;
    }

    private JPanel crearPanelSistemaArchivos() {
        JPanel tarjeta = new JPanel(new BorderLayout(8, 6));
        tarjeta.setBackground(COLOR_CARD_BG);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JPanel barraFS = new JPanel(new BorderLayout(6, 0));
        barraFS.setOpaque(false);

        lblRutaSeleccionada = new JLabel("📁 Sistema de Archivos Jerárquico  |  Ruta activa: /");
        lblRutaSeleccionada.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblRutaSeleccionada.setForeground(COLOR_TEXT_HEADER);

        JPanel botonesFS = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        botonesFS.setOpaque(false);

        JButton btnNuevoDir = crearBotonMini("+ Carpeta", COLOR_PRIMARY);
        JButton btnNuevoArch = crearBotonMini("+ Archivo", COLOR_SUCCESS);
        JButton btnRenombrar = crearBotonMini("Renombrar", COLOR_WARNING);
        JButton btnEliminar = crearBotonMini("Eliminar", COLOR_DANGER);

        btnNuevoDir.addActionListener(e -> accionCrearDirectorioGUI());
        btnNuevoArch.addActionListener(e -> accionCrearArchivoGUI());
        btnRenombrar.addActionListener(e -> accionRenombrarEntradaGUI());
        btnEliminar.addActionListener(e -> accionEliminarEntradaGUI());

        botonesFS.add(btnNuevoDir);
        botonesFS.add(btnNuevoArch);
        botonesFS.add(btnRenombrar);
        botonesFS.add(btnEliminar);

        barraFS.add(lblRutaSeleccionada, BorderLayout.WEST);
        barraFS.add(botonesFS, BorderLayout.EAST);
        tarjeta.add(barraFS, BorderLayout.NORTH);

        JSplitPane splitFS = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitFS.setResizeWeight(0.30);

        DefaultMutableTreeNode nodoRaiz = construirNodoJTree(sistemaArchivos.getRaiz());
        modeloArbolFS = new DefaultTreeModel(nodoRaiz);
        treeSistemaArchivos = new JTree(modeloArbolFS);
        treeSistemaArchivos.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        treeSistemaArchivos.addTreeSelectionListener(e -> alSeleccionarNodoArbol());

        JScrollPane scrollTree = new JScrollPane(treeSistemaArchivos);
        scrollTree.setBorder(BorderFactory.createTitledBorder("Árbol de Directorios"));

        // Tabla de Asignación Encadenada con Proceso Creador y Color Distintivo
        String[] columnas = {"Archivo", "Creador", "Owner", "Permiso", "Bloques", "Inicio", "Color", "Cadena en Disco"};
        modeloTablaAsignacion = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaAsignacion = new JTable(modeloTablaAsignacion);
        tablaAsignacion.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        tablaAsignacion.setRowHeight(21);

        // Renderizador visual para la columna "Color" (índice 6)
        tablaAsignacion.getColumnModel().getColumn(6).setMaxWidth(50);
        tablaAsignacion.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = new JLabel();
                lbl.setOpaque(true);
                if (value instanceof Color) {
                    lbl.setBackground((Color) value);
                    lbl.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
                }
                return lbl;
            }
        });

        JScrollPane scrollTabla = new JScrollPane(tablaAsignacion);
        scrollTabla.setBorder(BorderFactory.createTitledBorder("Tabla de Asignación Encadenada"));

        splitFS.setLeftComponent(scrollTree);
        splitFS.setRightComponent(scrollTabla);
        tarjeta.add(splitFS, BorderLayout.CENTER);

        txtContenidoArchivo = new JTextArea(2, 40);
        txtContenidoArchivo.setFont(new Font("Consolas", Font.PLAIN, 11));
        txtContenidoArchivo.setEditable(false);
        txtContenidoArchivo.setBackground(new Color(241, 245, 249));
        txtContenidoArchivo.setText("Seleccione un archivo en el árbol para inspeccionar su contenido (sujeto a permisos).");
        JScrollPane scrollContenido = new JScrollPane(txtContenidoArchivo);
        scrollContenido.setBorder(BorderFactory.createTitledBorder("Contenido / Inspección"));
        tarjeta.add(scrollContenido, BorderLayout.SOUTH);

        actualizarTablaAsignacion();
        return tarjeta;
    }

    private DefaultMutableTreeNode construirNodoJTree(EntradaSistemaArchivos entrada) {
        if (entrada == null) {
            return new DefaultMutableTreeNode("/");
        }
        DefaultMutableTreeNode nodo = new DefaultMutableTreeNode(entrada);
        if (entrada.esDirectorio()) {
            Directorio dir = (Directorio) entrada;
            ListaEnlazada hijos = dir.getHijos();
            for (int i = 0; i < hijos.getTamaño(); i++) {
                EntradaSistemaArchivos hijo = (EntradaSistemaArchivos) hijos.obtener(i);
                if (hijo != null) {
                    nodo.add(construirNodoJTree(hijo));
                }
            }
        }
        return nodo;
    }

    private void recargarArbolJTree() {
        if (modeloArbolFS != null && treeSistemaArchivos != null) {
            modeloArbolFS.setRoot(construirNodoJTree(sistemaArchivos.getRaiz()));
            modeloArbolFS.reload();
            for (int i = 0; i < treeSistemaArchivos.getRowCount(); i++) {
                treeSistemaArchivos.expandRow(i);
            }
        }
    }

    private void alSeleccionarNodoArbol() {
        DefaultMutableTreeNode nodoSel = (DefaultMutableTreeNode) treeSistemaArchivos.getLastSelectedPathComponent();
        if (nodoSel == null) return;

        Object obj = nodoSel.getUserObject();
        if (obj instanceof EntradaSistemaArchivos) {
            EntradaSistemaArchivos entrada = (EntradaSistemaArchivos) obj;
            if (entrada.esDirectorio()) {
                rutaDirectorioActual = entrada.getRutaCompleta();
                lblRutaSeleccionada.setText("📁 Sistema de Archivos  |  Directorio activo: " + rutaDirectorioActual);
                txtContenidoArchivo.setText("Directorio: " + entrada.getRutaCompleta()
                        + " | Propietario: " + entrada.getPropietario()
                        + " | Permiso: " + entrada.getPermiso());
            } else {
                Directorio padre = entrada.getPadre();
                rutaDirectorioActual = (padre != null) ? padre.getRutaCompleta() : "/";
                lblRutaSeleccionada.setText("📄 Archivo: " + entrada.getRutaCompleta() + " (Carpeta: " + rutaDirectorioActual + ")");

                Archivo leido = sistemaArchivos.leerArchivo(entrada.getRutaCompleta());
                if (leido != null) {
                    txtContenidoArchivo.setText(leido.getContenido().isEmpty() ? "(Archivo vacío)" : leido.getContenido());
                } else {
                    txtContenidoArchivo.setText("⛔ ACCESO DENEGADO: El usuario '" + sistemaArchivos.getUsuarioActual()
                            + "' (" + sistemaArchivos.getModoUsuarioActual() + ") no tiene permiso para leer este archivo privado.");
                    actualizarVisorLogs();
                }
            }
        }
    }

    private void accionCrearDirectorioGUI() {
        String nombre = JOptionPane.showInputDialog(this, "Nombre de la nueva carpeta en '" + rutaDirectorioActual + "':", "Crear Directorio", JOptionPane.PLAIN_MESSAGE);
        if (nombre == null || nombre.trim().isEmpty()) return;

        int priv = JOptionPane.showConfirmDialog(this, "¿Hacer el directorio PRIVADO (solo dueño y Administrador)?", "Permisos", JOptionPane.YES_NO_OPTION);
        PermisoArchivo permiso = (priv == JOptionPane.YES_OPTION) ? PermisoArchivo.PRIVADO : PermisoArchivo.PUBLICO;

        String rutaCompleta = "/".equals(rutaDirectorioActual) ? "/" + nombre.trim() : rutaDirectorioActual + "/" + nombre.trim();
        int id = ++contadorProcesosGUI;
        Proceso p = new Proceso(id, 0, "IO_Mkdir_" + id, 1, 2, 2,
                TipoProceso.IO_BOUND, TipoOperacionES.CREAR, rutaCompleta);
        p.setEsDirectorioTarget(true);
        p.setPermisoES(permiso);

        computador.encolarProceso(p);
        eventLog.registrar(cicloRelojGlobal, "Proceso E/S " + p.getNombre() + " encolado para CREAR carpeta '" + rutaCompleta + "'.");
        refrescarVistaCompleta();
    }

    private void accionCrearArchivoGUI() {
        JTextField txtNom = new JTextField("archivo.txt");
        JTextField txtBloq = new JTextField("3");
        JTextField txtCont = new JTextField("Datos de prueba en ÁvilaOS");
        JComboBox<PermisoArchivo> cmbPerm = new JComboBox<>(PermisoArchivo.values());

        JPanel form = new JPanel(new GridLayout(4, 2, 6, 6));
        form.add(new JLabel("Nombre Archivo:"));
        form.add(txtNom);
        form.add(new JLabel("Tamaño (Bloques):"));
        form.add(txtBloq);
        form.add(new JLabel("Permiso:"));
        form.add(cmbPerm);
        form.add(new JLabel("Contenido:"));
        form.add(txtCont);

        int res = JOptionPane.showConfirmDialog(this, form, "Crear Archivo en " + rutaDirectorioActual, JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return;

        try {
            int bloques = Math.max(1, Integer.parseInt(txtBloq.getText().trim()));
            String nombre = txtNom.getText().trim();
            if (nombre.isEmpty()) return;
            String rutaCompleta = "/".equals(rutaDirectorioActual) ? "/" + nombre : rutaDirectorioActual + "/" + nombre;

            int id = ++contadorProcesosGUI;
            Proceso p = new Proceso(id, 0, "IO_Create_" + id, 1, 2, 2,
                    TipoProceso.IO_BOUND, TipoOperacionES.CREAR, rutaCompleta);
            p.setEsDirectorioTarget(false);
            p.setBloquesSolicitadosES(bloques);
            p.setPermisoES((PermisoArchivo) cmbPerm.getSelectedItem());
            p.setContenidoES(txtCont.getText());

            computador.encolarProceso(p);
            eventLog.registrar(cicloRelojGlobal, "Proceso E/S " + p.getNombre() + " encolado para CREAR '" + rutaCompleta + "'.");
            refrescarVistaCompleta();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "La cantidad de bloques debe ser un número entero.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void accionRenombrarEntradaGUI() {
        DefaultMutableTreeNode nodoSel = (DefaultMutableTreeNode) treeSistemaArchivos.getLastSelectedPathComponent();
        if (nodoSel == null || !(nodoSel.getUserObject() instanceof EntradaSistemaArchivos)) {
            JOptionPane.showMessageDialog(this, "Seleccione un archivo o carpeta en el árbol.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        EntradaSistemaArchivos entrada = (EntradaSistemaArchivos) nodoSel.getUserObject();
        String nuevoNombre = JOptionPane.showInputDialog(this, "Nuevo nombre para '" + entrada.getNombre() + "':", entrada.getNombre());
        if (nuevoNombre == null || nuevoNombre.trim().isEmpty()) return;

        int id = ++contadorProcesosGUI;
        Proceso p = new Proceso(id, 0, "IO_Rename_" + id, 1, 2, 2,
                TipoProceso.IO_BOUND, TipoOperacionES.ACTUALIZAR, entrada.getRutaCompleta());
        p.setNuevoNombreES(nuevoNombre.trim());

        computador.encolarProceso(p);
        eventLog.registrar(cicloRelojGlobal, "Proceso E/S " + p.getNombre() + " encolado para RENOMBRAR '" + entrada.getRutaCompleta() + "'.");
        refrescarVistaCompleta();
    }

    private void accionEliminarEntradaGUI() {
        DefaultMutableTreeNode nodoSel = (DefaultMutableTreeNode) treeSistemaArchivos.getLastSelectedPathComponent();
        if (nodoSel == null || !(nodoSel.getUserObject() instanceof EntradaSistemaArchivos)) {
            JOptionPane.showMessageDialog(this, "Seleccione un archivo o carpeta en el árbol.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        EntradaSistemaArchivos entrada = (EntradaSistemaArchivos) nodoSel.getUserObject();
        int conf = JOptionPane.showConfirmDialog(this, "¿Crear proceso de E/S para eliminar '" + entrada.getRutaCompleta() + "'?", "Confirmar Eliminación", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;

        int id = ++contadorProcesosGUI;
        Proceso p = new Proceso(id, 0, "IO_Del_" + id, 1, 2, 2,
                TipoProceso.IO_BOUND, TipoOperacionES.ELIMINAR, entrada.getRutaCompleta());

        computador.encolarProceso(p);
        eventLog.registrar(cicloRelojGlobal, "Proceso E/S " + p.getNombre() + " encolado para ELIMINAR '" + entrada.getRutaCompleta() + "'.");
        rutaDirectorioActual = "/";
        refrescarVistaCompleta();
    }

    private void actualizarTablaAsignacion() {
        if (modeloTablaAsignacion == null) return;
        modeloTablaAsignacion.setRowCount(0);
        poblarFilasTablaRecursivo(sistemaArchivos.getRaiz());
    }

    private void poblarFilasTablaRecursivo(EntradaSistemaArchivos entrada) {
        if (entrada == null) return;
        if (!entrada.esDirectorio()) {
            Archivo arch = (Archivo) entrada;
            String cadena = construirCadenaBloques(arch.getPrimerBloque());
            modeloTablaAsignacion.addRow(new Object[]{
                arch.getNombre(),
                arch.getProcesoCreador(),
                arch.getPropietario(),
                arch.getPermiso().name(),
                arch.getTamanoBloques(),
                arch.getPrimerBloque(),
                arch.getColorDistintivo(),
                cadena
            });
        } else {
            Directorio dir = (Directorio) entrada;
            ListaEnlazada hijos = dir.getHijos();
            for (int i = 0; i < hijos.getTamaño(); i++) {
                poblarFilasTablaRecursivo((EntradaSistemaArchivos) hijos.obtener(i));
            }
        }
    }

    private String construirCadenaBloques(int primerBloque) {
        BloqueDisco[] bloques = disco.getBloques();
        if (primerBloque < 0 || primerBloque >= bloques.length) {
            return "Sin asignar";
        }
        StringBuilder sb = new StringBuilder();
        int actual = primerBloque;
        int seguridad = 0;
        while (actual >= 0 && actual < bloques.length && seguridad < bloques.length) {
            if (seguridad > 0) sb.append(" → ");
            sb.append("#").append(actual);
            actual = bloques[actual].getSiguienteBloque();
            seguridad++;
        }
        sb.append(" → EOF");
        return sb.toString();
    }

    // ==================== PANEL DISCO SIMULADO Y MAPA DE BLOQUES ====================

    private JPanel crearPanelDiscoSimulado() {
        JPanel tarjeta = new JPanel(new BorderLayout(8, 5));
        tarjeta.setBackground(COLOR_CARD_BG);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JPanel panelTopDisco = new JPanel(new GridLayout(2, 1, 0, 3));
        panelTopDisco.setOpaque(false);

        JPanel headerDisco = new JPanel(new BorderLayout(8, 0));
        headerDisco.setOpaque(false);

        lblInfoCabezal = new JLabel();
        lblInfoCabezal.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblInfoCabezal.setForeground(COLOR_TEXT_HEADER);

        comboPoliticaDisco = new JComboBox<>(new String[]{"FIFO", "SSTF", "SCAN", "C-SCAN"});
        comboPoliticaDisco.setSelectedItem(disco.getNombrePoliticaActual());
        comboPoliticaDisco.setFont(new Font("Segoe UI", Font.BOLD, 11));
        comboPoliticaDisco.addActionListener(e -> cambiarPoliticaDisco());

        JPanel panelPol = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        panelPol.setOpaque(false);
        panelPol.add(new JLabel("Planificador Disco:"));
        panelPol.add(comboPoliticaDisco);

        headerDisco.add(lblInfoCabezal, BorderLayout.WEST);
        headerDisco.add(panelPol, BorderLayout.EAST);

        lblColaESDisco = new JLabel("Cola E/S Disco: Vacía");
        lblColaESDisco.setFont(new Font("Consolas", Font.BOLD, 11));
        lblColaESDisco.setForeground(COLOR_PRIMARY);

        panelTopDisco.add(headerDisco);
        panelTopDisco.add(lblColaESDisco);
        tarjeta.add(panelTopDisco, BorderLayout.NORTH);

        actualizarInfoDisco();

        panelMapaBloques = new PanelMapaBloques();
        JScrollPane scrollMapa = new JScrollPane(panelMapaBloques);
        scrollMapa.setBorder(BorderFactory.createTitledBorder("Mapa Físico de Bloques del Disco (Color por Archivo y Área Swap)"));
        tarjeta.add(scrollMapa, BorderLayout.CENTER);

        JPanel leyenda = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 2));
        leyenda.setOpaque(false);
        leyenda.add(crearItemLeyenda("Libre (Archivos)", new Color(226, 232, 240)));
        leyenda.add(crearItemLeyenda("Ocupado (Color Archivo)", COLOR_PRIMARY));
        leyenda.add(crearItemLeyenda("Área Swap Libre", new Color(254, 243, 199)));
        leyenda.add(crearItemLeyenda("Área Swap Ocupada", COLOR_WARNING));
        leyenda.add(crearItemLeyenda("Posición Cabezal", COLOR_DANGER));
        tarjeta.add(leyenda, BorderLayout.SOUTH);

        return tarjeta;
    }

    private void cambiarPoliticaDisco() {
        String sel = (String) comboPoliticaDisco.getSelectedItem();
        if (sel == null) return;
        switch (sel) {
            case "SSTF":
                disco.cambiarPlanificador(new PlanificadorDiscoSSTF());
                break;
            case "SCAN":
                disco.cambiarPlanificador(new PlanificadorDiscoSCAN());
                break;
            case "C-SCAN":
                disco.cambiarPlanificador(new PlanificadorDiscoCSCAN());
                break;
            case "FIFO":
            default:
                disco.cambiarPlanificador(new PlanificadorDiscoFIFO());
                break;
        }
        eventLog.registrar(cicloRelojGlobal, "Política de planificación de disco cambiada a: " + sel);
        actualizarInfoDisco();
        actualizarVisorLogs();
    }

    private void actualizarInfoDisco() {
        if (lblInfoCabezal == null) return;
        SolicitudDisco enCurso = disco.getSolicitudEnCurso();
        String estadoSol = (enCurso != null)
                ? "Atendiendo #" + enCurso.getBloqueObjetivo() + " (" + enCurso.getOperacion() + ")"
                : "En espera";

        lblInfoCabezal.setText("💽 Cabezal: #" + disco.getPosicionCabezal()
                + " (" + disco.getDireccionCabezal() + ") | Desplaz.: " + disco.getDesplazamientoTotal()
                + " | " + estadoSol);

        if (lblColaESDisco != null) {
            ColaProcesos cola = disco.getColaSolicitudes();
            int n = (cola != null) ? cola.getTamaño() : 0;
            if (n == 0) {
                lblColaESDisco.setText("Cola E/S Disco (0): Vacía");
            } else {
                StringBuilder sb = new StringBuilder("Cola E/S Disco (").append(n).append("): ");
                for (int i = 0; i < n; i++) {
                    SolicitudDisco s = (SolicitudDisco) cola.obtener(i);
                    if (s != null) {
                        if (i > 0) sb.append(" → ");
                        String pName = (s.getProceso() != null) ? s.getProceso().getNombre() : "Sys";
                        sb.append("[").append(pName).append(" @#").append(s.getBloqueObjetivo()).append("]");
                    }
                }
                lblColaESDisco.setText(sb.toString());
            }
        }
    }

    private JPanel crearItemLeyenda(String texto, Color color) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        item.setOpaque(false);
        JPanel cuadro = new JPanel();
        cuadro.setPreferredSize(new Dimension(12, 12));
        cuadro.setBackground(color);
        cuadro.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        item.add(cuadro);
        item.add(lbl);
        return item;
    }

    private class PanelMapaBloques extends JPanel {
        public PanelMapaBloques() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(620, 200));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            BloqueDisco[] bloques = disco.getBloques();
            int total = bloques.length;
            int cabezal = disco.getPosicionCabezal();

            int cols = 20;
            int filas = (int) Math.ceil((double) total / cols);
            int celdaW = Math.max(24, (getWidth() - 20) / cols);
            int celdaH = 22;

            setPreferredSize(new Dimension(cols * celdaW + 20, filas * celdaH + 20));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 9));

            for (int i = 0; i < total; i++) {
                int f = i / cols;
                int c = i % cols;
                int x = 10 + c * celdaW;
                int y = 10 + f * celdaH;

                BloqueDisco b = bloques[i];
                Color relleno;
                Color texto = COLOR_TEXT_HEADER;

                if (b.isEsAreaSwap()) {
                    relleno = b.isOcupado() ? COLOR_WARNING : new Color(254, 243, 199);
                } else if (b.isOcupado()) {
                    relleno = (b.getColorArchivo() != null) ? b.getColorArchivo() : COLOR_PRIMARY;
                    texto = Color.WHITE;
                } else {
                    relleno = new Color(226, 232, 240);
                }

                g2.setColor(relleno);
                g2.fillRoundRect(x + 1, y + 1, celdaW - 3, celdaH - 3, 4, 4);

                if (i == cabezal) {
                    g2.setColor(COLOR_DANGER);
                    g2.setStroke(new BasicStroke(2.2f));
                    g2.drawRoundRect(x, y, celdaW - 2, celdaH - 2, 4, 4);
                }

                g2.setColor(texto);
                g2.drawString(String.valueOf(i), x + 4, y + 14);
            }

            g2.dispose();
        }
    }

    // ==================== DIÁLOGO DE MÉTRICAS (CPU + CABEZAL + SWAP) ====================

    private void mostrarVentanaMetricas() {
        JDialog dialog = new JDialog(this, "Métricas de Rendimiento: CPU, Disco y Memoria Virtual", true);
        dialog.setSize(760, 620);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.getContentPane().setBackground(COLOR_BG);

        JPanel panelTitulo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelTitulo.setBackground(COLOR_HEADER);
        JLabel lblTitulo = new JLabel("📊 INDICADORES DE RENDIMIENTO DEL SISTEMA (CPU, E/S DISCO Y SWAP)");
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panelTitulo.add(lblTitulo);

        JPanel panelStats = new JPanel(new GridLayout(4, 2, 12, 6));
        panelStats.setBorder(BorderFactory.createEmptyBorder(10, 20, 5, 20));
        panelStats.setOpaque(false);

        panelStats.add(crearLabelEtiqueta("Procesos Completados / Throughput:"));
        panelStats.add(crearLabelValor(
                metricasService.getProcesosCompletadosTotales() + " proc.  ("
                + String.format("%.3f", metricasService.calcularThroughput(cicloRelojGlobal)) + " proc/ciclo)",
                COLOR_PRIMARY));

        panelStats.add(crearLabelEtiqueta("Tiempo Respuesta Promedio / Deadlines:"));
        panelStats.add(crearLabelValor(
                String.format("%.2f ciclos  |  %.1f%% cumplidos",
                        metricasService.calcularTiempoRespuestaPromedio(),
                        metricasService.calcularTasaCumplimientoDeadlines()),
                COLOR_SUCCESS));

        panelStats.add(crearLabelEtiqueta("Desplazamiento Cabezal / Espera Prom. E/S:"));
        panelStats.add(crearLabelValor(
                disco.getDesplazamientoTotal() + " bloques (" + disco.getNombrePoliticaActual() + ")  |  "
                + String.format("%.2f ciclos espera", metricasService.calcularTiempoEsperaPromedioES()),
                COLOR_DANGER));

        panelStats.add(crearLabelEtiqueta("Operaciones Swap (Swap-Out / Swap-In):"));
        panelStats.add(crearLabelValor(
                metricasService.getTotalSuspensiones() + " suspensiones  /  "
                + metricasService.getTotalReactivaciones() + " reactivaciones",
                COLOR_PURPLE));

        JPanel panelGraficas = new JPanel(new GridLayout(2, 1, 0, 10));
        panelGraficas.setOpaque(false);
        panelGraficas.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));

        PanelGraficaCabezal graficaCabezal = new PanelGraficaCabezal(metricasService);
        PanelGraficaCPUUnica graficaCPU = new PanelGraficaCPUUnica(metricasService);

        panelGraficas.add(graficaCabezal);
        panelGraficas.add(graficaCPU);

        JPanel centro = new JPanel(new BorderLayout(5, 8));
        centro.setOpaque(false);
        centro.add(panelStats, BorderLayout.NORTH);
        centro.add(panelGraficas, BorderLayout.CENTER);

        JButton btnCerrar = crearBotonFlat("Cerrar", COLOR_PRIMARY, Color.WHITE);
        btnCerrar.addActionListener(e -> dialog.dispose());
        JPanel panelSur = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelSur.setOpaque(false);
        panelSur.add(btnCerrar);

        dialog.add(panelTitulo, BorderLayout.NORTH);
        dialog.add(centro, BorderLayout.CENTER);
        dialog.add(panelSur, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private static class PanelGraficaCPUUnica extends JPanel {
        private final MetricasService metricasService;

        public PanelGraficaCPUUnica(MetricasService ms) {
            this.metricasService = ms;
            setBackground(new Color(15, 23, 42));
            setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(new Color(51, 65, 85)),
                    "Utilización de CPU vs. Tiempo (Ciclos)",
                    TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 11), Color.WHITE));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int padL = 45, padR = 20, padT = 25, padB = 25;
            int gw = Math.max(10, w - padL - padR);
            int gh = Math.max(10, h - padT - padB);

            g2.setColor(new Color(51, 65, 85));
            for (int i = 0; i <= 4; i++) {
                int y = padT + (gh * i / 4);
                g2.drawLine(padL, y, w - padR, y);
                g2.setColor(new Color(148, 163, 184));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                g2.drawString((100 - i * 25) + "%", 12, y + 4);
                g2.setColor(new Color(51, 65, 85));
            }

            double[] hist = metricasService.getHistorialUsoCpu();
            if (hist.length > 1) {
                int maxC = hist.length - 1;
                g2.setColor(COLOR_SUCCESS);
                g2.setStroke(new BasicStroke(2.0f));
                for (int i = 0; i < maxC; i++) {
                    int x1 = padL + (i * gw / maxC);
                    int y1 = padT + gh - (int) (hist[i] * gh / 100.0);
                    int x2 = padL + ((i + 1) * gw / maxC);
                    int y2 = padT + gh - (int) (hist[i + 1] * gh / 100.0);
                    g2.drawLine(x1, y1, x2, y2);
                }
            }
            g2.dispose();
        }
    }

    // ==================== LOGS Y UTILIDADES UI ====================

    private JPanel crearPanelInferiorLogs() {
        JPanel panel = new JPanel(new BorderLayout(5, 4));
        panel.setBackground(COLOR_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_BORDER),
                BorderFactory.createEmptyBorder(6, 15, 8, 15)
        ));

        JLabel lblTituloLogs = new JLabel("📜 Bitácora de Auditoría de Eventos (EventLog)");
        lblTituloLogs.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTituloLogs.setForeground(COLOR_TEXT_HEADER);

        txtLogEventos = new JTextArea(4, 80);
        txtLogEventos.setFont(new Font("Consolas", Font.PLAIN, 11));
        txtLogEventos.setEditable(false);
        txtLogEventos.setBackground(COLOR_CARD_BG);

        JScrollPane scrollLog = new JScrollPane(txtLogEventos);
        scrollLog.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));

        panel.add(lblTituloLogs, BorderLayout.NORTH);
        panel.add(scrollLog, BorderLayout.CENTER);
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
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setForeground(fg);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        return btn;
    }

    private JButton crearBotonMini(String texto, Color bg) {
        JButton btn = crearBotonFlat(texto, bg, Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btn.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        return btn;
    }

    private void accionPausarReanudar(ActionEvent e) {
        simulacionEnEjecucion = !simulacionEnEjecucion;
        if (simulacionEnEjecucion) {
            btnPausarReanudar.setText("Pausar");
            btnPausarReanudar.setBackground(COLOR_DANGER);
            timerSimulacion.start();
        } else {
            btnPausarReanudar.setText("Iniciar");
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
        SwingUtilities.invokeLater(() -> new Vista2Monitoreo().setVisible(true));
    }
}