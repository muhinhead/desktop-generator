package org.dbdesktop.guiutil;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.text.ParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

public class DbTableView extends JTable implements ITableView {

    private TableRowSorter<MyTableModel> sorter;

    /**
     * @return the sorter
     */
    public TableRowSorter<MyTableModel> getSorter() {
        return sorter;
    }

    public class MyTableModel extends AbstractTableModel {

        private int colNumAdjusted(int col) {
            if (idRowNum >= 0 && col >= idRowNum) {
                return col + 1;
            } else {
                return col;
            }
        }

        public void fireTableCellUpdated(int row, int col) {
            super.fireTableCellUpdated(row, colNumAdjusted(col));
        }

        public String getColumnName(int col) {
            return (String) colName.get(colNumAdjusted(col));
        }

        public int getRowCount() {
            return rowData.size();
        }

        public int getColumnCount() {
            return idRowNum >= 0 ? (colName.size() - 1)
                    : colName.size();
            ///return colName.size();
        }

        public boolean isCellEditable(int row, int col) {
            return (getValueAt(row, col).getClass().equals(Boolean.class));
        }

        public Object getValueAt(int row, int col) {
            try {
                Vector line = (Vector) rowData.get(row);
                return line.get(colNumAdjusted(col)).toString();
            } catch (ArrayIndexOutOfBoundsException ae) {
                return null;
            }
        }

        public void setValueAt(Object val, int row, int col) {
            Vector line = (Vector) rowData.get(row);
            line.set(colNumAdjusted(col), val);
            fireTableCellUpdated(row, colNumAdjusted(col));
            synchronize();
        }

        public Class getColumnClass(int c) {
            try {
                return getValueAt(0, c).getClass();
            } catch (NullPointerException e) {
                return Object.class;
            }
        }
    }

    private Controller controller = null;
    private Vector colName = new Vector();
    private Vector rowData = new Vector();
    protected int selectedRow;
    private int idRowNum;
    private HashMap<Integer, Integer> maxColWidths = new HashMap<Integer, Integer>();
    private TableCellRenderer myCellRenderer = new MyColorRenderer(this);
    private String string2find;
    private boolean isMultilineSelection = false;

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
//        return column > 0 ? myCellRenderer : super.getCellRenderer(row, column);
        return myCellRenderer;
    }

    protected MyTableModel tableModel = new MyTableModel();

    public DbTableView() {
        super();
        ListSelectionModel rowSM = getSelectionModel();
        rowSM.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                ListSelectionModel lsm = (ListSelectionModel) e.getSource();
                if (lsm.isSelectionEmpty()) {
                    setSelectedRow(-1);
                } else {
                    setSelectedRow(lsm.getMinSelectionIndex());
                }
            }
        });
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sizeColumnsToFit(AUTO_RESIZE_OFF);
//        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
//        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
//        getColumnModel().getColumn(0).setCellRenderer(rightRenderer);
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public Controller getController() {
        return controller;
    }

    public void update(Document document) //initialize visible content by document
    {
//        toSync = false;
        Object[] body = (Object[]) document.getBody();
        colName = (Vector) body[0];
        rowData = (Vector) body[1];
        idRowNum = -1;
        for (int i = 0; i < colName.size(); i++) {
            if (colName.get(i).equals("_ID")) {
                idRowNum = i;
                break;
            }
        }
        if (getModel() != tableModel) {
            setModel(tableModel);
            this.sorter = new TableRowSorter<MyTableModel>(tableModel) {
                @Override
                public Comparator<?> getComparator(int column) {
                    return new Comparator<Object>() {
                        @Override
                        public int compare(Object o1, Object o2) {
                            if (o1 == null || o2 == null)
                                return 0;
                            try {
                                double n1 = Double.parseDouble(o1.toString());
                                double n2 = Double.parseDouble(o2.toString());
                                return Double.compare(n1, n2);
                            } catch (NumberFormatException e) {
                                return o1.toString().compareTo(o2.toString());
                            }
                        }
                    };
                }
            };
            setRowSorter(this.sorter);
//            RowFilter<MyTableModel, Object> rf = null;
//            try {
//                rf = RowFilter.regexFilter(filterText.getText(), 0);
//            } catch (java.util.regex.PatternSyntaxExceptione) {
//                return;
//            }
//            sorter.setRowFilter(rf);
        }
        this.invalidate();
        tableModel.fireTableStructureChanged();
        //getColumnModel().getColumn(0).setMaxWidth(50);
        if (null != maxColWidths) {
            for (Integer key : maxColWidths.keySet()) {
                try {
                    getColumnModel().getColumn(key).setMaxWidth(maxColWidths.get(key));
                } catch (ArrayIndexOutOfBoundsException ae) {
                }
            }
        }
    }

    public void synchronize() //update document through controller:
    {
        controller.getDocument().setBody(new Object[]{colName, rowData});
        controller.updateExcept(this);
    }

    public int getSelectedRow() {
        return selectedRow;
    }

    @Override
    public void setSelectedRow(int selectedRow) {
        this.selectedRow = selectedRow;
        if (!isMultilineSelection) {
            if (selectedRow >= 0 && selectedRow < getRowCount()) {
                setRowSelectionInterval(selectedRow, selectedRow);
            }
        }
        Rectangle rect = getCellRect(selectedRow, 0, true);
        scrollRectToVisible(rect);
    }

    public void gotoRow(int row) {
        if (row >= 0 && row < getRowCount()) {
            setSelectedRow(row);
//            Rectangle rect = getCellRect(row, 0, true);
//            scrollRectToVisible(rect);
        }
    }

    public void setMaxColWidth(int col, int width) {
        maxColWidths.put(col, width);
    }

    public void setMaxColWidths(HashMap<Integer, Integer> maxColWidths) {
        this.maxColWidths = maxColWidths;
    }

    public Vector getRowData() {
        return rowData;
    }

    public void gotoFirstMatch(String find) {
        Vector data = getRowData();
        for (int row = 0; row < getRowCount(); row++) {
            Vector line = (Vector) rowData.get(row);
            for (int col = 0; col < line.size(); col++) {
                String ceil = (String) line.get(col);
                if (ceil.toUpperCase().indexOf(find.toUpperCase()) >= 0) {
                    gotoRow(row);
                    return;
                }
            }
        }
    }

    @Override
    public void setSearchString(String find) {
        this.string2find = find;
        gotoFirstMatch(find);
    }

    @Override
    public String getSearchString() {
        return string2find;
    }

    /**
     * @return the isMultilineSelection
     */
    public boolean isIsMultilineSelection() {
        return isMultilineSelection;
    }

    /**
     * @param isMultilineSelection the isMultilineSelection to set
     */
    public void setIsMultilineSelection(boolean isMultilineSelection) {
        if (isMultilineSelection) {
            setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        } else {
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }
        this.isMultilineSelection = isMultilineSelection;
    }
}