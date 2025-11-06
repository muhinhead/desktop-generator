package org.dbdesktop.dbstructure;

public class NamedEssence {
    private final String name;
    private final String comment;

    public NamedEssence(String name, String comment) {
        this.name = name;
        this.comment = comment;
    }

    public String getName() {
        return name;
    }

    public String getHeader() {
        return getComment() == null || getComment().isEmpty() ? getName() : getComment();
    }

    public String getComment() {
        return comment;
    }
}
