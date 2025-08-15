package org.dbdesktop.generator;

import com.squareup.javapoet.*;
import org.dbdesktop.orm.DbObject;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Vector;

public class MySQLutilGenerator implements IClassesGenerator{

    private final String host;
    private final Integer port;
    private final String database;
    private final String packageName;

    public MySQLutilGenerator(String host, Integer port, String database, String packageName) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.packageName = packageName;
    }

    @Override
    public void generateClasses(String outFolder) throws Exception {

        generateIMessageSender(outFolder);
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

    private List<MethodSpec> iMessageSenderMethods() {
        return List.of(
                MethodSpec.methodBuilder("getDbObjects")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(ArrayTypeName.of(ClassName.get(DbObject.class)))
                        .addParameter(Class.class, "dbobClass")
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
                        .addParameter(Class.class, "dbobClass")
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
                        .build()
        );
    }
}
