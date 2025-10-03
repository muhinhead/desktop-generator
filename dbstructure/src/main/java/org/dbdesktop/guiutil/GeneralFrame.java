package org.dbdesktop.guiutil;

import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import org.dbdesktop.orm.ExchangeFactory;
import org.dbdesktop.orm.IMessageSender;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.rmi.RemoteException;
import java.util.HashMap;

public abstract class GeneralFrame extends JFrame implements WindowListener {


    private IMessageSender exchanger;
    private JPanel statusPanel = new JPanel();
    private JLabel statusLabel1 = new JLabel();
    private JLabel statusLabel2 = new JLabel();
    private JTabbedPane mainPanel;
    private ToolBarButton aboutButton;
    private ToolBarButton exitButton;
    private JToolBar toolBar;
    private ToolBarButton refreshButton;
    private ToolBarButton printButton;
    private JToggleButton searchButton;
    //    private JToggleButton filterButton;
    private HashMap<DbTableGridPanel, String> grids = new HashMap<DbTableGridPanel, String>();
    //    private HashMap<GeneralReportPanel, String> reports = new HashMap<GeneralReportPanel, String>();
//    private HashMap<HTMLpanel, String> browsers = new HashMap<HTMLpanel, String>();
    private JLabel srcLabel;
    private JTextField srcField;
    //    private JLabel fltrLabel;
//    private JTextField fltrField;
    private JPopupMenu sharePopup;
    private ToolBarButton zoomButton;
    private boolean isZoomed = false;

    public GeneralFrame(String title, IMessageSender exch) {
        super(title);
        addWindowListener(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.exchanger = exch;
        fillContentPane();
        setSizeFromConfig();
        setVisible(true);
    }

    private void setSizeFromConfig() {
        float width = Float.valueOf(ExchangeFactory.getPropLogEngine().readProperty("WindowWidth", "0.8"));
        float height = Float.valueOf(ExchangeFactory.getPropLogEngine().readProperty("WindowHeight", "0.8"));
        width = (width > 0.0 ? width : (float) 0.8);
        height = (height > 0.0 ? height : (float) 0.8);
        GeneralUtils.setSizes(this, width, height);
        GeneralUtils.centerOnScreen(this);
        if (width < 0 || width < 0) {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
    }

    public GeneralFrame(IMessageSender exch) {
        this("Works", exch);
    }

    protected abstract String[] getSheetList();

    public void setLookAndFeel(String lf) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException,
            UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(lf);
        SwingUtilities.updateComponentTreeUI(this);
        ExchangeFactory.getPropLogEngine().getProps().setProperty("LookAndFeel", lf);
    }

    public static File chooseFileForExport(String extension) {
        while (extension.startsWith(".")) {
            extension = extension.substring(1);
        }
        JFileChooser chooser = new JFileChooser(System.getProperty("user.home"));
        chooser.setFileFilter(new FileFilterOnExtension(extension));
        chooser.setDialogTitle("Экспорт в файл");
        chooser.setApproveButtonText("Сохранить");
        int retVal = chooser.showOpenDialog(null);
        if (retVal == JFileChooser.APPROVE_OPTION) {
            String name = chooser.getSelectedFile().getAbsolutePath();
            if (!name.endsWith("." + extension)) {
                name += "." + extension;
            }
            return new File(name);
        } else {
            return null;
        }
    }

    private void fillContentPane() {
        try {
            //GeneralUtils.setWindowIcon(this, "Xcost.png");
            getContentPane().setLayout(new BorderLayout());
            statusPanel.setBorder(BorderFactory.createEtchedBorder());
            statusPanel.setLayout(new BorderLayout());
            setStatusLabel1Text(" ");
            statusLabel1.setBorder(BorderFactory.createEtchedBorder());
            statusLabel2.setHorizontalTextPosition(SwingConstants.CENTER);
            statusLabel2.setText(" ");
            statusPanel.add(statusLabel2, BorderLayout.CENTER);

            printButton = new ToolBarButton("print.png");
            printButton.setToolTipText("Вывод текущего списка");
            printButton.addActionListener(getPrintAction());

            searchButton = new JToggleButton(new ImageIcon(GeneralUtils.loadImage("search.png")));
            getSearchButton().setToolTipText("Поиск");
            getSearchButton().addActionListener(getSearchAction());

//        filterButton = new JToggleButton(new ImageIcon(Util.loadImage("filter.png")));
//        filterButton.setToolTipText("Filter on fragment");
//        filterButton.addActionListener(getFilterAction());
            sharePopup = new JPopupMenu();
            sharePopup.add(new JMenuItem(new AbstractAction("Output to HTML") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    GeneralGridPanel grid = getActiveGrid();
                    if (grid != null) {
                        export2HTML(GeneralFrame.this, grid);
                    }
                }

            }));
            sharePopup.add(new JMenuItem(new AbstractAction("Output to PDF") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    GeneralGridPanel grid = getActiveGrid();
                    if (grid != null) {
                        export2PDF(GeneralFrame.this, grid);
                    }
                }

            }));
            sharePopup.add(new JMenuItem(new AbstractAction("Output to CSV") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    GeneralGridPanel grid = getActiveGrid();
                    if (grid != null) {
                        export2CSV(grid);
                    }
                }

            }));
