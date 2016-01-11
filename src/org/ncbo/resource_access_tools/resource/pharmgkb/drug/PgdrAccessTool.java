package org.ncbo.resource_access_tools.resource.pharmgkb.drug;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;

import org.ncbo.resource_access_tools.populate.Element;
import org.ncbo.resource_access_tools.populate.Structure;

import org.ncbo.resource_access_tools.resource.ResourceAccessTool;
import org.ncbo.resource_access_tools.enumeration.ResourceType;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;

/**
 * extract drugs from PharmGKB.
 * Use PharmGKB perl script to call a SOAP web services.
 * @author  Adrien Coulet
 * @version OBR v1
 * @date    15-May-2009
 */
public class PgdrAccessTool extends ResourceAccessTool {

	private static final String PGDR_URL         = "http://www.pharmgkb.org/";
	private static final String PGDR_NAME        = "PharmGKB [Drug]";
	private static final String PGDR_RESOURCEID  = "PGDR";
	private static final String PGDR_DESCRIPTION = "PharmGKB curates information that establishes knowledge about the relationships among drugs, diseases and genes, including their variations and gene products.";
	private static final String PGDR_LOGO        = "https://www.pharmgkb.org/images/header/title.png";
	private static final String PGDR_ELT_URL     = "https://www.pharmgkb.org/do/serve?objId=";

	private static final String[] PGDR_ITEMKEYS  = {"drugName", "drugTradeNames", "drugGenericNames", "drugRelatedGenes","drugRelatedPathways", "drugRelatedDiseases", "drugRelatedPhenotypeDatasets"};
	private static final Double[] PGDR_WEIGHTS 	 = { 1.0,              0.9,                0.9,                0.5,                  0.5,                   0.5,                            0.4};

	private static Structure PGDR_STRUCTURE      = new Structure(PGDR_ITEMKEYS, PGDR_RESOURCEID, PGDR_WEIGHTS);
	private static String PGDR_MAIN_ITEMKEY      = "drugName";

	public PgdrAccessTool( ){
		super(PGDR_NAME, PGDR_RESOURCEID, PGDR_STRUCTURE );
		try {
			this.getToolResource().setResourceURL(new URL(PGDR_URL));
			this.getToolResource().setResourceLogo(new URL(PGDR_LOGO));
			this.getToolResource().setResourceElementURL(PGDR_ELT_URL);
		} catch (MalformedURLException e) {
			logger.error(EMPTY_STRING, e);
		}
		this.getToolResource().setResourceDescription(PGDR_DESCRIPTION);
	}

	@Override
	public ResourceType  getResourceType() {
		return ResourceType.SMALL;
	}

	@Override
	public void updateResourceInformation() {
		// TODO See if it can be implemented for this resource.
	}

	@Override
	public HashSet<String> queryOnlineResource(String query) {
		// TODO See if it can be implemented for this resource.
		return new HashSet<String>();
	}

	@Override
	public int updateResourceContent() {
		int nbElement = 0;
		try {
			nbElement = this.updates();
		} catch (Exception e) {
			logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName() + " with service web", e);
		}
		return nbElement;
	}

	@Override
	public String elementURLString(String elementLocalID) {
		return PGDR_ELT_URL + elementLocalID;
	}

	@Override
	public String mainContextDescriptor() {
		return PGDR_MAIN_ITEMKEY;
	}

	/**
	 *
	 * The updating function that is just adding new entries in the database.
	 * No modification of existing entries is managed for now.
	 *
	 * @return the number of new element added to the OBR_XX_ET table.
	 */
	private int updates(){
		int nbAdded = 0;

		Element myDrug;

		// get the list of element
		HashSet<String> drugList = this.getAllDrugs();

		// gets the elements already in the corresponding _ET and keeps only the difference
		HashSet<String> allElementsInET = resourceUpdateService.getAllLocalElementIDs();
		drugList.removeAll(allElementsInET);

		// for each Drug element accessed by the tool
		for (String localDrugID: drugList){

			try{
				// populates OBR_PGDR_ET with each of these drugs
				myDrug = this.getOneDrugData(localDrugID);
				//System.out.println(myDrug.getElementStructure().toString());
				if(!myDrug.getElementStructure().hasNullValues()){
					if(resourceUpdateService.addElement(myDrug)){
						nbAdded++;
					}
					// System.out.println(myDrug.getDrugName()+", "+myDrug.getDrugAlternateNames()+", "+myDrug.getDrugRelatedPathways()+", "+myDrug.getDrugRelatedDrugs()+", "+myDrug.getDrugRelatedPhenotypeDatasets());
					/*if(toolPGDR_ET.addEntry(new DrugEntry(localDrugID, myDrug.getDrugName(),
							myDrug.getDrugAlternateNames(), myDrug.getDrugRelatedGenes(),
							myDrug.getDrugRelatedPathways(), myDrug.getDrugRelatedDiseases(),
							myDrug.getDrugRelatedPhenotypeDatasets()))){
						nbAdded++;
					}*/
				}
			} catch (Exception e) {
				logger.error("** PROBLEM ** Problem with drug "+ localDrugID +" when populating the OBR_PGDR_ET table.", e);
			}

		}
		logger.info(nbAdded+" drug added to the OBR_PGDR_ET table.");
		return nbAdded;
	}

	/**
	 * get all PharmGKB drug with supporting information
	 */
	public HashSet<String> getAllDrugs(){
		logger.info("* Get All PharmGKB Drugs... ");
		HashSet<String> drugList = new HashSet<String>();
		int nbAdded = 0;
		try{
			GetPgkbDrugList myClientLauncher = new GetPgkbDrugList();
			drugList = myClientLauncher.getList();
		}catch(Exception e){
			logger.error("** PROBLEM ** Problem with PharmGKB web service specialSearch.pl 6", e);
		}
		nbAdded = drugList.size();
		logger.info((nbAdded)+" drugs found.");
		return drugList;
	}
	/**
	 * get drug related data enclosed in an Element
	 */
	public Element getOneDrugData(String elementLocalId){
		// for one Drug, get relative data in pharmgkb (eg alternate names, related drugs, etc.)
		// and put it in the Structure of an Element
		Element myDrug = null;
		try{
			GetPgkbDrugData myClientLauncher2 = new GetPgkbDrugData(this.getToolResource());
			myDrug = myClientLauncher2.getDrugElement(elementLocalId);
		}catch (Exception e) {
				logger.error("** PROBLEM ** Problem with PharmGKB web service drugs.pl with drug "+ elementLocalId +".", e);
		}
		return myDrug;
	}
}
