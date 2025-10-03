package org.dbdesktop.dbstructure;

import org.dbdesktop.orm.DbObject;
import org.dbdesktop.orm.IMessageSender;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.sql.*;
import java.util.Vector;

public class DbClientDataSender implements IMessageSender {
    private Connection connection;

    public DbClientDataSender(Connection connection) {
        this.connection = connection;
    }

    @Override
    public DbObject[] getDbObjects(Class dbobClass, String whereCondition, String orderCondition) throws RemoteException {
        DbObject[] rows = null;
        try {
            Method method = dbobClass.getDeclaredMethod("load", Connection.class, String.class, String.class);
            rows = (DbObject[]) method.invoke(null, connection, whereCondition, orderCondition);
        } catch (Exception ex) {
            throw new java.rmi.RemoteException(ex.getMessage());
        }
        return rows;
    }

    @Override
    public DbObject saveDbObject(DbObject dbob) throws RemoteException {
        if (dbob != null) {
            try {
                boolean wasNew = dbob.isNew();
                dbob.setConnection(connection);
                dbob.save();
            } catch (Exception ex) {
                throw new java.rmi.RemoteException("Can't save DB object:", ex);
            }
        }
        return dbob;
    }

    @Override
    public void deleteObject(DbObject dbob) throws RemoteException {
        if (dbob != null) {
            try {
                dbob.setConnection(connection);
                dbob.delete();
            } catch (Exception ex) {
                throw new java.rmi.RemoteException(ex.getMessage());
            }
        }
    }

    @Override
    public DbObject loadDbObjectOnID(Class dbobClass, int id) throws RemoteException {
        DbObject dbob;
        try {
            Constructor constructor = dbobClass.getConstructor(Connection.class);
            dbob = (DbObject) constructor.newInstance(connection);
            dbob = dbob.loadOnId(id);
        } catch (Exception ex) {
            throw new java.rmi.RemoteException(ex.getMessage());
        }
        return dbob;
    }

    @Override
    public Vector[] getTableBody(String select) throws RemoteException {
        return getTableBody(select, 0, 0);
    }

    @Override
    public Vector[] getTableBody(String select, int page, int pagesize) throws RemoteException {
        Vector headers = getColNames(select);
        int startrow = 0, endrow = 0;
        if (page > 0 || pagesize > 0) {
            startrow = page * pagesize + 1; //int page starts from 0, int startrow starts from 1
            endrow = (page + 1) * pagesize; //last row of page
        }
        return new Vector[]{headers, getRows(select, headers.size(), startrow, endrow)};
    }

    private Vector getRows(String select, int cols, int startrow, int endrow) throws RemoteException {
        Vector rows = new Vector();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String pagedSelect;
            if (select.toUpperCase().indexOf(" LIMIT ") > 0 || (startrow == 0 && endrow == 0)) {
                pagedSelect = select;
            } else {
                pagedSelect = select.replaceFirst("select", "SELECT").replaceAll("Select", "SELECT");
                pagedSelect += (" LIMIT " + (startrow - 1) + "," + (endrow - startrow + 1));
            }
            Vector line;
            ps = connection.prepareStatement(pagedSelect);
            rs = ps.executeQuery();
            while (rs.next()) {
                line = new Vector();
                for (int c = 0; c < cols; c++) {
                    String ceil = rs.getString(c + 1);
                    ceil = ceil == null ? "" : ceil;
                    line.add(ceil);
                }
                rows.add(line);
            }
            return rows;
        } catch (SQLException ex) {
            throw new java.rmi.RemoteException(ex.getMessage());
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException se1) {
            } finally {
                try {
                    if (ps != null) {
                        ps.close();
                    }
                } catch (SQLException se2) {
                }
            }
        }
    }

    public Vector getColNames(String select) throws RemoteException {
        String original = null;
        Vector colNames = new Vector();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            int i;
            int bracesLevel = 0;
            StringBuffer sb = null;
            for (i = 0; i < select.length(); i++) {
                char c = select.charAt(i);
                if (c == '(') {
                    bracesLevel++;
                } else if (c == ')') {
                    bracesLevel--;
                } else if (bracesLevel == 0 && select.substring(i).toUpperCase().startsWith("WHERE ")) {
                    if (sb == null) {
                        original = select;
                        sb = new StringBuffer(select);
                    }
                    sb.insert(i + 6, "1=0 AND ");
                    break;
                }
            }
            if (sb != null) {
                select = sb.toString();
            }
            ps = connection.prepareStatement(select);
            rs = ps.executeQuery();
            ResultSetMetaData md = rs.getMetaData();
            for (i = 0; i < md.getColumnCount(); i++) {
                colNames.add(md.getColumnLabel(i + 1));
            }
        } catch (SQLException ex) {
            throw new java.rmi.RemoteException(ex.getMessage());
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException se1) {
            } finally {
                try {
                    if (ps != null) {
                        ps.close();
                    }
                } catch (SQLException se2) {
                }
            }
        }
        return colNames;
    }

    @Override
    public int getCount(String select) throws RemoteException {
        StringBuffer slct;
        int count = 0;
        int p = select.toLowerCase().lastIndexOf("order by");
        slct = new StringBuffer("select count(*) from (" + select.substring(0, p > 0 ? p : select.length()) + ") intab");
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = connection.prepareStatement(slct.toString());
            rs = ps.executeQuery();
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (SQLException ex) {
            throw new java.rmi.RemoteException(ex.getMessage());
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException se1) {
            } finally {
                try {
                    if (ps != null) {
                        ps.close();
                    }
                } catch (SQLException se2) {
                }
            }
        }
        return count;
    }

    @Override
    public boolean truncateTable(String tableName) throws RemoteException {
        boolean ok = false;
        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement("truncate " + tableName);
            ok = ps.execute();
        } catch (SQLException ex) {
            throw new java.rmi.RemoteException(ex.getMessage());
        } finally {
            try {
                ps.close();
            } catch (Exception e) {
            }
        }
        return ok;
    }

    @Override
    public void startTransaction(String transactionName) throws RemoteException {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void commitTransaction() throws RemoteException {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void rollbackTransaction(String transactionName) throws RemoteException {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getServerVersion() throws RemoteException {
        return "0.1";
    }

    public void callProcedure(String procName) throws RemoteException {
        PreparedStatement ps = null;
        try {
            CallableStatement cStmt = connection.prepareCall("{call " + procName + "()}");
            cStmt.execute();
        } catch (SQLException ex) {
            throw new java.rmi.RemoteException(ex.getMessage());
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException se2) {
            }
        }
    }
}
