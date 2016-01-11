package org.ncbo.resource_access_tools.dao.element;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;
import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;
import com.mysql.jdbc.exceptions.MySQLSyntaxErrorException;
import org.ncbo.resource_access_tools.dao.AbstractObrDao;
import org.ncbo.resource_access_tools.populate.Element;
import org.ncbo.resource_access_tools.populate.Structure;
import org.ncbo.resource_access_tools.util.MessageUtils;
import org.ncbo.resource_access_tools.util.StringUtilities;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

/**
 * This class is a generic representation for OBR element table OBR_XX_ET table. The table contains
 * the following columns:
 * <p/>
 * <ul>
 * <li> id 			            INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 * <li> local_element_id  	            VARCHAR(255) NOT NULL UNIQUE,
 * <li> dictionary_id 	            	SMALLINT UNSIGNED FOREIGN KEY,
 * <li> CHANGING:: contextIDText1       TEXT,
 * <li> CHANGING:: contextIDText2       TEXT,
 * <li> etc.
 * </ul>
 * <p/>
 * DictionaryID specifies the latest version (ordered) of the dictionary used to annotate the element.
 * Before the element is annotated, this field is NULL.
 *
 * @author Adrien Coulet, Clement Jonquet
 * @version OBR_v0.2
 * @created 20-Nov-2008
 */
public class ElementDao extends AbstractObrDao {

    private static final String TABLE_SUFFIX = MessageUtils.getMessage("obr.element.table.suffix");

    /**
     * Suffix used for temporary table
     */
    private static final String TEMP_TABLE_SUFFIX = MessageUtils.getMessage("obr.temp.element.table.suffix");

    private final ArrayList<String> contextNames;

    private PreparedStatement addEntryStatement;
    private PreparedStatement getAllLocalElementIDsStatement;

    /**
     * Creates a new elementTable with a given resourceID and a resource structure.
     * The suffix that will be added for AnnotationTable is "_ET".
     * This constructor is used for the population and the update of the element tables (OBR_XX_ET)
     */
    public ElementDao(String resourceID, Structure structure) {
        super(resourceID, TABLE_SUFFIX);
        this.contextNames = structure.getContextNames();
        this.alterElementTable();
        this.openAddEntryStatement();
    }

    /**
     * Returns the SQL table name for a given resourceID
     */
    public static String name(String resourceID) {
        return OBR_PREFIX + resourceID.toLowerCase() + TABLE_SUFFIX;
    }

    @Override
    protected String creationQuery() {
        return "CREATE TABLE " + this.getTableSQLName() + " (" +
                "id INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                "local_element_id VARCHAR(255) NOT NULL UNIQUE, " +
                "dictionary_id SMALLINT UNSIGNED, " +
                "INDEX X_" + this.getTableSQLName() + "_dictionary_id (dictionary_id) " +
                //"FOREIGN KEY (dictionary_id) REFERENCES " + dictionaryDao.getTableSQLName()  + "(dictionary_id) ON DELETE CASCADE ON UPDATE CASCADE"+
                ")ENGINE=MyISAM DEFAULT CHARSET=latin1;";
    }

    @Override
    public void reInitializeSQLTable() {
        super.reInitializeSQLTable();
        this.alterElementTable();
        this.openAddEntryStatement();
    }

    @Override
    protected void openPreparedStatements() {
        super.openPreparedStatements();
        // Exception for that one, because the contextNames is not affected yet.
        // this.openAddEntryStatement();
        // this.openGetElementStatement();
        this.openGetAllLocalElementIDsStatement();
    }

    @Override
    protected void closePreparedStatements() throws SQLException {
        super.closePreparedStatements();
        this.addEntryStatement.close();
        this.getAllLocalElementIDsStatement.close();
    }

    /******************************************
     * ALTERING TABLE
     ***************************/

    private void alterElementTable() {
        String query = "ALTER TABLE " + this.getTableSQLName() + " ADD (" + this.contextsForCreateQuery() + ");";
        try {
            this.executeSQLUpdate(query);
        } catch (MySQLSyntaxErrorException e) {
            //e.printStackTrace();
            logger.info("No needs to alter table " + this.getTableSQLName());
        } catch (SQLException e) {
            logger.error("** PROBLEM ** Cannot alter ElementTable " + this.getTableSQLName(), e);
        }
    }

