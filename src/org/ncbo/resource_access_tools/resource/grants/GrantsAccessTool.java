package org.ncbo.resource_access_tools.resource.grants;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;

import obs.obr.populate.Element;
import obs.obr.populate.Structure;
import obs.obr.populate.Element.BadElementStructureException;

import org.ncbo.stanford.obr.enumeration.ResourceType;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;
import org.ncbo.stanford.obr.util.MessageUtils;

import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;

/**
 *
 * @author coulet
 * @version April 6th, 2009
 **/

public class GrantsAccessTool extends ResourceAccessTool {

 	//private static final String GRANTS_URL         = null;
	private static final String GRANTS_NAME        = "Adrien's integrated grant resources";
	private static final String GRANTS_RESOURCEID  = "GRANTS";
	private static final String GRANTS_DESCRIPTION = "Database of granting related data coming three differents resources: researchcrossroads, CRISP(1972-1995 archives), and the Explortator of the NIH Reporter (for portfolio analysis)";
 	//private static final String GRANTS_LOGO        = null;
	private static final String GRANTS_ELT_URL     = null;

	private static final String[] GRANTS_ITEMKEYS   = {"title", "summary"};
	private static final Double[] GRANTS_WEIGHTS   = {    1.0, 		0.8};

	static Structure GRANTS_STRUCTURE      = new Structure(GRANTS_ITEMKEYS, GRANTS_RESOURCEID, GRANTS_WEIGHTS);
	private static String GRANTS_MAIN_ITEMKEY      = "title";

	//specific parameters
	private static Connection        tableConnection;
	private static PreparedStatement getElementDataStatement;
	private static PreparedStatement getElementListStatement;
	private static String            tableSQLName = "GRANTS_1972_2009";//TODO to change to the definitive database

	// Database connection properties.
	private static final String GRANTS_CONNECTION_STRING = MessageUtils.getMessage("obr.grants.jdbc.url");
	private static final String GRANTS_JDBC_DRIVER = MessageUtils.getMessage("obr.jdbc.driver");
	private static final String GRANTS_USER = MessageUtils.getMessage("obr.grants.jdbc.username");
	private static final String GRANTS_PASSWORD = MessageUtils.getMessage("obr.grants.jdbc.password");

	private static final int GRANTS_MAX_NUMBER_ELEMENT_TO_PROCESS = 50000;

	HashSet<Integer> localElementIDList = new HashSet<Integer>();

	public GrantsAccessTool(){
		super(GRANTS_NAME, GRANTS_RESOURCEID, GRANTS_STRUCTURE);
//		 try {
//			this.getToolResource().setResourceURL(new URL(GRANTS_URL));
//			this.getToolResource().setResourceLogo(new URL(GRANTS_LOGO));
//			this.getToolResource().setResourceElementURL(GRANTS_ELT_URL);
//		}
//		catch (MalformedURLException e) {
//			logger.error("", e);
//		}
		this.getToolResource().setResourceDescription(GRANTS_DESCRIPTION);
	}

	@Override
	public ResourceType  getResourceType() {
		return ResourceType.SMALL;
	}

	@Override
	public int getMaxNumberOfElementsToProcess(){
		return GRANTS_MAX_NUMBER_ELEMENT_TO_PROCESS;
	}

	@Override
	public void updateResourceInformation() {
	}

	@Override
	public HashSet<String> queryOnlineResource(String query) {
		return new HashSet<String>();
	}

