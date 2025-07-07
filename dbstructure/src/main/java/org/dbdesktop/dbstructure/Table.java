package org.dbdesktop.dbstructure;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;

public class Table {
    private String name;
    public static HashMap<String, Table> allTables = new HashMap<String, Table>();
    public Table(String name) {
        setName(name);
    }
    private List<Column> columns;
    private Column primaryColumn = null;
    private List<ForeignKey> foreignKeys;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public void setColumns(@NotNull List<Column> columns) {
        this.columns = columns;
        for(Column col: columns) {
            if(col.isPrimary()) {
                this.primaryColumn = col;
            }
        }
    }

    public Column getColumnByName(String colName) {
        for(Column col: columns) {
            if(col.getName().equals(colName)) {
                return col;
            }
        }
        return null;
    }

    public List<ForeignKey> getForeignKeys() {
        return foreignKeys;
    }

    public void setForeignKeys(List<ForeignKey> foreignKeys) {
        this.foreignKeys = foreignKeys;
    }

    public Column getPrimaryColumn() {
        return primaryColumn;
    }
}
