package org.dbdesktop.guiutil;

import org.dbdesktop.orm.ExchangeFactory;
import org.dbdesktop.orm.IMessageSender;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.rmi.RemoteException;
import java.util.*;

public abstract class GeneralGridPanel extends DbTableGridPanel {

    private static class ColumnParams {
        static Hashtable<String, ColumnParams> all = new Hashtable<>();
        String columnName;
        int minWidth;
        int maxWidth;
        int preferredWidth;
        boolean isResizable;

        ColumnParams(TableColumn column) {
            this.columnName = column.getHeaderValue().toString();
            this.minWidth = column.getMinWidth();
            this.maxWidth = column.getMaxWidth();
            this.preferredWidth = column.getPreferredWidth();
            this.isResizable = column.getResizable();
            all.put(columnName, this);
        }

        static void restore(TableColumn column) {
            ColumnParams params = all.get(column.getHeaderValue().toString());
            if (params != null) {
                column.setMinWidth(params.minWidth);
                column.setMaxWidth(params.maxWidth);
                column.setPreferredWidth(params.preferredWidth);
                column.setResizable(params.isResizable);
            }
        }
    }

    public static final int PAGESIZE = 0;//5000;

    private String select;
    protected IMessageSender exchanger;
    public boolean isExternalView = false;

    public GeneralGridPanel(IMessageSender exchanger, String select,
                            HashMap<Integer, Integer> maxWidths, boolean readOnly, DbTableView tabView) throws RemoteException {
        super();
        this.select = select;
        this.exchanger = exchanger;
        isExternalView = (tabView != null);

        init(new AbstractAction[]{readOnly ? null : addAction(),
                        readOnly ? null : editAction(),
                        readOnly ? null : delAction()},
                select, exchanger.getTableBody(select, 0, GeneralGridPanel.PAGESIZE), maxWidths, tabView);
        setIsMultilineSelection(false);
        refreshTotalRows();
        addHeaderPopupMenu();
    }

