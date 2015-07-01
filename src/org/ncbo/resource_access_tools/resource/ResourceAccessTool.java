package org.ncbo.resource_access_tools.resource;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;

import obs.common.beans.DictionaryBean;
import obs.common.utils.Utilities;
import obs.obr.populate.Resource;
import obs.obr.populate.Structure;

import org.apache.log4j.Logger;
import org.ncbo.stanford.obr.dao.AbstractObrDao;
import org.ncbo.stanford.obr.dao.context.ContexDao.ContextEntry;
import org.ncbo.stanford.obr.enumeration.ResourceType;
import org.ncbo.stanford.obr.util.LoggerUtils;
import org.ncbo.stanford.obr.util.MessageUtils;
import org.ncbo.stanford.obr.util.helper.StringHelper;

/**
 * This abstract class is used as a generic Resource access and manipulation tool.
 * A tool has a name, and give access to exactly one Resource.
 * A ResourceAccessTool is associated to several OBR tables in the OBS DB.
 *
 * @author Adrien Coulet, Clement Jonquet, Kuladip Yadav
 * @version OBR_v_0.2
 * @created 20-Nov-2008
 */
public abstract class ResourceAccessTool implements StringHelper {

	// Logger for ResourceAccessTool
	protected static Logger logger = Logger.getLogger(ResourceAccessTool.class);

	public static final String RESOURCE_NAME_PREFIX = "OBR_RESOURCE_";
	private Resource toolResource;
	private String toolName;

	protected ResourceUpdateService resourceUpdateService;

	/**
	 * Constructs a new ResourceAccessTool associated to a new Resource constructed with the given information
	 * Gets access also the associated tables in the DB (and eventually created them).
	 * If the corresponding contexts do not exist in OBR_CXT, they are created.
	 */
	public ResourceAccessTool(String resource, String resourceID, Structure structure){
		super();
		initializeLogger(resourceID);
		logger.info("*****************************************************");
		logger.info("ResourceAccessTool creation...");
		this.toolName = RESOURCE_NAME_PREFIX + resourceID + Utilities.getRandomString(3);
		String mainContext = Structure.generateContextName(resourceID, this.mainContextDescriptor());
		this.toolResource = new Resource(resource, resourceID, structure, mainContext);

		this.resourceUpdateService = new ResourceUpdateServiceImpl(this);

		// Adds the structure's contexts in OBR_CXT
		ContextEntry context;
		for(String contextName: structure.getContextNames()){
			context = new ContextEntry(contextName, structure.getWeight(contextName), structure.getOntoID(contextName));
			AbstractObrDao.contextTableDao.addEntry(context);
		}

		logger.info("ResourceAccessTool " + this.getToolResource().getResourceId() + " created to access " + this.getToolResource().getResourceName() +" (" + this.getToolResource().getResourceId() + ").\n");
	}

	/**
	 * This method initialize log4j logger for given resource id.
	 *
	 * @param resourceID
	 */
	private void initializeLogger(String resourceID) {
		logger= LoggerUtils.createRATSpecificLogger(this.getClass(), resourceID.toLowerCase() + MessageUtils.getMessage("obr.logs.suffix"));
	}

	/**
	 * @return the resourceUpdateService
	 */
	public ResourceUpdateService getResourceUpdateService() {
		return resourceUpdateService;
	}


    /**
	 * @param resourceUpdateService the resourceUpdateService to set
	 */
	public void setResourceUpdateService(ResourceUpdateService resourceUpdateService) {
		this.resourceUpdateService = resourceUpdateService;
	}


    /**
	 * Returns the log4j logger
	 */
	public static Logger getLogger() {
		return logger;
	}

	public void finalize() throws Throwable {
		super.finalize();
	}

	/**
	 * Returns the associated Resource.
	 */
	public Resource getToolResource() {
		return toolResource;
	}

	/**
	 * Returns the tool name.
	 */
	public String getToolName() {
		return toolName;
	}

	/**
	 * Default maximum number of element to process
	 * Need to override for MEDIUM and BIG resources
	 *
	 * @return int  Number fo element
	 */
	public int getMaxNumberOfElementsToProcess(){
		return Integer.parseInt(MessageUtils.getMessage("obr.elements.process.max"));
	}

	/**
	 * Updates the associated Resource information fields (name, URL, description, logo URL) automatically.
	 */
	public abstract void updateResourceInformation();

	/**
	 *  This method gives type of resources used for different behavior of
	 *  resource index workflow e.g SMALL, MEDIUM , BIG
	 *
	 * @return {@code ResourceType}
	 */
	public abstract ResourceType getResourceType();

	/**
	 * Enables to query a resource with a String as it is done online.
	 * Returns a set of localElementIDs (String) which answer the query.
	 */
	public abstract HashSet<String> queryOnlineResource(String query);

	/********************************* OBR WORKFLOW FUNCTIONS *****************************************************/

	/**
	 * Updates the resource content automatically (locally or remotely). Returns the number of elements updated.
	 * This function implements the step 1 of the OBR workflow.
	 */
	public abstract int updateResourceContent();

	/**
	 * Returns the number of elements in the _ET table.
	 */
	public long numberOfElement(){
		return this.resourceUpdateService.numberOfEntry();
	}

	public void reInitializeAllTables(){
		this.resourceUpdateService.reInitializeAllTables();
	}

	public void reInitializeAllTablesExcept_ET(){
		this.resourceUpdateService.reInitializeAllTablesExcept_ET();
	}

	/**
	 * Generates an URL for a given Element localID String.
	 */
	public URL generateElementURL(String localElementID){
		URL elementURL;
		try{
			elementURL = new URL(this.elementURLString(localElementID));
		}
		catch(MalformedURLException e){
			logger.error("Problem when creating the URL of element " + localElementID + ". The URL of the corresponding resource has been returned.", e);
			elementURL = this.getToolResource().getResourceURL();
		}
		return elementURL;
	}

	/**
	 * Returns a String to generate the URL of the given element.
	 */
	public abstract String elementURLString(String localElementID);

	/**
	 * Returns the main context (from the Structure) that describes the resource element.
	 * e.g., title or name
	 */
	public abstract String mainContextDescriptor();

	/**
	 * Adds the Resource tool entry into Resource table (OBR_RT)
	 *
	 */
	public void addResourceTableEntry(){
		this.resourceUpdateService.addResource(this.getToolResource());
	}


    /**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString(){
	    final String TAB = "\r\n";
	    StringBuffer retValue = new StringBuffer();
	    retValue.append("ResourceAccessTool [ ").append(TAB)
	        //.append(super.toString()).append(TAB)
	        .append("\ttoolResource = ").append(this.toolResource).append(TAB)
	        .append("\ttoolName = ").append(this.toolName).append(TAB)
	        .append("]");
	    return retValue.toString();
	}

    /**
	 * Method update resource table with total number of element and update date.
	 *
	 * @return boolean  {@code true} if updated successfully.
	 */
	public boolean updateResourceUpdateInfo() {
		return resourceUpdateService.updateResourceUpdateInfo(this.getToolResource());
	}

	/**
	 * Method update resource table after completion of resource workflow.
	 * It includes updation of dictionary and date for resource workflow completed.
	 *
	 * @return boolean {@code true} if updated successfully.
	 */
	public boolean updateResourceWorkflowInfo() {
		return resourceUpdateService.updateResourceWorkflowInfo(this.getToolResource());
	}

}
