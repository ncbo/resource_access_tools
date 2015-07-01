package org.ncbo.resource_access_tools.resource.ncbi.geo;

import gov.nih.nlm.ncbi.www.soap.eutils.esummary.DocSumType;
import gov.nih.nlm.ncbi.www.soap.eutils.esummary.ESummaryRequest;
import gov.nih.nlm.ncbi.www.soap.eutils.esummary.ESummaryResult;
import gov.nih.nlm.ncbi.www.soap.eutils.esummary.ItemType;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;

import obs.obr.populate.Element;
import obs.obr.populate.Structure;
import obs.obr.populate.Element.BadElementStructureException;

import org.ncbo.stanford.obr.enumeration.ResourceType;
import org.ncbo.resource_access_tools.resource.ncbi.AbstractNcbiResourceAccessTool;

/**
 * GeoAccessTool is responsible for getting data elements for
 * Gene Expression Omnibus (GEO) DataSets.
 * It process all data elements from GDS and GSE data.
 * For the getting element it uses E-Utilities
 *
 * @author
 * @version $$
 */
public class GeoAccessTool extends AbstractNcbiResourceAccessTool {

	// Home URL of the resource
	private static final String GEO_URL			= "http://www.ncbi.nlm.nih.gov/geo/";

	// Name of the resource
	private static final String GEO_NAME 		= "Gene Expression Omnibus DataSets";

	// Short name of the resource
	private static final String GEO_RESOURCEID 	= "GEO";

	// Text description of the resource
	private static final String GEO_DESCRIPTION = "A gene expression/molecular abundance repository supporting  MIAME compliant data submissions, and a curated, online resource for gene expression data browsing, query and retrieval.";

	// URL that points to the logo of the resource
	private static final String GEO_LOGO 		= "http://www.ncbi.nlm.nih.gov/projects/geo/img/geo_main.gif";

	//Optra: basic URL that points to an element when concatenated with an local element ID
	private static final String GEO_ELT_URL 	= "http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=";
	//->

	//Database name for E-Utils.
	private static final String GEO_EUTILS_DB 	= "gds";

	//Optra:
	//Query terms for E-Utils .This terms get all elements for GDS and GSE data for GEO
	private static final String GEO_EUTILS_TERM =  "GDS[filter] OR GSE[filter]";
	//->

	// The set of context names
	private static final String[] GEO_ITEMKEYS = {UID_COLUMN,					"title",							"summary", 							"organism"};
	// Weight associated to a context
	private static final Double[] GEO_WEIGHTS  = {0.0, 							1.0, 								0.8, 								1.0};
	// OntoID associated for reported annotations (	NCBI organismal classification(1132) for organism)
	private static final String[] GEO_ONTOIDS  = {Structure.NOT_FOR_ANNOTATION, Structure.FOR_CONCEPT_RECOGNITION, 	Structure.FOR_CONCEPT_RECOGNITION, 	"1132"};

	// Structure for GEO Access tool
	private static final Structure GEO_STRUCTURE = new Structure(GEO_ITEMKEYS, GEO_RESOURCEID, GEO_WEIGHTS, GEO_ONTOIDS);

	// A context name used to describe the associated element
	private static final String GEO_MAIN_ITEMKEY = "title";


	// Prefix used to element id for GSE data element
	private static final String GEO_GSE_STRING = "GSE";

	// Prefix used to element id for GDS data element
	private static final String GEO_GDS_STRING = "GDS";

	// Taxon context
	private static final String GEO_TAXON_STRING = "taxon";

	// Entry Type
	private static final String GEO_ENTRY_TYPE_STRING = "entryType";




	/**
	 * Construct GeoAccessTool using database connection property
	 * It set properties for tool Resource
	 *
	 */
	public GeoAccessTool(){
		super(GEO_NAME, GEO_RESOURCEID, GEO_STRUCTURE);
		try {
			this.getToolResource().setResourceURL(new URL(GEO_URL));
			this.getToolResource().setResourceLogo(new URL(GEO_LOGO));
			this.getToolResource().setResourceElementURL(GEO_ELT_URL);
		} catch (MalformedURLException e) {
			logger.error(EMPTY_STRING, e);
		}
		this.getToolResource().setResourceDescription(GEO_DESCRIPTION);
	}

