package org.dbdesktop.generator;

import com.squareup.javapoet.*;
import org.dbdesktop.orm.DbObject;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.sql.*;
import java.util.List;
import java.util.Vector;

public class SQLutilGenerator implements IClassesGenerator {

    private static final String NOTSUPPORTED_MSG = "throw new $T(\"Not supported yet.\")";
    private static final String SERVER_VERSION = "0.1";
//    private final String host;
//    private final Integer port;
//    private final String database;
    private final String packageName;

    public SQLutilGenerator(String packageName) {
//        this.host = host;
//        this.port = port;
//        this.database = database;
        this.packageName = packageName;
    }

    @Override
    public void generateClasses(String outFolder) throws Exception {

        generateIMessageSender(outFolder);
        generateDbClientSender(outFolder);
    }

    private void generateIMessageSender(String outFolder) throws IOException {
        TypeSpec iMessageSender = TypeSpec.interfaceBuilder("IMessageSender")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(java.rmi.Remote.class)
                .addMethods(iMessageSenderMethods())
                .build();
        JavaFile javaFile = JavaFile.builder(this.packageName, iMessageSender)
                .build();
        javaFile.writeTo(Paths.get(outFolder));
    }

    private void generateDbClientSender(String outFolder) throws IOException {
        ClassName connection = ClassName.get(Connection.class);
        ClassName remoteEx = ClassName.get(RemoteException.class);
//        ClassName preparedStatement = ClassName.get(PreparedStatement.class);
        ClassName dbObject = ClassName.get(DbObject.class);
        ClassName method = ClassName.get(java.lang.reflect.Method.class);
        ClassName constructor = ClassName.get(Constructor.class);
        FieldSpec connectionField = FieldSpec.builder(connection, "connection", Modifier.PRIVATE).build();
        MethodSpec ctor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(connection, "connection")
                .addStatement("this.connection = connection")
                .build();

        MethodSpec getServerVersion = MethodSpec.methodBuilder("getServerVersion")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addException(remoteEx)
                .addStatement("return $S", SERVER_VERSION)
                .build();

        MethodSpec truncateTable = MethodSpec.methodBuilder("truncateTable")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addParameter(String.class, "tableName")
                .addException(remoteEx)
                .addStatement("boolean ok = false")
                .addStatement("$T ps = null", PreparedStatement.class)
                .beginControlFlow("try")
                .addStatement("ps = connection.prepareStatement($S + tableName)", "truncate ")
                .addStatement("ok = ps.execute()")
                .nextControlFlow("catch ($T ex)", ClassName.get(SQLException.class))
                .addStatement("throw new $T(ex.getMessage())", remoteEx)
                .nextControlFlow("finally")
                .beginControlFlow("try")
                .addStatement("ps.close()")
                .nextControlFlow("catch (Exception e)")
                .endControlFlow()
                .endControlFlow()
                .addStatement("return ok")
                .build();

        MethodSpec getDbObjects = MethodSpec.methodBuilder("getDbObjects")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ArrayTypeName.of(dbObject))
                .addException(remoteEx)
                .addParameter(Class.class, "dbobClass")
                .addParameter(String.class, "whereCondition")
                .addParameter(String.class, "orderCondition")
                .addStatement("$T[] rows = null", dbObject)
                .beginControlFlow("try")
                .addStatement("$T method = dbobClass.getDeclaredMethod($S, $T.class, $T.class, $T.class)",
                        method, "load", connection, String.class, String.class)
                .addStatement("rows = ($T[]) method.invoke(null, connection, whereCondition, orderCondition)", dbObject)
                .nextControlFlow("catch (Exception ex)")
                .addStatement("throw new $T(ex.getMessage())", remoteEx)
                .endControlFlow()
                .addStatement("return rows")
                .build();

        MethodSpec saveDbObject = MethodSpec.methodBuilder("saveDbObject")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(dbObject)
                .addException(remoteEx)
                .addParameter(dbObject, "dbob")
                .beginControlFlow("if (dbob != null)")
                .beginControlFlow("try")
                .addStatement("boolean wasNew = dbob.isNew()")
                .addStatement("dbob.setConnection(connection)")
                .addStatement("dbob.save()")
                .nextControlFlow("catch (Exception ex)")
                .addStatement("throw new $T($S, ex)", remoteEx, "Can't save DB object:")
                .endControlFlow()
                .endControlFlow()
                .addStatement("return dbob")
                .build();

        // deleteObject method
        MethodSpec deleteObject = MethodSpec.methodBuilder("deleteObject")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addException(remoteEx)
                .addParameter(dbObject, "dbob")
                .beginControlFlow("if (dbob != null)")
                .beginControlFlow("try")
                .addStatement("dbob.setConnection(connection)")
                .addStatement("dbob.delete()")
                .nextControlFlow("catch ($T ex)", Exception.class)
                .addStatement("throw new $T(ex.getMessage())", remoteEx)
                .endControlFlow()
                .endControlFlow()
                .build();

