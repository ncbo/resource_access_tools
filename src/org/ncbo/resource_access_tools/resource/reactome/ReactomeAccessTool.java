package org.ncbo.resource_access_tools.resource.reactome;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;

import obs.obr.populate.Element;
import obs.obr.populate.Structure;

import org.ncbo.stanford.obr.enumeration.ResourceType;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;

/**
 * extract reaction and pathway from Reactome.
 * Use Reactome SOAP web services.
 * @author  Adrien Coulet
 * @version OBR v1
 * @date    25-Fev-2009
 */

public class ReactomeAccessTool extends ResourceAccessTool {

	private static final String REAC_URL         = "http://www.reactome.org/";
	private static final String REAC_NAME        = "Reactome";
	private static final String REAC_RESOURCEID  = "REAC";
	private static final String REAC_DESCRIPTION = "A curated knowledgebase of biological pathways.";
	private static final String REAC_LOGO        = "http://www.reactome.org/icons/R-purple.png";
	private static final String REAC_ELT_URL     = "http://www.reactome.org/cgi-bin/eventbrowser?DB=gk_current&ID=";

	private static final String[] REAC_ITEMKEYS  = {"name", 							"participants", 					"goBiologicalProcess",	"goCellCompartiment"};
	private static final Double[] REAC_WEIGHTS 	 = {  1.0,       						0.8,             					0.7,                	0.2};

	// // OntoID associated for reported annotations  Gene ontology (GO) with virtual ontology id 1070
	private static final String[] REAC_ONTOIDS 	 = {Structure.FOR_CONCEPT_RECOGNITION,	Structure.FOR_CONCEPT_RECOGNITION, 	"1070",					"1070"};

	private static Structure REAC_STRUCTURE      = new Structure(REAC_ITEMKEYS, REAC_RESOURCEID, REAC_WEIGHTS, REAC_ONTOIDS);
	private static String    REAC_MAIN_ITEMKEY   = "name";

	// constructor
	public ReactomeAccessTool(){
		super(REAC_NAME, REAC_RESOURCEID, REAC_STRUCTURE);
		try {
			this.getToolResource().setResourceURL(new URL(REAC_URL));
			this.getToolResource().setResourceDescription(REAC_DESCRIPTION);
			this.getToolResource().setResourceLogo(new URL(REAC_LOGO));
			this.getToolResource().setResourceElementURL(REAC_ELT_URL);
		} catch (MalformedURLException e) {
			logger.error(EMPTY_STRING, e);
		}
	}

	@Override
	public ResourceType  getResourceType() {
		return ResourceType.SMALL;
	}

	@Override
	public void updateResourceInformation() {
		// TODO
		// can be used to update resource name, description, logo, elt_url.
	}

	@Override
	public HashSet<String> queryOnlineResource(String query) {
		// TODO
		// not used for Reactome
		return new HashSet<String>();
	}

	@Override
	public String elementURLString(String elementLocalID) {
		return REAC_ELT_URL + elementLocalID;
	}

	@Override
	public String mainContextDescriptor() {
		return REAC_MAIN_ITEMKEY;
	}

	@Override
	public int updateResourceContent() {
		int nbElement = 0;
		try {
			Element myElement = null;

			/************* variable part ****************/
			// get the list of element present in the original resource
			HashSet<Long> elementIDList = this.getLocalElementIds();

			// gets the elements already in the corresponding _ET and keeps only the difference
			HashSet<String> allElementsInET = resourceUpdateService.getAllLocalElementIDs();
			HashSet<Long> allElementsInETasLong = new HashSet<Long>(allElementsInET.size());
			for(String elementInET: allElementsInET){
				allElementsInETasLong.add(Long.parseLong(elementInET));
			}
			elementIDList.removeAll(allElementsInETasLong);
			logger.info("Number of new elements to dump: " + elementIDList.size());

			// get data associated with each of these elements
			// and populate the ElementTable
			Iterator<Long> i = elementIDList.iterator();
			while(i.hasNext()){
				Long elementID = i.next();
				// get data of this element
				myElement = this.getElement(elementID);
				// populates OBR_REAC_ET with this element
				if(resourceUpdateService.addElement(myElement)){
					nbElement++;
				}
			}
			/*********************************************/
		} catch (Exception e) {
			logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName(), e);
		}
		logger.info(nbElement+" elements from "+REAC_NAME+" added to the element table.");

		return nbElement;
	}

	/**
	 * Get the list of all elementID of the resource.
	 * For Reactome database this is the list of elementIDs of Pathways and Reactions (Events).
	 */
	public HashSet<Long> getLocalElementIds(){
		HashSet<Long> elementIDList = new HashSet<Long>();
		logger.info(" Get the list of all elmentIDs from "+REAC_NAME+" ... ");
		try{
			GetReactomeData myExtractor = new GetReactomeData(this.getToolResource(), this);
			elementIDList = myExtractor.getLocalElementIds();
			logger.info(elementIDList.size()+" pathways and reactions found. ");
		}catch(Exception e){
			logger.error("** PROBLEM ** Problem when extracting elementID from the original resource. " +
					"Check the AccessTool", e);
		}
		return elementIDList;
	}

	/**
	 * Get a complete Element (Structure filled) from the resource.
	 * For Reactome database this is a Reaction or an Pathway and its associated data organized in a Structure
	 * @return
	 */
	public Element getElement(Long elementID){
		Element element = null;
		//System.out.println("Get data for the Element "+elementID.toString()+" ... ");
		try{
			GetReactomeData myExtractor = new GetReactomeData(this.getToolResource(), this);
			element = myExtractor.getElement(elementID);
		}catch(Exception e){
			logger.error("** PROBLEM ** Problem when extracting"+ elementID.toString()+" from "+REAC_NAME+
					". Check the AccessTool", e);
		}
		return element;
	}
}
