package org.ncbo.resource_access_tools.dao;

import com.mysql.jdbc.CommunicationsException;
import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;
import org.apache.log4j.Logger;
import org.ncbo.resource_access_tools.util.MessageUtils;
import org.ncbo.resource_access_tools.util.helper.StringHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.HashSet;

/**
 * This abstract class is a representation for a DB table for OBS.
 * It is an interface for a SQL table in the OBS DB. <p>
 * This class contains also basic functions to set up the DB connection.<p>
 * <p/>
 * The creation of a Table open a set of prepared statements that are used to query the table.
 * These prepared statements are closed when the tool is destructed.
 * All the subclasses of this class share the same DB connexion.
 * <p/>
 * ParametersBean to connect to the MySQL server should be in a String[3] array such as:
 * {"jdbc:mysql://ncbo-db2.stanford.edu:3306/obs-schema-name","login","password"}
 *
 * @author Clement Jonquet
 * @version OBS_v1
 * @created 20-Aug-2008
 */
public abstract class AbstractObrDao implements DaoFactory, StringHelper {

    protected static final Logger logger = Logger.getLogger(AbstractObrDao.class);

    protected static final String OBR_PREFIX = MessageUtils.getMessage("obr.tables.prefix");
    private static final String OBS_PREFIX = MessageUtils.getMessage("obs.tables.prefix");
    private static final String OBR_MEMORY_SUFFIX = MessageUtils.getMessage("obr.memory.table.suffix");

    // Database connection properties.
    private static final String DATABASE_CONNECTION_STRING = MessageUtils.getMessage("obr.jdbc.url");
    private static final String DATABASE_JDBC_DRIVER = MessageUtils.getMessage("obr.jdbc.driver");
    private static final String DATABASE_USER = MessageUtils.getMessage("obr.jdbc.username");
    private static final String DATABASE_PASSWORD = MessageUtils.getMessage("obr.jdbc.password");
    private final String tableSQLName;

    private String resourceID;

    private static Connection tableConnection;
    private static Statement tableStatement;

    private PreparedStatement numberOfEntryStatement;

    // ***** log file to log the SQL update statement
    private static BufferedWriter sqlLogBuffer;
    private static File sqlLogFile;

    protected static final HashSet<String> EMPTY_SET = new HashSet<String>();

    /**
     * Construct a new Table object and the corresponding DB table if does not exists.
     * Uses the SQL code returned by {@see creationQuery()} to create the table.
     */
    protected AbstractObrDao(String resourceID, String suffix) {
        this.createConnection();
        this.resourceID = resourceID;
        this.tableSQLName = OBR_PREFIX + resourceID.toLowerCase() + suffix;
        if (!this.exist(this.getTableSQLName())) {
            try {
                //logger.info(this.creationQuery());
                this.executeSQLUpdate(this.creationQuery());
            } catch (SQLException e) {
                logger.error("** PROBLEM ** Cannot create SQL table " + this.getTableSQLName(), e);
            }
        }
        this.openPreparedStatements();
    }

    /**
     * @param tableName
     */
    private AbstractObrDao(String tableName) {
        this.createConnection();
        this.tableSQLName = tableName;
        if (!this.exist(this.getTableSQLName())) {
            try {
                logger.info(this.creationQuery());
                this.executeSQLUpdate(this.creationQuery());
            } catch (SQLException e) {
                logger.error("** PROBLEM ** Cannot create SQL table " + this.getTableSQLName(), e);
            }
        }
        this.openPreparedStatements();
    }


    private void createConnection() {
        if (tableConnection == null) {
            try {
                Class.forName(DATABASE_JDBC_DRIVER).newInstance();

                tableConnection = DriverManager.getConnection(DATABASE_CONNECTION_STRING, DATABASE_USER, DATABASE_PASSWORD);
            } catch (Exception e) {
                logger.error("** PROBLEM ** Cannot create connection to database " + DATABASE_CONNECTION_STRING, e);
            }
        }
    }

    protected void openPreparedStatements() {
        this.openNumberOfEntryStatement();
    }

    protected void closePreparedStatements() throws SQLException {
        try {
            this.numberOfEntryStatement.close();
        } catch (SQLException e) {
            logger.error("** PROBLEM ** Cannot close one of the prepared statements of the table " + this.getTableSQLName() + ".", e);
        }
    }

    /**
     * Returns the SQL code needed to create the table in the SQL DB.
     */
    protected abstract String creationQuery();

    /**
     * Open the addEntryStatement. Must specify the INSERT query for the table.
     */
    protected abstract void openAddEntryStatement() throws SQLException;

