package org.ncbo.resource_access_tools.resource.clinicaltrials;

import org.ncbo.resource_access_tools.enumeration.ResourceType;
import org.ncbo.resource_access_tools.populate.Element;
import org.ncbo.resource_access_tools.populate.Element.BadElementStructureException;
import org.ncbo.resource_access_tools.populate.Structure;
import org.ncbo.resource_access_tools.resource.AbstractXmlResourceAccessTool;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class ClinicalTrialsAccessTool extends AbstractXmlResourceAccessTool {

    private static final String CT_URL = "http://clinicaltrials.gov/";
    private static final String CT_NAME = "ClinicalTrials.gov";
    private static final String CT_RESOURCEID = "CT";
    private static final String CT_DESCRIPTION = "ClinicalTrials.gov provides regularly updated information about federally and privately supported clinical research in human volunteers. ClinicalTrials.gov gives you information about a trial's purpose, who may participate, locations, and phone numbers for more details. The information provided on ClinicalTrials.gov should be used in conjunction with advice from health care professionals. Before searching, you may want to learn more about clinical trials.";
    private static final String CT_LOGO = "http://clinicaltrials.gov/ct2/html/images/frame/title.gif";
    private static final String CT_ELT_URL = "http://clinicaltrials.gov/ct/show/";
    private static final String CT_SERVICE = "http://clinicaltrials.gov/ct2/results?displayxml=true";

    private static final String[] CT_ITEMKEYS = {"brief_title", "official_title", "brief_summary", "detailed_description", "condition", "intervention"};
    private static final Double[] CT_WEIGHTS = {1.0, 1.0, 0.8, 0.7, 0.6, 0.6};

    private static final Structure CT_STRUCTURE = new Structure(CT_ITEMKEYS, CT_RESOURCEID, CT_WEIGHTS);
    private static final String CT_MAIN_ITEMKEY = "brief_title";

    private static final String CT_XML_ROOT = "clinical_study";
    private static final String CT_NCT_ID_STRING = "nct_id";
    private static final String CT_TXT_BLOCK_STRING = "textblock";

    // Constant for
    private static final int ELT_PER_PAGE = 1000;
    private int totalNumberElement;

    private static final int CT_MAX_NUMBER_ELEMENT_TO_PROCESS = 10000;

    public ClinicalTrialsAccessTool() {
        super(CT_NAME, CT_RESOURCEID, CT_STRUCTURE);
        try {
            this.getToolResource().setResourceURL(new URL(CT_URL));
            this.getToolResource().setResourceLogo(new URL(CT_LOGO));
            this.getToolResource().setResourceElementURL(CT_ELT_URL);
        } catch (MalformedURLException e) {
            logger.error(EMPTY_STRING, e);
        }

        this.getToolResource().setResourceDescription(CT_DESCRIPTION);

    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.MEDIUM;
    }

    @Override
    public int getMaxNumberOfElementsToProcess() {
        return CT_MAX_NUMBER_ELEMENT_TO_PROCESS;
    }

    @Override
    public void updateResourceInformation() {
        // TODO See if it can be implemented for this resource.
    }

    @Override
    public int updateResourceContent() {
        int nbElement = 0;
        ClinicalTrialElement ctElement;
        Element element;
        //ArrayList<String> newNCTIds = this.newNCTIds();
        ArrayList<String> newNCTIds = this.allNCTIds();
        HashSet<String> allElementLocalIDs = resourceUpdateService.getAllLocalElementIDs();
        newNCTIds.removeAll(allElementLocalIDs);
        logger.info("Number of elements to download: " + newNCTIds.size());
        String NCTId = null;

        /*
         * Optra: Exception handling for creating element. If one of the element throws
		 * exception then continue with next element
		 */
        for (Iterator<String> it = newNCTIds.iterator(); it.hasNext(); ) {
            try {
                NCTId = it.next();
                ctElement = new ClinicalTrialElement(NCTId, this);
                element = ctElement.getElement();
                if (resourceUpdateService.addElement(element)) {
                    nbElement++;
                }
            } catch (Exception e) {
                logger.error("Problem in processing element with ID " + NCTId, e);
            }
        }

		/*
		 * If number of elements in database are less then total number of element
		 * present on Clinical trails site then again update database.
		 */
        if (nbElement + allElementLocalIDs.size() < totalNumberElement) {
            return nbElement + updateResourceContent();
        }
        //->

        return nbElement;
    }

/*
	private ArrayList<String> newNCTIds(){
		ArrayList<String> newNCTIds = new ArrayList<String>();
		int nbDays = this.numberOfDaysSinceLastUpdate();
		Document dom = XMLResourceAccessTool.parseXML(CT_SERVICE + "&rcv_d=" + nbDays);
		//extraction of the number of result
		int nbNewElt = Integer.parseInt(dom.getDocumentElement().getAttribute("count"));
		logger.info("Last modification of the table was " + nbDays + " days ago. There are "+ nbNewElt+" new elements online.");
		int numberOfPage = (int)Math.ceil(nbNewElt/50)+1;
		//generation of a new XML document for each pages
		for(int p=1; p<=numberOfPage; p++){
			dom = XMLResourceAccessTool.parseXML(CT_SERVICE + "&pg=" + p + "&rcv_d=" + nbDays);
			//addition of the extracted NCTIds to the ArrayList
			newNCTIds.addAll(this.getNCTIdsFromXML(dom));
		}
		return newNCTIds;
	}
*/

/*
	@Override
	public int numberOfDaysSinceLastUpdate(){
		return 10;
	}
*/

    private ArrayList<String> allNCTIds() {
        ArrayList<String> allNCTIds = new ArrayList<String>();
        Document dom = AbstractXmlResourceAccessTool.parseXML(CT_SERVICE);

        //Extraction of the number of result
        totalNumberElement = Integer.parseInt(dom.getDocumentElement().getAttribute("count"));
        logger.info("Total number of elements on " + this.getToolResource().getResourceName() + ": " + totalNumberElement);
        int numberOfPage = (int) Math.ceil(totalNumberElement / ELT_PER_PAGE) + 1;
        String requestURL;
        //
		/*
		 * Generation of a new XML document for each page.
		 * e.g.URL http://clinicaltrials.gov/ct2/results?displayxml=true&count=1000&start=1
		 * gives 1000 element start from order 1.
		 *
		 */
        for (int p = 0; p < numberOfPage; p++) {
            // Request URL to get 1000 clinical stydy Element.
            requestURL = CT_SERVICE + "&count=" + ELT_PER_PAGE + "&start=" + (p * ELT_PER_PAGE + 1);
            logger.info("Getting NCT IDs for URL : " + requestURL);
            dom = AbstractXmlResourceAccessTool.parseXMLWithReconnect(requestURL);
            //Optra: If Dom is not null then get NCTIds from XML.
            if (dom != null) {
                //Addition of the extracted NCTIds to the collection of NCTIds
                allNCTIds.addAll(this.getNCTIdsFromXML(dom));
            } else {
                logger.error("Problem in processing Page Number : " + (p + 1) + " with URL : " + requestURL);
            }
            //->
        }
        return allNCTIds;
    }

    private ArrayList<String> getNCTIdsFromXML(Document document) {
        ArrayList<String> newNCTIds = new ArrayList<String>();
        org.w3c.dom.Element domRoot = document.getDocumentElement();
        NodeList clinicalStudyList = domRoot.getElementsByTagName(CT_XML_ROOT);
        org.w3c.dom.Element clinicalStudy;
        if (clinicalStudyList != null && clinicalStudyList.getLength() > 0) {
            int listSize = clinicalStudyList.getLength();
            for (int i = 0; i < listSize; i++) {
                clinicalStudy = (org.w3c.dom.Element) clinicalStudyList.item(i);
                //logger.info("Adding NCT ID for element with order " + clinicalStudy.getElementsByTagName("order").item(0).getTextContent());
                newNCTIds.add(clinicalStudy.getElementsByTagName(CT_NCT_ID_STRING).item(0).getTextContent());
            }
        }
        return newNCTIds;
    }

    @Override
    public String elementURLString(String elementLocalID) {
        return CT_ELT_URL + elementLocalID;
    }

    @Override
    public String mainContextDescriptor() {
        return CT_MAIN_ITEMKEY;
    }

    @Override
    public HashSet<String> queryOnlineResource(String query) {
        HashSet<String> answerIDs = new HashSet<String>();
        String term = query.replaceAll(" ", "+");
        Document dom = AbstractXmlResourceAccessTool.parseXML(CT_SERVICE + "&term=%22" + term + "%22");
        //extraction of the number of result
        int nbNewElt = Integer.parseInt(dom.getDocumentElement().getAttribute("count"));
        int numberOfPage = (int) Math.ceil(nbNewElt / 50) + 1;
        //generation of a new XML document for each pages
        for (int p = 1; p <= numberOfPage; p++) {
            dom = AbstractXmlResourceAccessTool.parseXML(CT_SERVICE + "&pg=" + p + "&term=%22" + term + "%22");
            //addition of the extracted NCTIds to the Set
            answerIDs.addAll(this.getNCTIdsFromXML(dom));
        }
        return answerIDs;
    }

    private static String cleanString(String ctString) {
        ctString = ctString.replaceAll("\n", " ");
        ctString = ctString.replaceAll(" +", " ");
        return ctString;
    }

    private class ClinicalTrialElement {

        final ClinicalTrialsAccessTool eltCTTool;
        final String eltNCTId;
        final HashMap<String, String> eltInfo;
        final String ELT_BRIEF_TITLE = Structure.generateContextName(CT_RESOURCEID, CT_ITEMKEYS[0]);
        final String ELT_OFFICIAL_TITLE = Structure.generateContextName(CT_RESOURCEID, CT_ITEMKEYS[1]);
        final String ELT_BRIEF_SUMMARY = Structure.generateContextName(CT_RESOURCEID, CT_ITEMKEYS[2]);
        final String ELT_DETAILED_DESCRIPTION = Structure.generateContextName(CT_RESOURCEID, CT_ITEMKEYS[3]);
        final String ELT_CONDITION = Structure.generateContextName(CT_RESOURCEID, CT_ITEMKEYS[4]);
        final String ELT_INTERVENTION = Structure.generateContextName(CT_RESOURCEID, CT_ITEMKEYS[5]);

        ClinicalTrialElement(String NCTId, ClinicalTrialsAccessTool ctTool) {
            this.eltCTTool = ctTool;
            this.eltNCTId = NCTId;
            this.eltInfo = new HashMap<String, String>(6);
            Document dom = AbstractXmlResourceAccessTool.parseXMLWithReconnect(CT_ELT_URL + NCTId + "?&displayxml=true");

            org.w3c.dom.Element domRoot = dom.getDocumentElement();

            //done according to the DTD available at http://clinicaltrials.gov/ct2/html/images/info/public.dtd
            //NodeList briefTitleList = domRoot.getElementsByTagName(ELT_BRIEF_TITLE);

            //Optra: Get brief_title from domRoot
            NodeList briefTitleList = domRoot.getElementsByTagName(CT_ITEMKEYS[0]);
            this.eltInfo.put(ELT_BRIEF_TITLE, cleanString(briefTitleList.item(0).getTextContent()));
            //->

            //NodeList officialTitleList = domRoot.getElementsByTagName(ELT_OFFICIAL_TITLE);

            //Optra: Get official_title from domRoot
            NodeList officialTitleList = domRoot.getElementsByTagName(CT_ITEMKEYS[1]);
            if (officialTitleList != null && officialTitleList.getLength() > 0) {
                this.eltInfo.put(ELT_OFFICIAL_TITLE, cleanString(officialTitleList.item(0).getTextContent()));
            } else {
                this.eltInfo.put(ELT_OFFICIAL_TITLE, EMPTY_STRING);
            }
            //->

            //NodeList briefSummaryList = domRoot.getElementsByTagName(ELT_BRIEF_SUMMARY);

            //Optra: Get brief_summary from domRoot
            NodeList briefSummaryList = domRoot.getElementsByTagName(CT_ITEMKEYS[2]);
            if (briefSummaryList != null && briefSummaryList.getLength() > 0) {
                org.w3c.dom.Element briefSummaryElt = (org.w3c.dom.Element) briefSummaryList.item(0);
                this.eltInfo.put(ELT_BRIEF_SUMMARY, cleanString(briefSummaryElt.getElementsByTagName(CT_TXT_BLOCK_STRING).item(0).getTextContent()));
            } else {
                this.eltInfo.put(ELT_BRIEF_SUMMARY, EMPTY_STRING);
            }

            //->

            //NodeList detailedDescriptionList = domRoot.getElementsByTagName(ELT_DETAILED_DESCRIPTION);

            //Optra: Get detailed description from dom element
            NodeList detailedDescriptionList = domRoot.getElementsByTagName(CT_ITEMKEYS[3]);
            //->

            if (detailedDescriptionList != null && detailedDescriptionList.getLength() > 0) {
                org.w3c.dom.Element detailedDescriptionElt = (org.w3c.dom.Element) detailedDescriptionList.item(0);
                this.eltInfo.put(ELT_DETAILED_DESCRIPTION, cleanString(detailedDescriptionElt.getElementsByTagName(CT_TXT_BLOCK_STRING).item(0).getTextContent()));
            } else {
                this.eltInfo.put(ELT_DETAILED_DESCRIPTION, EMPTY_STRING);
            }

            //NodeList conditionList = domRoot.getElementsByTagName("condition");

            //Optra: Get condition from domRoot
            NodeList conditionList = domRoot.getElementsByTagName(CT_ITEMKEYS[4]);
            //->

            int listSize1 = conditionList.getLength();
            String eltCondition = new String();
            for (int i = 0; i < listSize1; i++) {
                eltCondition += cleanString(conditionList.item(i).getTextContent());
                if (i < listSize1 - 1) {
                    eltCondition += COMMA_SEPARATOR;
                }
                //Optra: removed dot at end string.
                //else{
                //		eltCondition += ". ";
                //}
            }
            this.eltInfo.put(ELT_CONDITION, eltCondition);

            //NodeList interventionList = domRoot.getElementsByTagName(ELT_INTERVENTION);

            //Optra: Get Intervention name
            NodeList interventionList = domRoot.getElementsByTagName(CT_ITEMKEYS[5]);
            //->

            int listSize2 = interventionList.getLength();
            String eltIntervention = new String();
            if (interventionList != null && listSize2 > 0) {
                org.w3c.dom.Element interventionElt;
                for (int i = 0; i < listSize2; i++) {
                    interventionElt = (org.w3c.dom.Element) interventionList.item(i);
                    eltIntervention += cleanString(interventionElt.getElementsByTagName("intervention_name").item(0).getTextContent());
                    if (i < listSize2 - 1) {
                        eltIntervention += COMMA_SEPARATOR;
                    }
                    //Optra: removing dot at end of string
                    //else{
                    //	eltIntervention += ". ";
                    //}
                }
            } else {
                this.eltInfo.put(ELT_INTERVENTION, EMPTY_STRING);
            }
            this.eltInfo.put(ELT_INTERVENTION, eltIntervention);
        }

        Element getElement() {
            Element element = null;
            ArrayList<String> contextNames = this.eltCTTool.getToolResource().getResourceStructure().getContextNames();
            Structure eltStructure = new Structure(contextNames);

            for (String contextName : contextNames) {
                eltStructure.putContext(contextName, this.eltInfo.get(contextName));
            }
            try {
                element = new Element(this.eltNCTId, eltStructure);
            } catch (BadElementStructureException e) {
                logger.error("** PROBLEM ** Cannot create Element for ClinicalTrialElement with NCTId: " + this.eltNCTId + ". Null have been returned.", e);
            }
            return element;
        }
    }
}
