package org.ncbo.resource_access_tools.resource.ncbi.gap;

import gov.nih.nlm.ncbi.www.soap.eutils.esummary.DocSumType;
import gov.nih.nlm.ncbi.www.soap.eutils.esummary.ESummaryRequest;
import gov.nih.nlm.ncbi.www.soap.eutils.esummary.ESummaryResult;
import gov.nih.nlm.ncbi.www.soap.eutils.esummary.ItemType;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;

import org.ncbo.resource_access_tools.enumeration.ResourceType;
import org.ncbo.resource_access_tools.populate.Element;
import org.ncbo.resource_access_tools.populate.Structure;
import org.ncbo.resource_access_tools.populate.Element.BadElementStructureException;
import org.ncbo.resource_access_tools.resource.ncbi.AbstractNcbiResourceAccessTool;

/**
 * DbGapAccessTool is responsible for getting data elements for
 * Database of Genotypes and Phenotypes (dbGaP) DataSets.
 * It process all data elements from study data.
 * For the getting element it uses E-Utilities
 *
 * @author
 * @version $$
 */
public class DbGapAccessTool extends AbstractNcbiResourceAccessTool {

	// Home URL of the resource
	private static final String GAP_URL			= "http://www.ncbi.nlm.nih.gov/sites/entrez?Db=gap";

	// Name of the resource
	private static final String GAP_NAME 		= "Database of Genotypes and Phenotypes";

	// Short name of the resource
	private static final String GAP_RESOURCEID 	= "GAP";

	// Text description of the resource
	private static final String GAP_DESCRIPTION = "The database of Genotypes and Phenotypes (dbGaP) was developed to archive and distribute the results of studies that have investigated the interaction of genotype and phenotype. Such studies include genome-wide association studies, medical sequencing, molecular diagnostic assays, as well as association between genotype and non-clinical traits.";

	// URL that points to the logo of the resource
	private static final String GAP_LOGO 		= "http://www.ncbi.nlm.nih.gov/projects/gap/images/dbGaP_logo_final.jpg";

	//basic URL that points to an element when concatenated with an local element ID
	private static final String GAP_ELT_URL 	= "http://www.ncbi.nlm.nih.gov/projects/gap/cgi-bin/study.cgi?study_id=";
	//->

	//Database name for E-Utils.
	private static final String GAP_EUTILS_DB 	= "gap";

	//Query terms for E-Utils .This terms get all elements for gap data for GAP
	private static final String GAP_EUTILS_TERM =  "1[s_discriminator]";

	// The set of context names
	private static final String[] GAP_ITEMKEYS = {	UID_COLUMN,  "study_name",							"study_disease_list_msh",		"study_disease_list_snomedct",		"study_disease_list_ncit"	};
	// Weight associated to a context
	private static final Double[] GAP_WEIGHTS  = {	0.0,         1.0,									1.0,							1.0,								1.0							};
	// OntoID associated for reported annotations (MSH ontology : 1351, SNOMEDCT : 1353, NCI Thesaurus :1032)
	private static final String[] GAP_ONTOIDS  = {	Structure.NOT_FOR_ANNOTATION, Structure.FOR_CONCEPT_RECOGNITION,		"1351",							"1353",							"1032"						};

	// Structure for GAP Access tool
	private static final Structure GAP_STRUCTURE = new Structure(GAP_ITEMKEYS, GAP_RESOURCEID, GAP_WEIGHTS, GAP_ONTOIDS);

	// String constant for null string
	protected static final String NULL_STRING=null;

	// A context name used to describe the associated element
	private static final String GAP_MAIN_ITEMKEY = "study_name";

	// A context name used to describe the associated element
	private static final String GAP_TAGNAME_STUDY_ID = "d_study_id";

	// Contents name for study tag(study information contains below this tags')
	private static final String GAP_TAGNAME_STUDY =	"d_study_results";

	// Contents name for study name
	private static final String GAP_TAGNAME_STUDY_NAME =	"d_study_name";

	// Contents name for study tag(study information contains below this tags')
	private static final String GAP_TAGNAME_STUDY_LIST = "d_study_disease_list";


	/**
	 * Construct DbGapAccessTool using database connection property
	 * It set properties for tool Resource
	 *
	 */
	public DbGapAccessTool(){
		super(GAP_NAME, GAP_RESOURCEID, GAP_STRUCTURE);
		try {
			this.getToolResource().setResourceURL(new URL(GAP_URL));
			this.getToolResource().setResourceLogo(new URL(GAP_LOGO));
			this.getToolResource().setResourceElementURL(GAP_ELT_URL);
		} catch (MalformedURLException e) {
			logger.error(EMPTY_STRING, e);
		}
		this.getToolResource().setResourceDescription(GAP_DESCRIPTION);
	}