	@Override
	public ResourceType  getResourceType() {
		return ResourceType.SMALL;
	}

	@Override
	protected String getEutilsDB() {
		return GEO_EUTILS_DB;
	}

	@Override
	protected String getEutilsTerm() {
		return GEO_EUTILS_TERM;
	}

	@Override
	public void updateResourceInformation() {
		// TODO See if it can be implemented for this resource.
	}

	@Override
	public int updateResourceContent(){
		// eutilsUpdateFromDate will not work for GEO as NCBI doesn't take into account reldate for gds.
		return super.eutilsUpdateAll(UID_COLUMN);
	}

	/**
	 * This method extract data from GEO resource
	 * and populate the Table OBR_GEOGSE_ET  with data elements for GSE and GDS data
	 *
	 */
	 @Override
	 protected int updateElementTableWithUIDs(HashSet<String> UIDs) throws BadElementStructureException{
		int nbElement = 0;

		// Create request for e-utils
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
				for(int i=0; i<resultDocSums.length; i++){
					docSumItems = resultDocSums[i].getItem();
					// This section depends of the structure and the type of content we want to get back

					// resultDocSums[i].getID contains the UID
					eltStructure.putContext(Structure.generateContextName(GEO_RESOURCEID, GEO_ITEMKEYS[0]), resultDocSums[i].getId());
					//	logger.info("result UID: " + resultDocSums[i].getId());

					String localElementID =null;
					String entryType= null;

					for (ItemType docSumItem : docSumItems) {


						if(GEO_ITEMKEYS[1].equals(docSumItem.getName())){
							//  docSumItems contains title context
							eltStructure.putContext(Structure.generateContextName(GEO_RESOURCEID, GEO_ITEMKEYS[1]), getItemTypeContent(docSumItem));
						}
						else if(GEO_ITEMKEYS[2].equals(docSumItem.getName())){
							//  docSumItems contains summary context
							eltStructure.putContext(Structure.generateContextName(GEO_RESOURCEID, GEO_ITEMKEYS[2]), getItemTypeContent(docSumItem));
						}else if(GEO_TAXON_STRING.equals(docSumItem.getName())){
							//  element of docSumItems contains organism context
							String organism = getItemTypeContent(docSumItem) ;
							String concepts = resourceUpdateService.mapTermsToVirtualLocalConceptIDs(organism, GEO_ONTOIDS[3], SEMICOLON_STRING);
							// if mapping concepts are null or empty then log message for it.
							if(concepts== null || concepts.trim().length()== 0){
								logger.error("Cannot map Organism  '" + organism + "' to local concept id for element with ID " + localElementID +".");

							}
							eltStructure.putContext(Structure.generateContextName(GEO_RESOURCEID, GEO_ITEMKEYS[3]), concepts);

						}else if(GEO_ENTRY_TYPE_STRING.equals(docSumItem.getName())){
							entryType = getItemTypeContent(docSumItem);

							// if the entryType is GDS then append 'GDS' before element id
							if(GEO_GDS_STRING.equals(entryType)){

								// 0th element contains local element id for GDS
								localElementID=GEO_GDS_STRING + getItemTypeContent(docSumItems[0]);
							}

							// if the entryType is GSE then append  'GSE' before element id
							else if(GEO_GSE_STRING.equals(entryType)){
								// 4th element contains local element id for GSE
								localElementID=GEO_GSE_STRING +getItemTypeContent(docSumItems[4]);
							}

						}

					}
					// 1st element of docSumItems contains name context

					if(localElementID != null){
						element = new Element(localElementID, eltStructure);
						if (resourceUpdateService.addElement(element)){
								nbElement ++;
						}

					}else{
						logger.error("** PROBLEM ** In getting Element with null localElementID .");
					}
					//->
				}
			} catch (RemoteException e) {
				logger.error("** PROBLEM ** Cannot get information using ESummary." , e);
			}
		}
		return nbElement;
	}

	@Override
	public String elementURLString(String elementLocalID) {
		return GEO_ELT_URL + elementLocalID;
	}

	@Override
	public String mainContextDescriptor() {
		return GEO_MAIN_ITEMKEY;
	}

	@Override
	protected String stringToNCBITerm(String query){
		return super.stringToNCBITerm(query)+ "+AND+(GDS[filter]+OR+GSE[filter])";
	}

}
