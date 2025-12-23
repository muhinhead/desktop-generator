package org.dbdesktop.dbstructure;

import java.util.HashMap;

public abstract class AbstractSqlType {
    protected static HashMap<String, Class> sql2java = null;
    private String sqlType;
    private Class javaType;

    public AbstractSqlType(String sqlType) {
        if (sql2java == null) {
            buildMap();
        }
        this.setSqlType(sqlType);
    }

    protected HashMap<String, Class> getTypesHashMap() {
        return new HashMap<String, Class>();
    }

    public static String getODBCfuncGetName(String prefix, String javaType, String postFix, int n) {
        if (javaType.equals("Integer")) {
            if (prefix.endsWith("get"))
                return "new Integer(" + prefix + "Int(" + n + "))";
            else
                return prefix + "Int(" + n + (postFix == null ? "" : postFix) + ")";
        }
        return prefix + javaType + "(" + n + (postFix == null ? "" : postFix) + ")";
    }

    protected abstract void buildMap();

    public String getSqlType() {
        return sqlType;
    }

    public void setSqlType(String sqlType) {
        this.sqlType = sqlType;
        javaType = sql2java.get(sqlType);
//        System.out.println("TYPEMAP: "+toString());
    }

    public Class getJavaType() {
        return javaType;
    }

    public String toString() {
        return "sql:" + getSqlType() + " java:" + getJavaType();
    }
}
