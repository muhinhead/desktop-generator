package org.dbdesktop.generator;

import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

public class AppGenerator implements IClassesGenerator {

    private String packageName;
    private final String password;
    private Connection connection;

    public AppGenerator(Connection connection, String password) {
        this.connection = connection;
        this.password = password;
    }



    @Override
    public void generateClasses(String outFolder) throws Exception {
        DatabaseMetaData meta = this.connection.getMetaData();

        String dbName = extractDatabaseName(meta.getURL());
        System.out.println("URL: " + meta.getURL());
        System.out.println("User: " + meta.getUserName().replaceAll("@.*", ""));
        System.out.println("Password: " + this.password);
        System.out.println("Database: " + meta.getDatabaseProductName());
        System.out.println("Version: " + dbName);
        System.out.println("Driver: " + meta.getDriverName());

        this.packageName = "org.dbdesktop.app." + dbName;

        MethodSpec mainMethod = MethodSpec.methodBuilder("main")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(String[].class, "args")
                .build();

        TypeSpec mainAppClass = TypeSpec.classBuilder(dbName.substring(0, 1).toUpperCase() + dbName.substring(1))
                .addModifiers(Modifier.PUBLIC)
                .addMethod(mainMethod)
                .build();
        JavaFile javaFile = JavaFile.builder(this.packageName, mainAppClass)
                .build();
        javaFile.writeTo(Paths.get(outFolder));
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
        if (question >= 0) {
            return afterSlash.substring(0, question);
        } else {
            return afterSlash;
        }
    }
}
