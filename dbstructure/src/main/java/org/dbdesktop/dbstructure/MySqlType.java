package org.dbdesktop.dbstructure;

import java.util.HashMap;

public class MySqlType extends AbstractSqlType {

    @Override
    protected HashMap<String, String> getTypesHashMap() {
        return new HashMap<String, String>() {
            @Override
            public String put(String key, String value) {
                return super.put(key.toLowerCase(), value);
            }

            @Override
            public String get(Object key) {
                return super.get(key.toString().toLowerCase());
            }

            @Override
            public boolean containsKey(Object key) {
                return super.containsKey(key.toString().toLowerCase());
            }

            @Override
            public String remove(Object key) {
                return super.remove(key.toString().toLowerCase());
            }
        };
    }

    @Override
    protected void buildMap() {
        sql2java = getTypesHashMap();
        sql2java.put("int", "Integer");
        sql2java.put("bigint", "Integer");
        sql2java.put("bit", "Integer");
        sql2java.put("smallint", "Integer");
        sql2java.put("tinyint", "Integer");
        sql2java.put("numeric", "Integer");
        sql2java.put("number", "Integer");
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
