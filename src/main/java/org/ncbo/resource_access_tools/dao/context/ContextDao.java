package org.ncbo.resource_access_tools.dao.context;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;
import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;
import org.ncbo.resource_access_tools.dao.AbstractObrDao;
import org.ncbo.resource_access_tools.util.MessageUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * This class is a representation for the the OBR DB OBR_PGDI_CTX table. The table contains
 * the following columns:
 * <p/>
 * <ul>
 * <li> id 			        SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 * <li> name  	            VARCHAR(100) NOT NULL UNIQUE,
 * <li> weight  	            FLOAT NOT NULL ,
 * <li> static_ontology_id            VARCHAR(XX) NOT NULL. 		//This attribute is used when the context is an existing annotation. Then it is the static ontology ID to which this annot refers to.
 * </ul>
 *
 * @author Adrien Coulet
 * @version OBR_v0.2
 * @created 12-Nov-2008
 */
public class ContextDao extends AbstractObrDao {

    private static final String TABLE_SUFFIX = MessageUtils.getMessage("obr.context.table.suffix");

    private static PreparedStatement addEntryStatement;
    private static PreparedStatement getContextIDByContextNameStatement;

    private ContextDao() {
        super(EMPTY_STRING, TABLE_SUFFIX);
    }

    @Override
    protected String creationQuery() {
        return "CREATE TABLE " + getTableSQLName() + " (" +
                "id SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(100) NOT NULL UNIQUE, " +
                "weight DOUBLE default '1.0', " +
                "static_ontology_id VARCHAR(20) default '0'" +
                ")ENGINE=MyISAM DEFAULT CHARSET=latin1;";
    }

    @Override
    protected void openPreparedStatements() {
        super.openPreparedStatements();
        this.openAddEntryStatement();
        this.openGetContextIDByContextNameStatement();
    }

    @Override
    protected void closePreparedStatements() throws SQLException {
        super.closePreparedStatements();
        addEntryStatement.close();
        getContextIDByContextNameStatement.close();
    }

    private static class ContexDaoHolder {
        private final static ContextDao CONTEXT_DAO_INSTANCE = new ContextDao();
    }

    /**
     * Returns a ContextDao object by creating one if a singleton not already exists.
     */
    public static ContextDao getInstance() {
        return ContexDaoHolder.CONTEXT_DAO_INSTANCE;
    }

    /******************************************
     * FUNCTIONS ON THE TABLE
     ***************************/

    @Override
    protected void openAddEntryStatement() {
        StringBuffer queryb = new StringBuffer();
        queryb.append("INSERT INTO ");
        queryb.append(this.getTableSQLName());
        queryb.append(" (name, weight, static_ontology_id) VALUES (?,?,?)");
        addEntryStatement = this.prepareSQLStatement(queryb.toString());
    }

    /**
     * Add an new entry in corresponding SQL table.
     *
     * @return True if the entry was added to the SQL table, false if a problem occurred during insertion.
     */
    public boolean addEntry(ContextEntry entry) {
        boolean inserted = false;
        try {
            addEntryStatement.setString(1, entry.getContextName());
            addEntryStatement.setDouble(2, entry.getContextWeight());
            addEntryStatement.setString(3, entry.getContextOnto());
            this.executeSQLUpdate(addEntryStatement);
            inserted = true;
        } catch (MySQLNonTransientConnectionException e) {
            this.openAddEntryStatement();
            return this.addEntry(entry);
        } catch (MySQLIntegrityConstraintViolationException e) {
            //logger.error("Table " + this.getTableSQLName() + " already contains an entry for the concept: " + entry.getLocalConceptID() +".");
        } catch (SQLException e) {
            logger.error("** PROBLEM ** Cannot add an entry on table " + this.getTableSQLName(), e);
            logger.error(entry.toString());
        }
        return inserted;
    }

    private void openGetContextIDByContextNameStatement() {
        StringBuffer queryb = new StringBuffer();
        queryb.append("SELECT id FROM ");
        queryb.append(this.getTableSQLName());
        queryb.append(" WHERE ");
        queryb.append(this.getTableSQLName());
        queryb.append(".name=?");
        queryb.append(";");
        getContextIDByContextNameStatement = this.prepareSQLStatement(queryb.toString());
    }

    public int getContextIDByContextName(String contextName) {
        int contextID = -1;
        try {
            getContextIDByContextNameStatement.setString(1, contextName);
            ResultSet rSet = this.executeSQLQuery(getContextIDByContextNameStatement);
            rSet.first();
            contextID = rSet.getInt(1);
            rSet.close();
        } catch (MySQLNonTransientConnectionException e) {
            this.openGetContextIDByContextNameStatement();
            return this.getContextIDByContextName(contextName);
        } catch (SQLException e) {
            logger.error("** PROBLEM ** Cannot get contextID from " + this.getTableSQLName() + " for " + contextName + ". -1 returned.", e);
        }
        return contextID;
    }

    /********************************* ENTRY CLASS *****************************************************/

    /**
     * This class is a representation for a OBR_CTX table entry.
     *
     * @author Adrien Coulet
     * @version OBR_v0.2
     * @created 12-Nov-2008
     */
    public static class ContextEntry {

        private String contextName;
        private Double contextWeight;
        private String contextOnto;

        public ContextEntry(String contextName, double contextWeight, String contextOnto) {
            super();
            this.contextName = contextName;
            this.contextWeight = contextWeight;
            this.contextOnto = contextOnto;
        }

        public String getContextName() {
            return contextName;
        }

        public Double getContextWeight() {
            return contextWeight;
        }

        public String getContextOnto() {
            return contextOnto;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("ContextEntry: [");
            sb.append(this.contextName);
            sb.append(", ");
            sb.append(this.contextWeight);
            sb.append(", ");
            sb.append(this.contextOnto);
            sb.append("]");
            return sb.toString();
        }
    }

    /**
     * This method loads obr_context table into memory.
     *
     * @param resourceID
     */
    public void loadTableIntoMemory(String resourceID) {
        callStoredProcedure("load_context_table_into_memory", resourceID);
        logger.info("\t" + this.getTableSQLName() + " loaded in to memory.");
    }
}
