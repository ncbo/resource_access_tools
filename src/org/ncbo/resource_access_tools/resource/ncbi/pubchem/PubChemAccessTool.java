package org.ncbo.resource_access_tools.resource.ncbi.pubchem;

import gov.nih.nlm.ncbi.www.soap.eutils.esummary.DocSumType;
import gov.nih.nlm.ncbi.www.soap.eutils.esummary.ESummaryRequest;
import gov.nih.nlm.ncbi.www.soap.eutils.esummary.ESummaryResult;
import gov.nih.nlm.ncbi.www.soap.eutils.esummary.ItemType;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.ncbo.resource_access_tools.enumeration.ResourceType;
import org.ncbo.resource_access_tools.populate.Element;
import org.ncbo.resource_access_tools.populate.Structure;
import org.ncbo.resource_access_tools.populate.Element.BadElementStructureException;
import org.ncbo.resource_access_tools.resource.ncbi.AbstractNcbiResourceAccessTool;


/**
 * PubChemAccessTool is responsible for getting data elements for
 * PubChem Compound database.
 * It process records with associated MeSH terms using term "has_mesh[filter]"
 * using E-Utils.
 *
 * @author kyadav
 * @version $$
 */
public class PubChemAccessTool extends AbstractNcbiResourceAccessTool {

	// Home URL of the resource
	private static final String PCM_URL			= "http://pubchem.ncbi.nlm.nih.gov/";

	// Name of the resource
	private static final String PCM_NAME 		= "PubChem";

	// Short name of the resource
	private static final String PCM_RESOURCEID 	= "PCM";

	// Text description of the resource
	private static final String PCM_DESCRIPTION = "PubChem provides information on the biological activities of small molecules. It is a component of NIH's Molecular Libraries Roadmap Initiative.";

	// URL that points to the logo of the resource
	private static final String PCM_LOGO 		= "http://pubchem.ncbi.nlm.nih.gov/images/pubchemlogob.gif";

	// Basic URL that points to an element when concatenated with an local element ID
	private static final String PCM_ELT_URL 	= "http://pubchem.ncbi.nlm.nih.gov/summary/summary.cgi?cid=";

	//PubChem Compound Database name for E-Utils.
	private static final String PCM_EUTILS_DB 	= "pccompound";

	//Query terms for E-Utils.This term get records with associated MeSH terms.
	private static final String PCM_EUTILS_TERM =  "has_mesh[filter]";

	// The set of context names
	private static final String[] PCM_ITEMKEYS = { "MeSHHeadingList", "MeSHTermList", "PharmActionList",					"SynonymList"};
	// Weight associated to a context
	private static final Double[] PCM_WEIGHTS  = { 1.0, 	 		      1.0,  		 0.8,							    0.9};
	// OntoID associated for reported annotations(MSH ontology : 1351)
	private static final String[] PCM_ONTOIDS  = { "1351", 		      "1351",         Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION};

	// Structure for GEO Access tool
	private static final Structure PCM_STRUCTURE = new Structure(PCM_ITEMKEYS, PCM_RESOURCEID, PCM_WEIGHTS, PCM_ONTOIDS);

	// A context name used to describe the associated element
	private static final String PCM_MAIN_ITEMKEY = "MeSHHeadingList";

	/**
	 * Construct PubChemAccessTool using database connection property
	 * It set properties for tool Resource
	 *
	 */
	public PubChemAccessTool(){
		super(PCM_NAME, PCM_RESOURCEID, PCM_STRUCTURE);
		try {
			this.getToolResource().setResourceURL(new URL(PCM_URL));
			this.getToolResource().setResourceLogo(new URL(PCM_LOGO));
			this.getToolResource().setResourceElementURL(PCM_ELT_URL);
		} catch (MalformedURLException e) {
			logger.error("Malformed URL Exception", e);
		}
		this.getToolResource().setResourceDescription(PCM_DESCRIPTION);
	}

	@Override
	public ResourceType  getResourceType() {
		return ResourceType.SMALL;
	}

	@Override
	protected String getEutilsDB() {
		return PCM_EUTILS_DB;
	}

	@Override
	protected String getEutilsTerm() {
		return PCM_EUTILS_TERM;
	}

	@Override
	public void updateResourceInformation() {
		// TODO See if it can be implemented for this resource.
	}

	@Override
	public int updateResourceContent(){
		return super.eutilsUpdateAll(null);
	}