    public void finalize() throws Throwable {
        super.finalize();
        //this.closePreparedStatements();
        //tableStatement.close();
        // Remove March 2009: if we close the connection other object might be affected.
        //tableConnection.close();
        if (AbstractObrDao.sqlLogBuffer != null) {
            AbstractObrDao.sqlLogBuffer.close();
        }
    }

    public String getTableSQLName() {
        return this.tableSQLName;
    }

    public String getMemoryTableSQLName() {
        return (this.tableSQLName + OBR_MEMORY_SUFFIX).replace(OBS_PREFIX, OBR_PREFIX);
    }

    public String getTempTableSQLName() {
        return this.tableSQLName + "_temp";
    }

    public static Connection getTableConnection() {
        return tableConnection;
    }

    public static Statement getTableStatement() {
        return tableStatement;
    }

    public static File getSqlLogFile() {
        return sqlLogFile;
    }

    public static void setSqlLogFile(File sqlLogFile) throws IOException {
        if (sqlLogFile == null) {
            AbstractObrDao.sqlLogBuffer.close();
        } else {
            AbstractObrDao.sqlLogFile = sqlLogFile;
            AbstractObrDao.sqlLogBuffer = new BufferedWriter(new FileWriter(AbstractObrDao.sqlLogFile));
        }
    }

    /**
     * Executes the given SQL query with the table generic statement and returns the number of row in the table.
     */
    protected long executeSQLUpdate(String query) throws SQLException {
        long nbRow;
        try {
            tableStatement = tableConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            nbRow = tableStatement.executeUpdate(query);
            try {
                if (AbstractObrDao.sqlLogFile != null) {
                    AbstractObrDao.sqlLogBuffer.write(query);
                    AbstractObrDao.sqlLogBuffer.newLine();
                    AbstractObrDao.sqlLogBuffer.flush();
                }
            } catch (IOException e) {
                logger.error("** PROBLEM ** Cannot write SQL log file BufferedWritter. ");
            }
        } catch (CommunicationsException e) {
            reOpenConnectionIfClosed();
            tableStatement = tableConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            nbRow = tableStatement.executeUpdate(query);
        }
        tableStatement.close();
        return nbRow;
    }

    /**
     * Executes the given SQL query with the table generic statement and returns the number of row in the table.
     */
    protected long executeWithStoreProcedure(String tableName, String query, boolean disableKeys) throws SQLException {
        long nbRow = 0;
        try {
            CallableStatement callableStatement = tableConnection.prepareCall("CALL common_batch_insert(?,?, ?, ?)");
            callableStatement.setString(1, tableName);
            callableStatement.setString(2, query);
            callableStatement.setBoolean(3, disableKeys);
            callableStatement.registerOutParameter(4, java.sql.Types.BIGINT);
            callableStatement.execute();
            nbRow = callableStatement.getLong(4);

            try {
                if (AbstractObrDao.sqlLogFile != null) {
                    AbstractObrDao.sqlLogBuffer.write(query);
                    AbstractObrDao.sqlLogBuffer.newLine();
                    AbstractObrDao.sqlLogBuffer.flush();
                }
            } catch (IOException e) {
                logger.error("** PROBLEM ** Cannot write SQL log file BufferedWritter. ");
            }
        } catch (CommunicationsException e) {
            reOpenConnectionIfClosed();
        }
        return nbRow;
    }

    /**
     * Call the store procedure LoadObsSlaveTablesIntoMemeory
     */
    public void callLoadObsSlaveTablesIntoMemoryProcedure() throws SQLException {
        try {
            CallableStatement callableStatement = tableConnection.prepareCall("CALL load_obs_tables_into_memory();");
            callableStatement.execute();

            try {
                if (AbstractObrDao.sqlLogFile != null) {
                    AbstractObrDao.sqlLogBuffer.write("call load_obs_tables_into_memory();");
                    AbstractObrDao.sqlLogBuffer.newLine();
                    AbstractObrDao.sqlLogBuffer.flush();
                }
            } catch (IOException e) {
                logger.error("** PROBLEM ** Cannot write SQL log file BufferedWritter. ");
            }
        } catch (SQLException e) {
            logger.error("Problem in calling LoadObsSlaveTablesIntoMemeoryProcedure", e);
            throw e;
        }
    }

    /**
     * @param paramaters
     */
    protected void callStoredProcedure(String... paramaters) {
        try {
            StringBuffer callSPQuery = new StringBuffer();
            callSPQuery.append("CALL ");
            callSPQuery.append("enable_indexes");
            callSPQuery.append("(");
            if (paramaters != null && paramaters.length > 0) {
                for (String parameter : paramaters) {
                    callSPQuery.append("'");
                    callSPQuery.append(parameter);
                    callSPQuery.append("', ");
                }
                callSPQuery.delete(callSPQuery.length() - 2, callSPQuery.length());
            }
            callSPQuery.append(" );");

            CallableStatement callableStatement = tableConnection.prepareCall(callSPQuery.toString());
            callableStatement.execute();

            try {
                if (AbstractObrDao.sqlLogFile != null) {
                    AbstractObrDao.sqlLogBuffer.write(callSPQuery.toString());
                    AbstractObrDao.sqlLogBuffer.newLine();
                    AbstractObrDao.sqlLogBuffer.flush();
                }
            } catch (IOException e) {
                logger.error("** PROBLEM ** Cannot write SQL log file BufferedWritter. ");
            }
        } catch (SQLException e) {
            logger.error("Problem in calling stored procedure " + "enable_indexes", e);
        }


    }

