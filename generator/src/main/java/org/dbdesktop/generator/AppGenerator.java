package org.dbdesktop.generator;

import com.squareup.javapoet.*;
import org.dbdesktop.dbstructure.DbClientDataSender;
import org.dbdesktop.dbstructure.Table;
import org.dbdesktop.guiutil.*;
import org.dbdesktop.orm.ExchangeFactory;
import org.dbdesktop.orm.IMessageSender;

import javax.lang.model.element.Modifier;
import javax.swing.*;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
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
                .beginControlFlow("if (!initPropAndLog())")
                .addStatement("quit(1)")
                .endControlFlow()
                .beginControlFlow("try ($T connection = $T.getConnection(\"$L\", \"$L\", \"$L\"))",
                        Connection.class, DriverManager.class, meta.getURL(), user, this.password)
                .addStatement("$T exchanger = new $T(connection)", DbClientDataSender.class, DbClientDataSender.class)
                .addStatement("System.out.println(\"✅ Successfully connected to the database [$L]. DB exchanger object created\")", dbName)
                .addStatement("MainFrame mainFrame = new MainFrame(exchanger)")
                .addStatement("javax.swing.SwingUtilities.invokeLater(() -> { mainFrame.setVisible(true); })")
                .nextControlFlow("catch ($T e)", Exception.class)
                .addStatement("System.err.println(\"❌ Connection failed: \" + e.getMessage())")
                .addStatement("e.printStackTrace()")
                .endControlFlow()
                .build();

        MethodSpec quitMethod = MethodSpec.methodBuilder("quit")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(TypeName.INT, "code")
                .addStatement("$T.getPropLogEngine().saveProps()", ExchangeFactory.class)
                .addStatement("System.exit(code)")
                .build();

        MethodSpec initPropAndLogMethod = MethodSpec.methodBuilder("initPropAndLog")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(TypeName.BOOLEAN)
                .addStatement("PropLogEngine.setOwner($L.class)", capitalize(dbName))
                .beginControlFlow("try")
                .addStatement("$T.setPropLogEngine(PropLogEngine.getInstance($T.ALL))", ExchangeFactory.class, Level.class)
                .nextControlFlow("catch($T.NoOwnerException ex)", PropLogEngine.class)
                .addStatement("ex.printStackTrace()")
                .addStatement("return false")
                .endControlFlow()
                .addStatement("return true")
                .build();

        TypeSpec mainAppClass = TypeSpec.classBuilder(capitalize(dbName))
                .addModifiers(Modifier.PUBLIC)
                .addMethods(List.of(mainMethod, quitMethod, initPropAndLogMethod))
                .build();
        JavaFile javaFile = JavaFile.builder(this.packageName, mainAppClass)
                .build();
        javaFile.writeTo(Paths.get(outFolder));

        generateMainFrame(capitalize(dbName), this.packageName, outFolder);

        generateGrids(packageName, outFolder);
    }

    private void generateGrids(String packageName, String outFolder) throws Exception {

        TypeName hashMapType = ParameterizedTypeName.get(
                ClassName.get(HashMap.class),
                ClassName.get(Integer.class),
                ClassName.get(Integer.class)
        );

        FieldSpec maxWidthsField = FieldSpec.builder(hashMapType, "maxWidths",
                        Modifier.PRIVATE, Modifier.STATIC)
                .initializer("new $T<>()", HashMap.class)
                .build();

        // Static initializer block:
        CodeBlock staticBlock = CodeBlock.builder()
                .addStatement("maxWidths.put(0, 50)")
                .build();

        for (String tableName : Table.allTables.keySet()) {
            String gridClassName = capitalize(tableName) + "Grid";

            System.out.println("Generating "+gridClassName);

            MethodSpec constructor = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ClassName.get(IMessageSender.class), "exchanger")
                    .addException(ClassName.get(RemoteException.class))
                    .addStatement("super(exchanger,\"select * from " + tableName + " limit 100\", maxWidths, true)")
                    .build();

            TypeSpec gridClass = TypeSpec.classBuilder(gridClassName)
                    .superclass(AbstractGridAdapter.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addField(maxWidthsField)
                    .addStaticBlock(staticBlock)
                    .addMethod(constructor)
                    .build();
            JavaFile javaFile = JavaFile.builder(packageName, gridClass)
                    .build();
            // Write to file system (or print to console)
            javaFile.writeTo(Paths.get(outFolder));
        }
    }

    private void generateMainFrame(String dbName, String packageName, String outFolder) throws Exception {

        TypeSpec mainAppClass = TypeSpec.classBuilder("MainFrame")
                .addModifiers(Modifier.PUBLIC)
                .superclass(GeneralFrame.class)
                .addFields(mainFrameFields())
                .addMethods(List.of(MethodSpec.constructorBuilder()
                                        .addParameter(IMessageSender.class, "exchanger")
                                        .addModifiers(Modifier.PUBLIC)
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
                .addMethods(generateGridPanelsMethods())
                .build();
        JavaFile javaFile = JavaFile.builder(packageName, mainAppClass)
                .build();
        javaFile.writeTo(Paths.get(outFolder));
    }

    private List<MethodSpec> generateGridPanelsMethods() {
        ArrayList<MethodSpec> gridMethods = new ArrayList<>(Table.allTables.size());
        for(String tabName: Table.allTables.keySet()) {

            CodeBlock codeBlock = CodeBlock.builder()
                    .beginControlFlow("if ($LPanel == null)", tabName)
                    .beginControlFlow("try")
                    .addStatement("registerGrid($LPanel = new $LGrid(getExchanger()))",tabName, capitalize(tabName))
                    .nextControlFlow("catch ($T ex)", RemoteException.class)
                    .addStatement("$T.getPropLogEngine().log(ex)", ExchangeFactory.class)
                    .addStatement("$T.errMessageBox($T.ERROR, ex.getMessage())", GeneralUtils.class, GeneralUtils.class)
                    .endControlFlow()
                    .endControlFlow()
                    .addStatement("return $LPanel", tabName)
                    .build();

            gridMethods.add(MethodSpec.methodBuilder("get"+capitalize(tabName)+"Panel")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(JPanel.class)
                    .addCode(codeBlock)
                    .build());
        }
        return gridMethods;
    }

    private CodeBlock generateTabsArrayCode() {
        CodeBlock.Builder cb = CodeBlock.builder();
        for(String tabName : Table.allTables.keySet()) {
            cb.addStatement("mainTabPanel.addTab(get$LPanel(), sheetList[n++])", capitalize(tabName));
        }
        return cb.build();
    }

    private Iterable<FieldSpec> mainFrameFields() {
        ArrayList<FieldSpec> flds = new ArrayList<>();

        flds.add(FieldSpec.builder(ClassName.bestGuess("MainFrame"), "instance")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .build());
        for(String tabName : Table.allTables.keySet()) {
            flds.add(FieldSpec.builder(GeneralGridPanel.class, tabName+"Panel")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .build());
        }

        flds.add((sheetListField = FieldSpec.builder(ArrayTypeName.of(ClassName.get(String.class)), "sheetList",
                        Modifier.PRIVATE, Modifier.STATIC)
                .initializer("new String[] { $L }",
                        getTabsHeaders())
                .build()));

        return flds;
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
