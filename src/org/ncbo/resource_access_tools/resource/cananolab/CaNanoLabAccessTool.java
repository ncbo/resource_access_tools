package org.ncbo.resource_access_tools.resource.cananolab;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;

import obs.obr.populate.Structure;

import org.ncbo.stanford.obr.enumeration.ResourceType;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;

import edu.wustl.utill.CaNanoLabContextDetail;
import edu.wustl.utill.CaNanoLabNodeDetail;
import edu.wustl.utill.CaNanoLabUtility;

/**
 * This Class defines all details needed for annotating free context from
 * caNanoLab resource.It also populates ET table ..
 *
 * @author lalit_chand
 */

public class CaNanoLabAccessTool extends ResourceAccessTool {

    // Home URL of caNanoLab
    private static final String CANANO_LAB_URL = CaNanoLabUtility.getCananoLabHomeURL();

    // Resource Name
    private static final String CANANO_LAB_NAME = CaNanoLabUtility.getCananoLabResourceName();

    // Resource Id
    private static final String CANANOLAB_RESOURCE_ID = CaNanoLabUtility.getCananoLabResourceId();

    // Resource Description
    private static final String CANANO_DESCRIPTION = CaNanoLabUtility.getCananoLabDescription();

// // This is the URL which points to specific information of caNanoLab nanoParticle by appending nanoParticle Id
//    private static final String CANANO_ELT_URL = CaNanoLabUtility.getCananoLabElementURL();

    // URL of caNano logo
    private static final String CANANO_LOGO = CaNanoLabUtility.getCananoLabLogoURL();

    // Context Names for caNanoLab
   private static final String[] CANANO_ITEMKEYS = CaNanoLabContextDetail.getItemKeys();
   //  private static final String[] CANANO_ITEMKEYS = CaNanoLabUtility.getSubContexts();

    // Weights associated with each context
    private static final Double[] CANANO_WEIGHTS = CaNanoLabUtility.getWeights();
    //private static final Double[] CANANO_WEIGHTS = CaNanoLabUtility.getSubWeights();

    // Default Ontology Id associated with each context
    private static final String[] CANANO_ONTOIDS = CaNanoLabUtility.getOntologyIds();

   // private static final String[] CANANO_ONTOIDS = CaNanoLabUtility.getSubOntologies();

    private static Structure CANANO_STRUCTURE = new Structure(CANANO_ITEMKEYS, CANANOLAB_RESOURCE_ID,
            CANANO_WEIGHTS, CANANO_ONTOIDS);

    private static String CANANO_MAIN_ITEMKEY = CANANO_ITEMKEYS[4];

    public CaNanoLabAccessTool() {

        super(CANANO_LAB_NAME, CANANOLAB_RESOURCE_ID, CANANO_STRUCTURE);
        try {
            this.getToolResource().setResourceURL(new URL(CANANO_LAB_URL));
            this.getToolResource().setResourceLogo(new URL(CANANO_LOGO));
        } catch (MalformedURLException e) {
            logger.error("Malformed URL Exception Occured", e);
        }
        this.getToolResource().setResourceDescription(CANANO_DESCRIPTION);
        this.getToolResource().setResourceElementURL(NOT_APPLICABLE);

    }

    @Override
	public ResourceType  getResourceType() {
		return ResourceType.SMALL;
	}

    @Override
    public String elementURLString(String localElementID) {
    	return resourceUpdateService.getContextValueByContextName(
                                                 localElementID,
                                                 Structure.generateContextName(CANANOLAB_RESOURCE_ID,
                                                		 			CANANO_ITEMKEYS[CANANO_ITEMKEYS.length-1]));
    }

    @Override
    public void updateResourceInformation() {
        // not implemented
    }

    @Override
    public String mainContextDescriptor() {
        return CANANO_MAIN_ITEMKEY;
    }



    public String itemKeyForAnnotationForBP() {
        return CANANO_MAIN_ITEMKEY;
    }


    @Override
    public HashSet<String> queryOnlineResource(String query) {
        // Not implemented for caNanoLab resource
        return null;
    }

    @Override
    /**
     * This method updates the ET table
     */
    public int updateResourceContent() {

        List<CaNanoLabNodeDetail> caNanoLabNodeDetails = getAllCananoLabDetails();
        return insertElements(caNanoLabNodeDetails);
    }

    /**
     * @param np  NanoParticleSample for which context to be populated.
     * @return It returns the element
     */
    private int insertElements(List<CaNanoLabNodeDetail> details) {
        GetCananoLabData myExtractor = new GetCananoLabData();
        return myExtractor.insertElements(details, this);
    }

    private List<CaNanoLabNodeDetail> getAllCananoLabDetails() {
        GetCananoLabData dataProvider = new GetCananoLabData();
        return dataProvider.getCananolabNodeDetails();
    }


    public static void main(String[] args) {
    	GetCananoLabData dataProvider = new GetCananoLabData();
        for(CaNanoLabNodeDetail detail:dataProvider.getCananolabNodeDetails()){
        	System.out.println(detail.getSampleSet());
        }
	}
}
