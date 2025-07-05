package org.dbdesktop.dbstructure;

import java.util.HashMap;
import java.util.List;

public class Table {
    private String name;
    public static HashMap<String, Table> allTables = new HashMap<String, Table>();
    public Table(String name) {
        setName(name);
    }
    private List<Column> columns;
    private Column primaryColumn;
    private HashMap<String, ForeignKey> foreignKeys;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }
}
