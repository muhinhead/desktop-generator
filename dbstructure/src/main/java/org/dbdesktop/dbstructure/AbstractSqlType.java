package org.dbdesktop.dbstructure;

import java.util.HashMap;

public abstract class AbstractSqlType {
    protected static HashMap<String, String> sql2java = null;
    private String sqlType;
    private String javaType;

    public AbstractSqlType(String sqlType) {
        if (sql2java == null) {
            buildMap();
        }
        this.setSqlType(sqlType);
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
        javaType = (String) sql2java.get(sqlType);
    }

    public String getJavaType() {
        return javaType;
    }

    public String toString() {
        return "sql:" + getSqlType() + " java:" + getJavaType();
    }
}
