package org.ncbo.resource_access_tools.resource.wikipathways;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.ncbo.resource_access_tools.enumeration.ResourceType;
import org.ncbo.resource_access_tools.populate.Element;
import org.ncbo.resource_access_tools.populate.Structure;
import org.ncbo.resource_access_tools.populate.Element.BadElementStructureException;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;
import org.wikipathways.www.webservice.WSPathwayInfo;
import org.wso2.www.php.WikiPathwaysPortTypeProxy;

/**
 * WikiPathwaysAccessTool is responsible for getting data elements for
 * WikiPathways. Gets the pathway data to populate _ET table
 * using web service.
 * Web Service definition found at location
 * http://www.wikipathways.org/wpi/webservice/webservice.php?wsdl
 *
 * @author Kuladip Yadav
 * @version $$
 */
public class WikiPathwaysAccessTool extends ResourceAccessTool {

	// Home URL of the resource
	private static final String WP_URL			= "http://www.wikipathways.org/index.php/WikiPathways";

	// Name of the resource
	private static final String WP_NAME 		= "WikiPathways";

	// Short name of the resource
	private static final String WP_RESOURCEID 	= "WP";

	// Text description of the resource
	private static final String WP_DESCRIPTION = "WikiPathways was established to facilitate the contribution and maintenance of pathway information by the biology community. WikiPathways is an open, collaborative platform dedicated to the curation of biological pathways.";

	// URL that points to the logo of the resource
	private static final String WP_LOGO 		= "http://www.wikipathways.org/skins/common/images/earth-or-pathway_text3_beta.png";

	//Base URL that points to an element when concatenated with an local element ID
	private static final String WP_ELT_URL 	= "http://www.wikipathways.org/index.php/Pathway:";

	// The set of context names
	private static final String[] WP_ITEMKEYS = {"name", "organism" };

	// Weight associated to a context
	private static final Double[] WP_WEIGHTS  = { 1.0, 0.9 };

	// OntoID associated for reported annotations
	private static final String[] WP_ONTOIDS  = {Structure.FOR_CONCEPT_RECOGNITION, "1132"};

	// Structure for resource Access tool
	private static final Structure WP_STRUCTURE = new Structure(WP_ITEMKEYS, WP_RESOURCEID, WP_WEIGHTS, WP_ONTOIDS);

	// A context name used to describe the associated element
	private static final String WP_MAIN_ITEMKEY = "name";

	/**
	 * Construct WikiPathwaysAccessTool using database connection property
	 * It set properties for tool Resource
	 *
	 */
	public WikiPathwaysAccessTool(){
		super(WP_NAME, WP_RESOURCEID, WP_STRUCTURE);
		try {
			this.getToolResource().setResourceURL(new URL(WP_URL));
			this.getToolResource().setResourceLogo(new URL(WP_LOGO));
			this.getToolResource().setResourceElementURL(WP_ELT_URL);
		} catch (MalformedURLException e) {
			logger.error(EMPTY_STRING, e);
		}
		this.getToolResource().setResourceDescription(WP_DESCRIPTION);
	}

	@Override
	public ResourceType  getResourceType() {
		return ResourceType.SMALL;
	}

	@Override
	public void updateResourceInformation() {

	}

	@Override
	public int updateResourceContent(){
		return updateAllElements( );
	}

	private int updateAllElements(){
		int nbElement = 0;
		logger.info("Updating " + this.getToolResource().getResourceName() + " elements...");
		HashSet<String> allElementLocalIDs = resourceUpdateService.getAllLocalElementIDs();
		// Get pathway map using web service.
		HashMap<String, WSPathwayInfo> pathwayMap= this.getAllWikiPathways();

		// Remove pathway form pathwayMap which are already present in database.
		for (String localElementID : allElementLocalIDs) {
			if(pathwayMap.containsKey(localElementID)){
				pathwayMap.remove(localElementID);
			}
		}

		logger.info("Number of elements to process : " + pathwayMap.size());

 		try {
			nbElement = this.updateElementTableWithPathways(pathwayMap);
		} catch (BadElementStructureException e) {
			 logger.error("** PROBLEM ** Cannot update " + this.getToolResource().getResourceName() +" because of a Structure problem.", e);
		}
		return nbElement;
	}

