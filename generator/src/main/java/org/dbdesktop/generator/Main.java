package org.dbdesktop.generator;

import org.dbdesktop.dbstructure.MySqlType;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

public class Main {
    public static void main(@org.jetbrains.annotations.NotNull String[] args) {
        String host = "localhost";
        String port = "3306";
        String database;
        String username;
        String password;
        switch (args.length) {
            case 3:
                database = args[0];
                username = args[1];
                password = args[2];
                break;
            case 4:
                try {
                    int portNum = Integer.parseInt(args[0]);
                    port = "" + portNum;
                } catch (NumberFormatException ne) {
                    host = args[0];
                }
                database = args[1];
                username = args[2];
                password = args[3];
                break;
            case 5:
                host = args[0];
                port = args[1];
                database = args[2];
                username = args[3];
                password = args[4];
                break;
            default:
                System.out.println("Usage: java org.dbdesktop.Main <host> <port> <database> <username> <password>");
                System.out.println("   Or: java org.dbdesktop.Main [<host> | <port>] <database> <username> <password>");
                System.out.println("   Or: java org.dbdesktop.Main <database> <username> <password>");
                return;
        }


        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            System.out.println("✅ Successfully connected to the database.");
            new ORMGenerator(conn, database, MySqlType.class).generateORMclasses("./generator/target/generated-sources");
        } catch (Exception e) {
            System.err.println("❌ Connection failed: " + e.getMessage());
        }

    }
}