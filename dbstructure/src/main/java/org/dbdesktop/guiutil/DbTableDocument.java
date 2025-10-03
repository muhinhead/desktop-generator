package org.dbdesktop.guiutil;

import javax.swing.*;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DbTableDocument extends Document {

    private String selectStatement;
    private Object[] selectParams = null;
    private Vector rowData;
    private Vector colNames;
    private Connection connection;
    private static final String errDb = "Database error:";
    private String filterText = null;

    public DbTableDocument(String name, Object[] body) {
        super(name);
        setBody(body);
    }

    public DbTableDocument(String name, String select, Connection connection) {
        super(name, new Object[]{select, connection});
    }

    public DbTableDocument(String name, String select, Object[] parameters,
                           Connection connection) {
        super(name, new Object[]{select, connection, parameters});
        selectParams = parameters;
    }

    protected void initialize(Object initObject) {
        if (colNames == null) {
            colNames = new Vector();
        } else {
            colNames.clear();
        }
        PreparedStatement ps = null;
        ResultSet rs = null;
        Object[] arr = (Object[]) initObject;
        setSelectStatement((String) arr[0]);
        connection = (Connection) arr[1];
        try {
            ps = connection.prepareStatement(getSelectStatement());
            if (arr.length > 2) {
                selectParams = (Object[]) arr[2];
                int n = 1;
                for (Object param : selectParams) {
                    ps.setObject(n++, param);
                }
            }
            rs = ps.executeQuery();
            ResultSetMetaData md = rs.getMetaData();
            for (int i = 0; i < md.getColumnCount(); i++) {
                colNames.add(md.getColumnLabel(i + 1));
            }
        } catch (SQLException se) {
            JOptionPane.showMessageDialog(null, se.toString(), errDb, JOptionPane.ERROR_MESSAGE);
            Logger.getLogger(DbTableDocument.class.getName()).log(Level.SEVERE, null, se);
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

    protected Vector loadData() {
        Vector rows = new Vector();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Vector line;
            int i;
            ps = connection.prepareStatement(getSelectStatement());
            if (selectParams != null) {
                int n = 1;
                for (Object param : selectParams) {
                    ps.setObject(n++, param);
                }
            }
            rs = ps.executeQuery();
            boolean filtered = true;
            while (rs.next()) {
                line = new Vector();
                for (i = 1; i <= colNames.size(); i++) {
                    String ceil = rs.getString(i);
                    ceil = ceil == null ? "" : ceil;
                    if (filterText != null && filterText.trim().length() > 0) {
                        filtered = (ceil.toUpperCase().indexOf(filterText.toUpperCase()) >= 0);
                    }
                    line.add(ceil);
                }
                if (filtered) {
                    rows.add(line);
                }
            }
        } catch (SQLException se) {
            JOptionPane.showMessageDialog(null, se.toString(), errDb, JOptionPane.ERROR_MESSAGE);
            Logger.getLogger(DbTableDocument.class.getName()).log(Level.SEVERE, null, se);
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
        return rows;
    }

    public Object getBody() {
        if (rowData == null || rowData.size() == 0) {
            if (connection != null) {
                rowData = loadData();
            }
        }
        Object[] content = new Object[]{colNames, rowData};
        return content;
    }

    public void setBody(String select, Object[] params) {
        initialize(new Object[]{select, connection, params});
        rowData = loadData();
    }

    public void setBody(Object body) {
        if (body instanceof String) {
            initialize(new Object[]{body, connection});
            rowData = loadData();
        } else {
            Object[] content = (Object[]) body;
            colNames = (Vector) content[0];
            rowData = (Vector) content[1];
        }
    }

    public void generateCSV(File expFile) throws Exception {
        if (expFile == null) {
            return;
        }
        Object[] content = (Object[]) getBody();
        BufferedOutputStream bufferedOutput = null;
        try {
            Vector tds = (Vector) content[0];
            Vector lines = (Vector) content[1];
            bufferedOutput = new BufferedOutputStream(new FileOutputStream(expFile));
            int col = 0;
            for (Object td : tds) {
                if (col > 0) {
                    bufferedOutput.write(", ".getBytes());
                }
                bufferedOutput.write(("\"" + td.toString().replaceAll("\"", "\"\"") + "\"").getBytes());
                col++;
            }
            bufferedOutput.write("\n".getBytes());
            for (Object l : lines) {
                Vector line = (Vector) l;
                col = 0;
                for (Object c : line) {
                    if (col > 0) {
                        bufferedOutput.write(", ".getBytes());
                    }
                    bufferedOutput.write("\"".getBytes());
                    bufferedOutput.write(c.toString().replaceAll("\"", "\"\"").getBytes());
                    bufferedOutput.write("\"".getBytes());
                    col++;
                }
                bufferedOutput.write("\n".getBytes());
            }
        } finally {
            if (bufferedOutput != null) {
                try {
                    bufferedOutput.flush();
                    bufferedOutput.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    public void generateHTML(File expFile) throws Exception {
        generateHTML(expFile, null);
    }

    public void generateHTML(File expFile, String header) throws Exception {
        if (expFile == null) {
            return;
        }
        Object[] content = (Object[]) getBody();
        BufferedOutputStream bufferedOutput = null;
        try {
            Vector tds = (Vector) content[0];
            Vector lines = (Vector) content[1];
            bufferedOutput = new BufferedOutputStream(new FileOutputStream(expFile));
            bufferedOutput.write("<html>\n".getBytes());
            bufferedOutput.write("<head>\n".getBytes());
            if (header!=null) {
                bufferedOutput.write("<center><H2>".getBytes());
                bufferedOutput.write(header.getBytes());
                bufferedOutput.write("</H2></center>".getBytes());
            }
            bufferedOutput.write("</head>\n".getBytes());
            bufferedOutput.write(("<style type=\"text/css\">"
                    + "table.mystyle"
                    + "{"
                    + "border-width: 1 1 1px 1px;"
                    + "border-spacing: 0;"
                    + "border-collapse: collapse;"
                    + "border-style: solid;"
                    + "}"
                    + ".mystyle td, .mystyle th"
                    + "{"
                    + "margin: 0;"
                    + "padding: 4px;"
                    + "border-width: 1px 1px 1 1;"
                    + "border-style: solid;"
                    + "}"
                    + "</style>\n").getBytes());
            bufferedOutput.write("<body>\n".getBytes());
            bufferedOutput.write("<table class=\"mystyle\">\n".getBytes());
            bufferedOutput.write("<tr>\n".getBytes());
            for (Object td : tds) {
                bufferedOutput.write(("<th>" + td.toString() + "</th>\n").getBytes());
            }
            bufferedOutput.write("</tr>\n".getBytes());
            for (Object l : lines) {
                bufferedOutput.write("<tr>\n".getBytes());
                Vector line = (Vector) l;
                for (Object c : line) {
                    bufferedOutput.write("<td>\n".getBytes());
                    bufferedOutput.write(c.toString().getBytes());
                    bufferedOutput.write("</td>\n".getBytes());
                }
                bufferedOutput.write("</tr>\n".getBytes());
            }
            bufferedOutput.write("</table>\n".getBytes());
            bufferedOutput.write("</body></html>\n".getBytes());
        } finally {
            if (bufferedOutput != null) {
                try {
                    bufferedOutput.flush();
                    bufferedOutput.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    public String getCeil(int row, int col) {
        Vector line = (Vector) rowData.get(row);
        return (String) line.get(col);
    }

    public String getSelectStatement() {
        return selectStatement;
    }

    public Vector getRowData() {
        return rowData;
    }

    public void setFilter(String text) {
        filterText = text;
    }

    /**
     * @param selectStatement the selectStatement to set
     */
    public void setSelectStatement(String selectStatement) {
        this.selectStatement = selectStatement;
    }
}
