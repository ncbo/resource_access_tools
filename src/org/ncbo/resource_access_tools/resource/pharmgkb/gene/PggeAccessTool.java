package org.ncbo.resource_access_tools.resource.pharmgkb.gene;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;

import org.ncbo.resource_access_tools.populate.Element;
import org.ncbo.resource_access_tools.populate.Structure;

import org.ncbo.resource_access_tools.enumeration.ResourceType;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;

/**
 * extract genes from PharmGKB.
 * Use PharmGKB perl script to call a SOAP web services.
 * @author  Adrien Coulet
 * @version OBR v1
 * @date    15-May-2009
 */
public class PggeAccessTool extends ResourceAccessTool {

	private static final String PGGE_URL         = "http://www.pharmgkb.org/";
	private static final String PGGE_NAME        = "PharmGKB [Gene]";
	private static final String PGGE_RESOURCEID  = "PGGE";
	private static final String PGGE_DESCRIPTION = "PharmGKB curates information that establishes knowledge about the relationships among drugs, diseases and genes, including their variations and gene products.";
	private static final String PGGE_LOGO        = "https://www.pharmgkb.org/images/header/title.png";
	private static final String PGGE_ELT_URL     = "https://www.pharmgkb.org/do/serve?objId=";

	private static final String[] PGGE_ITEMKEYS  = {"geneName", "geneSymbol", "geneAlternateSymbols", "geneRelatedDrugs","geneRelatedPathways", "geneRelatedDiseases", "geneRelatedPhenotypeDatasets"};
	private static final Double[] PGGE_WEIGHTS 	 = { 1.0,              0.9,                0.9,                0.5,                  0.5,                   0.5,                            0.4};

	private static Structure PGGE_STRUCTURE      = new Structure(PGGE_ITEMKEYS, PGGE_RESOURCEID, PGGE_WEIGHTS);
	private static String PGGE_MAIN_ITEMKEY      = "geneName";

	public PggeAccessTool(){
		super(PGGE_NAME, PGGE_RESOURCEID, PGGE_STRUCTURE);
		try {
			this.getToolResource().setResourceURL(new URL(PGGE_URL));
			this.getToolResource().setResourceLogo(new URL(PGGE_LOGO));
			this.getToolResource().setResourceElementURL(PGGE_ELT_URL);
		} catch (MalformedURLException e) {
			logger.error(EMPTY_STRING, e);
		}
		this.getToolResource().setResourceDescription(PGGE_DESCRIPTION);
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
			logger.info("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName() + " with service web");
		}
		return nbElement;
	}

	@Override
	public String elementURLString(String elementLocalID) {
		return new String(PGGE_ELT_URL + elementLocalID);
	}

	@Override
	public String mainContextDescriptor() {
		return PGGE_MAIN_ITEMKEY;
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

		Element myGene;

		// get the list of element
		HashSet<String> geneList = this.getAllGenes();

		// gets the elements already in the corresponding _ET and keeps only the difference
		HashSet<String> allElementsInET = resourceUpdateService.getAllLocalElementIDs();
		geneList.removeAll(allElementsInET);

		// for each Gene element accessed by the tool
		for (String localGeneID: geneList){
			try{
				// populates OBR_PGGE_ET with each of these genes
				myGene = this.getOneGeneData(localGeneID);
				//System.out.println(myGene.getElementStructure().toString());
				if(!myGene.getElementStructure().hasNullValues()){
					if(resourceUpdateService.addElement(myGene)){
						nbAdded++;
					}
					// System.out.println(myGene.getGeneName()+", "+myGene.getGeneAlternateNames()+", "+myGene.getGeneRelatedPathways()+", "+myGene.getGeneRelatedGenes()+", "+myGene.getGeneRelatedPhenotypeDatasets());
					/*if(toolPGGE_ET.addEntry(new GeneEntry(localGeneID, myGene.getGeneName(),
							myGene.getGeneAlternateNames(), myGene.getGeneRelatedGenes(),
							myGene.getGeneRelatedPathways(), myGene.getGeneRelatedDiseases(),
							myGene.getGeneRelatedPhenotypeDatasets()))){
						nbAdded++;
					}*/
				}
			} catch (Exception e) {
				logger.error("** PROBLEM ** Problem with gene "+ localGeneID +" when populating the OBR_PGGE_ET table.", e);
			}
		}
		logger.info(nbAdded+" gene added to the OBR_PGGE_ET table.");
		return nbAdded;
	}

	/**
	 * get all PharmGKB gene with supporting information
	 */
	public HashSet<String> getAllGenes(){
		logger.info("* Get All PharmGKB Genes... ");
		HashSet<String> geneList = new HashSet<String>();
		int nbAdded = 0;
		try{
			GetPgkbGeneList myClientLauncher = new GetPgkbGeneList();
			geneList = myClientLauncher.getList();
		}catch(Exception e){
			logger.error("** PROBLEM ** Problem with PharmGKB web service specialSearch.pl 6", e);
		}
		nbAdded = geneList.size();
		logger.info((nbAdded)+" genes found.");
		return geneList;
	}
	/**
	 * get gene related data enclosed in an Element
	 */
	public Element getOneGeneData(String elementLocalId){
		// for one Gene, get relative data in pharmgkb (eg alternate names, related genes, etc.)
		// and put it in the Structure of an Element
		Element myGene = null;
		try{
			GetPgkbGeneData myClientLauncher2 = new GetPgkbGeneData(this.getToolResource());
			myGene = myClientLauncher2.getGeneElement(elementLocalId);
		}catch (Exception e) {
				logger.error("** PROBLEM ** Problem with PharmGKB web service genes.pl with gene "+ elementLocalId +".", e);
		}
		return myGene;
	}
}
