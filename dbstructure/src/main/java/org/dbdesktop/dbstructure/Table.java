package org.dbdesktop.dbstructure;

import java.util.HashMap;

public class Table {
    private String name;
    public static HashMap<String, Table> allTables = new HashMap<String, Table>();
    private HashMap<String, Column> columns;
    private Column primaryColumn;
    private HashMap<String, ForeignKey> foreignKeys;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
