package org.dbdesktop.generator;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

public class AppGenerator implements IClassesGenerator {

    private final String password;
    private Connection connection;

    public AppGenerator(Connection connection, String password) {
        this.connection = connection;
        this.password = password;
    }

    @Override
    public void generateClasses(String outFolder) throws Exception {
        DatabaseMetaData meta = this.connection.getMetaData();

        System.out.println("URL: " + meta.getURL());
        System.out.println("User: " + meta.getUserName());
        System.out.println("Database: " + meta.getDatabaseProductName());
        System.out.println("Version: " + meta.getDatabaseProductVersion());
        System.out.println("Driver: " + meta.getDriverName());
    }
}