	@Override
	public ResourceType  getResourceType() {
		return ResourceType.SMALL;
	}

	@Override
	protected String getEutilsDB() {
		return GAP_EUTILS_DB;
	}

	@Override
	protected String getEutilsTerm() {
		return GAP_EUTILS_TERM;
	}

	@Override
	public void updateResourceInformation() {
		// TODO See if it can be implemented for this resource.
	}

	@Override
	public int updateResourceContent(){
		// eutilsUpdateFromDate will not work for GAP as NCBI doesn't take into account reldate for gap.
		return super.eutilsUpdateAll(UID_COLUMN);
	}

	/**
	 * This method extract data from GAP resource
	 * and populate the Table OBR_GAP_ET  with data elements for GAP data
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
					// This section depends of the structure and the type of content we want to get back

					docSumItems = resultDocSums[i].getItem();

					for (ItemType docSumItem : docSumItems) {
						if(docSumItem.getName().equals(GAP_TAGNAME_STUDY)){

							String localElementID= null;
							eltStructure.putContext(Structure.generateContextName(GAP_RESOURCEID, GAP_ITEMKEYS[0]), resultDocSums[i].getId());
							for(ItemType itemType : docSumItem.getItem()){
								if(GAP_TAGNAME_STUDY_ID.equalsIgnoreCase(itemType.getName())){
									localElementID= getItemTypeContent(itemType);
								}
								else if(GAP_TAGNAME_STUDY_NAME.equalsIgnoreCase(itemType.getName())){
									// 1st element of docSumItems contains study name context
									eltStructure.putContext(Structure.generateContextName(GAP_RESOURCEID, GAP_ITEMKEYS[1]),getItemTypeContent(itemType));
								}
								//rajesh
								/*else if(GAP_TAGNAME_STUDY_LIST.equalsIgnoreCase(itemType.getName())){

									String diseaseList = getItemTypeContent(itemType, COMMA_STRING);
									String concepts_MSH = resourceUpdateService.mapTermsToVirtualLocalConceptIDs(diseaseList, GAP_ONTOIDS[2], COMMA_STRING);
									// if mapping concepts are null or empty then log message for it.
									if(concepts_MSH== null || concepts_MSH.trim().length()== 0){
										logger.error("Cannot map study_disease_list_MSH  '" + diseaseList + "' to local concept id for element with ID " + localElementID +".");

									}
									eltStructure.putContext(Structure.generateContextName(GAP_RESOURCEID, GAP_ITEMKEYS[2]), concepts_MSH);

									// 3rd elements of docSumItems contains study disease SNOMEDCT list context
									String concepts_SNOMEDCT = resourceUpdateService.mapTermsToVirtualLocalConceptIDs(diseaseList, GAP_ONTOIDS[3], COMMA_STRING);
									// if mapping concepts are null or empty then log message for it.
									if(concepts_SNOMEDCT== null || concepts_SNOMEDCT.trim().length()== 0){
										logger.error("Cannot map study_disease_list_SNOMEDCT  '" + diseaseList + "' to local concept id for element with ID " + localElementID +".");

									}
									eltStructure.putContext(Structure.generateContextName(GAP_RESOURCEID, GAP_ITEMKEYS[3]), concepts_SNOMEDCT);

									//4th elements of docSumItems contains study disease 13578 list context
									String concepts_13578 = resourceUpdateService.mapTermsToVirtualLocalConceptIDs(diseaseList, GAP_ONTOIDS[4], COMMA_STRING);
									// if mapping concepts are null or empty then log message for it.
									if(concepts_13578== null || concepts_13578.trim().length()== 0){
										logger.error("Cannot map study_disease_list_13578  '" + diseaseList + "' to local concept id for element with ID " + localElementID +".");

									}
									eltStructure.putContext(Structure.generateContextName(GAP_RESOURCEID, GAP_ITEMKEYS[4]), concepts_13578);

								}*/
							}

							if(localElementID != null){
								element = new Element(localElementID, eltStructure);
								if (this.resourceUpdateService.addElement(element)){
										nbElement ++;
								}

							}else{
								logger.error(" In getting Element with null localElementID .");
							}
						}
					}
					//Checks parent tag start with "d_study_results"


				}
			} catch (RemoteException e) {
				logger.error("** PROBLEM ** Cannot get information using ESummary." , e);
			}
		}
		return nbElement;
	}

	@Override
	public String elementURLString(String elementLocalID) {
		return GAP_ELT_URL + elementLocalID;
	}

	@Override
	public String mainContextDescriptor() {
		return GAP_MAIN_ITEMKEY;
	}
	@Override
	protected String stringToNCBITerm(String query){
		return super.stringToNCBITerm(query)+ "+AND+("+GAP_EUTILS_TERM+")";
	}
}
