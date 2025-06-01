package org.dbdesktop.generator;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ORMGenerator {
    public Connection connection;

    public ORMGenerator(Connection connection) {
        this.connection = connection;
    }

    public void generateORMclasses(File outFolder) {
        try {
            for (String table : getTables(connection, "vinyl")) {
                System.out.println("-> " + table);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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
