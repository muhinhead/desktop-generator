package org.dbdesktop.dbstructure;

import java.util.HashMap;

public class MySqlType extends AbstractSqlType {

    protected void buildMap() {
        sql2java = new HashMap<String, String>();
        sql2java.put("int", "Integer");
        sql2java.put("INT", "Integer");
        sql2java.put("bigint", "Integer");
        sql2java.put("bit", "Integer");
        sql2java.put("smallint", "Integer");
        sql2java.put("tinyint", "Integer");
        sql2java.put("numeric", "Integer");
        sql2java.put("number", "Integer");
        sql2java.put("VARCHAR", "String");
        sql2java.put("varchar", "String");
        sql2java.put("varchar2", "String");
        sql2java.put("longvarchar", "String");
        sql2java.put("varchar_ignorecase", "String");
        sql2java.put("text", "String");
        sql2java.put("char", "String");
        sql2java.put("date", "Date");
        sql2java.put("time", "Timestamp");
        sql2java.put("datetime", "Timestamp");
        sql2java.put("timestamp", "Timestamp");
        sql2java.put("year", "Integer");
        sql2java.put("boolean", "Boolean");
        sql2java.put("decimal", "Double");
        sql2java.put("float", "Float");
        sql2java.put("double", "Double");
        sql2java.put("mediumblob", "Object");
        sql2java.put("blob", "Object");
        sql2java.put("other", "Object");
    }

    public MySqlType(String sqlType) {
        super(sqlType);
    }

}
