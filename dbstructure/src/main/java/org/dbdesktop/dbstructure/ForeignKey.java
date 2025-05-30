package org.dbdesktop.dbstructure;

public class ForeignKey {
    private String name;
    private Column fkColumn;
    private Table tableFrom;
    private Table tableTo;
    private final boolean deleteCascade = false;
    private final boolean deleteSetNull = false;

    public String toString() {
        return "foreign key on " + getTableFrom().getName() + "(" + getFkColumn().getName() + ") to " + getTableTo().getName();
    }

    public Table getTableFrom() {
        return tableFrom;
    }

    public Table getTableTo() {
        return tableTo;
    }

    public String getName() {
        return name;
    }

    public Column getFkColumn() {
        return fkColumn;
    }

    public boolean isDeleteCascade() {
        return deleteCascade;
    }

    public boolean isDeleteSetNull() {
        return deleteSetNull;
    }
}
