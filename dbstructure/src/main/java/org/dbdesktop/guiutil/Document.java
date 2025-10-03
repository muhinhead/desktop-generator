package org.dbdesktop.guiutil;

public abstract class Document {
    private String name;

    protected Document(String name) {
        this.name = name;
    }

    protected Document(String name, Object initialObject) {
        this.name = name;
        initialize(initialObject);
    }

    protected abstract void initialize(Object initObject);

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract Object getBody();

    public abstract void setBody(Object body);
}