package org.dbdesktop.generator;

import com.squareup.javapoet.*;
import org.dbdesktop.dbstructure.AbstractSqlType;
import org.dbdesktop.dbstructure.MySqlType;
import org.dbdesktop.orm.AbstractTriggers;
import org.dbdesktop.orm.DbObject;
import org.dbdesktop.orm.ForeignKeyViolationException;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ORMGenerator {
    private static HashMap<String, String> tablesPksMap = new HashMap<>();
    private final Class<? extends AbstractSqlType> sqlTypeClass;
    public Connection connection;

    public ORMGenerator(Connection connection, Class<? extends AbstractSqlType> sqlTypeClass) {
        this.sqlTypeClass = sqlTypeClass;
        this.connection = connection;
    }

    private List<FieldSpec> genFields(String tableName) throws Exception {
        List<FieldSpec> fields = new ArrayList<>();
        fields.add(FieldSpec.builder(AbstractTriggers.class, "activeTriggers").addModifiers(Modifier.PROTECTED, Modifier.STATIC).build());
        for (String colNameType : getTablesFields(connection, tableName)) {
            int pointIndex = getPointIndex(colNameType);
            fields.add(FieldSpec.builder(getSqlType(colNameType).getJavaType(),
                            colNameType.substring(0, pointIndex))
                    .addModifiers(Modifier.PRIVATE)
                    .build());
        }
        return fields;
    }

    private AbstractSqlType getSqlType(String colNameType) throws Exception {
        int pointIndex = getPointIndex(colNameType);
        String sqlType = colNameType.substring(pointIndex + 1);
        Constructor<? extends AbstractSqlType> constructor = sqlTypeClass.getConstructor(String.class);
        return constructor.newInstance(sqlType);
    }

    private List<String> getTablesFields(Connection connection, String tableName) throws SQLException {
        List<String> columnNames = new ArrayList<>();
        String sql = "SELECT concat(column_name,'.',data_type) col FROM information_schema.columns WHERE table_name = ? ORDER BY ORDINAL_POSITION";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    columnNames.add(rs.getString("col"));
                }
            }
        }
        return columnNames;
    }

    public void generateORMclasses(File outFolder) throws Exception {
        try {
            for (String table : getTables(connection, "vinyl")) {
                System.out.println("Processing table: " + table);

                String pkColName = getPrimaryKeyColumnName(table);
                CodeBlock.Builder lastIdCodeBlock = CodeBlock.builder();
                if (this.sqlTypeClass.equals(MySqlType.class)) {
                    lastIdCodeBlock.addStatement("stmt = \"SELECT last_insert_id()\"");
                } else {
                    lastIdCodeBlock.addStatement("stmt = \"SELECT max($L) FROM $L\"", pkColName, table);
                }
                CodeBlock.Builder codeBlock = CodeBlock.builder().add("setColumnNames(new String[]{");
                CodeBlock.Builder codeBlock1 = CodeBlock.builder();
                CodeBlock.Builder codeBlock2 = CodeBlock.builder();
                CodeBlock.Builder codeBlock3 = CodeBlock.builder();
                CodeBlock.Builder codeBlock4 = CodeBlock.builder();
                CodeBlock.Builder codeBlock5 = CodeBlock.builder();
                StringBuilder columnsList = new StringBuilder();
                StringBuilder insertSQL = new StringBuilder("\"INSERT INTO " + table +
                        " (\" + (get" + DbObject.capitalizedString(pkColName) + "().intValue()!=0?\"" + pkColName + ", \":\"\")+");
                StringBuilder insertValues = new StringBuilder("values (\" + (get" + DbObject.capitalizedString(pkColName) + "().intValue()!=0?\"?, \":\"\")+");
                int n = 0;
                List<String> tableFields = getTablesFields(connection, table);
                List<MethodSpec> setters = new ArrayList<>(tableFields.size());
                List<MethodSpec> getters = new ArrayList<>(tableFields.size());
                for (String colNameType : tableFields) {
                    int pointIndex = getPointIndex(colNameType);
                    String colName = colNameType.substring(0, pointIndex);
                    codeBlock.add("$L\"$L\"", n > 0 ? ", " : "", colName);
                    codeBlock1.addStatement("this.$L = $L", colName, colName);
                    codeBlock2.addStatement("$L.set$L(rs.get$L(" + (n + 1) + "))", table, DbObject.capitalizedString(colName),
                            getSqlType(colNameType).getJavaType().getSimpleName()
                                    .replace("Integer", "Int")
                    );
                    if (n > 0) {
                        columnsList.append(", ");
                        insertSQL.append(n > 1 ? "," : "\"").append(colName);
                        insertValues.append(n > 1 ? "," : "\"").append("?");
                        codeBlock3.addStatement("ps.setObject(++n, get$L())", DbObject.capitalizedString(colName));
                        codeBlock4.addStatement("ps.setObject($L, get$L())", "" + n, DbObject.capitalizedString(colName));
                    }
                    columnsList.append(colName);
                    MethodSpec.Builder setter = MethodSpec.methodBuilder("set" + DbObject.capitalizedString(colName))
                            .addModifiers(Modifier.PUBLIC)
                            .addException(ForeignKeyViolationException.class)
                            .addParameter(getSqlType(colNameType).getJavaType(), colName)
                            .addStatement("setWasChanged(this.$L != null && !this.$L.equals($L))", colName, colName, colName)
                            .addStatement("this.$L = $L", colName, colName);
                    if (colName.equals(pkColName)) {
                        setter.addStatement("setNew($L.intValue() == 0)", pkColName);
                    }
                    codeBlock5.addStatement("columnValues[$L] = get$L()", n, DbObject.capitalizedString(colName));
                    setters.add(setter.build());
                    getters.add(
                            MethodSpec.methodBuilder("get" + DbObject.capitalizedString(colName))
                                    .addModifiers(Modifier.PUBLIC)
                                    .returns(getSqlType(colNameType).getJavaType())
                                    .addStatement("return this.$L", colName)
                                    .build()
                    );
                    n++;
                }
                insertSQL.append(") ").append(insertValues.toString()).append(")\"");
                codeBlock.addStatement("})");
                codeBlock3.addStatement("ps.execute()");

                MethodSpec constructor = MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(Connection.class, "connection")
                        .addStatement("super(connection, \"$L\", \"$L\")", table, pkColName)
                        .addCode(codeBlock.build())
                        .build();

                MethodSpec constructor2 = MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(Connection.class, "connection")
                        .addParameters(getFieldsParameters(tableFields))
                        .addStatement("super(connection, \"$L\", \"$L\")", table, pkColName)
                        .addStatement("setNew($L.intValue() <= 0)", pkColName)
                        .addCode(codeBlock1.build())
                        .build();

                MethodSpec loadOnId = MethodSpec.methodBuilder("loadOnId")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .returns(DbObject.class)
                        .addParameter(TypeName.INT, "id")
                        .addException(SQLException.class)
                        .addException(ForeignKeyViolationException.class)
                        .addStatement("$L $L = null", DbObject.capitalizedString(table), table)
                        .addStatement("$T ps = null", PreparedStatement.class)
                        .addStatement("$T rs = null", ResultSet.class)
                        .addStatement("$T stmt = \"SELECT $L FROM $L WHERE $L = \" + id", String.class,
                                columnsList.toString(), table, pkColName)
                        .beginControlFlow("try")
                        .addStatement("ps = getConnection().prepareStatement(stmt)")
                        .addStatement("rs = ps.executeQuery()")
                        .beginControlFlow("if (rs.next())")
                        .addStatement("$L = new $L(getConnection())", table, DbObject.capitalizedString(table))
                        .addCode(codeBlock2.build())
                        .addStatement("$L.setNew(false)", table)
                        .endControlFlow()
                        .nextControlFlow("finally")
                        .beginControlFlow("try")
                        .beginControlFlow("if (rs != null)")
                        .addStatement("rs.close()")
                        .endControlFlow()
                        .nextControlFlow("finally")
                        .beginControlFlow("if (ps != null)")
                        .addStatement("ps.close()")
                        .endControlFlow()
                        .endControlFlow()
                        .endControlFlow()
                        .addStatement("return $L", table)
                        .build();

                MethodSpec insert = MethodSpec.methodBuilder("insert")
                        .addModifiers(Modifier.PROTECTED)
                        .addAnnotation(Override.class)
                        .addException(SQLException.class)
                        .addException(ForeignKeyViolationException.class)
                        .beginControlFlow("if(getTriggers() != null)")
                        .addStatement("getTriggers().beforeInsert(this)")
                        .endControlFlow()
                        .addStatement("PreparedStatement ps = null")
                        .addStatement("String stmt = $L", insertSQL.toString())
                        .beginControlFlow("try")
                        .addStatement("ps = getConnection().prepareStatement(stmt)")
                        .addStatement("int n = 0")
                        .beginControlFlow("if(get$L().intValue() != 0)", DbObject.capitalizedString(pkColName))
                        .addStatement("ps.setObject(++n, get$L())", DbObject.capitalizedString(pkColName))
                        .endControlFlow()
                        .addCode(codeBlock3.build())
                        .nextControlFlow("finally")
                        .addStatement("if (ps != null) ps.close()")
                        .endControlFlow()
                        .addStatement("ResultSet rs = null")
                        .beginControlFlow("if(get$L().intValue() == 0)", DbObject.capitalizedString(pkColName))
                        .addCode(lastIdCodeBlock.build())
                        .beginControlFlow("try")
                        .addStatement("ps = getConnection().prepareStatement(stmt)")
                        .addStatement("rs = ps.executeQuery()")
                        .beginControlFlow("if (rs.next())")
                        .addStatement("setId(new Integer(rs.getInt(1)))")
                        .endControlFlow()
                        .nextControlFlow("finally")
                        .beginControlFlow("try")
                        .addStatement("if (rs != null) rs.close()")
                        .nextControlFlow("finally")
                        .addStatement("if (ps != null) ps.close()")
                        .endControlFlow()
                        .endControlFlow()
                        .endControlFlow()
                        .addStatement("setNew(false)")
                        .addStatement("setWasChanged(false)")
                        .beginControlFlow("if (getTriggers() != null)")
                        .addStatement("getTriggers().afterInsert(this);")
                        .endControlFlow()
                        .build();

                MethodSpec save = MethodSpec.methodBuilder("save")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addException(SQLException.class)
                        .addException(ForeignKeyViolationException.class)
                        .beginControlFlow("if(isNew())")
                        .addStatement("insert()")
                        .nextControlFlow("else")
                        .beginControlFlow("if (getTriggers() != null)")
                        .addStatement("getTriggers().beforeUpdate(this)")
                        .endControlFlow()
                        .addStatement("PreparedStatement ps = null")
                        .addStatement("String stmt = \"UPDATE $L SET $L WHERE $L = \" + get$L()",
                                table,
                                tableFields.stream().map(s -> s.substring(0, s.indexOf('.'))).filter(s -> !s.equals(pkColName))
                                        .collect(Collectors.joining(" = ?, ", "", " = ?")),
                                pkColName, DbObject.capitalizedString(pkColName))
                        .beginControlFlow("try")
                        .addStatement("ps = getConnection().prepareStatement(stmt)")
                        .addCode(codeBlock4.build())
                        .addStatement("ps.execute()")
                        .nextControlFlow("finally")
                        .addStatement("if (ps != null) ps.close()")
                        .endControlFlow()
                        .addStatement("setWasChanged(false)")
                        .beginControlFlow("if (getTriggers() != null)")
                        .addStatement("getTriggers().afterUpdate(this)")
                        .endControlFlow()
                        .endControlFlow()
                        .build();

                MethodSpec delete = MethodSpec.methodBuilder("delete")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addException(SQLException.class)
                        .addException(ForeignKeyViolationException.class)
                        .beginControlFlow("if (getTriggers() != null)")
                        .addStatement("getTriggers().beforeDelete(this)")
                        .endControlFlow()
                        .addStatement("PreparedStatement ps = null")
                        .addStatement("String stmt = \"DELETE FROM $L WHERE $L = \" + get$L()",
                                table, pkColName, DbObject.capitalizedString(pkColName))
                        .beginControlFlow("try")
                        .addStatement("ps = getConnection().prepareStatement(stmt)")
                        .addStatement("ps.execute()")
                        .nextControlFlow("finally")
                        .addStatement("if (ps != null) ps.close()")
                        .endControlFlow()
                        .addStatement("set$L(new Integer(-get$L().intValue()))", DbObject.capitalizedString(pkColName), DbObject.capitalizedString(pkColName))
                        .beginControlFlow("if (getTriggers() != null)")
                        .addStatement("getTriggers().afterDelete(this)")
                        .endControlFlow()
                        .build();

                MethodSpec isDeleted = MethodSpec.methodBuilder("isDeleted")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .returns(TypeName.BOOLEAN)
                        .addStatement("return (get$L().intValue() < 0)", DbObject.capitalizedString(pkColName))
                        .build();

                MethodSpec getPK_ID = MethodSpec.methodBuilder("getPK_ID")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .returns(Integer.class)
                        .addStatement("return this.$L", pkColName)
                        .build();

                MethodSpec setPK_ID = MethodSpec.methodBuilder("setPK_ID")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addException(ForeignKeyViolationException.class)
                        .addParameter(Integer.class, pkColName)
                        .addStatement("boolean prevIsNew = isNew()")
                        .addStatement("set$L($L)", DbObject.capitalizedString(pkColName), pkColName)
                        .addStatement("setNew(prevIsNew)")
                        .build();

                MethodSpec getTriggers = MethodSpec.methodBuilder("getTriggers")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(AbstractTriggers.class)
                        .addStatement("return activeTriggers")
                        .build();

                MethodSpec getAsRow = MethodSpec.methodBuilder("getAsRow")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .returns(ArrayTypeName.of(ClassName.get(Object.class)))
                        .addStatement("Object[] columnValues = new Object[$L]", tableFields.size())
                        .addCode(codeBlock5.build())
                        .addStatement("return columnValues")
                        .build();

                MethodSpec fillFromString = MethodSpec.methodBuilder("fillFromString")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addException(SQLException.class)
                        .addException(ForeignKeyViolationException.class)
                        .addParameter(String.class, "row")
                        .addStatement("String[] flds = splitStr(row, delimiter)")
                        .addCode(getFillFromStringFieldsCode(tableFields))
                        .build();

                TypeSpec tableORM = TypeSpec.classBuilder(DbObject.capitalizedString(table))
                        .addModifiers(Modifier.PUBLIC)
                        .superclass(DbObject.class)
                        .addFields(genFields(table))
                        .addMethod(constructor)
                        .addMethod(constructor2)
                        .addMethod(getTriggers)
                        .addMethod(loadOnId)
                        .addMethod(insert)
                        .addMethod(save)
                        .addMethod(delete)
                        .addMethod(isDeleted)
                        .addMethod(getPK_ID)
                        .addMethod(setPK_ID)
                        .addMethods(setters)
                        .addMethods(getters)
                        .addMethod(getAsRow)
                        .addMethod(fillFromString)
                        .build();

                JavaFile javaFile = JavaFile.builder("org.dbdesktop.orm", tableORM)
                        .build();

                javaFile.writeTo(Paths.get("./target/generated-sources"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private CodeBlock getFillFromStringFieldsCode(List<String> tableFields) throws Exception {
        CodeBlock.Builder cb = CodeBlock.builder();
        int n = 0;
        for (String colNameType : tableFields) {
            int pointIndex = getPointIndex(colNameType);
            String colName = colNameType.substring(0, pointIndex);
            Class colJavaType = getSqlType(colNameType).getJavaType();
            if (colJavaType.equals(String.class)) {
                cb.addStatement("set$L(flds[$L])", DbObject.capitalizedString(colName), n);
            } else if (colJavaType.equals(Timestamp.class)) {
                cb.addStatement("set$L(toTimeStamp(flds[$L]))", DbObject.capitalizedString(colName), n);
            } else if (colJavaType.equals(Date.class)) {
                cb.addStatement("set$L(toDate(flds[$L]))", DbObject.capitalizedString(colName), n);
            } else if (colJavaType.equals(Integer.class)) {
                cb.beginControlFlow("try");
                cb.addStatement("set$L(Integer.parseInt(flds[$L]))", DbObject.capitalizedString(colName), n);
                cb.nextControlFlow("catch(NumberFormatException ne)");
                cb.addStatement("set$L(null)", DbObject.capitalizedString(colName));
                cb.endControlFlow();
            } else if (colJavaType.equals(Double.class)) {
                cb.beginControlFlow("try");
                cb.addStatement("set$L(Double.parseDouble(flds[$L]))", DbObject.capitalizedString(colName), n);
                cb.nextControlFlow("catch(NumberFormatException ne)");
                cb.addStatement("set$L(null)", DbObject.capitalizedString(colName));
                cb.endControlFlow();
            } else if (colJavaType.equals(Float.class)) {
                cb.beginControlFlow("try");
                cb.addStatement("set$L(Float.parseFloat(flds[$L]))", DbObject.capitalizedString(colName), n);
                cb.nextControlFlow("catch(NumberFormatException ne)");
                cb.addStatement("set$L(null)", DbObject.capitalizedString(colName));
                cb.endControlFlow();
            }
            n++;
        }
        return cb.build();
    }

    private static int getPointIndex(String colNameType) throws Exception {
        int pointIndex = colNameType.indexOf('.');
        if (pointIndex < 1) {
            throw new Exception("Column definition [" + colNameType
                    + "] does not contain a point delimiter. Check your getTablesFields() method!");
        }
        return pointIndex;
    }

    private Iterable<ParameterSpec> getFieldsParameters(List<String> tableFields) throws Exception {
        List<ParameterSpec> params = new ArrayList<>(tableFields.size());
        for (String colNameType : tableFields) {
            int pointIndex = getPointIndex(colNameType);
            params.add(ParameterSpec.builder(getSqlType(colNameType).getJavaType(),
                    colNameType.substring(0, pointIndex)).build());
        }
        return params;
    }

    private String getPrimaryKeyColumnName(String table) throws SQLException {
        String pkColumnName = tablesPksMap.get(table);
        if (pkColumnName == null) {
            String sql = "SELECT column_name FROM information_schema.KEY_COLUMN_USAGE WHERE table_name = ? " +
                    "AND CONSTRAINT_NAME = 'PRIMARY'";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, table);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        pkColumnName = rs.getString("column_name");
                    }
                }
            }
            tablesPksMap.put(table, pkColumnName);
        }
        return pkColumnName;
    }

    public static List<String> getTables(Connection connection, String dbName) throws SQLException {
        List<String> tableNames = new ArrayList<>();
        String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, dbName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tableNames.add(rs.getString("table_name"));
                }
            }
        }
        return tableNames;
    }
}
