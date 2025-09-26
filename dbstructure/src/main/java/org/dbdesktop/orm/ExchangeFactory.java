package org.dbdesktop.orm;

import org.dbdesktop.dbstructure.DbClientDataSender;
import org.dbdesktop.guiutil.GeneralUtils;
import org.dbdesktop.guiutil.PropLogEngine;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import static org.dbdesktop.guiutil.GeneralUtils.quit;

public class ExchangeFactory {

    public static String protocol = "jdbc";
    //private static final String NO_ACCESS_TO_PROCEDURE_BODIESTRUE = "&noAccessToProcedureBodies=true";
    private static PropLogEngine propLogEngine = null;
    private static String[] fixLocalDBsqls = new String[]{ // TODO: here is a place for updating DDLs
            "insert into mats_usr (login,password,isAdmin) values('admin','admin',1)"
    };

    public static String getRMIkey(String ipAddress, String mainClientClassName) {
        return "rmi://" + ipAddress + "/" + mainClientClassName + "Server";
    }

    public static IMessageSender getExchanger(String connectString, Properties props) {
        IMessageSender exchanger = null;
        if (connectString.startsWith("rmi:")) {
            try {
                exchanger = (IMessageSender) Naming.lookup(connectString);
                protocol = "rmi";
            } catch (Exception ex) {
                propLogEngine.log(ex);
            }
        }
        if (exchanger == null) {
            connectString = props.getProperty("JDBCconnection", "jdbc:mysql://localhost/materials?useUnicode=true&characterEncoding=UTF8");
//            connectString = props.getProperty("JDBCconnection", "jdbc:derby://localhost:1527//home/nick/Derby/TheLaserShop");
            //jdbc:derby://localhost:1527/orders
        }
        if (connectString.startsWith("jdbc:")) {
            String dbUser = props.getProperty("dbUser", "nick");
            String dbPassword = props.getProperty("dbPasword", "ghbdtnnt");
            String dbDriver = props.getProperty("dbDriverName", "com.mysql.cj.jdbc.Driver");
//            String dbDriver = props.getProperty("dbDriverName", "org.apache.derby.jdbc.ClientDriver");
            try {
                exchanger = createJDBCexchanger(dbDriver, connectString, dbUser, dbPassword);
                protocol = "jdbc";
            } catch (Exception ex) {
                ex.printStackTrace();
                propLogEngine.log(ex);
                GeneralUtils.quit(1);
            }
        }
        return exchanger;
    }

    public static IMessageSender createRMIexchanger(String address) throws NotBoundException, MalformedURLException, RemoteException {
        protocol = "rmi";
        return (IMessageSender) Naming.lookup(getRMIkey(address, PropLogEngine.getOwner().getName()));
    }

    public static IMessageSender createJDBCexchanger(String[] dbParams) throws SQLException, IllegalAccessException, ClassNotFoundException, InstantiationException {
        if (dbParams.length < 4) {
            return null;
        }
        return createJDBCexchanger(dbParams[0], dbParams[1], dbParams[2], dbParams[3]);
    }

    public static IMessageSender createJDBCexchanger(String dbDriver, String connectString,
                                                     String dbUser, String dbPassword) throws SQLException, IllegalAccessException, ClassNotFoundException, InstantiationException {
        if (dbDriver == null || dbDriver.isEmpty() || connectString == null || connectString.isEmpty()
                || dbUser == null || dbUser.isEmpty() || dbPassword == null || dbPassword.isEmpty()) {
            throw new SQLException("Incomplete DB connection parameters");
        }
        protocol = "jdbc";
        IMessageSender exchanger;
        DriverManager.registerDriver(
                (java.sql.Driver) Class.forName(dbDriver).newInstance());
        Connection connection = DriverManager.getConnection(connectString, dbUser, dbPassword);
        connection.setAutoCommit(true);
        sqlBatch(fixLocalDBsqls, connection, false);
        exchanger = new DbClientDataSender(connection);
        return exchanger;
    }

    public static void sqlBatch(String[] sqls, Connection connection, boolean tolog) {
        PreparedStatement ps = null;
        for (int i = 0; i < sqls.length; i++) {
            try {
                ps = connection.prepareStatement(sqls[i]);
                ps.execute();
                if (tolog) {
                    propLogEngine.log("STATEMENT [" + sqls[i].substring(0,
                            sqls[i].length() > 60 ? 60 : sqls[i].length()) + "]... processed");
                }
            } catch (SQLException e) {
                if (tolog) {
                    propLogEngine.log(e);
                }
            } finally {
                try {
                    ps.close();
                } catch (SQLException ex) {
                }
            }
        }
    }

    /**
     * @return the propLogEngine
     */
    public static PropLogEngine getPropLogEngine() {
        return propLogEngine;
    }

    /**
     * @param aPropLogEngine the propLogEngine to set
     */
    public static void setPropLogEngine(PropLogEngine aPropLogEngine) {
        propLogEngine = aPropLogEngine;
    }

    public static void setProtocol(String protocol) {
        ExchangeFactory.protocol = protocol;
    }

    public static String getProtocol() {
        return ExchangeFactory.protocol;
    }
}