	/**
	 * This method get list of pathways present on WikiPathways.
	 * It uses web service client(placed in wikipathways-client.jar) created for web service definition
	 * found at <b>http://www.wikipathways.org/wpi/webservice/webservice.php?wsdl</b>
	 *
	 * @return Map containing WSPathwayInfo with pathway id as key
	 */
	private HashMap<String, WSPathwayInfo> getAllWikiPathways() {

		HashMap<String, WSPathwayInfo> pathwayMap = new HashMap<String, WSPathwayInfo>();
		WikiPathwaysPortTypeProxy proxy = new WikiPathwaysPortTypeProxy();
		try {
			WSPathwayInfo[] pathways =proxy.listPathways();
			for (WSPathwayInfo pathwayInfo : pathways) {
				pathwayMap.put(pathwayInfo.getId(), pathwayInfo);
			}
		} catch (RemoteException e) {
			logger.error("Problem in getting pathway list from web service.", e);
		}
		return pathwayMap;
	}

	/**
	 * This method update _ET table with  pathway information extracted from pathwayMap
	 *
	 * @param pathwayMap
	 * @return 				number of element processed
	 * @throws BadElementStructureException
	 */
	private int updateElementTableWithPathways(HashMap<String, WSPathwayInfo> pathwayMap ) throws BadElementStructureException{
		int nbElement = 0;
		ArrayList<String> contextNames = this.getToolResource().getResourceStructure().getContextNames();
		Element element;
		Structure eltStructure = new Structure(contextNames);
		String localElementID= null;
		String name;
		String organism = null;

		// Iterating each pathwayInfo and process it.
		for (WSPathwayInfo pathwayInfo : pathwayMap.values()) {
			try {
				// Extracting localElementID for Pathway.
				localElementID = pathwayInfo.getId();

				// Extracting pathway name
			    if(pathwayInfo.getName()!= null){
			    	name = pathwayInfo.getName();
			    }else{
			    	name = EMPTY_STRING;
			    }

			    // Mapping organism string to concepts.
			    if(pathwayInfo.getSpecies()!= null && !pathwayInfo.getSpecies().trim().equals(EMPTY_STRING)){
			    	organism = resourceUpdateService.mapTermsToVirtualLocalConceptIDs(pathwayInfo.getSpecies(), WP_ONTOIDS[1], SEMICOLON_STRING);
			    }
		    	 // if mapping concepts are null or empty then log message for it.
				if(organism== null || organism.trim().length()== 0){
					organism = EMPTY_STRING;
					logger.error("Cannot map Organism  '" + pathwayInfo.getSpecies() + "' to local concept id for element with ID " + localElementID +".");

				}

				// Putting contexts into element structure.
				eltStructure.putContext(Structure.generateContextName(WP_RESOURCEID, WP_ITEMKEYS[0]),  name);
				eltStructure.putContext(Structure.generateContextName(WP_RESOURCEID, WP_ITEMKEYS[1]),  organism);

				// Creating element
				element = new Element(localElementID, eltStructure);

				// Persisting element into database.
				if (resourceUpdateService.addElement(element)){
						nbElement ++;
				}
			}catch (Exception e) {
				logger.error("** PROBLEM ** In creating Element with localElementID " + localElementID ,e );
			}
		}


		return nbElement;
	}

	@Override
	public String elementURLString(String elementLocalID) {
		return WP_ELT_URL + elementLocalID;
	}

	@Override
	public String mainContextDescriptor() {
		return WP_MAIN_ITEMKEY;
	}

	@Override
	public HashSet<String> queryOnlineResource(String query) {
		return new HashSet<String>();
	}

}
