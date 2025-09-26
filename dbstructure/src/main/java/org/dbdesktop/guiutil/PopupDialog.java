package org.dbdesktop.guiutil;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;

public abstract class PopupDialog extends JDialog {

    protected Frame ownerFrame;
    private Object object;

    public PopupDialog() {
        super((Frame)null,true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    public PopupDialog(Frame owner, String title, Object obj) {
        super(owner, title);
        ownerFrame = owner;
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setObject(obj);
        init();
    }

    protected void init() {
        fillContent();
        initSize();
//        setMinimumSize(getSize());
        setVisible(true);
    }

    protected Color getHeaderBackground() {
        return new Color(53,100,230);//(226, 148, 37);
    }

    protected Color getHeaderForeground() {
        return new Color(255, 255, 255);
    }

    protected void fillContent() {
        getContentPane().setLayout(new BorderLayout(10, 10));
        Color bg = getHeaderBackground();
        if (bg != null) {
            JPanel headerPanel = new JPanel();//ImagePanel(GeneralUtils.loadImage("Login.jpg", PopupDialog.class));//JPanel();
            headerPanel.setBackground(bg);
            JLabel lbl = new JLabel(getTitle(), SwingConstants.CENTER);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 18));
            lbl.setForeground(getHeaderForeground());
            headerPanel.add(lbl);
            getContentPane().add(headerPanel, BorderLayout.NORTH);
        }
    }

    protected void initSize() {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        pack();
        if (d.width < getWidth()) {
            setSize((int) (d.width), getHeight());
        }
        if (d.height * .93 < getHeight()) {
            setSize(getWidth(), (int) (d.height * .93));
        }
        setLocation(d.width / 2 - getWidth() / 2, d.height / 2 - getHeight() / 2);
        this.setModal(true);
    }

    protected void centerWindow() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = getSize();
        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }
        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }
        setLocation((screenSize.width - frameSize.width) / 2,
                (screenSize.height - frameSize.height) / 2);

        validate();
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    private void removeComponents(Container cont) {
        //System.out.println("!! removeComponents(" + cont.getClass().getName() + ")");
        Component[] components = cont.getComponents();
        for (int i = components.length - 1; i >= 0; i--) {
            Component comp = components[i];
            if (comp != null) {
                if (comp instanceof Container) {
                    removeComponents((Container) comp);
                }
                if (comp instanceof PopupDialog) {
                    ((PopupDialog) comp).freeResources();
                }
                cont.remove(comp);
                comp = null;
            }
        }
    }

    public void freeResources(){
        getContentPane().removeAll();
    }

    @Override
    public void dispose() {
        removeComponents(getContentPane());
        freeResources();
        super.dispose();
    }

    public static void updateList(final JTable tableView) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    Thread.currentThread().sleep(1000);
                } catch (InterruptedException ex) {
                }
                AbstractTableModel model = (AbstractTableModel) tableView.getModel();
                int selectedRow = tableView.getSelectedRow();
                model.fireTableDataChanged();
                tableView.setRowSelectionInterval(selectedRow, selectedRow);
            }
        });
    }
}