	/**
	 * This method extract data from PubChem Compound
	 * and populate the Element Table () with data elements for GSE and GDS data
	 *
	 * @param UIDs - Set of uid strings
	 * @return number of elements processed
	 *
	 */
	 @Override
	 protected int updateElementTableWithUIDs(HashSet<String> UIDs) throws BadElementStructureException{
		int nbElement = 0;

		// Create summary request for e-utils
		ESummaryRequest esummaryRequest = new ESummaryRequest();
		esummaryRequest.setEmail(EUTILS_EMAIL);
		esummaryRequest.setTool(EUTILS_TOOL);
		esummaryRequest.setDb(this.getEutilsDB());

		ESummaryResult esummaryResult;
		StringBuffer UIDlist;
		DocSumType[] resultDocSums;
		ItemType[] docSumItems;
		ArrayList<String> contextNames = this.getToolResource().getResourceStructure().getContextNames();
		Element element;
		Structure eltStructure = new Structure(contextNames);

		String[] UIDsTab = new String[UIDs.size()];
		UIDsTab = UIDs.toArray(UIDsTab);
		int max;

		String concepts;
		List<String> itemKeys= Arrays.asList(PCM_ITEMKEYS);
		String meSHHeadingList;

		// Process UIDs
		for(int step=0; step<UIDsTab.length; step+=EUTILS_MAX){
			max = step+EUTILS_MAX;
			UIDlist = new StringBuffer();
			if(max>UIDsTab.length) {max = UIDsTab.length;}
			for(int u=step; u<max; u++){
				UIDlist.append(UIDsTab[u]);
				if(u<max-1) {UIDlist.append(COMMA_STRING);}
			}
			esummaryRequest.setId(UIDlist.toString());
			try {

				// Fire request to E-utils tool
				esummaryResult = this.getToolEutils().run_eSummary(esummaryRequest);
				resultDocSums = esummaryResult.getDocSum();
				// Process each item
				for(int i=0; i<resultDocSums.length; i++){
					docSumItems = resultDocSums[i].getItem();
					// This section depends of the structure and the type of content we want to get back
					// UID as localElementID (same as CID)
					String localElementID =resultDocSums[i].getId();

					for (int j = 0; j < docSumItems.length; j++) {
						if(!itemKeys.contains(docSumItems[j].getName())){
							continue;
						}

						// Extract MeSHHeadingList and map to as MESH ontology concepts
						if(PCM_ITEMKEYS[0].equals(docSumItems[j].getName())){

							meSHHeadingList= getItemTypeContent(docSumItems[j], GT_SEPARATOR_STRING);
							// Map terms to MESH concepts.
							concepts = resourceUpdateService.mapTermsToVirtualLocalConceptIDs(meSHHeadingList, PCM_ONTOIDS[0], GT_SEPARATOR_STRING);

							if(!EMPTY_STRING.equals(meSHHeadingList)
									&& (concepts== null || concepts.trim().length()== 0)){
								logger.error("Cannot map MESH term " + meSHHeadingList + " to local concept id for element with ID " + localElementID +".");

							}
							eltStructure.putContext(Structure.generateContextName(PCM_RESOURCEID, PCM_ITEMKEYS[0]), concepts);

						}
						// Extract MeSHTermList and map to as MESH ontology concepts
						else if(PCM_ITEMKEYS[1].equals(docSumItems[j].getName())){

							meSHHeadingList= getItemTypeContent(docSumItems[j], GT_SEPARATOR_STRING);
							// Map terms to MESH concepts.
							concepts = resourceUpdateService.mapTermsToVirtualLocalConceptIDs(meSHHeadingList, PCM_ONTOIDS[1], GT_SEPARATOR_STRING);

							if(!EMPTY_STRING.equals(meSHHeadingList)
									&& (concepts== null || concepts.trim().length()== 0)){
								logger.error("Cannot map MESH term " + meSHHeadingList + " to local concept id for element with ID " + localElementID +".");

							}
							eltStructure.putContext(Structure.generateContextName(PCM_RESOURCEID, PCM_ITEMKEYS[1]), concepts);

						}
						// Extract PharmActionList
						else if(PCM_ITEMKEYS[2].equals(docSumItems[j].getName())){
							eltStructure.putContext(Structure.generateContextName(PCM_RESOURCEID, PCM_ITEMKEYS[2]), getItemTypeContent(docSumItems[j], COMMA_SEPARATOR));

						}
						// Extract SynonymList
						else if(PCM_ITEMKEYS[3].equals(docSumItems[j].getName())){
							eltStructure.putContext(Structure.generateContextName(PCM_RESOURCEID, PCM_ITEMKEYS[3]), getItemTypeContent(docSumItems[j], COMMA_SEPARATOR));

						}
					}

					if(localElementID != null){
						element = new Element(localElementID, eltStructure);
						// Insert element into database.
						if (resourceUpdateService.addElement(element)){
								nbElement ++;
						}

					}else{
						logger.error("** PROBLEM ** In getting Element with null localElementID .");
					}
				}
			} catch (RemoteException e) {
				logger.error("** PROBLEM ** Cannot get information using ESummary." , e);
			}
		}
		return nbElement;
	}

	@Override
	public String elementURLString(String elementLocalID) {
		return PCM_ELT_URL + elementLocalID;
	}

	@Override
	public String mainContextDescriptor() {
		return PCM_MAIN_ITEMKEY;
	}

	@Override
	protected String stringToNCBITerm(String query){
		return super.stringToNCBITerm(query)+ "+AND+has_mesh[filter]";
	}

}