//        sharePopup.add(new JMenuItem(new AbstractAction("As PDF") {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                //GeneralFrame.notImplementedYet();
//                DbTableDocument doc = (DbTableDocument) activeGridPanel.getController().getDocument();
//                File pdfFile = null, htmlFile = null;
//                try {
//                    String header = JOptionPane.showInputDialog(rootPane, "Enter title for the document:", "Title");
//                    pdfFile = chooseFileForExport("pdf");
//                    String htmlFileName = pdfFile.getName() + ".html";
//                    doc.generateHTML(htmlFile = new File(htmlFileName), header);
//                    if (htmlFile != null) {
//                        Document document = new Document();
//                        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
//                        document.open();
//                        XMLWorkerHelper.getInstance().parseXHtml(writer, document,
//                                new FileInputStream(htmlFile));
//                        document.close();
//                        if (pdfFile != null) {
//                            Desktop desktop = Desktop.getDesktop();
//                            desktop.open(pdfFile);
//                            JOptionPane.showMessageDialog(rootPane,
//                                    "File " + pdfFile.getAbsolutePath() + " generated",
//                                    "Ok!", JOptionPane.INFORMATION_MESSAGE);
//                        } else {
//                            JOptionPane.showMessageDialog(rootPane,
//                                    "Can't create file " + pdfFile.getAbsolutePath()
//                                    + "! Check the target folder permissions", "Error!",
//                                    JOptionPane.ERROR_MESSAGE);
//                        }
//                    } else {
//                        JOptionPane.showMessageDialog(rootPane,
//                                "Can't create temporary file " + htmlFileName
//                                + "! Check the target folder permissions", "Error!",
//                                JOptionPane.ERROR_MESSAGE);
//                    }
//                } catch (Exception ex) {
//                    XlendWorks.logAndShowMessage(ex);
//                } finally {
//                    if (htmlFile != null) {
//                        htmlFile.delete();
//                    }
//                }
//            }
//        }));
            refreshButton = new ToolBarButton("refresh.png");
            refreshButton.setToolTipText("Обновить из базы");
            refreshButton.addActionListener(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    refreshGrids();
                }
            });
            aboutButton = new ToolBarButton("help.png");
            aboutButton.setToolTipText("О программе");
            exitButton = new ToolBarButton("exitcross.png");
            exitButton.setToolTipText("Закрыть программу");
            exitButton.addActionListener(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    //setVisible(false);
                    closeAndExit();
                }
            });

            zoomButton = new ToolBarButton("fullscreen.png");
            zoomButton.setToolTipText("Полный экран");

            toolBar = new JToolBar();
            toolBar.add(getSearchButton());
            toolBar.add(srcLabel = new JLabel("  Фильтр:"));
            toolBar.add(srcField = new JTextField(20));
            srcLabel.setVisible(false);
            srcField.setVisible(false);
            srcField.addKeyListener(getSrcFieldKeyListener());
            srcField.setMaximumSize(srcField.getPreferredSize());

            toolBar.add(printButton);
            additionalButtonAfterPrint(toolBar);
            toolBar.add(refreshButton);

            toolBar.add(aboutButton);
            toolBar.add(zoomButton);
            toolBar.add(exitButton);
            aboutButton.setToolTipText("О программе...");
            aboutButton.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    //new AboutDialog();
                }
            });

            zoomButton.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                    if (isZoomed) {
                        GeneralFrame.this.setExtendedState(getExtendedState() & ~JFrame.MAXIMIZED_BOTH);
                        GeneralFrame.this.setUndecorated(false);
                        setSizeFromConfig();
                    } else {
                        GeneralFrame.this.setExtendedState(JFrame.MAXIMIZED_BOTH);
                        GeneralFrame.this.setUndecorated(true);
                    }
                    setVisible(true);
                    isZoomed = !isZoomed;
                }
            });

            exitButton.setToolTipText("Закрыть это окно");

            getContentPane().add(toolBar, BorderLayout.NORTH);

            mainPanel = getMainPanel();
            getContentPane().add(mainPanel, BorderLayout.CENTER);
            mainPanel.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