    private String contextsForCreateQuery() {
        StringBuffer queryb = new StringBuffer();
        String context = null;
        for (Iterator<String> it = this.contextNames.iterator(); it.hasNext(); ) {
            context = it.next().toLowerCase();
            queryb.append(context);

            if (context.contains("_biological_characterization")) {
                queryb.append(" LONGTEXT");
            } else {
                queryb.append(" TEXT");
            }

            if (it.hasNext()) {
                queryb.append(", ");
            }
        }
        return queryb.toString();
    }

    /**
     * This method creates temporary tables for non annotated element with MAX_NUMBER_ELEMENTS_TO_PROCESS
     *
     * @param dictionaryID                 used to find non annotated element
     * @param maxNumberOfElementsToProcess
     * @return number of rows inserted in temporary table.
     */
    public long createTemporaryTable(int dictionaryID, int maxNumberOfElementsToProcess) {
        // Delete temporary table if exist
        deleteTemporaryTable();
        long noRows = 0;
        StringBuffer createQuery = new StringBuffer();
        createQuery.append("CREATE TEMPORARY TABLE ");
        createQuery.append(this.getTableSQLName());
        createQuery.append(TEMP_TABLE_SUFFIX);
        createQuery.append(" SELECT * FROM ");
        createQuery.append(this.getTableSQLName());
        createQuery.append(" WHERE dictionary_id IS NULL OR dictionary_id<");
        createQuery.append(dictionaryID);
        createQuery.append(" LIMIT ");
        createQuery.append(maxNumberOfElementsToProcess);
        createQuery.append(";");

        try {
            noRows = this.executeSQLUpdate(createQuery.toString());
            // if zero rows updated then drop the temporary table
            if (noRows == 0) {
                deleteTemporaryTable();
            }
        } catch (SQLException e) {
            logger.error("** PROBLEM ** Cannot create temp element Table " + this.getTableSQLName() + TEMP_TABLE_SUFFIX, e);
        }

        return noRows;
    }

    /**
     * This method delete temporary table.
     */
    private void deleteTemporaryTable() {
        String dropQuery = "DROP TABLE IF EXISTS  " + this.getTableSQLName() + TEMP_TABLE_SUFFIX + ";";

        try {
            this.executeSQLUpdate(dropQuery);
        } catch (SQLException e) {
            logger.error("** PROBLEM ** Cannot drop tempor Table " + this.getTableSQLName() + TEMP_TABLE_SUFFIX, e);
        }

    }

    public boolean resetDictionary() {
        boolean updated = false;
        StringBuffer queryb = new StringBuffer();
        queryb.append("UPDATE ");
        queryb.append(this.getTableSQLName());
        queryb.append(" SET dictionary_id = NULL;");
        try {
            this.executeSQLUpdate(queryb.toString());
            updated = true;
        } catch (MySQLNonTransientConnectionException e) {
            return this.resetDictionary();
        } catch (SQLException e) {
            logger.error("** PROBLEM ** Cannot update dictionary ID on table " + this.getTableSQLName(), e);
        }
        return updated;
    }

    /******************************************
     * FUNCTIONS ON THE TABLE
     ***************************/

    @Override
    protected void openAddEntryStatement() {
        StringBuffer queryb = new StringBuffer();
        queryb.append("INSERT INTO ");
        queryb.append(this.getTableSQLName());
        queryb.append(" SET local_element_id=?, ");
        queryb.append(this.contextsForInsertQuery());
        queryb.append(";");
        this.addEntryStatement = this.prepareSQLStatement(queryb.toString());
    }

    private String contextsForInsertQuery() {
        StringBuffer queryb = new StringBuffer();
        for (Iterator<String> it = this.contextNames.iterator(); it.hasNext(); ) {
            queryb.append(it.next());
            queryb.append("=?");
            if (it.hasNext()) {
                queryb.append(", ");
            }
        }
        return queryb.toString();
    }

    /**
     * Add an new entry in the corresponding _ET table.
     *
     * @return True if the entry was added to the SQL table, false if a problem occurred during insertion.
     */
    public boolean addEntry(Element element) {
        boolean inserted = false;
        int index = 2;
        try {
            this.addEntryStatement.setString(1, element.getLocalElementId());
            for (String contextName : this.contextNames) {
                String itemValue = element.getElementStructure().getText(contextName);
                this.addEntryStatement.setString(index, StringUtilities.escapeLine(itemValue));
                index++;
            }
            this.executeSQLUpdate(this.addEntryStatement);
            inserted = true;
        } catch (MySQLNonTransientConnectionException e) {
            this.openAddEntryStatement();
            return this.addEntry(element);
        } catch (MySQLIntegrityConstraintViolationException e) {
            // TODO: not to catch this exception here
            //logger.info("Table " + this.getTableSQLName() + " already contains a row for element " + element.getLocalElementId() +".");
        } catch (SQLException e) {
            logger.error("** PROBLEM ** Cannot add an entry on table " + this.getTableSQLName(), e);
        }
        return inserted;
    }

