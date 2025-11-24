package org.dbdesktop.guiutil;

import org.dbdesktop.orm.ComboItem;
import org.dbdesktop.orm.DbObject;
import org.dbdesktop.orm.ExchangeFactory;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.text.ParseException;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class RecordEditPanel extends JPanel {

    public static final String DD_MM_YYYY = "dd/MM/yyyy";
    //public static final String MM_DD_YYYY = "MM/dd/yyyy";
    public static final String MMM_YYYY = "MMM-yyyy";
    protected static final String SELECT_HERE = "--select here--";
    private DbObject dbObject;
    protected JPanel lblPanel;
    protected JPanel editPanel;
    protected JPanel upPanel;
    protected PagesPanel pagesdPanel;
    protected JComponent[] edits;
    protected JLabel[] labels;
    protected final DbObject[] params;
    private EditRecordDialog ownerDialog;
    protected JTextField idField;

    protected static JLabel[] createLabelsArray(String[] titles) {
        JLabel[] lblArray = new JLabel[titles.length];
        int n = 0;
        for (String t : titles) {
            if (t.startsWith("images/")) {
                lblArray[n++] = new JLabel(new javax.swing.ImageIcon(t, ""), SwingConstants.RIGHT);
            } else {
                lblArray[n++] = new JLabel(t, SwingConstants.RIGHT);
            }
        }
        return lblArray;
    }
    protected boolean isViewOnly = false;

    protected void organizePanels(int labelLength, int editsLen) {
        setLayout(new BorderLayout());
        lblPanel = new JPanel(new GridLayout(labelLength, 1, 5, 5));
        editPanel = new JPanel(new GridLayout(editsLen, 1, 5, 5));
        upPanel = new JPanel(new BorderLayout());
        add(upPanel, BorderLayout.NORTH);
        upPanel.add(lblPanel, BorderLayout.WEST);
        upPanel.add(editPanel, BorderLayout.CENTER);
        upPanel.add(getRightUpperPanel(), BorderLayout.EAST);
    }

    protected void organizePanels(String[] titles, JComponent[] edits, HashSet<JComponent> except) {
        int diff = (except == null ? 0 : except.size());
        organizePanels(titles.length - diff, edits.length - diff);
        labels = createLabelsArray(titles);
        for (int i = 0; i < labels.length; i++) {
            if (except == null || !except.contains(edits[i])) {
                lblPanel.add(labels[i]);
            }
        }
        for (int i = 0; i < edits.length; i++) {
            if (except == null || !except.contains(edits[i])) {
                editPanel.add(edits[i]);
            }
        }
    }

    protected JPanel comboPanelWithLookupBtn(JComboBox cb, AbstractAction lookupButtonAction) {
        JPanel comboBoxPanel = new JPanel(new BorderLayout());
        comboBoxPanel.add(cb);
        if (lookupButtonAction != null) {
            comboBoxPanel.add(new JButton(lookupButtonAction), BorderLayout.EAST);
        }
        return comboBoxPanel;
    }

    protected JComponent getRightUpperPanel() {
        return new JPanel();
    }

    /**
     * @return the ownerDialog
     */
    public EditRecordDialog getOwnerDialog() {
        return ownerDialog;
    }

    /**
     * @param ownerDialog the ownerDialog to set
     */
    public void setOwnerDialog(EditRecordDialog ownerDialog) {
        this.ownerDialog = ownerDialog;
        ownerDialog.setGlobalFont((Container)this, 16);
    }

    protected void disable4view(JComponent[] comps) {
        for (JComponent comp : comps) {
            comp.setEnabled(!isViewOnly);
        }
    }

    protected static class EmptyValueException extends Exception {

        public EmptyValueException(String msg) {
            super(msg);
        }
    }

    public RecordEditPanel() {
        super(new BorderLayout());
        this.dbObject = null;
        this.params = null;
        fillContent();
    }

    public RecordEditPanel(DbObject dbObject) {
        super(new BorderLayout());
        this.dbObject = dbObject;
        this.params = null;
        fillContent();
        loadData();
    }

    public RecordEditPanel(DbObject[] params) {
        super(new BorderLayout());
        this.params = params;
        fillContent();
        loadData();
    }


    protected abstract void fillContent();

    public abstract void loadData();

    public abstract boolean save() throws Exception;

    public abstract void freeResources();

    /**
     * @return the dbObject
     */
    public DbObject getDbObject() {
        return dbObject;
    }

    /**
     * @param dbObject the dbObject to set
     */
    public void setDbObject(DbObject dbObject) {
        this.dbObject = dbObject;
        loadData();
    }

    protected String notEmpty(JTextComponent fld, String name) throws Exception {
        if (fld.getText().trim().length() == 0) {
            fld.requestFocus();
            throw new EmptyValueException("Enter value for " + name);
        }
        return fld.getText();
    }

    protected static void alignPanelOnWidth(JComponent one, JComponent two) {
        Dimension a = one.getPreferredSize();
        Dimension b = two.getPreferredSize();
        int width = a.width > b.width ? a.width : b.width;
        one.setPreferredSize(new Dimension(width, a.height));
        two.setPreferredSize(new Dimension(width, b.height));
    }

    //    protected static void addSiteItem(DefaultComboBoxModel cbm, Integer id) {
//        for (int i = 0; id != null && i < cbm.getSize(); i++) {
//            ComboItem itm = (ComboItem) cbm.getElementAt(i);
//            if (itm.getId() == id) {
//                return;
//            }
//        }
//        ComboItem newItm = XlendWorks.loadSite(id);
//        if (newItm != null) {
//            cbm.addElement(newItm);
//        }
//    }
    protected static void selectComboItem(JComboBox cb, Integer id) {
        for (int i = 0; id != null && i < cb.getItemCount(); i++) {
            ComboItem itm = (ComboItem) cb.getItemAt(i);
            if (itm.getId() == id) {
                cb.setSelectedIndex(i);
                return;
            }
        }
        if (id != null) {
            ComboItem ci = new ComboItem(id, "--unknown or inactive (" + id + ")--");
            DefaultComboBoxModel cmb = (DefaultComboBoxModel) cb.getModel();
            cmb.addElement(ci);
            cb.setSelectedItem(ci);
        }
    }

    protected static MaskFormatter createFormatter(String s) {
        MaskFormatter formatter = null;
        try {
            formatter = new MaskFormatter(s);
        } catch (ParseException ex) {
            ExchangeFactory.getPropLogEngine().log(ex);
        }
        return formatter;
    }

    public static JComponent getBorderPanel(JComponent[] comps) {
        JPanel ans = new JPanel(new BorderLayout());
        if (comps != null && comps.length > 0) {
            if (comps[0] != null) {
                ans.add(comps[0], BorderLayout.WEST);
            }
            if (comps.length > 1 && comps[1] != null) {
                ans.add(comps[1], BorderLayout.CENTER);
            }
            if (comps.length > 2 && comps[2] != null) {
                ans.add(comps[2], BorderLayout.EAST);
            }
        }
        return ans;
    }

    public static JComponent getGridPanel(JComponent comp, int ceils) {
        JPanel ans = new JPanel(new GridLayout(1, ceils));
        ans.add(comp);
        for (int i = 1; i < ceils; i++) {
            ans.add(new JPanel());
        }
        return ans;
    }

    public static JComponent getGridPanel(JComponent[] comps) {
        JPanel ans = new JPanel(new GridLayout(1, comps.length, 5, 1));
        for (int i = 0; i < comps.length; i++) {
            ans.add(comps[i]);
        }
        return ans;
    }

    public static JComponent getGridPanel(JComponent[] comps, int ceils) {
        ceils = (comps.length > ceils ? comps.length : ceils);
        JPanel ans = new JPanel(new GridLayout(1, ceils));
        for (int i = 0; i < ceils; i++) {
            if (i < comps.length) {
                ans.add(comps[i]);
            } else {
                ans.add(new JPanel());
            }
        }
        return ans;
    }

    protected Integer getSelectedCbItem(JComboBox cb) {
        ComboItem ci = (ComboItem) cb.getSelectedItem();
        return ci == null || ci.getId() == 0 ? null : ci.getId();
    }

    protected String getSelectedCbValue(JComboBox cb) {
        ComboItem ci = (ComboItem) cb.getSelectedItem();
        return ci == null || ci.getId() == 0 ? "" : ci.getValue();
    }

    protected boolean saveAndNotSet(DbObject dbOb, boolean isNew) {
        try {
            dbOb.setNew(isNew);
            LookupDialog.getExchanger().saveDbObject(dbOb);
            return true;
        } catch (Exception ex) {
            ExchangeFactory.getPropLogEngine().log(ex);
            GeneralUtils.errMessageBox(GeneralUtils.ERROR, ex.getMessage());
        }
        return false;
    }

    protected boolean saveDbRecord(DbObject dbOb, boolean isNew) {
        try {
            dbOb.setNew(isNew);
            setDbObject(LookupDialog.getExchanger().saveDbObject(dbOb));
            return true;
        } catch (Exception ex) {
            //Logger.getLogger(EditFilePanel.class.getName()).log(Level.SEVERE, null, ex);
            ExchangeFactory.getPropLogEngine().log(ex);
            GeneralUtils.errMessageBox(GeneralUtils.ERROR, ex.getMessage());
        }
        return false;
    }

    protected static java.sql.Date getSqlDateFromSpinner(SelectedDateSpinner ds) {
        java.util.Date dt = (java.util.Date) ds.getValue();
        if (dt != null) {
            return new java.sql.Date(dt.getTime());
        }
        return null;
    }
}
