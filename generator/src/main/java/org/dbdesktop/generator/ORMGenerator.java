package org.dbdesktop.generator;

import com.squareup.javapoet.*;
import org.dbdesktop.dbstructure.AbstractSqlType;
import org.dbdesktop.orm.DbObject;
import org.dbdesktop.orm.ForeignKeyViolationException;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
        for (String colNameType : getTablesFields(connection, tableName)) {
            int pointIndex = getPointIndex(colNameType);
            fields.add(FieldSpec.builder(getSqlType(colNameType).getJavaType(),
                            colNameType.substring(0, pointIndex))
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
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

                CodeBlock.Builder codeBlock = CodeBlock.builder().add("setColumnNames(new String[]{");
                CodeBlock.Builder codeBlock1 = CodeBlock.builder();
                StringBuilder columnsList = new StringBuilder();
                int n = 0;
                List<String> tableFields = getTablesFields(connection, table);
                for (String colNameType : tableFields) {
                    int pointIndex = getPointIndex(colNameType);
                    String colName = colNameType.substring(0, pointIndex);
                    codeBlock.add("$L\"$L\"", n > 0 ? ", " : "", colName);
                    codeBlock1.addStatement("this.$L = $L", colName, colName);
                    if (n > 0) {
                        columnsList.append(", ");
                    }
                    columnsList.append(colName);
                    n++;
                }
                codeBlock.addStatement("})");

                MethodSpec constructor = MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(Connection.class, "connection")
                        .addStatement("super(connection, \"$L\", \"$L\")", table, getPrimaryKeyColumnName(table))
                        .addCode(codeBlock.build())
                        .build();

                MethodSpec constructor2 = MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameters(getFieldsParameters(tableFields))
                        .addStatement("super(connection, \"$L\", \"$L\")", table, getPrimaryKeyColumnName(table))
                        .addStatement("setNew($L.intValue() <= 0)", getPrimaryKeyColumnName(table))
                        .addCode(codeBlock1.build())
                        .build();

                MethodSpec loadOnId = MethodSpec.methodBuilder("loadOnId")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(DbObject.class)
                        .addParameter(TypeName.INT, "id")
                        .addException(SQLException.class)
                        .addException(ForeignKeyViolationException.class)
                        .addStatement("$L $L = null", DbObject.capitalizedString(table), table)
                        .addStatement("$T ps = null", PreparedStatement.class)
                        .addStatement("$T rs = null", ResultSet.class)
                        .addStatement("$T stmt = \"SELECT $L FROM $L WHERE $L = \" + id", String.class,
                                columnsList.toString(), table, getPrimaryKeyColumnName(table))
                        .beginControlFlow("try")
                        .addStatement("ps = getConnection().prepareStatement(stmt)")
                        .addStatement("rs = ps.executeQuery()")
                        .beginControlFlow("if (rs.next())")
                        .addStatement("$L = new $L(getConnection())", table, DbObject.capitalizedString(table))
                        .addStatement("//TODO:")
                        .addStatement("//$L.setXbankbalanceId(new Integer(rs.getInt(1)))", table)
                        .addStatement("//$L.setBalancedate(rs.getTimestamp(2))", table)
                        .addStatement("//$L.setTotalvalue(rs.getDouble(3))", table)
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
                        .build();

                TypeSpec tableORM = TypeSpec.classBuilder(DbObject.capitalizedString(table))
                        .addModifiers(Modifier.PUBLIC)
                        .superclass(DbObject.class)
                        .addFields(genFields(table))
                        .addMethod(constructor)
                        .addMethod(constructor2)
                        .addMethod(loadOnId)
                        .build();

                JavaFile javaFile = JavaFile.builder("org.dbdesktop.orm", tableORM)
                        .build();

                javaFile.writeTo(Paths.get("./target/generated-sources"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static int getPointIndex(String colNameType) throws Exception {
        int pointIndex = colNameType.indexOf('.');
        if (pointIndex < 2) {
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