    private void openGetAllLocalElementIDsStatement() {
        StringBuffer queryb = new StringBuffer();
        queryb.append("SELECT local_element_id FROM ");
        queryb.append(this.getTableSQLName());
        queryb.append(";");
        this.getAllLocalElementIDsStatement = this.prepareSQLStatement(queryb.toString());
    }

    /**
     * Returns a set of all the localElementIDs contained in the table.
     */
    public HashSet<String> getAllLocalElementIDs() {
        HashSet<String> localElementIDs = new HashSet<String>();
        try {
            ResultSet rSet = this.executeSQLQuery(this.getAllLocalElementIDsStatement);
            while (rSet.next()) {
                localElementIDs.add(rSet.getString(1));
            }
            rSet.close();
        } catch (MySQLNonTransientConnectionException e) {
            this.openGetAllLocalElementIDsStatement();
            return this.getAllLocalElementIDs();
        } catch (SQLException e) {
            logger.error("** PROBLEM ** Cannot get lcoalElementIDs from " + this.getTableSQLName() + ". Empty set returned.", e);
        }
        return localElementIDs;
    }

    /**
     * This method gives total number of elements presents currently in element table.
     *
     * @return
     */
    public int getTotalNumberOfElement() {
        int totalNumberOfElement = 0;

        StringBuffer queryb = new StringBuffer();
        queryb.append("SELECT COUNT(ET.id) FROM ");
        queryb.append(this.getTableSQLName());
        queryb.append(" ET;");

        try {
            ResultSet rSet = this.executeSQLQuery(queryb.toString());
            if (rSet.first()) {
                totalNumberOfElement = rSet.getInt(1);
            }
            rSet.close();
        } catch (SQLException e) {
            logger.error("Problem in getting number of " + this.getTableSQLName() + ". Null returned.", e);
        }
        return totalNumberOfElement;
    }

    /**
     * Returns the value of a given context for a given element in the table.
     */
    public String getContextValueByContextName(String localElementID, String contextName) {
        StringBuffer queryb = new StringBuffer();
        queryb.append("SELECT " + contextName + " FROM ");
        queryb.append(this.getTableSQLName());
        queryb.append(" WHERE " + this.getTableSQLName() + ".local_element_id='");
        queryb.append(localElementID);
        queryb.append("';");

        String contextValue;
        try {
            ResultSet rSet = this.executeSQLQuery(queryb.toString());
            rSet.first();
            contextValue = rSet.getString(1);
            rSet.close();
        } catch (SQLException e) {
            logger.error("** PROBLEM ** Cannot get contexteValue for " + contextName + " and for localElementID:" + localElementID + " in " + this.getTableSQLName() + ". Null returned.", e);
            contextValue = null;
        }
        return contextValue;
    }

    /**
     * Returns a set of all the values contained in the given column of table.
     */
    public HashSet<String> getAllValuesByColumn(String columName) {
        HashSet<String> values = new HashSet<String>();
        StringBuffer queryb = new StringBuffer();
        queryb.append("SELECT ").append(columName).append(" FROM ").append(this.getTableSQLName()).append(";");
        try {
            ResultSet valuesRSet = this.executeSQLQuery(queryb.toString());
            while (valuesRSet.next()) {
                values.add(valuesRSet.getString(1));
            }
            valuesRSet.close();
        } catch (SQLException e) {
            logger.error("** PROBLEM ** Cannot get values of column " + columName + " for table " + this.getTableSQLName(), e);
        }
        return values;
    }

    /**
     * This method get local element id for last element.
     *
     * @return localElementID
     */
    public String getLastElementLocalID() {

        StringBuffer queryb = new StringBuffer();
        queryb.append("SELECT local_element_id FROM ");
        queryb.append(this.getTableSQLName());
        queryb.append(" WHERE id =(SELECT MAX(id) FROM ");
        queryb.append(this.getTableSQLName());
        queryb.append(");");

        try {
            ResultSet rSet = this.executeSQLQuery(queryb.toString());
            if (rSet.first()) {
                return rSet.getString(1);

            }
        } catch (SQLException e) {
            logger.error("Problem in getting non annotated element count", e);
        }

        return null;
    }

}