    private void addHeaderPopupMenu() {

        JTableHeader header = getTableView().getTableHeader();
        JPopupMenu headerMenu = new JPopupMenu();
        TableColumnModel liveModel = getTableView().getColumnModel();
        LinkedHashMap<String, TableColumn> columnMap = new LinkedHashMap<>(liveModel.getColumnCount());

        for (int i = 0; i < liveModel.getColumnCount(); i++) {
            TableColumn col = liveModel.getColumn(i);
            String name = col.getHeaderValue().toString();
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(name, true);
            headerMenu.add(item);
            columnMap.put(name, col);
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    toggleColumnVisibility(item.getText(), item.isSelected());
                }

                private void toggleColumnVisibility(String colName, boolean selected) {
                    TableColumnModel liveModel = getTableView().getColumnModel();
                    for (int i = 0; i < liveModel.getColumnCount(); i++) {
                        TableColumn column = liveModel.getColumn(i);
                        if(column.getHeaderValue().toString().equals(colName)) {
                            if(selected) {
                                ColumnParams.restore(column);
                            } else {
                                new ColumnParams(column);
                                column.setMinWidth(0);
                                column.setMaxWidth(0);
                                column.setPreferredWidth(0);
                                column.setResizable(false);
                            }
                        }
                    }
                }
            });
        }

        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { maybeShowPopup(e); }
            @Override
            public void mouseReleased(MouseEvent e) { maybeShowPopup(e); }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    // keep checkboxes synced before showing
                    headerMenu.show(header, e.getX(), e.getY());
                }
            }
        });
    }


    public GeneralGridPanel(IMessageSender exchanger, String select,
                            HashMap<Integer, Integer> maxWidths, boolean readOnly) throws RemoteException {
        this(exchanger, select, maxWidths, readOnly, null);
    }

    protected abstract AbstractAction addAction();

    protected abstract AbstractAction editAction();

    protected abstract AbstractAction delAction();

    protected void init(AbstractAction[] acts, String select, Vector[] tableBody,
                        HashMap<Integer, Integer> maxWidths, DbTableView tabView) {
        super.init(acts, select, tableBody, maxWidths, tabView);
        if (getAddButton() != null) {
            getAddButton().setIcon(new ImageIcon(GeneralUtils.loadImage("plus.png", getClass())));
        }
        if (getEditButton() != null) {
            getEditButton().setIcon(new ImageIcon(GeneralUtils.loadImage("edit16.png", getClass())));
        }
        if (getDelButton() != null) {
            getDelButton().setIcon(new ImageIcon(GeneralUtils.loadImage("minus.png", getClass())));
        }
        getPageSelector().addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getProgressBar().setIndeterminate(false);
//                if (filterPanel != null) {
//                    filterPanel.setEnabled(false);
//                }
                Thread r = new Thread() {
                    public void run() {
                        getPageSelector().setEnabled(false);
                        int pageNum = getPageSelector().getSelectedIndex();
                        try {
                            getProgressBar().setIndeterminate(true);
                            getTableDoc().setBody(exchanger.getTableBody(getTableDoc().getSelectStatement(), pageNum, PAGESIZE));
                            getController().updateExcept(null);
                        } catch (RemoteException ex) {
                            ExchangeFactory.getPropLogEngine().log(ex);
                        } finally {
                            getProgressBar().setIndeterminate(false);
                            getPageSelector().setEnabled(true);
//                            if (filterPanel != null) {
//                                filterPanel.setEnabled(true);
//                            }
                        }
                    }
                };
                r.start();
            }
        });

        try {
            updatePageCounter(select);
        } catch (RemoteException ex) {
            ExchangeFactory.getPropLogEngine().log(ex);
        }
    }

    public void addExportMenuItems() {
        popMenu.add(new JSeparator());
        popMenu.add(new AbstractAction("Export as CSV") {
            @Override
            public void actionPerformed(ActionEvent e) {
                GeneralFrame.export2CSV(GeneralGridPanel.this);
            }
        });
        popMenu.add(new AbstractAction("Export as HTML") {
            @Override
            public void actionPerformed(ActionEvent e) {
                GeneralFrame.export2HTML(null, GeneralGridPanel.this);
            }
        });
    }

    public void updatePageCounter(String select) throws RemoteException {
        int qty = exchanger.getCount(select);
        int pagesCount = GeneralGridPanel.PAGESIZE == 0 ? 1 : qty / GeneralGridPanel.PAGESIZE + 1;
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        for (int i = 0; i < pagesCount; i++) {
            int maxrow = ((i + 1) * GeneralGridPanel.PAGESIZE + 1);
            maxrow = (maxrow > qty) ? qty : maxrow;
            model.addElement(Integer.valueOf(i + 1).toString() + " (" + (i * GeneralGridPanel.PAGESIZE + 1) + " - " + (maxrow - (i < pagesCount - 1 ? 1 : 0)) + ")");
        }
        getPageSelector().setModel(model);
        //getPageSelector().setEnabled(pagesCount>1);
        showPageSelector(pagesCount > 1);
        refreshTotalRows();
    }

    protected void refreshTotalRows() {
        countLabel.setText("Total: " + getTableView().getRowCount() + " rows");
    }

    public void refresh() {
        int id = getSelectedID();
        if (id > 0) {
            try {
                GeneralFrame.updateGrid(exchanger, getTableView(),
                        getTableDoc(), getSelect(), id, getPageSelector().getSelectedIndex());
                refreshTotalRows();
            } catch (RemoteException ex) {
                ExchangeFactory.getPropLogEngine().log(ex);
            }
        }
    }

    /**
     * @return the select
     */
    public String getSelect() {
        return select;
    }

    /**
     * @param select the select to set
     */
    public void setSelect(String select) {
        this.select = select;
    }

//    protected void enableActions() {
//        boolean enableDelete = (InvJediPrototype.getCurrentUser().getManager() == 1 || XlendWorks.getCurrentUser().getSupervisor() == 1);
//        if (getDelAction() != null) {
//            getDelAction().setEnabled(enableDelete);
//        }
//    }

    void highlightSearch(String text) {
        getTableView().setSearchString(text);
        refresh();
    }

    void setFilter(String text) {
        getTableDoc().setFilter(text);
        try {
            GeneralFrame.updateGrid(exchanger, getTableView(),
                    getTableDoc(), getSelect(), null, getPageSelector().getSelectedIndex());
        } catch (RemoteException ex) {
            ExchangeFactory.getPropLogEngine().log(ex);
        }
    }

    public void setIsMultilineSelection(boolean b) {
        getTableView().setIsMultilineSelection(b);
    }
}
