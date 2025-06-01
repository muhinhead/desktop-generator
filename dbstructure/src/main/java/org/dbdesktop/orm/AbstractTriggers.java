package org.dbdesktop.orm;

import java.sql.SQLException;

public abstract class AbstractTriggers {
    public abstract void beforeInsert(DbObject dbObject) throws SQLException;
    public abstract void afterInsert(DbObject dbObject) throws SQLException;

    public abstract void beforeUpdate(DbObject dbObject) throws SQLException;
    public abstract void afterUpdate(DbObject dbObject) throws SQLException;

    public abstract void beforeDelete(DbObject dbObject) throws SQLException;
    public abstract void afterDelete(DbObject dbObject) throws SQLException;
}