//                    getSearchButton().setSelected(false);
//                    srcField.setText(null);
//                    srcField.setVisible(false);
//                    srcLabel.setVisible(false);

//                fltrField.setText(null);
//                fltrField.setVisible(false);
//                fltrLabel.setVisible(false);
                    highlightFound();
                }
            });

            getContentPane().add(statusPanel, BorderLayout.SOUTH);
            buildMenu();
        } catch (Exception e) {
            e.printStackTrace();
            ExchangeFactory.getPropLogEngine().logAndShowMessage(e);
        }
    }

    private void closeAndExit() {
        //System.out.println("!!! Close And Exit !!!!");
        float xRatio = -1;
        float yRatio = -1;
        if (this.getExtendedState() != JFrame.MAXIMIZED_BOTH) {
            xRatio = GeneralUtils.getXratio(this);
            yRatio = GeneralUtils.getYratio(this);
        }
        ExchangeFactory.getPropLogEngine().getProps().setProperty("WindowWidth", "" + xRatio);
        ExchangeFactory.getPropLogEngine().getProps().setProperty("WindowHeight", "" + yRatio);
        ExchangeFactory.getPropLogEngine().saveProps();
        GeneralUtils.quit(0);
    }

    private KeyAdapter getSrcFieldKeyListener() {
        return new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                highlightFound();
            }
        };
    }

    private GeneralGridPanel getActiveGrid() {
        Component selectedPanel = mainPanel.getSelectedComponent();
        if (selectedPanel instanceof GeneralGridPanel) {
            return (GeneralGridPanel) selectedPanel;
        }
        return null;
    }

    private void highlightFound() {
        Component selectedPanel = mainPanel.getSelectedComponent();
        if (selectedPanel instanceof GeneralGridPanel) {
            GeneralGridPanel selectedGridPanel = (GeneralGridPanel) selectedPanel;
            try {
                RowFilter<DbTableView.MyTableModel, Object> rf = RowFilter.regexFilter("(?i)" + srcField.getText());
                selectedGridPanel.getTableView().getSorter().setRowFilter(rf);
            } catch (Exception ex) {
                ExchangeFactory.getPropLogEngine().log(ex);
            }
//            selectedGridPanel.highlightSearch(srcField.getText());
        }
    }

    public void setVisible(boolean b) {
        if (b) {
            refreshGrids();
        }
        super.setVisible(b);
    }

    private ActionListener getSearchAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean pressed = getSearchButton().isSelected();
                srcLabel.setVisible(pressed);
                srcField.setVisible(pressed);
                if (pressed) {
                    srcField.requestFocus();
                } else {
                    srcField.setText("");
                    highlightFound();
                }
            }
        };
    }

    private void refreshGrids() {
        for (DbTableGridPanel grid : grids.keySet()) {
            try {
                updateGrid(getExchanger(), grid.getTableView(), grid.getTableDoc(), grids.get(grid), null,
                        grid.getPageSelector().getSelectedIndex());
            } catch (RemoteException ex) {
                ExchangeFactory.getPropLogEngine().logAndShowMessage(ex);
            }
        }
//        for (GeneralReportPanel report : reports.keySet()) {
//            report.updateReport();
//        }
//        for (HTMLpanel html : browsers.keySet()) {
//            try {
//                html.refresh();
//            } catch (IOException ex) {
//                XlendWorks.logAndShowMessage(ex);
//            }
//        }
    }

    //    public static void errMessageBox(String title, String msg) {
