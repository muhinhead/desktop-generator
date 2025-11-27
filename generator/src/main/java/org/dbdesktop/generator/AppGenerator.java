package org.dbdesktop.generator;

import com.squareup.javapoet.*;
import org.dbdesktop.dbstructure.DbClientDataSender;
import org.dbdesktop.dbstructure.Table;
import org.dbdesktop.guiutil.*;
import org.dbdesktop.orm.ExchangeFactory;
import org.dbdesktop.orm.IMessageSender;

import javax.lang.model.element.Modifier;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class AppGenerator implements IClassesGenerator {

    private final ArrayList<String> tables;
    private String packageName;
    private final String password;
    private Connection connection;
    private FieldSpec sheetListField;

    public AppGenerator(Connection connection, String password, ArrayList<String> tables) {
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
                .addStatement("String url = \"$L\"", meta.getURL())
                .addStatement("String user = \"$L\"", user)
                .addStatement("String password = \"$L\"", this.password)
                .beginControlFlow("try ($T connection = $T.getConnection(url, user, password))",
                        Connection.class, DriverManager.class)
                .addStatement("$T exchanger = new $T(url, user, password)", DbClientDataSender.class, DbClientDataSender.class)
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
        generateEditPanels(packageName, outFolder);
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

        for (Table table : Table.allTables.values()) {
            String gridClassName = capitalize(table.getName()) + "Grid";

            System.out.println("Generating " + gridClassName);

            MethodSpec constructor = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ClassName.get(IMessageSender.class), "exchanger")
                    .addException(ClassName.get(RemoteException.class))
                    .addStatement("super(exchanger,\"select * from " + table.getName() + " limit 10000\", maxWidths, false)")
                    .build();

            // TODO: How to get rid of this hardcoded package name (org.dbdesktop.orm.)
            String recordItemClass = "org.dbdesktop.orm." + capitalize(table.getName());
            String editPanelClass = "Edit" + capitalize(table.getName()) + "Panel";

            MethodSpec addAction = MethodSpec.methodBuilder("addAction")
                    .addModifiers(Modifier.PROTECTED)
                    .addAnnotation(Override.class)
                    .returns(AbstractAction.class)
                    .addStatement(    "   return new $T($S) {\n"
                                    + "    @Override\n"
                                    + "    public void actionPerformed($T e) {\n"
                                    + "        try {\n"
                                    + "            $T<$L> dlg = new $T<>($L.class, $S, null);\n"
                                    + "            if (dlg.isOkPressed()) {\n"
                                    + "                $T.updateGrid(exchanger, getTableView(),\n"
                                    + "                        getTableDoc(), getSelect(), null, getPageSelector().getSelectedIndex());\n"
                                    + "            }\n"
                                    + "        } catch ($T ex) {\n"
                                    + "            $T.getPropLogEngine().log(ex);\n"
                                    + "            $T.errMessageBox($T.ERROR, ex.getMessage());\n"
                                    + "        }\n"
                                    + "    }\n"
                                    + "}",
                            AbstractAction.class,
                            "Add",
                            ActionEvent.class,
                            GenericEditDialog.class,
                            editPanelClass,
                            GenericEditDialog.class,
                            editPanelClass,
                            "Add " + capitalize(table.getHeader()),
                            GeneralFrame.class,
                            RemoteException.class,
                            ExchangeFactory.class,
                            GeneralUtils.class,
                            GeneralUtils.class
                    )
                    .build();

            MethodSpec editAction = MethodSpec.methodBuilder("editAction")
                    .addModifiers(Modifier.PROTECTED)
                    .addAnnotation(Override.class)
                    .returns(AbstractAction.class)
                    .addStatement("   return new $T($S) {\n"
                                    + "    @Override\n"
                                    + "    public void actionPerformed($T e) {\n"
                                    + "        int id = getSelectedID();\n"
                                    + "        if (id > 0) {\n"
                                    + "            try {\n"
                                    + "                $L $L = ($L) exchanger.loadDbObjectOnID($L.class, id);\n"
                                    + "                $T<$L> dlg = new $T<>($L.class, $S, null);\n"
                                    + "                if (dlg.isOkPressed()) {\n"
                                    + "                    $T.updateGrid(exchanger, getTableView(),\n"
                                    + "                            getTableDoc(), getSelect(), id, getPageSelector().getSelectedIndex());\n"
                                    + "                }\n"
                                    + "            } catch ($T ex) {\n"
                                    + "                $T.getPropLogEngine().log(ex);\n"
                                    + "                $T.errMessageBox($T.ERROR, ex.getMessage());\n"
                                    + "            }\n"
                                    + "        }\n"
                                    + "    }\n"
                                    + "}",
                            AbstractAction.class,
                            "Edit",
                            ActionEvent.class,
                            recordItemClass,
                            table.getName(),
                            recordItemClass,
                            recordItemClass,
                            GenericEditDialog.class,
                            editPanelClass,
                            GenericEditDialog.class,
                            editPanelClass,
                            "Edit " + capitalize(table.getHeader()),
                            GeneralFrame.class,
                            RemoteException.class,
                            ExchangeFactory.class,
                            GeneralUtils.class,
                            GeneralUtils.class
                    )
                    .build();

            MethodSpec delAction = MethodSpec.methodBuilder("delAction")
                    .addModifiers(Modifier.PROTECTED)
                    .addAnnotation(Override.class)
                    .returns(AbstractAction.class)
                    .addStatement("   return new $T($S) {\n"
                                    + "    @Override\n"
                                    + "    public void actionPerformed($T e) {\n"
                                    + "        int id = getSelectedID();\n"
                                    + "        if (id > 0) {\n"
                                    + "            try {\n"
                                    + "                $L $L = ($L) exchanger.loadDbObjectOnID($L.class, id);\n"
                                    + "                if($L != null && $T.yesNo(\"Attention!\", \"Do you want to delete the record?\") == $T.YES_OPTION) {\n"
                                    + "                    exchanger.deleteObject($L);\n"
                                    + "                    $T.updateGrid(exchanger, getTableView(),\n"
                                    + "                            getTableDoc(), getSelect(), id, getPageSelector().getSelectedIndex());\n"
                                    + "                }\n"
                                    + "            } catch ($T ex) {\n"
                                    + "                $T.getPropLogEngine().log(ex);\n"
                                    + "                $T.errMessageBox($T.ERROR, ex.getMessage());\n"
                                    + "            }\n"
                                    + "        }\n"
                                    + "    }\n"
                                    + "}",
                            AbstractAction.class,
                            "Delete",
                            ActionEvent.class,
                            recordItemClass,
                            table.getName(),
                            recordItemClass,
                            recordItemClass,
                            table.getName(),
                            GeneralUtils.class,
                            JOptionPane.class,
                            table.getName(),
                            GeneralFrame.class,
                            RemoteException.class,
                            ExchangeFactory.class,
                            GeneralUtils.class,
                            GeneralUtils.class
                    )
                    .build();


            TypeSpec gridClass = TypeSpec.classBuilder(gridClassName)
                    .superclass(AbstractGridAdapter.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addField(maxWidthsField)
                    .addStaticBlock(staticBlock)
                    .addMethod(constructor)
                    .addMethod(addAction)
                    .addMethod(editAction)
                    .addMethod(delAction)
                    .build();
            JavaFile javaFile = JavaFile.builder(packageName, gridClass)
                    .build();
            // Write to file system (or print to console)
            javaFile.writeTo(Paths.get(outFolder));
        }
    }

    private void generateEditPanels(String packageName, String outFolder) throws Exception {
        for (Table table : Table.allTables.values()) {
            //ClassName myImport = ClassName.get("org.dbdesktop.orm", capitalize(table.getName()));
            TypeSpec editPanelClass = TypeSpec.classBuilder("Edit" + capitalize(table.getName()) + "Panel")
                    .addModifiers(Modifier.PUBLIC)
                    .superclass(RecordEditPanel.class)
                    .addMethods(getEditPanelsMethods(table))
                    .build();

            JavaFile javaFile = JavaFile.builder(packageName, editPanelClass)
                    //.addStaticImport(myImport)
                    .build();
            javaFile.writeTo(Paths.get(outFolder));
        }
    }

    private List<MethodSpec> getEditPanelsMethods(Table table) {
        return List.of(
                MethodSpec.methodBuilder("fillContent")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PROTECTED)
                        .returns(void.class)
                        .addCode(CodeBlock.builder()
                                .add("// TODO: Add widget building UI code here\n")
                                .build())
                        .build(),
                MethodSpec.methodBuilder("loadData")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(void.class)
                        .addCode(CodeBlock.builder()
                                .addStatement("org.dbdesktop.orm.$L $L = (org.dbdesktop.orm.$L) getDbObject()",
                                        capitalize(table.getName()),
                                        table.getName(),
                                        capitalize(table.getName()))
                                .add("// TODO: Your actual widget loading logic\n")
                                .beginControlFlow("if ($L != null)", table.getName())
                                .addStatement("isViewOnly = $L.getPK_ID() < 0", table.getName())
                                .addStatement("idField.setText(String.valueOf(Math.abs($L.getPK_ID().intValue())))", table.getName())
                                .add("// TODO: Add more widget loads...\n")
                                .endControlFlow()
                                .add("// TODO: Continue with other parts of load logic...\n")
                                .build())
                        .build(),
                MethodSpec.methodBuilder("save")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(boolean.class)
                        .addException(Exception.class)
                        .addCode(CodeBlock.builder()
                                .addStatement("boolean isNew = true")
                                .addStatement("org.dbdesktop.orm.$L $L = (org.dbdesktop.orm.$L) getDbObject()",
                                        capitalize(table.getName()),
                                        table.getName(),
                                        capitalize(table.getName()))
                                .beginControlFlow("if ($L == null)", table.getName())
                                .addStatement("$L = new org.dbdesktop.orm.$L(null)", table.getName(), capitalize(table.getName()))
                                .addStatement("$L.setPK_ID(0)", table.getName())
                                .endControlFlow()
                                .add("// TODO: load widget values → dbObject fields\n")
                                .addStatement("return saveDbRecord($L, isNew)", table.getName())
                                .build())
                        .build(),
                MethodSpec.methodBuilder("freeResources")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(void.class)
                        .addCode(CodeBlock.builder()
                                .add("// TODO: cleanup code\n")
                                .build())
                        .build()
        );
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
                                        //.addStatement("mainTabPanel.addMouseListener(new @T(popMenu))", PopupListener.class)
                                        .addCode(generateTabsArrayCode())
                                        .addCode(generateTabPopupMenu())
                                        .addStatement("mainTabPanel.addMouseListener(new $T(popMenu))", PopupListener.class)
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
        for (Table table : Table.allTables.values()) {

            CodeBlock codeBlock = CodeBlock.builder()
                    .beginControlFlow("if ($LPanel == null)", table.getName())
                    .beginControlFlow("try")
                    .addStatement("registerGrid($LPanel = new $LGrid(getExchanger()))", table.getName(), capitalize(table.getName()))
                    .nextControlFlow("catch ($T ex)", RemoteException.class)
                    .addStatement("$T.getPropLogEngine().log(ex)", ExchangeFactory.class)
                    .addStatement("$T.errMessageBox($T.ERROR, ex.getMessage())", GeneralUtils.class, GeneralUtils.class)
                    .endControlFlow()
                    .endControlFlow()
                    .addStatement("return $LPanel", table.getName())
                    .build();

            gridMethods.add(MethodSpec.methodBuilder("get" + capitalize(table.getName()) + "Panel")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(JPanel.class)
                    .addCode(codeBlock)
                    .build());
        }
        return gridMethods;
    }

    public CodeBlock generateTabPopupMenu() {
        CodeBlock.Builder cb = CodeBlock.builder();
        //cb.addStatement("$T popMenu = new $T(600)", ScrollablePopupMenu.class, ScrollablePopupMenu.class);
        cb.addStatement("$T popMenu = new $T()", JPopupMenu.class, JPopupMenu.class);
        cb.addStatement("popMenu.setLightWeightPopupEnabled(false)");
        int n = 0;
        for (Table table : Table.allTables.values()) {

            cb.addStatement("$T item" + n, JCheckBoxMenuItem.class);
            cb.addStatement("popMenu.add(item" + n + " = new $T(sheetList[" + n + "], true))", JCheckBoxMenuItem.class);

            TypeSpec actionListener = TypeSpec.anonymousClassBuilder("")
                    .superclass(ClassName.get("javax.swing", "AbstractAction"))
                    .addMethod(MethodSpec.methodBuilder("actionPerformed")
                            .addAnnotation(Override.class)
                            .addModifiers(Modifier.PUBLIC)
                            .returns(void.class)
                            .addParameter(ClassName.get("java.awt.event", "ActionEvent"), "e")
                            .addStatement("if (item" + n + ".isSelected()) {")
                            .addStatement("    mainTabPanel.addTab(get$LPanel(), sheetList[" + n + "])", capitalize(table.getName()))
                            .addStatement("} else {")
                            .addStatement("    mainTabPanel.remove(get$LPanel())", capitalize(table.getName()))
                            .addStatement("}")
                            .build())
                    .build();

            cb.addStatement("item" + n + ".addActionListener($L)", actionListener);
            n++;
        }
        return cb.build();
    }

    private CodeBlock generateTabsArrayCode() {
        CodeBlock.Builder cb = CodeBlock.builder();
        for (Table table : Table.allTables.values()) {
            cb.addStatement("mainTabPanel.addTab(get$LPanel(), sheetList[n++])", capitalize(table.getName()));
        }
        return cb.build();
    }

    private Iterable<FieldSpec> mainFrameFields() {
        ArrayList<FieldSpec> flds = new ArrayList<>();

        flds.add(FieldSpec.builder(ClassName.bestGuess("MainFrame"), "instance")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .build());
        for (String tabName : Table.allTables.keySet()) {
            flds.add(FieldSpec.builder(GeneralGridPanel.class, tabName + "Panel")
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
