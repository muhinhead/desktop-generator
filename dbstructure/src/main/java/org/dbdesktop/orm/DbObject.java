package org.dbdesktop.orm;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static java.lang.Integer.parseInt;

@SuppressWarnings("MagicConstant")
public abstract class DbObject implements Serializable {

    private transient Connection connection = null;
    private String tableName = null;
    private String idColumnName = null;
    private String[] columnNames = null;
    private boolean isNew = true;
    private boolean wasChanged = false;
    protected static String delimiter = "\t";

    public DbObject(Connection connection) {
        this.setConnection(connection);
    }

    public DbObject(Connection connection, String tableName, String idColumnName) {
        this.setConnection(connection);
        this.setTableName(tableName);
        this.setIdColumnName(idColumnName);
    }

    public abstract DbObject loadOnId(int id) throws SQLException, ForeignKeyViolationException;

    protected abstract void insert() throws SQLException, ForeignKeyViolationException;

    public abstract void save() throws SQLException, ForeignKeyViolationException;

    public abstract void delete() throws SQLException, ForeignKeyViolationException;

    public abstract boolean isDeleted();

    public abstract Integer getPK_ID();

    public abstract void setPK_ID(Integer id) throws ForeignKeyViolationException;

    public static String capitalizedString(String s) {
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }

    public static DbObject[] load(Connection con, String whereCondition, String orderCondition)
            throws SQLException {
        return null;
    }

    public static boolean exists(Connection con, String whereCondition) throws SQLException {
        return false;
    }

    public String getHeaderLine() {
        String result = "";
        for (int i = 0; i < getColumnNames().length; i++) {
            if (i > 0) {
                result += getDelimiter();
            }
            result += getColumnNames()[i];
        }
        return result;
    }

    public abstract Object[] getAsRow();

    public String toString() {
        Object[] objRow = getAsRow();
        StringBuilder row = new StringBuilder();
        for (Object fld : objRow) {
            if (!row.isEmpty()) {
                row.append(delimiter);
            }
            row.append(fld == null ? " " : fld.toString());
        }
        return row.toString();
    }

    public abstract void fillFromString(String row) throws ForeignKeyViolationException, SQLException;

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getIdColumnName() {
        return idColumnName;
    }

    public void setIdColumnName(String idColumnName) {
        this.idColumnName = idColumnName;
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(String[] columnNames) {
        this.columnNames = columnNames;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public boolean isWasChanged() {
        return wasChanged;
    }

    public void setWasChanged(boolean wasChanged) {
        this.wasChanged = wasChanged;
    }

    public static String getDelimiter() {
        return delimiter;
    }

    public static void setDelimiter(String delimiter) {
        DbObject.delimiter = delimiter;
    }

    public static Date toDate(String yearMonthDay) {
        Calendar cal = Calendar.getInstance();
        cal.set(parseInt(yearMonthDay.substring(0, 4)), //year
                parseInt(yearMonthDay.substring(5, 7)), //month
                parseInt(yearMonthDay.substring(9, 11))); //sec
        return new Date(cal.getTimeInMillis());
    }

    public static Timestamp toTimeStamp(String yearMonthDayHourMinuteSecond) {
        Calendar cal = Calendar.getInstance();
        cal.set(parseInt(yearMonthDayHourMinuteSecond.substring(0, 4)), //year
                parseInt(yearMonthDayHourMinuteSecond.substring(5, 7)), //month
                parseInt(yearMonthDayHourMinuteSecond.substring(9, 11)), //day
                parseInt(yearMonthDayHourMinuteSecond.substring(13, 15)), //hour of day
                parseInt(yearMonthDayHourMinuteSecond.substring(17, 19)), //minute
                parseInt(yearMonthDayHourMinuteSecond.substring(21, 23))); //sec
        return new Timestamp(cal.getTimeInMillis());
    }

    public static String[] splitStr(String line, String delimiter) {
        List<String> list = split(line, delimiter.charAt(0));
        String[] ans = new String[list.size()];
        for (int i = 0; i < ans.length; i++) {
            ans[i] = list.get(i);
        }
        return ans;
    }

    public static List<String> split(String line, char delimiter) {
        List<String> result = new ArrayList<>();
        if (line == null || line.isEmpty()) {
            return result;
        }

        int start = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == delimiter) {
                result.add(line.substring(start, i));
                start = i + 1;
            }
        }
        result.add(line.substring(start));
        return result;
    }
}