//        JOptionPane.showMessageDialog(null, msg, title, JOptionPane.ERROR_MESSAGE);
//    }
//
//    public static void infoMessageBox(String title, String msg) {
//        JOptionPane.showMessageDialog(null, msg, title, JOptionPane.INFORMATION_MESSAGE);
//    }
//
//    public static int yesNo(String msg, String title) {
//        int ok = JOptionPane.showConfirmDialog(null, title, msg,
//                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
//        return ok;
//    }
//
//    public static void notImplementedYet() {
//        errMessageBox("Sorry!", "Not implemented yet");
//    }
//
//    public static void notImplementedYet(String msg) {
//        errMessageBox("Sorry!", "Not implemented yet " + msg);
//    }
    public void setStatusLabel1Text(String lbl) {
        statusLabel1.setText(lbl);
    }

    private void buildMenu() {
        JMenuBar bar = new JMenuBar();
        JMenu m = createMenu("File", "File Operations");
        JMenuItem mi = createMenuItem("Exit", "Close the program");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //setVisible(false);
                closeAndExit();
            }
        });
        m.add(mi);
        bar.add(m);
        m = createMenu("Настройки", "Настройки приложения");
        mi = createMenuItem("Подключение", "Напстройка подключения к БД");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                //TODO: implement analog of TheLaserShop.configureConnection();
            }
        });
        m.add(mi);
//        m.add(XlendWorks.appearanceMenu("Theme",this));
//        bar.add(m);