	@Override
	public int updateResourceContent() {
		int nbElement = 0;
		try {
			// connection to the local resource (a sql db coulet)
			this.createConnection();
			if(!this.exist(this.getTableSQLName())){
				logger.error("** PROBLEM ** The table " + this.getTableSQLName()+" does not exist in "+GRANTS_CONNECTION_STRING);
			}
			this.openPreparedStatements();

			nbElement = this.updates();
		} catch (Exception e) {
			logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName() + " with the db coulet", e);
		}finally{
			this.closeConnection();
		}
		return nbElement;
	}

	@Override
	public String elementURLString(String elementLocalID) {
		return GRANTS_ELT_URL + elementLocalID;
	}

	@Override
	public String mainContextDescriptor() {
		return GRANTS_MAIN_ITEMKEY;
	}

	/**
	 * The updating function that is just adding new entries in the database.
	 * No modification of existing entries is managed for now.
	 * @return the number of new element added to the OBR_XX_ET table.
	 */
	private int updates(){
		int nbAdded = 0;

		Element myGrant;

		// get the list of element in the database (ie. list of grants in rxroads db)
		localElementIDList       = this.getElementList();
		HashSet<String> allElementLocalIDs = resourceUpdateService.getAllLocalElementIDs();
		// Removed already existed element.
		HashSet<Integer> allElemenIDs = new HashSet<Integer>();
		for (String localElementID : allElementLocalIDs) {
			allElemenIDs.add(Integer.valueOf(localElementID));
		}

		localElementIDList.removeAll(allElemenIDs);

		logger.info(localElementIDList.size()+" new distinct grants found...");
		Iterator<Integer> i      = localElementIDList.iterator();
		Integer localElementID   = 0;
		Structure basicStructure = GRANTS_STRUCTURE;

		while(i.hasNext()){
			try{
				localElementID = i.next();
				// get data related to each element and create this element
				myGrant = this.getElementData(localElementID, basicStructure);
				// write element data in the OBR_XX_ET table
				if(!myGrant.getElementStructure().hasNullValues()){
					if(resourceUpdateService.addElement(myGrant)){
						nbAdded++;
					}
				}
			} catch (Exception e) {
				logger.error("** PROBLEM ** Problem with grant "+ localElementID +" when populating the OBR_GRANTS_ET table.", e);
			}
		}
		logger.info(nbAdded+" grant added to the OBR_GRANTS_ET table.");
		return nbAdded;
	}

	protected void openGetElementListStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT id FROM "+this.getTableSQLName()+" WHERE fiscal_year>=1995;");
		//THING TO REMOVE sub grants from the all set like :: ends with S something eg S01
		getElementListStatement = this.prepareSQLStatement(queryb.toString());
	}

	/**
	 * get a list of localElementIDs of DISTINCT grants from the rx_roads database of innolyst
	 *
	 * DISTINCT means different investigator_id, title, and summary
	 */
	public HashSet<Integer> getElementList(){

		localElementIDList = new HashSet<Integer>();

		try {
			ResultSet rSet = this.executeSQLQuery(getElementListStatement);
			while(rSet.next()){
				localElementIDList.add(rSet.getInt(1));

			}
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {
            return this.getElementList();
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get localConceptIDs from "+this.getTableSQLName()+". Empty set returned.", e);
		}
		return localElementIDList;
	}

	protected void openGetElementDataStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT title, summary FROM "+this.getTableSQLName()+" WHERE ");
		queryb.append("id=?;");
		getElementDataStatement = this.prepareSQLStatement(queryb.toString());
	}

	/**
	 * get all the data for an entry in the original database, create and return the corresponding element
	 */
	public Element getElementData(Integer localElementID, Structure basicStructure){
		Element myElement = null;
		Structure elementStructure = new Structure(basicStructure.getContextNames());
		try {
			getElementDataStatement.setInt(1, localElementID);
			ResultSet rSet = this.executeSQLQuery(getElementDataStatement);
			String summary = EMPTY_STRING;

			while(rSet.next()){
				// extract data and
				// fill the corresponding structure

				//title
				if(rSet.getString(1)!=null){
					elementStructure.putContext(Structure.generateContextName(GRANTS_RESOURCEID, GRANTS_ITEMKEYS[0]), rSet.getString(1));
				}else{
					elementStructure.putContext(Structure.generateContextName(GRANTS_RESOURCEID, GRANTS_ITEMKEYS[0]), "");
				}

				//summary
				if(rSet.getString(2)!=null){
					summary = rSet.getString(2).replaceFirst("<br .>", "");
					summary = summary.replaceFirst("^DESCRIPTION.*?:\\s*", "");
					summary = summary.replaceFirst("^DESCRIPTION", "");
					summary = summary.replaceAll("\\s{2,}", "\\s");
				}

				elementStructure.putContext(Structure.generateContextName(GRANTS_RESOURCEID, GRANTS_ITEMKEYS[1]), summary);

				/*

				// keywords
				if(rSet.getString(2)!=null){
					elementStructure.putContext(Structure.generateContextName(GRANTS_RESOURCEID, GRANTS_ITEMKEYS[1]), rSet.getString(2));
				}else{
					elementStructure.putContext(Structure.generateContextName(GRANTS_RESOURCEID, GRANTS_ITEMKEYS[1]), "");
				}
				*/
			}
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openGetElementDataStatement();
			return this.getElementData(localElementID, basicStructure);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get data related to the grant "+localElementID+" from "+this.getTableSQLName()+". Empty set returned.", e);
		}
		// insert the new filled structure in a new element
		try{
			myElement = new Element(localElementID+"", elementStructure);
		}catch(BadElementStructureException e){
			logger.error("", e);
		}
		return myElement;
	}


	// ++++++++++++++++ SET OF DATABASE UTILITY FUNCTIONS [start] ++++++++++++++
	/**
	 * Create a connection to the original Innolyst'Cross Reseach data base.
	 */
	private void createConnection(){
		if(tableConnection == null){
			try{
				Class.forName(GRANTS_JDBC_DRIVER).newInstance();
				tableConnection = DriverManager.getConnection(GRANTS_CONNECTION_STRING, GRANTS_USER, GRANTS_PASSWORD);
			}
			catch(Exception e){
				logger.error("** PROBLEM ** Cannot create connection to database " + GRANTS_CONNECTION_STRING, e);
			}
		}
	}

	/**
	 * Returns true if the table already exists in the DB.
	 */
	private boolean exist(String tableName) {
		boolean exist;
		try{
			DatabaseMetaData dmd = tableConnection.getMetaData();
			ResultSet tables = dmd.getTables(tableConnection.getCatalog(), null, tableName, null);
			exist = tables.next();
			tables.close();
		}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot determin if table " + tableName + "exists.", e);
			exist = false;
		}
		return exist;
	}

	/**
	 * Open a prepared statement that corresponds to the given SQL query.
	 */
	protected PreparedStatement prepareSQLStatement(String query){
		try {
			return tableConnection.prepareStatement(query);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot open prepared statement for query: "+query+" of the table "+ this.getTableSQLName() +". Null returned.", e);
			return null;
		}
	}

	/**
	 * Executes the SQL query on the given prepared statement. As it returns a ResultSet,
	 * this statement needs to be explicitly closed after the processing of the ResultSet.
	 */
	protected ResultSet executeSQLQuery(PreparedStatement stmt) throws SQLException {
		ResultSet rSet;
		try{
			rSet = stmt.executeQuery();
		}
		catch (Exception e){
			reOpenConnectionIfClosed();
			// TODO : change that probably... this call is not necessary... else raise an exception
			rSet = stmt.executeQuery();
		}
		return rSet;
	}

	/**
	 * Reopens the DB connection if closed and reopens all the prepared statement for all instances of sub-classes.
	 */
	public static void reOpenConnectionIfClosed(){
		try{
			if (tableConnection.isClosed()){
				tableConnection = DriverManager.getConnection(GRANTS_CONNECTION_STRING, GRANTS_USER, GRANTS_PASSWORD);
				logger.info("\t[SQL Connection just reopenned.]");
			}
		}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot create connection to database " + GRANTS_CONNECTION_STRING, e);
		}
	}
	public String getTableSQLName() {
		return tableSQLName;
	}

	protected void openPreparedStatements() {
		this.openGetElementDataStatement();
		this.openGetElementListStatement();
	}

	protected void closePreparedStatements() throws SQLException {
		getElementDataStatement.close();
		getElementListStatement.close();
	}

	private void closeConnection(){
		if(tableConnection != null){
			try{
				closePreparedStatements();
			}
			catch(Exception e){
				logger.error("** PROBLEM ** Cannot create connection to database " + GRANTS_CONNECTION_STRING, e);
			}finally{
				 try{
					 tableConnection.close();
				 }catch (Exception e) {
					logger.error("Problem in closing connection.") ;
				}
			}
		}
	}
	// ++++++++++++++++ SET OF DATABASE UTILITY FUNCTIONS [end] ++++++++++++++++
}

