package org.dbdesktop.guiutil;

import org.dbdesktop.orm.IMessageSender;

import javax.swing.*;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.HashMap;

public abstract class AbstractGridAdapter extends GeneralGridPanel {

    public static SimpleDateFormat americanMMDDYYYYDateFormat = new SimpleDateFormat("MM/dd/yyyy");
    public static final String MM_DD_YYYY = "%m/%d/%Y";
    public static final String MM_DD_YYYY_HH_MI_SS = "%m/%d/%Y %h:%i:%S";
    public static final String DD_MM_YYYY_HH_MI_SS = "%d/%m/%Y %h:%i:%S";

    public AbstractGridAdapter(IMessageSender exchanger, String select, HashMap<Integer, Integer> maxWidths, boolean readOnly) throws RemoteException {
        super(exchanger, select, maxWidths, readOnly);
    }

    @Override
    protected AbstractAction addAction() {
        return null;
    }

    @Override
    protected AbstractAction editAction() {
        return null;
    }

    @Override
    protected AbstractAction delAction() {
        return null;
    }
}
