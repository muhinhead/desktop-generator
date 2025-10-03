package org.dbdesktop.guiutil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Vector;

public class DbTableGridPanel extends JPanel {

    protected static HashMap<Integer, Integer> getMaxWidths(int[] widths) {
        HashMap<Integer, Integer> maxWidths = new HashMap<Integer, Integer>();
        for (int i = 0; i < widths.length; i++) {
            maxWidths.put(i, widths[i]);
        }
        return maxWidths;
    }
    private DbTableView tableView = null;
    private DbTableDocument tableDoc = null;
    private JScrollPane sp = null;
    private AbstractAction addAction = null;
    private AbstractAction editAction = null;
    private AbstractAction delAction = null;
    private JButton addButton = null;
    private JButton editButton = null;
    private JButton delButton = null;
    private MouseAdapter doubleClickAdapter;
    private Controller controller;
    private JComboBox pageSelector;
    private JProgressBar progressBar;
    private JLabel pageLbl;
    protected JPopupMenu popMenu;
    protected JLabel countLabel;

    public void showPageSelector(boolean toshow) {
        pageSelector.setVisible(toshow);
        progressBar.setVisible(toshow);
        pageLbl.setVisible(toshow);
    }

    public DbTableGridPanel(
            AbstractAction addAction,
            AbstractAction editAction,
            AbstractAction delAction,
            Vector[] tableBody) {
        this(addAction, editAction, delAction, tableBody, null);
    }

    public DbTableGridPanel() {
        super(new BorderLayout());
    }

    public DbTableGridPanel(
            AbstractAction addAction,
            final AbstractAction editAction,
            AbstractAction delAction,
            Vector[] tableBody, HashMap<Integer, Integer> maxWidths) {
        this();
        init(new AbstractAction[]{addAction, editAction, delAction}, null, tableBody, maxWidths, null);
    }

