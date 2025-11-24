package org.dbdesktop.guiutil;

import org.dbdesktop.orm.ForeignKeyViolationException;

import java.sql.SQLException;

public interface IPage {
    Object getPagescan();
    void setPagescan(Object pagescan) throws SQLException, ForeignKeyViolationException;
    String getFileextension();
    void setFileextension(String fileextension) throws SQLException, ForeignKeyViolationException;
}