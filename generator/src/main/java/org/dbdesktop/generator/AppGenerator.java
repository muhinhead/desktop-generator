package org.dbdesktop.generator;

import com.squareup.javapoet.*;
import org.dbdesktop.dbstructure.DbClientDataSender;
import org.dbdesktop.guiutil.GeneralFrame;
import org.dbdesktop.guiutil.GeneralGridPanel;
import org.dbdesktop.guiutil.MyJideTabbedPane;
import org.dbdesktop.orm.IMessageSender;

import javax.lang.model.element.Modifier;
import javax.swing.*;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AppGenerator implements IClassesGenerator {

    private final Set<String> tables;
    private String packageName;
    private final String password;
    private Connection connection;
    private FieldSpec sheetListField;

    public AppGenerator(Connection connection, String password, Set<String> tables) {
        this.connection = connection;
        this.password = password;
        this.tables = tables;
    }

    private static String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    @Override
    public void generateClasses(String outFolder) throws Exception {
        DatabaseMetaData meta = this.connection.getMetaData();

        String dbName = extractDatabaseName(meta.getURL());
        String user = meta.getUserName().replaceAll("@.*", "");
        System.out.println("URL: " + meta.getURL());
        System.out.println("User: " + user);
        System.out.println("Password: " + this.password);
        System.out.println("Database: " + dbName);
        System.out.println("Version: " + meta.getDatabaseProductName());
        System.out.println("Driver: " + meta.getDriverName());

        this.packageName = "org.dbdesktop.app." + dbName;

        MethodSpec mainMethod = MethodSpec.methodBuilder("main")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(String[].class, "args")
                .beginControlFlow("try ($T connection = $T.getConnection(\"$L\", \"$L\", \"$L\"))",
                        Connection.class, DriverManager.class, meta.getURL(), user, this.password)
                .addStatement("$T exchanger = new $T(connection)", DbClientDataSender.class, DbClientDataSender.class)
                .addStatement("System.out.println(\"✅ Successfully connected to the database [$L]. DB exchanger object created\")", dbName)
                .nextControlFlow("catch ($T e)", Exception.class)
                .addStatement("System.err.println(\"❌ Connection failed: \" + e.getMessage())")
                .addStatement("e.printStackTrace()")
                .endControlFlow()
                .build();

        TypeSpec mainAppClass = TypeSpec.classBuilder(capitalize(dbName))
                .addModifiers(Modifier.PUBLIC)
                .addMethod(mainMethod)
                .build();
        JavaFile javaFile = JavaFile.builder(this.packageName, mainAppClass)
                .build();
        javaFile.writeTo(Paths.get(outFolder));

        generateMainFrame(capitalize(dbName), this.packageName, outFolder);
    }

    private void generateMainFrame(String dbName, String packageName, String outFolder) throws Exception {
        TypeSpec mainAppClass = TypeSpec.classBuilder("MainFrame")
                .addModifiers(Modifier.PUBLIC)
                .superclass(GeneralFrame.class)
                .addFields(mainFrameFields())
                .addMethods(List.of(MethodSpec.constructorBuilder()
                                        .addParameter(IMessageSender.class, "exchanger")
                                        .addStatement("super(\"Database $L\", exchanger)", dbName)
                                        .build(),
                                MethodSpec.methodBuilder("getSheetList")
                                        .addAnnotation(Override.class)  // 👈 add @Override
                                        .addModifiers(Modifier.PUBLIC)
                                        .returns(sheetListField.type)
                                        .addStatement("return $N", sheetListField)
                                        .build(),
                                MethodSpec.methodBuilder("getMainPanel")
                                        .addAnnotation(Override.class)  // 👈 add @Override
                                        .addModifiers(Modifier.PUBLIC)
                                        .returns(JTabbedPane.class)
                                        .addStatement("int n = 0")
                                        .addStatement("$T mainTabPanel = new $T()", MyJideTabbedPane.class, MyJideTabbedPane.class)
                                        .addCode(generateTabsArrayCode())
                                        .addStatement("return mainTabPanel")
                                        .build()
                        )
                )
                .build();
        JavaFile javaFile = JavaFile.builder(packageName, mainAppClass)
                .build();
        javaFile.writeTo(Paths.get(outFolder));
    }

    private CodeBlock generateTabsArrayCode() {
        CodeBlock.Builder cb = CodeBlock.builder();
        return cb.beginControlFlow("for(String sl : sheetList)")
                .addStatement("mainTabPanel.addTab(new $T(), sl)", JPanel.class)
                .endControlFlow().build();
    }

    private Iterable<FieldSpec> mainFrameFields() {
        return List.of(
                FieldSpec.builder(ClassName.bestGuess("MainFrame"), "instance")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .build(),
                FieldSpec.builder(GeneralGridPanel.class, "usersPanel")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .build(),
                (sheetListField = FieldSpec.builder(ArrayTypeName.of(ClassName.get(String.class)), "sheetList",
                                Modifier.PRIVATE, Modifier.STATIC)
                        .initializer("new String[] { $L }",
                                getTabsHeaders())
                        .build())
        );
    }

    private String getTabsHeaders() {
        return tables.stream()
                .map(s -> "\"" + capitalize(s) + "\"")           // wrap each string in quotes
                .collect(Collectors.joining(", ", "", ", "));
    }

    private static String extractDatabaseName(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        // Remove prefix like jdbc:mysql://
        String noPrefix = url.substring(url.indexOf("://") + 3);

        // Find the first "/" after host:port
        int slash = noPrefix.indexOf('/');
        if (slash < 0) return null;

        String afterSlash = noPrefix.substring(slash + 1);

        // Cut at "?" if parameters exist
        int question = afterSlash.indexOf('?');
        return question > 0 ? afterSlash.substring(0, question) : afterSlash;
    }
}
