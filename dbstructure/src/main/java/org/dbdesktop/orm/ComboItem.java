package org.dbdesktop.orm;

public class ComboItem {
    private int id;
    private String value;

    public ComboItem(int id, String value) {
        this.id = id;
        this.value = value;
    }

    public String toString() {
        return getValue();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