    /**
     * Executes the SQL query on the given prepared statement and returns the number of row in the table.
     *
     * @param stmt
     * @return
     * @throws SQLException
     */
    protected long executeSQLUpdate(PreparedStatement stmt) throws SQLException {
        long nbRow;
        try {
            nbRow = stmt.executeUpdate();
            try {
                if (AbstractObrDao.sqlLogFile != null) {
                    AbstractObrDao.sqlLogBuffer.write(stmt.toString());
                    AbstractObrDao.sqlLogBuffer.newLine();
                    AbstractObrDao.sqlLogBuffer.flush();
                }
            } catch (IOException e) {
                logger.error("** PROBLEM ** Cannot write SQL log file BufferedWritter. ");
            }
        } catch (CommunicationsException e) {
            reOpenConnectionIfClosed();
            // Re-calling the execution will generate a MySQLNonTransientConnectionException
            // Those exceptions are catched in each functions to re-execute the query correctly.
            nbRow = stmt.executeUpdate();
        }
        return nbRow;
    }

    protected long executeSQLBatchUpdate(PreparedStatement stmt) throws SQLException {
        int[] nbRow;
        try {
            nbRow = stmt.executeBatch();
            try {
                if (AbstractObrDao.sqlLogFile != null) {
                    AbstractObrDao.sqlLogBuffer.write(stmt.toString());
                    AbstractObrDao.sqlLogBuffer.newLine();
                    AbstractObrDao.sqlLogBuffer.flush();
                }
            } catch (IOException e) {
                logger.error("** PROBLEM ** Cannot write SQL log file BufferedWritter. ");
            }
        } catch (CommunicationsException e) {
            reOpenConnectionIfClosed();
            // Re-calling the execution will generate a MySQLNonTransientConnectionException
            // Those exceptions are catched in each functions to re-execute the query correctly.
            nbRow = stmt.executeBatch();
        }

        if (nbRow == null || nbRow.length == 0) {
            return 0;

        }
        long updatedRows = 0;
        for (int i : nbRow) {
            updatedRows += i;
        }

        return updatedRows;

    }

    /**
     * Executes a given SQL query as String using a generic statement. As it returns a ResultSet,
     * this statement needs to be explicitly closed after the processing of the ResultSet with function
     * {see closeTableStatement()}.
     */
    protected ResultSet executeSQLQuery(String query) throws SQLException {
        ResultSet rSet;
        try {
            tableStatement = tableConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rSet = tableStatement.executeQuery(query);
        } catch (CommunicationsException e) {
            reOpenConnectionIfClosed();
            tableStatement = tableConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rSet = tableStatement.executeQuery(query);
        }
        //logger.info("query: " + query);
        return rSet;
    }

    /**
     * Executes a given SQL query as String using a generic statement.
     * Fetch resultset row by row (cf. MySQL JDBC driver doc).
     * As it returns a ResultSet, this statement needs to be explicitly closed after the processing of the ResultSet with function
     * {see closeTableStatement()}.
     * Attention, no other use of the connection must be done before the generic table statement to be closed.
     */
    protected ResultSet executeSQLQueryWithFetching(String query) throws SQLException {
        ResultSet rSet;
        try {
            tableStatement = tableConnection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            tableStatement.setFetchSize(Integer.MIN_VALUE);
            rSet = tableStatement.executeQuery(query);
        } catch (CommunicationsException e) {
            reOpenConnectionIfClosed();
            tableStatement = tableConnection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            tableStatement.setFetchSize(Integer.MIN_VALUE);
            rSet = tableStatement.executeQuery(query);
        }
        //logger.info("query: " + query);
        return rSet;
    }

    /**
     * Executes the SQL query on the given prepared statement. As it returns a ResultSet,
     * this statement needs to be explicitly closed after the processing of the ResultSet.
     */
    protected ResultSet executeSQLQuery(PreparedStatement stmt) throws SQLException {
        ResultSet rSet;
        try {
            rSet = stmt.executeQuery();
        } catch (CommunicationsException e) {
            reOpenConnectionIfClosed();
            // Re-calling the execution will generate a MySQLNonTransientConnectionException
            // Those exceptions are catched in each functions to re-execute the query correctly.
            rSet = stmt.executeQuery();
        }
        return rSet;
    }