    protected void init(AbstractAction[] acts, String select, Vector[] tableBody, HashMap<Integer, Integer> maxWidths, DbTableView tabView) {
        this.setAddAction(acts.length > 0 ? acts[0] : null);
        this.setEditAction(acts.length > 1 ? acts[1] : null);
        this.setDelAction(acts.length > 2 ? acts[2] : null);

        tableView = (tabView == null ? new DbTableView() : tabView);
        if (maxWidths != null) {
            getTableView().setMaxColWidths(maxWidths);
        } else {
            getTableView().setMaxColWidth(0, 40);
        }

        Font font = getTableView().getFont();
        font = font.deriveFont(Font.BOLD);//(float) (font.getSize2D() * 2));
        getTableView().setFont(font);

        tableDoc = new DbTableDocument(toString(), tableBody);
        tableDoc.setSelectStatement(select);
        controller = new Controller(getTableDoc(), getTableView());
        JPanel btnPanel = new JPanel(new GridLayout(acts.length, 1, 5, 5));
        if (getAddAction() != null) {
            btnPanel.add(addButton = new JButton(getAddAction()));
        }
        if (getEditAction() != null) {
            btnPanel.add(editButton = new JButton(getEditAction()));
        }
        if (getDelAction() != null) {
            btnPanel.add(delButton = new JButton(getDelAction()));
        }
        for (int i = 3; i < acts.length; i++) {
            btnPanel.add(new JButton(acts[i]));
        }
        JPanel pageChoosePanel = new JPanel(new FlowLayout());
        pageChoosePanel.add(progressBar = new JProgressBar());
        progressBar.setIndeterminate(false);
        pageChoosePanel.add(progressBar);
        pageChoosePanel.add(pageLbl = new JLabel("Page:", SwingConstants.RIGHT));
        pageChoosePanel.add(pageSelector = new JComboBox());
        add(sp = new JScrollPane((JComponent) getTableView()), BorderLayout.CENTER);
        add(getRightPanel(btnPanel), BorderLayout.EAST);
        add(pageChoosePanel, BorderLayout.SOUTH);
        getTableView().addMouseListener(doubleClickAdapter = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && getEditAction() != null) {
                    getEditAction().actionPerformed(null);
                }
            }
        });
        activatePopup(getAddAction(), getEditAction(), getDelAction());
    }

    protected JPanel getRightPanel(JPanel btnPanel) {
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(btnPanel, BorderLayout.NORTH);
        rightPanel.add(countLabel = new JLabel("", SwingConstants.CENTER), BorderLayout.SOUTH);
        countLabel.setBorder(BorderFactory.createEtchedBorder());
        return rightPanel;
    }

    protected void activatePopup(AbstractAction addAction,
                                 final AbstractAction editAction,
                                 AbstractAction delAction) {
        popMenu = new JPopupMenu();

        popMenu.add(addAction);
        popMenu.add(editAction);
        popMenu.add(delAction);
        getTableView().addMouseListener(new PopupListener(popMenu));
        sp.addMouseListener(new PopupListener(popMenu));
    }

    public void selectRowOnId(int id) {
        selectRowOnId(getTableView(), id);
    }

    public static void selectRowOnId(ITableView view, int id) {
        for (int row = 0; row < view.getRowData().size(); row++) {
            Vector line = (Vector) view.getRowData().get(row);
            try {
                if (Integer.parseInt(line.get(0).toString()) == id) {
                    view.setSelectedRow(row);
                    break;
                }
            } catch (NumberFormatException ne) {
            }
        }
    }

    public int getSelectedID() {
        int row = getTableView().getSelectedRow();
        if (row >= 0 && row < getTableView().getRowCount()) {//&& row < tableView.getSelectedRow()) {
            String sid = //(String)
                    getTableView().getValueAt(row, 0).toString();
            return Integer.valueOf(sid);
        }
        return 0;
    }

    public int[] getSelectedIDs() {
        int rows[] = getTableView().getSelectedRows();
        int ids[] = new int[rows.length];
        int r = 0;
        for (int row : rows) {
            Vector line = (Vector) getTableView().getRowData().get(row);
            ids[r++] = Integer.parseInt((String) line.get(0));
        }
        return ids;
    }

    public String getSelectedRowCeil(int col) {
        int row = getTableView().getSelectedRow();
        if (row >= 0) {//&& row < tableView.getSelectedRow()) {
            Vector line = (Vector) getTableView().getRowData().get(row);
            return (String) line.get(col);
        }
        return "";
    }

    /**
     * @return the tableView
     */
    public DbTableView getTableView() {
        return tableView;
    }

    /**
     * @return the tableDoc
     */
    public DbTableDocument getTableDoc() {
        return tableDoc;
    }

    /**
     * @return the addAction
     */
    public AbstractAction getAddAction() {
        return addAction;
    }

    /**
     * @return the editAction
     */
    public AbstractAction getEditAction() {
        return editAction;
    }

    /**
     * @return the delAction
     */
    public AbstractAction getDelAction() {
        return delAction;
    }

    /**
     * @return the addButton
     */
    public JButton getAddButton() {
        return addButton;
    }

    /**
     * @return the editButton
     */
    public JButton getEditButton() {
        return editButton;
    }

    /**
     * @return the delButton
     */
    public JButton getDelButton() {
        return delButton;
    }

    /**
     * @return the doubleClickAdapter
     */
    public MouseAdapter getDoubleClickAdapter() {
        return doubleClickAdapter;
    }

    /**
     * @param addAction the addAction to set
     */
    public void setAddAction(AbstractAction addAction) {
        this.addAction = addAction;
    }

    /**
     * @param editAction the editAction to set
     */
    public void setEditAction(AbstractAction editAction) {
        this.editAction = editAction;
    }

    /**
     * @param delAction the delAction to set
     */
    public void setDelAction(AbstractAction delAction) {
        this.delAction = delAction;
    }

    /**
     * @return the controller
     */
    public Controller getController() {
        return controller;
    }

    /**
     * @return the pageSelector
     */
    public JComboBox getPageSelector() {
        return pageSelector;
    }

    /**
     * @return the progressBar
     */
    public JProgressBar getProgressBar() {
        return progressBar;
    }

}
