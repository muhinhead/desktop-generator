package org.dbdesktop.dbstructure;

public class ForeignKey {
    private final String name;
    private final Column fkColumn;
    private final Table tableFrom;
    private final Table tableTo;
    private final boolean deleteCascade;
    private final boolean deleteSetNull;

    public ForeignKey(String name, Column fkColumn, Table tableFrom, Table tableTo, boolean deleteCascade, boolean deleteSetNull) {
        this.name = name;
        this.fkColumn = fkColumn;
        this.tableFrom = tableFrom;
        this.tableTo = tableTo;
        this.deleteCascade = deleteCascade;
        this.deleteSetNull = deleteSetNull;
    }

//    public ForeignKey(String name, Column fkColumn, Table tableFrom, Table tableTo, ) {
//        this.name = name;
//        this.fkColumn = fkColumn;
//        this.tableFrom = tableFrom;
//        this.tableTo = tableTo;
//    }

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
