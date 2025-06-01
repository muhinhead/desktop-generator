import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.TestInstance;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;

//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Ignore
public class TableTest {

    private Connection connection;

    void setUp() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/vinyl?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
        String username = "nick";
        String password = "ghbdtnnt";
        connection = DriverManager.getConnection(url, username, password);
        System.out.println("✅ Connected: " + (connection != null));
    }

    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            System.out.println("🔒 Connection closed.");
        }
    }

    @Test
    public void testTableListing() throws SQLException {
        setUp();
        System.out.println("Connection in test = " + connection);
        assertNotNull(connection, "Connection is null!");

        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()"
        );
             ResultSet rs = stmt.executeQuery()) {

            System.out.println("Tables in current DB:");
            while (rs.next()) {
                System.out.println(" - " + rs.getString("table_name"));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Query failed", e);
        } finally {
            tearDown();
        }
    }
}