//        m = createMenu("Edit", "Edit operations");
//        mi = createMenuItem("Find...", "Search");
//        mi.addActionListener(getSearchAction());
//        m.add(mi);
//        bar.add(m);
        setJMenuBar(bar);
    }

    protected abstract JTabbedPane getMainPanel();

    protected JMenuItem createMenuItem(String label, String microHelp) {
        JMenuItem m = new JMenuItem(label);
        setMenuStatusMicroHelp(m, microHelp);
        return m;
    }

    protected JMenuItem createMenuItem(String label) {
        return createMenuItem(label, label);
    }

    protected JMenu createMenu(String label, String microHelp) {
        JMenu m = new JMenu(label);
        setMenuStatusMicroHelp(m, microHelp);
        return m;
    }

    protected JMenu createMenu(String label) {
        return createMenu(label, label);
    }

    protected void setMenuStatusMicroHelp(final JMenuItem m, final String msg) {
        m.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                statusLabel2.setText(msg == null ? m.getText() : msg);
            }
        });
    }

    public static void updateGrid(IMessageSender exchanger,
                                  ITableView view, DbTableDocument doc, String select, Integer id, int page)
            throws RemoteException {
        int row = view.getSelectedRow();
        try {
            if (select != null) {
                ((JComponent) view).setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                if (page >= 0) {
                    doc.setBody(exchanger.getTableBody(select, page, GeneralGridPanel.PAGESIZE));
                } else {
                    doc.setBody(exchanger.getTableBody(select));
                }
                view.getController().updateExcept(null);
                if (id != null) {
                    DbTableGridPanel.selectRowOnId(view, id);
                } else {
                    row = row < view.getRowCount() ? row : row - 1;
                    if (row >= 0 && row < view.getRowCount()) {
                        view.setRowSelectionInterval(row, row);
                        if (view instanceof JTable) {
                            ((JTable) view).scrollRectToVisible(((JTable) view).getCellRect(row, 0, true));
                        }
                    }
                }
            }
        } finally {
            ((JComponent) view).setCursor(Cursor.getDefaultCursor());
        }
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
        closeAndExit();
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    protected void registerGrid(GeneralGridPanel grid) {
        grids.put(grid, grid.getSelect());
    }

    /**
     * @return the exchanger
     */
    public IMessageSender getExchanger() {
        return exchanger;
    }

    protected ActionListener getPrintAction() {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComponent src = (JComponent) e.getSource();
                sharePopup.show(src, src.getWidth() / 2, src.getHeight() / 2);
            }
        };
    }

    /**
     * @return the searchButton
     */
    protected JToggleButton getSearchButton() {
        return searchButton;
    }

    public static void export2CSV(GeneralGridPanel grid) {
        DbTableDocument doc = (DbTableDocument) grid.getController().getDocument();
        try {
            File csvFile;
            doc.generateCSV(csvFile = chooseFileForExport("csv"));
            if (csvFile != null) {
                Desktop desktop = Desktop.getDesktop();
                desktop.open(csvFile);
            }
        } catch (Exception ex) {
            ExchangeFactory.getPropLogEngine().logAndShowMessage(ex);
        }
    }

    public static void export2HTML(JFrame f, GeneralGridPanel grid) {
        DbTableDocument doc = (DbTableDocument) grid.getController().getDocument();
        try {
            File htmlFile;
            String header = JOptionPane.showInputDialog(f == null ? null : f.getRootPane(), "Enter title for the document:", "Title");
            doc.generateHTML(htmlFile = chooseFileForExport("html"), header);
            if (htmlFile != null) {
                Desktop desktop = Desktop.getDesktop();
                desktop.open(htmlFile);
            }
        } catch (Exception ex) {
            ExchangeFactory.getPropLogEngine().logAndShowMessage(ex);
        }
    }

    public static void export2PDF(JFrame f, GeneralGridPanel grid) {
        DbTableDocument doc = (DbTableDocument) grid.getController().getDocument();
        File pdfFile = null, htmlFile = null;
        try {
            String header = JOptionPane.showInputDialog(f == null ? null : f.getRootPane(), "Enter title for the document:", "Title");
            pdfFile = chooseFileForExport("pdf");
            String htmlFileName = pdfFile.getName() + ".html";
            doc.generateHTML(htmlFile = new File(htmlFileName), header);
            if (htmlFile != null) {
                Document document = new Document();
                PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
                document.open();
                XMLWorkerHelper.getInstance().parseXHtml(writer, document,
                        new FileInputStream(htmlFile));
                document.close();
                if (pdfFile != null) {
                    Desktop desktop = Desktop.getDesktop();
                    desktop.open(pdfFile);
//                    JOptionPane.showMessageDialog(f == null ? null : f.getRootPane(),
//                            "File " + pdfFile.getAbsolutePath() + " generated",
//                            "Ok!", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(f == null ? null : f.getRootPane(),
                            "Can't create file " + pdfFile.getAbsolutePath()
                                    + "! Check the target folder permissions", "Error!",
                            JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(f == null ? null : f.getRootPane(),
                        "Can't create temporary file " + htmlFileName
                                + "! Check the target folder permissions", "Error!",
                        JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            ExchangeFactory.getPropLogEngine().logAndShowMessage(ex);
        }
    }

    protected void additionalButtonAfterPrint(JToolBar toolBar) {
    }

}
