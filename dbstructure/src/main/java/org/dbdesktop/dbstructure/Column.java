package org.dbdesktop.dbstructure;

public class Column {
    private int number;
    private String name = null;
    private AbstractSqlType type = null;
    private Integer length = null;
    private Integer precision = null;
    private boolean isNullable = true;
    private boolean isPrimary = true;

    public Column(int number, String name, AbstractSqlType type, Integer length, Integer precision, boolean isNullable, boolean isPrimary) {
        this.number = number;
        this.name = name;
        this.type = type;
        this.length = length;
        this.precision = precision;
        this.isNullable = isNullable;
        this.isPrimary = isPrimary;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public String getJavaName() {
        StringBuilder javaName = new StringBuilder();
        char[] arr = name.toCharArray();
        boolean nextBig = false;
        for (char c : arr) {
            if (c == '_') {
                nextBig = true;
            } else if (nextBig) {
                javaName.append(("" + c).toUpperCase());
                nextBig = false;
            } else {
                javaName.append(c);
            }
        }
        return javaName.toString();
    }

    public void setName(String name) {
        this.name = name;
    }

    public AbstractSqlType getType() {
        return type;
    }

    public void setType(AbstractSqlType type) {
        this.type = type;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public Integer getPrecision() {
        return precision;
    }

    public void setPrecision(Integer precision) {
        this.precision = precision;
    }

    public boolean isNullable() {
        return isNullable;
    }

    public void setNullable(boolean nullable) {
        isNullable = nullable;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }
}