    /**
     * Open a prepared statement that corresponds to the given SQL query.
     */
    protected PreparedStatement prepareSQLStatement(String query) {
        try {
            return tableConnection.prepareStatement(query);
        } catch (SQLException e) {
            logger.error("** PROBLEM ** Cannot open prepared statement for query: " + query + " of the table " + this.getTableSQLName() + ". Null returned.", e);
            return null;
        }
    }

    /**
     * Explicitly closes the generic statement of the Table.
     */
    protected void closeTableGenericStatement() throws SQLException {
        tableStatement.close();
    }

    /**
     * Reopens the DB connection if closed and reopens all the prepared statement for all instances of sub-classes.
     */
    private static void reOpenConnectionIfClosed() {
        try {
            if (tableConnection.isClosed()) {
                tableConnection = DriverManager.getConnection(DATABASE_CONNECTION_STRING, DATABASE_USER, DATABASE_PASSWORD);
                logger.info("\t[SQL Connection just reopenned.]");
            }
        } catch (SQLException e) {
            logger.error("** PROBLEM ** Cannot create connection to database " + DATABASE_CONNECTION_STRING, e);
        }
    }

    /**
     * Returns true if the table already exists in the DB.
     */
    private boolean exist(String tableName) {
        boolean exist;
        try {
            DatabaseMetaData dmd = tableConnection.getMetaData();
            ResultSet tables = dmd.getTables(tableConnection.getCatalog(), null, tableName, null);
            exist = tables.next();
            tables.close();
        } catch (SQLException e) {
            logger.error("** PROBLEM ** Cannot determin if table " + tableName + "exists.", e);
            exist = false;
        }
        return exist;
    }

    /**
     * Reinitializes the corresponding SQL table by dropping it and recreating it (with function creationQuery ).
     */
    protected void reInitializeSQLTable() {
        String dropQuery = "DROP TABLE " + this.getTableSQLName() + ";";
        if (this.exist(this.getTableSQLName())) {
            try {
                this.executeSQLUpdate(dropQuery);
                this.executeSQLUpdate(this.creationQuery());
                //this.alterSQLElementTable();
            } catch (SQLException e) {
                logger.error("** PROBLEM ** Cannot drop Table " + this.getTableSQLName(), e);
            }
        }
    }

    /***********************************
     * FUNCTIONS ON THE TABLES
     *****************************/

    private void openNumberOfEntryStatement() {
        String query = "SELECT COUNT(*) FROM " + this.getTableSQLName() + ";";
        this.numberOfEntryStatement = this.prepareSQLStatement(query);
    }

    /**
     * Returns the number of elements in the table (-1 if a problem occurs during the count).
     */
    public long numberOfEntry() {
        long nbEntry = -1;
        try {
            ResultSet rSet = this.executeSQLQuery(numberOfEntryStatement);
            rSet.first();
            nbEntry = rSet.getLong(1);
            rSet.close();
        } catch (MySQLNonTransientConnectionException e) {
            this.openNumberOfEntryStatement();
            return this.numberOfEntry();
        } catch (SQLException e) {
            logger.error("** PROBLEM ** Cannot get number of entry on table " + this.getTableSQLName() + ". -1 returned.", e);
        }
        return nbEntry;
    }

    /**
     * Disabled All indexes for given table.
     *
     * @return true if successful
     */
    public boolean disableIndexes() {
        try {
            this.executeSQLUpdate("ALTER TABLE " + this.getTableSQLName() + " DISABLE KEYS;");
            return true;
        } catch (SQLException e) {
            logger.error("** PROBLEM ** Cannot disables indexes for table " + this.getTableSQLName(), e);
        }

        return false;
    }

    /**
     * Enabled All indexes for given table.
     *
     * @return true if successful
     */
    public boolean enableIndexes(boolean bigTable) {
        try {
            if (bigTable) {
                this.callStoredProcedure(this.getTableSQLName(), "1");
            } else {
                this.callStoredProcedure(this.getTableSQLName(), "0");
            }
            return true;
        } catch (Exception e) {
            logger.error("** PROBLEM ** Cannot enable indexes for table " + this.getTableSQLName(), e);
        }
        return false;
    }

    /**
     * Set session variable myisam_repair_threads
     *
     * @return true if successful
     */
    public boolean setMyisamRepairThreads(int numberOfThreads) {
        try {
            this.executeSQLUpdate("SET SESSION myisam_repair_threads = " + numberOfThreads + ";");
            return true;
        } catch (SQLException e) {
            logger.error("** PROBLEM ** Cannot set session variable 'myisam_repair_threads'.", e);
        }

        return false;
    }
}
