package org.ncbo.resource_access_tools.resource.rxroads;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import org.ncbo.resource_access_tools.dao.AbstractObrDao;
import org.ncbo.resource_access_tools.dao.context.ContexDao;
import org.ncbo.resource_access_tools.enumeration.ResourceType;
import org.ncbo.resource_access_tools.populate.Element;
import org.ncbo.resource_access_tools.populate.Structure;
import org.ncbo.resource_access_tools.populate.Element.BadElementStructureException;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;
import org.ncbo.resource_access_tools.util.MessageUtils;

import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;

/**
 *
 * @author coulet
 * @version April 6th, 2009
 **/

public class RxrdAccessTool extends ResourceAccessTool {

	private static Double EURO_TO_DOLLAR   = 1.25505; // 05 Mar 2009
	private static Double POUNDS_TO_DOLLAR = 1.41319; // 05 Mar 2009


	private static final String RXRD_URL         = "http://www.researchcrossroads.org/";
	private static final String RXRD_NAME        = "ResearchCrossroads";
	private static final String RXRD_RESOURCEID  = "RXRD";
	private static final String RXRD_DESCRIPTION = "Centralizing scientific and medical funding data so that researchers gain recognition for their work and funders make better investments.";
	private static final String RXRD_LOGO        = "http://www.researchcrossroads.org/templates/ja_zeolite/images/logo.png";
	//private static final String RXRD_ELT_URL     = "http://www.researchcrossroads.org/index.php?view=article&id=50%3Agrant-details&grant_id=";
	private static final String RXRD_ELT_URL     = "http://www.researchcrossroads.org/grants/";

	//public static final String[] RXRD_ITEMKEYS   = {"title", "summary","keywords", "grantingOrganizationName"};
	public static final String[] RXRD_ITEMKEYS   = {"title"};
	private static final Double[] RXRD_WEIGHTS 	 = {    1.0};

	private static Structure RXRD_STRUCTURE      = new Structure(RXRD_ITEMKEYS, RXRD_RESOURCEID, RXRD_WEIGHTS);
	private static String RXRD_MAIN_ITEMKEY      = "title";

	// Database connection properties.
	private static final String RXRD_CONNECTION_STRING = MessageUtils.getMessage("obr.rxrd.jdbc.url");
	private static final String RXRD_JDBC_DRIVER = MessageUtils.getMessage("obr.jdbc.driver");
	private static final String RXRD_USER = MessageUtils.getMessage("obr.rxrd.jdbc.username");
	private static final String RXRD_PASSWORD = MessageUtils.getMessage("obr.rxrd.jdbc.password");

	private static final int RXRD_MAX_NUMBER_ELEMENT_TO_PROCESS = 50000;

	//specific parameters
	private static Connection        tableConnection;
	private static PreparedStatement getElementDataStatement;
	private static PreparedStatement getElementListStatement;
	private static PreparedStatement getGrantIdsByFiscalYearStatement;
	private static PreparedStatement getFundingAmountByGrantAndYearStatement;
	private static PreparedStatement getObrLocalElementIdStatement;
	private static String            tableSQLName = "innolyst_grants";
        private static String Unique_field_column = "rxrd_title";

	public RxrdAccessTool(){
		super(RXRD_NAME, RXRD_RESOURCEID, RXRD_STRUCTURE);
		try {
			this.getToolResource().setResourceURL(new URL(RXRD_URL));
			this.getToolResource().setResourceLogo(new URL(RXRD_LOGO));
			this.getToolResource().setResourceElementURL(RXRD_ELT_URL);
		}
		catch (MalformedURLException e) {
			logger.error(EMPTY_STRING, e);
		}
		this.getToolResource().setResourceDescription(RXRD_DESCRIPTION);
	}

	@Override
	public ResourceType  getResourceType() {
		return ResourceType.MEDIUM;
	}

