package org.dbdesktop.guiutil;

import org.dbdesktop.orm.DbObject;
import org.dbdesktop.orm.ExchangeFactory;

public class GenericEditDialog<T extends RecordEditPanel> extends EditRecordDialog {

    private boolean okPressed = false;
    private Class<T> panelType;
    private T panel;

    public GenericEditDialog(Class<T> type, String title, Object obj) {
        super();
        setTitle(title);
        setObject(obj);
        panelType = type;
        init();
    }

    public GenericEditDialog(Class<T> type, String title) {
        this(type, title, null);
    }

    @Override
    protected void fillContent() {
        try {
            panel = panelType.newInstance();
            panel.setDbObject((DbObject) getObject());
            panel.setOwnerDialog(this);
            super.fillContent(panel);
        } catch (InstantiationException | IllegalAccessException ex) {
            ExchangeFactory.getPropLogEngine().logAndShowMessage(ex);
        }
    }

    @Override
    protected void setOkPressed(boolean b) {
        okPressed = b;
    }

    /**
     * @return the okPressed
     */
    public boolean isOkPressed() {
        return okPressed;
    }
}
