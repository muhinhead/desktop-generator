package org.dbdesktop.orm;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Vector;

public interface IMessageSender extends Remote {
    DbObject[] getDbObjects(Class<? extends DbObject> dbobClass, String whereCondition,
                            String orderCondition) throws RemoteException;

    DbObject saveDbObject(DbObject dbob) throws RemoteException;

    void deleteObject(DbObject dbob) throws RemoteException;

    DbObject loadDbObjectOnID(Class<? extends DbObject> dbobClass, int id) throws RemoteException;

    Vector[] getTableBody(String select) throws RemoteException;

    Vector[] getTableBody(String select, int page, int pageSize) throws RemoteException;

    int getCount(String select) throws RemoteException;

    String getServerVersion() throws RemoteException;

    boolean truncateTable(String tableName) throws RemoteException;

    void commitTransaction() throws RemoteException;

    void startTransaction(String transactionName) throws RemoteException;

    void rollbackTransaction(String transactionName) throws RemoteException;

    void callProcedure(String procName) throws RemoteException;
}