	@Override
	public int getMaxNumberOfElementsToProcess(){
		return RXRD_MAX_NUMBER_ELEMENT_TO_PROCESS;
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
			// connection to the local resource (a sql db rx_roads)
			this.createConnection();
			if(!this.exist(this.getTableSQLName())){
				logger.error("** PROBLEM ** The table " + this.getTableSQLName()+" does not exist in "+RXRD_CONNECTION_STRING);
			}
			this.openPreparedStatements();

			nbElement = this.updates();
		} catch (Exception e) {
			logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName() + " with the db rx_roads", e);
		}finally{
			this.closeConnection();
		}
		return nbElement;
	}

	@Override
	public String elementURLString(String elementLocalID) {
		return RXRD_ELT_URL + elementLocalID;
	}

	@Override
	public String mainContextDescriptor() {
		return RXRD_MAIN_ITEMKEY;
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
		HashSet<Integer> localElementIDList       = this.getElementList();
		HashSet<String> allElementLocalIDs = resourceUpdateService.getAllLocalElementIDs();

                //for unique check on "rxrd_title"
                HashSet<String> allElementsTitlesInET = this.resourceUpdateService.getAllValuesByColumn(Unique_field_column);

		// Removed already existed element.
		HashSet<Integer> allElemenIDs = new HashSet<Integer>();
		for (String localElementID : allElementLocalIDs) {
			allElemenIDs.add(Integer.valueOf(localElementID));
		}

		localElementIDList.removeAll(allElemenIDs);

		logger.info(localElementIDList.size()+" new distinct grants found...");
		Iterator<Integer> i      = localElementIDList.iterator();
		Integer localElementID   = 0;
		Structure basicStructure = RXRD_STRUCTURE;

		while(i.hasNext()){
			try{
				localElementID = i.next();
				// get data related to each element and create this element
				myGrant = this.getElementData(localElementID, basicStructure);
				// write element data in the OBR_XX_ET table

				if(!myGrant.getElementStructure().hasNullValues()){

                                    //check if title already exists

                                    String title="";
                                    for(Entry<String, String> e :   myGrant.getElementStructure().getContexts().entrySet()){
                                        if(e.getKey().contains("title")){
                                           title = e.getValue();
                                        }
                                    }

                                    if(!allElementsTitlesInET.contains(title)){
                                        // System.out.println("** added " );
                                         if(resourceUpdateService.addElement(myGrant)){
                                                allElementsTitlesInET.add(title);
                                                nbAdded++;
                                            }
                                    }
				}
				// populates the OBR_RXRD_CTX table
				try{
					// for the first grant we also use the structure data to populate the OBR_RXRD_CXT Table.
					if(nbAdded==1){
						int nbContext = 0;
						Structure grantStruc = myGrant.getElementStructure();
						for (String contextName: grantStruc.getContextNames()){
							if(AbstractObrDao.contextTableDao.addEntry(new ContexDao.ContextEntry(contextName,
									this.getToolResource().getResourceStructure().getWeight(contextName),
									this.getToolResource().getResourceStructure().getOntoID(contextName)))){ // the itemkey that are already annotations are then associated to the staticOntologyID of this ontology
								nbContext++;
							}
						}
						logger.info("* Populate OBR_CXT table... "+(nbContext)+" contexts added...");
					}
				}catch (Exception e) {
					logger.error("** PROBLEM ** Problem when populating the OBR_CXT table.", e);
				}
			} catch (Exception e) {
				logger.error("** PROBLEM ** Problem with grant "+ localElementID +" when populating the OBR_RXRD_ET table.", e);
			}
		}
		logger.info(nbAdded+" grant added to the OBR_RXRD_ET table.");
		return nbAdded;
	}

	protected void openGetElementListStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT id FROM "+this.getTableSQLName()+" GROUP BY investigator_id, title, summary;");
		//queryb.append("SELECT id FROM "+this.getTableSQLName()+" WHERE fiscal_year>=2006 GROUP BY investigator_id, title, summary;");
		getElementListStatement = this.prepareSQLStatement(queryb.toString());
	}

	/**
	 * get a list of localElementIDs of DISTINCT grants from the rx_roads database of innolyst
	 *
	 * DISTINCT means different investigator_id, title, and summary
	 */
	public HashSet<Integer> getElementList(){

		HashSet<Integer> localElementIDList = new HashSet<Integer>();

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
		//queryb.append("SELECT title, summary, keywords, granting_organization_name FROM "+this.getTableSQLName()+" WHERE ");
		queryb.append("SELECT title FROM "+this.getTableSQLName()+" WHERE ");
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
			//System.out.println(getElementDataStatement);
			getElementDataStatement.setInt(1, localElementID);
			ResultSet rSet = this.executeSQLQuery(getElementDataStatement);
			//String summary = EMPTY_STRING;

			while(rSet.next()){
				// extract data and
				// fill the corresponding structure

				//title
				if(rSet.getString(1)!=null){
					elementStructure.putContext(Structure.generateContextName(RXRD_RESOURCEID, RXRD_ITEMKEYS[0]), rSet.getString(1));
				}else{
					elementStructure.putContext(Structure.generateContextName(RXRD_RESOURCEID, RXRD_ITEMKEYS[0]), EMPTY_STRING);
				}
				/*
				//summary
				if(rSet.getString(2)!=null){
					summary = rSet.getString(2).replaceFirst("<br .>", EMPTY_STRING);
					summary = summary.replaceFirst("^DESCRIPTION.*?:\\s*", EMPTY_STRING);
					summary = summary.replaceFirst("^DESCRIPTION", EMPTY_STRING);
					summary = summary.replaceAll("\\s{2,}", "\\s");
				}
				elementStructure.putContext(Structure.generateContextName(RXRD_RESOURCEID, RXRD_ITEMKEYS[1]), summary);


				// keywords
				if(rSet.getString(2)!=null){
					elementStructure.putContext(Structure.generateContextName(RXRD_RESOURCEID, RXRD_ITEMKEYS[1]), rSet.getString(2));
				}else{
					elementStructure.putContext(Structure.generateContextName(RXRD_RESOURCEID, RXRD_ITEMKEYS[1]), EMPTY_STRING);
				}

				// granting organisation

				if(rSet.getString(4)!=null){
					elementStructure.putContext(Structure.generateContextName(RXRD_RESOURCEID, RXRD_ITEMKEYS[3]), rSet.getString(4));
				}else{
					elementStructure.putContext(Structure.generateContextName(RXRD_RESOURCEID, RXRD_ITEMKEYS[3]), EMPTY_STRING);
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
			myElement = new Element(localElementID+EMPTY_STRING, elementStructure);
		}catch(BadElementStructureException e){
			logger.error(EMPTY_STRING, e);
		}
		return myElement;
	}

	protected void openGetGrantIdsByFiscalYearStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT id FROM "+this.getTableSQLName()+" WHERE fiscal_year=? AND GROUP BY investigator_id, title, summary;");
		getGrantIdsByFiscalYearStatement = this.prepareSQLStatement(queryb.toString());
	}

	/**
	 * get a list of localElementIDs of DISTINCT grants from the rx_roads database of innolyst
	 *
	 * DISTINCT means different investigator_id, title, and summary
	 *
	 * For a specific fiscal year
	 */
	public HashSet<Integer> getGrantIdsByFiscalYear(int fiscalYear){

		HashSet<Integer> localElementIDList = new HashSet<Integer>();

		try {
			getGrantIdsByFiscalYearStatement.setInt(1, fiscalYear);
			ResultSet rSet = this.executeSQLQuery(getGrantIdsByFiscalYearStatement);
			while(rSet.next()){
				localElementIDList.add(rSet.getInt(1));
			}
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {
            this.openGetGrantIdsByFiscalYearStatement();
			return this.getGrantIdsByFiscalYear(fiscalYear);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get localConceptIDs for fiscal_year"+fiscalYear+" from "+this.getTableSQLName()+". Empty set returned.", e);
		}
		return localElementIDList;
	}

	protected void openGetFundingAmountByGrantAndYearStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT id FROM "+this.getTableSQLName()+" WHERE fiscal_year=? AND GROUP BY investigator_id, title, summary;");
		getFundingAmountByGrantAndYearStatement = this.prepareSQLStatement(queryb.toString());
	}

	/**
	 * get a list of localElementIDs of DISTINCT grants from the rx_roads database of innolyst
	 *
	 * DISTINCT means different investigator_id, title, and summary
	 *
	 * For a specific fiscal year
	 */
	public Double getFundingAmountByGrantAndYear(int fiscalYear, int localElementID){

		Double fundingAmount = 0d;
		String currency      = null;

		try {
			getFundingAmountByGrantAndYearStatement.setInt(1, fiscalYear);
			getFundingAmountByGrantAndYearStatement.setInt(2, localElementID);
			ResultSet rSet = this.executeSQLQuery(getFundingAmountByGrantAndYearStatement);
			rSet.first();

			fundingAmount = rSet.getDouble(1);
			currency      = rSet.getString(2);

			if (currency.equals("EURO")||currency.equals("EUR")){
				fundingAmount=fundingAmount*EURO_TO_DOLLAR;
			}
			if (currency.equals("Pounds")){
				fundingAmount=fundingAmount*POUNDS_TO_DOLLAR;
			}
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {
            this.openGetGrantIdsByFiscalYearStatement();
			return this.getFundingAmountByGrantAndYear(fiscalYear,localElementID);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get fundingAmount for fiscal_year"+fiscalYear+" and id "+localElementID+" from "+this.getTableSQLName()+". Empty set returned.", e);
		}
		return fundingAmount;
	}

	protected void openGetObrLocalElementIdStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT min(id) FROM "+this.getTableSQLName()+" AS t1, "+this.getTableSQLName()+" AS t2 ");
		queryb.append("WHERE t1.investigator_id=t2.investigator_id AND ");
		queryb.append("t1.title=t2.title AND ");
		queryb.append("t1.summary=t2.summary AND ");
		queryb.append("t2.id=?;");
		getObrLocalElementIdStatement = this.prepareSQLStatement(queryb.toString());
	}

	/**
	 * get a localElementID that is valid in the obs.OBR_RXRD_ET table
	 * on the basis of any rxroads.innolyst_grants.id
	 *
	 */
	public int getObrLocalElementId(int localElementID){

		int obrLocalElementID = 0;

		try {
			getObrLocalElementIdStatement.setInt(1, localElementID);
			ResultSet rSet = this.executeSQLQuery(getObrLocalElementIdStatement);
			rSet.first();

			obrLocalElementID = rSet.getInt(1);
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {
            this.openGetObrLocalElementIdStatement();
			return this.getObrLocalElementId(localElementID);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get the corresponding localeElmeentID in obr for rxrd/id"+localElementID+" from "+this.getTableSQLName()+". Empty set returned.", e);
		}
		return obrLocalElementID;
	}
	// ++++++++++++++++ SET OF DATABASE UTILITY FUNCTIONS [start] ++++++++++++++
	/**
	 * Create a connection to the original Innolyst'Cross Reseach data base.
	 */
	private void createConnection(){
		if(tableConnection == null){
			try{
				Class.forName(RXRD_JDBC_DRIVER).newInstance();

				tableConnection = DriverManager.getConnection(RXRD_CONNECTION_STRING, RXRD_USER, RXRD_PASSWORD);
			}
			catch(Exception e){
				logger.error("** PROBLEM ** Cannot create connection to database " + RXRD_CONNECTION_STRING, e);
			}
		}
	}

	private void closeConnection(){
		if(tableConnection != null){
			try{
				closePreparedStatements();
			}
			catch(Exception e){
				logger.error("** PROBLEM ** Cannot create connection to database " + RXRD_CONNECTION_STRING, e);
			}finally{
				 try{
					 tableConnection.close();
				 }catch (Exception e) {
					logger.error("Problem in closing connection.") ;
				}
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
				tableConnection = DriverManager.getConnection(RXRD_CONNECTION_STRING, RXRD_USER, RXRD_PASSWORD);
				logger.info("\t[SQL Connection just reopenned.]");
			}
		}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot create connection to database " + RXRD_CONNECTION_STRING, e);
		}
	}
	public String getTableSQLName() {
		return tableSQLName;
	}

	private void openPreparedStatements() {
		this.openGetElementDataStatement();
		this.openGetElementListStatement();
		this.openGetGrantIdsByFiscalYearStatement();
		this.openGetFundingAmountByGrantAndYearStatement();
		this.openGetObrLocalElementIdStatement();
	}

	private void closePreparedStatements() throws SQLException {
		getElementDataStatement.close();
		getElementListStatement.close();
		getGrantIdsByFiscalYearStatement.close();
		getFundingAmountByGrantAndYearStatement.close();
		getObrLocalElementIdStatement.close();
	}
	// ++++++++++++++++ SET OF DATABASE UTILITY FUNCTIONS [end] ++++++++++++++++
}

