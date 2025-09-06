package org.dbdesktop.dbstructure;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.HashMap;

public class MySqlType extends AbstractSqlType {

    @Override
    protected HashMap<String, Class> getTypesHashMap() {
        return new HashMap<String, Class>() {
            @Override
            public Class put(String key, Class value) {
                return super.put(key.toLowerCase(), value);
            }

            @Override
            public Class get(Object key) {
                return super.get(key.toString().toLowerCase());
            }

            @Override
            public boolean containsKey(Object key) {
                return super.containsKey(key.toString().toLowerCase());
            }

            @Override
            public Class remove(Object key) {
                return super.remove(key.toString().toLowerCase());
            }
        };
    }

    @Override
    protected void buildMap() {
        sql2java = getTypesHashMap();
        sql2java.put("int", Integer.class);
        sql2java.put("bigint", Integer.class);
        sql2java.put("bit", Integer.class);
        sql2java.put("smallint", Integer.class);
        sql2java.put("tinyint", Integer.class);
        sql2java.put("numeric", Integer.class);
        sql2java.put("number", Integer.class);
        sql2java.put("varchar", String.class);
        sql2java.put("varchar2", String.class);
        sql2java.put("longvarchar", String.class);
        sql2java.put("mediumtext", String.class);
        sql2java.put("text", String.class);
        sql2java.put("varchar_ignorecase", String.class);
        sql2java.put("text", String.class);
        sql2java.put("char", String.class);
        sql2java.put("date", Date.class);
        sql2java.put("time", Timestamp.class);
        sql2java.put("datetime", Timestamp.class);
        sql2java.put("timestamp", Timestamp.class);
        sql2java.put("year", Integer.class);
        sql2java.put("boolean", Boolean.class);
        sql2java.put("decimal", Double.class);
        sql2java.put("float", Float.class);
        sql2java.put("double", Double.class);
        sql2java.put("mediumblob", Object.class);
        sql2java.put("blob", Object.class);
        sql2java.put("longblob", Object.class);
        sql2java.put("other", Object.class);
    }

    public MySqlType(String sqlType) {
        super(sqlType);
    }
}
