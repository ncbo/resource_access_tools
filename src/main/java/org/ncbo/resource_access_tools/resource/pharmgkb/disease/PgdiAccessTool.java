package org.ncbo.resource_access_tools.resource.pharmgkb.disease;

import org.ncbo.resource_access_tools.enumeration.ResourceType;
import org.ncbo.resource_access_tools.populate.Element;
import org.ncbo.resource_access_tools.populate.Structure;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;

/**
 * extract disease from PharmGKB.
 * Use PharmGKB perl script to call a SOAP web services.
 *
 * @author Adrien Coulet
 * @version OBR v1
 * @date 15-May-2009
 */
public class PgdiAccessTool extends ResourceAccessTool {

    private static final String PGDI_URL = "http://www.pharmgkb.org/";
    private static final String PGDI_NAME = "PharmGKB [Disease]";
    private static final String PGDI_RESOURCEID = "PGDI";
    private static final String PGDI_DESCRIPTION = "PharmGKB curates information that establishes knowledge about the relationships among drugs, diseases and genes, including their variations and gene products.";
    private static final String PGDI_LOGO = "https://www.pharmgkb.org/images/header/title.png";
    private static final String PGDI_ELT_URL = "https://www.pharmgkb.org/do/serve?objId=";

    private static final String[] PGDI_ITEMKEYS = {"diseaseName", "diseaseAlternateNames", "diseaseRelatedGenes", "diseaseRelatedPathways", "diseaseRelatedDrugs", "diseaseRelatedPhenotypeDatasets"};
    private static final Double[] PGDI_WEIGHTS = {1.0, 0.8, 0.5, 0.5, 0.5, 0.4};

    private static final Structure PGDI_STRUCTURE = new Structure(PGDI_ITEMKEYS, PGDI_RESOURCEID, PGDI_WEIGHTS);

    public PgdiAccessTool() {
        super(PGDI_NAME, PGDI_RESOURCEID, PGDI_STRUCTURE);
        try {
            this.getToolResource().setResourceURL(new URL(PGDI_URL));
            this.getToolResource().setResourceLogo(new URL(PGDI_LOGO));
            this.getToolResource().setResourceElementURL(PGDI_ELT_URL);
        } catch (MalformedURLException e) {
            logger.error(EMPTY_STRING, e);
        }
        this.getToolResource().setResourceDescription(PGDI_DESCRIPTION);
    }

    @Override
    public ResourceType getResourceType() {
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
            logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName() + " with service web");
        }
        return nbElement;
    }

    @Override
    public String elementURLString(String elementLocalID) {
        return PGDI_ELT_URL + elementLocalID;
    }

    @Override
    public String mainContextDescriptor() {
        String PGDI_MAIN_ITEMKEY = "diseaseName";
        return PGDI_MAIN_ITEMKEY;
    }

    /**
     * The updating function that is just adding new entries in the database.
     * No modification of existing entries is managed for now.
     *
     * @return the number of new element added to the OBR_XX_ET table.
     */
    private int updates() {
        int nbAdded = 0;

        Element myDisease;

        // get the list of element
        HashSet<String> diseaseList = this.getAllDiseases();

        // gets the elements already in the corresponding _ET and keeps only the difference
        HashSet<String> allElementsInET = resourceUpdateService.getAllLocalElementIDs();
        diseaseList.removeAll(allElementsInET);

        // for each disease element accessed by the tool
        for (String localDiseaseID : diseaseList) {
            //System.out.println("localDiseaseID: "+localDiseaseID);
            try {
                // populates OBR_PGDI_ET with each of these diseases
                myDisease = this.getOneDiseaseData(localDiseaseID);
                //System.out.println(myDisease.getElementStructure().toString());
                if (!myDisease.getElementStructure().hasNullValues()) {
                    if (resourceUpdateService.addElement(myDisease)) {
                        nbAdded++;
                    }
                    // System.out.println(myDisease.getDiseaseName()+", "+myDisease.getDiseaseAlternateNames()+", "+myDisease.getDiseaseRelatedPathways()+", "+myDisease.getDiseaseRelatedDrugs()+", "+myDisease.getDiseaseRelatedPhenotypeDatasets());
                    /*if(toolPGDI_ET.addEntry(new DiseaseEntry(localDiseaseID, myDisease.getDiseaseName(),
							myDisease.getDiseaseAlternateNames(), myDisease.getDiseaseRelatedGenes(),
							myDisease.getDiseaseRelatedPathways(), myDisease.getDiseaseRelatedDrugs(),
							myDisease.getDiseaseRelatedPhenotypeDatasets()))){
						nbAdded++;
					}*/
                }
            } catch (Exception e) {
                logger.error("** PROBLEM ** Problem with disease " + localDiseaseID + " when populating the OBR_PGDI_ET table.", e);
            }
        }
        logger.info(nbAdded + " disease added to the OBR_PGDI_ET table.");
        return nbAdded;
    }

    /**
     * get all PharmGKB disease with supporting information
     */
    private HashSet<String> getAllDiseases() {
        logger.info("* Get All PharmGKB Diseases... ");
        HashSet<String> diseaseList = new HashSet<String>();
        int nbAdded;
        try {
            GetPgkbDiseaseList myClientLauncher = new GetPgkbDiseaseList();
            diseaseList = myClientLauncher.getList();
        } catch (Exception e) {
            logger.error("** PROBLEM ** Problem with PharmGKB web service specialSearch.pl 6", e);
        }
        nbAdded = diseaseList.size();
        logger.info((nbAdded) + " diseases found.");
        return diseaseList;
    }

    /**
     * get disease related data enclosed in an Element
     */
    private Element getOneDiseaseData(String elementLocalId) {
        // for one disease, get relative data in pharmgkb (eg alternate names, related drugs, etc.)
        // and put it in the Structure of an Element
        Element myDisease = null;
        try {
            GetPgkbDiseaseData myClientLauncher2 = new GetPgkbDiseaseData(this.getToolResource());
            myDisease = myClientLauncher2.getDiseaseElement(elementLocalId);
        } catch (Exception e) {
            logger.error("** PROBLEM ** Problem with PharmGKB web service diseases.pl with disease " + elementLocalId + ".", e);
        }
        return myDisease;
    }
}