        MethodSpec loadDbObjectOnID = MethodSpec.methodBuilder("loadDbObjectOnID")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(dbObject)
                .addException(remoteEx)
                .addParameter(Class.class, "dbobClass")
                .addParameter(int.class, "id")
                .addStatement("$T dbob", dbObject)
                .beginControlFlow("try")
                .addStatement("$T constructor = dbobClass.getConstructor($T.class)", constructor, connection)
                .addStatement("dbob = ($T) constructor.newInstance(connection)", dbObject)
                .addStatement("dbob = dbob.loadOnId(id)")
                .nextControlFlow("catch (Exception ex)")
                .addStatement("throw new $T(ex.getMessage())", remoteEx)
                .endControlFlow()
                .addStatement("return dbob")
                .build();

        MethodSpec getTableBodySimple = MethodSpec.methodBuilder("getTableBody")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ArrayTypeName.of(Vector.class))
                .addException(remoteEx)
                .addParameter(String.class, "select")
                .addStatement("return getTableBody(select, 0, 0)")
                .build();

        MethodSpec getTableBodyPaged = MethodSpec.methodBuilder("getTableBody")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ArrayTypeName.of(Vector.class))
                .addException(remoteEx)
                .addParameter(String.class, "select")
                .addParameter(int.class, "page")
                .addParameter(int.class, "pagesize")
                .addStatement("$T headers = getColNames(select)", Vector.class)
                .addStatement("int startrow = 0, endrow = 0")
                .beginControlFlow("if (page > 0 || pagesize > 0)")
                .addStatement("startrow = page * pagesize + 1")
                .addStatement("endrow = (page + 1) * pagesize")
                .endControlFlow()
                .addStatement("return new $T[]{headers, getRows(select, headers.size(), startrow, endrow)}", Vector.class)
                .build();

        MethodSpec getCount = MethodSpec.methodBuilder("getCount")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addException(remoteEx)
                .addParameter(String.class, "select")
                .addStatement("StringBuffer slct")
                .addStatement("int count = 0")
                .addStatement("int p = select.toLowerCase().lastIndexOf($S)", "order by")
                .addStatement("slct = new StringBuffer($S + select.substring(0, p > 0 ? p : select.length()) + $S)",
                        "select count(*) from (", ") intab")
                .addStatement("$T ps = null", PreparedStatement.class)
                .addStatement("$T rs = null", ResultSet.class)
                .beginControlFlow("try")
                .addStatement("ps = connection.prepareStatement(slct.toString())")
                .addStatement("rs = ps.executeQuery()")
                .beginControlFlow("if (rs.next())")
                .addStatement("count = rs.getInt(1)")
                .endControlFlow()
                .nextControlFlow("catch ($T ex)", ClassName.get("java.sql", "SQLException"))
                .addStatement("throw new $T(ex.getMessage())", remoteEx)
                .nextControlFlow("finally")
                .beginControlFlow("try")
                .beginControlFlow("if (rs != null)")
                .addStatement("rs.close()")
                .endControlFlow()
                .nextControlFlow("catch ($T se1)", ClassName.get("java.sql", "SQLException"))
                .endControlFlow()
                .beginControlFlow("finally")
                .beginControlFlow("try")
                .beginControlFlow("if (ps != null)")
                .addStatement("ps.close()")
                .endControlFlow()
                .nextControlFlow("catch ($T se2)", ClassName.get("java.sql", "SQLException"))
                .endControlFlow()
                .endControlFlow()
                .endControlFlow()
                .addStatement("return count")
                .build();

        MethodSpec getColNames = MethodSpec.methodBuilder("getColNames")
                .addModifiers(Modifier.PUBLIC)
                .returns(Vector.class)
                .addException(remoteEx)
                .addParameter(String.class, "select")
                .addStatement("String original = null")
                .addStatement("$T colNames = new $T()", Vector.class, Vector.class)
                .addStatement("$T ps = null", PreparedStatement.class)
                .addStatement("$T rs = null", ResultSet.class)
                .beginControlFlow("try")
                .addStatement("int i")
                .addStatement("int bracesLevel = 0")
                .addStatement("StringBuffer sb = null")
                .beginControlFlow("for (i = 0; i < select.length(); i++)")
                .addStatement("char c = select.charAt(i)")
                .beginControlFlow("if (c == '(')")
                .addStatement("bracesLevel++")
                .nextControlFlow("else if (c == ')')")
                .addStatement("bracesLevel--")
                .nextControlFlow("else if (bracesLevel == 0 && select.substring(i).toUpperCase().startsWith($S))", "WHERE ")
                .beginControlFlow("if (sb == null)")
                .addStatement("original = select")
                .addStatement("sb = new StringBuffer(select)")
                .endControlFlow()
                .addStatement("sb.insert(i + 6, $S)", "1=0 AND ")
                .addStatement("break")
                .endControlFlow()
                .endControlFlow()
                .beginControlFlow("if (sb != null)")
                .addStatement("select = sb.toString()")
                .endControlFlow()
                .addStatement("ps = connection.prepareStatement(select)")
                .addStatement("rs = ps.executeQuery()")
                .addStatement("$T md = rs.getMetaData()", ResultSetMetaData.class)
                .beginControlFlow("for (i = 0; i < md.getColumnCount(); i++)")
                .addStatement("colNames.add(md.getColumnLabel(i + 1))")
                .endControlFlow()
                .nextControlFlow("catch ($T ex)", ClassName.get("java.sql", "SQLException"))
                .addStatement("throw new $T(ex.getMessage())", remoteEx)
                .nextControlFlow("finally")
                .beginControlFlow("try")
                .beginControlFlow("if (rs != null)")
                .addStatement("rs.close()")
                .endControlFlow()
                .nextControlFlow("catch ($T se1)", ClassName.get("java.sql", "SQLException"))
                .endControlFlow()
                .beginControlFlow("finally")
                .beginControlFlow("try")
                .beginControlFlow("if (ps != null)")
                .addStatement("ps.close()")
                .endControlFlow()
                .nextControlFlow("catch ($T se2)", ClassName.get("java.sql", "SQLException"))
                .endControlFlow()
                .endControlFlow()
                .endControlFlow()
                .addStatement("return colNames")
                .build();

        MethodSpec getRows = MethodSpec.methodBuilder("getRows")
                .addModifiers(Modifier.PRIVATE)
                .returns(Vector.class)
                .addException(remoteEx)
                .addParameter(String.class, "select")
                .addParameter(int.class, "cols")
                .addParameter(int.class, "startrow")
                .addParameter(int.class, "endrow")
                .addStatement("$T rows = new $T()", Vector.class, Vector.class)
                .addStatement("$T ps = null", PreparedStatement.class)
                .addStatement("$T rs = null", ResultSet.class)
                .beginControlFlow("try")
                .addStatement("String pagedSelect")
                .beginControlFlow("if (select.toUpperCase().indexOf($S) > 0 || (startrow == 0 && endrow == 0))", " LIMIT ")
                .addStatement("pagedSelect = select")
                .nextControlFlow("else")
                .addStatement("pagedSelect = select.replaceFirst($S, $S).replaceAll($S, $S)",
                        "select", "SELECT", "Select", "SELECT")
                .addStatement("pagedSelect += $S + (startrow - 1) + $S + (endrow - startrow + 1)",
                        " LIMIT ", ",")
                .endControlFlow()
                .addStatement("$T line", Vector.class)
                .addStatement("ps = connection.prepareStatement(pagedSelect)")
                .addStatement("rs = ps.executeQuery()")
                .beginControlFlow("while (rs.next())")
                .addStatement("line = new $T()", Vector.class)
                .beginControlFlow("for (int c = 0; c < cols; c++)")
                .addStatement("String ceil = rs.getString(c + 1)")
                .addStatement("ceil = (ceil == null ? $S : ceil)", "")
                .addStatement("line.add(ceil)")
                .endControlFlow()
                .addStatement("rows.add(line)")
                .endControlFlow()
                .addStatement("return rows")
                .nextControlFlow("catch ($T ex)", ClassName.get("java.sql", "SQLException"))
                .addStatement("throw new $T(ex.getMessage())", remoteEx)
                .nextControlFlow("finally")
                .beginControlFlow("try")
                .beginControlFlow("if (rs != null)")
                .addStatement("rs.close()")
                .endControlFlow()
                .nextControlFlow("catch ($T se1)", ClassName.get("java.sql", "SQLException"))
                .endControlFlow()
                .beginControlFlow("finally")
                .beginControlFlow("try")
                .beginControlFlow("if (ps != null)")
                .addStatement("ps.close()")
                .endControlFlow()
                .nextControlFlow("catch ($T se2)", ClassName.get("java.sql", "SQLException"))
                .endControlFlow()
                .endControlFlow()
                .endControlFlow()
                .build();

        MethodSpec startTransaction = MethodSpec.methodBuilder("startTransaction")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(String.class, "transactionName")
                .addStatement(NOTSUPPORTED_MSG, UnsupportedOperationException.class)
                .addException(ClassName.get(RemoteException.class))
                .build();

        MethodSpec commitTransaction = MethodSpec.methodBuilder("commitTransaction")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addStatement(NOTSUPPORTED_MSG, UnsupportedOperationException.class)
                .addException(ClassName.get(RemoteException.class))
                .build();
        MethodSpec callProcedure = MethodSpec.methodBuilder("callProcedure")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(String.class, "procName")
                .addStatement(NOTSUPPORTED_MSG, UnsupportedOperationException.class)
                .addException(ClassName.get(RemoteException.class))
                .build();
        MethodSpec rollbackTransaction = MethodSpec.methodBuilder("rollbackTransaction")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(String.class, "transactionName")
                .addStatement(NOTSUPPORTED_MSG, UnsupportedOperationException.class)
                .addException(ClassName.get(RemoteException.class))
                .build();

        TypeSpec dbClientDataSender = TypeSpec.classBuilder("DbClientDataSender")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get(this.packageName, "IMessageSender"))
                .addJavadoc("Generated with JavaPoet\n")
                .addField(connectionField)
                .addMethod(ctor)
                .addMethods(List.of(
                                getServerVersion, truncateTable, getDbObjects, saveDbObject, deleteObject, loadDbObjectOnID,
                                getTableBodySimple, getTableBodyPaged, getCount, getColNames, getRows,
                                startTransaction, commitTransaction, rollbackTransaction, callProcedure
                        )
                )
                .build();

        // JavaFile
        JavaFile javaFile = JavaFile.builder(this.packageName, dbClientDataSender)
                .indent("    ")
                .build();
        javaFile.writeTo(Paths.get(outFolder));
    }

    private List<MethodSpec> iMessageSenderMethods() {
        // Build Class<? extends DbObject> type
        TypeName classOfDbObjectSubtype =
                ParameterizedTypeName.get(
                        ClassName.get(Class.class),
                        WildcardTypeName.subtypeOf(ClassName.get(DbObject.class))
                );

        return List.of(
                MethodSpec.methodBuilder("getDbObjects")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(ArrayTypeName.of(ClassName.get(DbObject.class)))
                        .addParameter(classOfDbObjectSubtype, "dbobClass")
                        .addParameter(String.class, "whereCondition")
                        .addParameter(String.class, "orderCondition")
                        .addException(ClassName.get(RemoteException.class))
                        .build(),
                MethodSpec.methodBuilder("saveDbObject")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(ClassName.get(DbObject.class))
                        .addParameter(ClassName.get(DbObject.class), "dbob")
                        .addException(ClassName.get(RemoteException.class))
                        .build(),
                MethodSpec.methodBuilder("deleteObject")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(ClassName.get(DbObject.class), "dbob")
                        .addException(ClassName.get(RemoteException.class))
                        .build(),
                MethodSpec.methodBuilder("loadDbObjectOnID")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(ClassName.get(DbObject.class))
                        .addParameter(classOfDbObjectSubtype, "dbobClass")
                        .addParameter(TypeName.INT, "id")
                        .addException(ClassName.get(RemoteException.class))
                        .build(),
                MethodSpec.methodBuilder("getTableBody")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(ArrayTypeName.of(ClassName.get(Vector.class)))
                        .addParameter(String.class, "select")
                        .addException(ClassName.get(RemoteException.class))
                        .build(),
                MethodSpec.methodBuilder("getTableBody")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(ArrayTypeName.of(ClassName.get(Vector.class)))
                        .addParameter(String.class, "select")
                        .addParameter(TypeName.INT, "page")
                        .addParameter(TypeName.INT, "pageSize")
                        .addException(ClassName.get(RemoteException.class))
                        .build(),
                MethodSpec.methodBuilder("getCount")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(TypeName.INT)
                        .addParameter(String.class, "select")
                        .addException(ClassName.get(RemoteException.class))
                        .build(),
                MethodSpec.methodBuilder("getServerVersion")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(String.class)
                        .addException(ClassName.get(RemoteException.class))
                        .build(),
                MethodSpec.methodBuilder("truncateTable")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(String.class, "tableName")
                        .returns(TypeName.BOOLEAN)
                        .addException(ClassName.get(RemoteException.class))
                        .build(),
                MethodSpec.methodBuilder("commitTransaction")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addException(ClassName.get(RemoteException.class))
                        .build(),
                MethodSpec.methodBuilder("startTransaction")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(String.class, "transactionName")
                        .addException(ClassName.get(RemoteException.class))
                        .build(),
                MethodSpec.methodBuilder("rollbackTransaction")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(String.class, "transactionName")
                        .addException(ClassName.get(RemoteException.class))
                        .build(),
                MethodSpec.methodBuilder("callProcedure")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(String.class, "procName")
                        .addException(ClassName.get(RemoteException.class))
                        .build()
        );
    }
}
