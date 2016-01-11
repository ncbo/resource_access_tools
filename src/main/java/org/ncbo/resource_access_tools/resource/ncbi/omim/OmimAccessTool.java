package org.ncbo.resource_access_tools.resource.ncbi.omim;

import gov.nih.nlm.ncbi.www.soap.eutils.esummary.DocSumType;
import gov.nih.nlm.ncbi.www.soap.eutils.esummary.ESummaryRequest;
import gov.nih.nlm.ncbi.www.soap.eutils.esummary.ESummaryResult;
import gov.nih.nlm.ncbi.www.soap.eutils.esummary.ItemType;
import org.ncbo.resource_access_tools.enumeration.ResourceType;
import org.ncbo.resource_access_tools.populate.Element;
import org.ncbo.resource_access_tools.populate.Element.BadElementStructureException;
import org.ncbo.resource_access_tools.populate.Structure;
import org.ncbo.resource_access_tools.resource.ncbi.AbstractNcbiResourceAccessTool;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * This class is used to access thr OMIM resource on the basis of a eUtils tool.
 *
 * @author Adrien Coulet
 * @version OBR_v1
 * @created 23-Fev-2009
 */

public class OmimAccessTool extends AbstractNcbiResourceAccessTool {

    private static final String OMIM_URL = "http://www.ncbi.nlm.nih.gov/omim";
    private static final String OMIM_NAME = "Online Mendelian Inheritance in Man";
    private static final String OMIM_RESOURCEID = "OMIM";
    private static final String OMIM_DESCRIPTION = "OMIM is a comprehensive, authoritative, and timely compendium of human genes and genetic phenotypes. " +
            "The full-text, referenced overviews in OMIM contain information on all known mendelian disorders and over 12,000 genes. " +
            "OMIM focuses on the relationship between phenotype and genotype.";
    private static final String OMIM_LOGO = "http://www.ncbi.nlm.nih.gov/entrez/query/static/gifs/entrez_omim.gif";
    private static final String OMIM_ELT_URL = "http://www.ncbi.nlm.nih.gov/omim/";

    private static final String OMIM_EUTILS_DB = "omim";
    private static final String OMIM_EUTILS_TERM = "all[filter]";

    private static final String[] OMIM_ITEMKEYS = {UID_COLUMN, "title", "altTitles", "locus"};
    private static final Double[] OMIM_WEIGHTS = {0.0, 1.0, 0.7, 0.0};
    // OntoID associated for reported annotations
    private static final String[] OMIM_ONTOIDS = {Structure.NOT_FOR_ANNOTATION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION};
    private static final Structure OMIM_STRUCTURE = new Structure(OMIM_ITEMKEYS, OMIM_RESOURCEID, OMIM_WEIGHTS, OMIM_ONTOIDS);

    public OmimAccessTool() {
        super(OMIM_NAME, OMIM_RESOURCEID, OMIM_STRUCTURE);
        try {
            this.getToolResource().setResourceURL(new URL(OMIM_URL));
            this.getToolResource().setResourceLogo(new URL(OMIM_LOGO));
            this.getToolResource().setResourceElementURL(OMIM_ELT_URL);
        } catch (MalformedURLException e) {
            logger.error(EMPTY_STRING, e);
        }
        this.getToolResource().setResourceDescription(OMIM_DESCRIPTION);
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.SMALL;
    }

    @Override
    protected String getEutilsDB() {
        return OMIM_EUTILS_DB;
    }

    @Override
    protected String getEutilsTerm() {
        return OMIM_EUTILS_TERM;
    }

    @Override
    public void updateResourceInformation() {
        // TODO See if it can be implemented for this resource.
    }

    @Override
    public int updateResourceContent() {
        // eutilsUpdateFromDate will not work for OMIM as NCBI doesn't take into account reldate for gds.
        return super.eutilsUpdateAll(UID_COLUMN);
    }

    @Override
    protected int updateElementTableWithUIDs(HashSet<String> UIDs) throws BadElementStructureException {
        int nbElement = 0;
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
        String localElementId = null;

        for (int step = 0; step < UIDsTab.length; step += EUTILS_MAX) {
            max = step + EUTILS_MAX;
            UIDlist = new StringBuffer();
            if (max > UIDsTab.length) {
                max = UIDsTab.length;
            }
            for (int u = step; u < max; u++) {
                UIDlist.append(UIDsTab[u]);
                if (u < max - 1) {
                    UIDlist.append(COMMA_STRING);
                }
            }
            esummaryRequest.setId(UIDlist.toString());
            try {
                esummaryResult = this.getToolEutils().run_eSummary(esummaryRequest);
                resultDocSums = esummaryResult.getDocSum();
                for (int i = 0; i < resultDocSums.length; i++) {
                    docSumItems = resultDocSums[i].getItem();

                    // This section depends of the structure and the type of content we want to get back
                    // resultDocSums[i].getID contains the UID
                    eltStructure.putContext(Structure.generateContextName(OMIM_RESOURCEID, OMIM_ITEMKEYS[0]), resultDocSums[i].getId());

                    localElementId = null;
                    for (ItemType itemType : docSumItems) {
                        String OMIM_ID_STRING = "Oid";
                        if (OMIM_ID_STRING.equalsIgnoreCase(itemType.getName())) {
                            localElementId = getItemTypeContent(itemType);
                        } else if (OMIM_ITEMKEYS[1].equalsIgnoreCase(itemType.getName())) {
                            eltStructure.putContext(Structure.generateContextName(OMIM_RESOURCEID, OMIM_ITEMKEYS[1]), getItemTypeContent(itemType));
                        } else if (OMIM_ITEMKEYS[2].equalsIgnoreCase(itemType.getName())) {
                            eltStructure.putContext(Structure.generateContextName(OMIM_RESOURCEID, OMIM_ITEMKEYS[2]), getItemTypeContent(itemType, GT_SEPARATOR_STRING));
                        } else if (OMIM_ITEMKEYS[3].equalsIgnoreCase(itemType.getName())) {
                            eltStructure.putContext(Structure.generateContextName(OMIM_RESOURCEID, OMIM_ITEMKEYS[3]), getItemTypeContent(itemType, GT_SEPARATOR_STRING));
                        } else if (OMIM_ID_STRING.equalsIgnoreCase(itemType.getName())) {
                            localElementId = getItemTypeContent(itemType);
                        }
                    }
                    // localElementID and structure into a new element
                    if (localElementId != null) {
                        element = new Element(localElementId, eltStructure);
                        if (resourceUpdateService.addElement(element)) {
                            nbElement++;
                        }
                    }

                }
            } catch (RemoteException e) {
                logger.error("** PROBLEM ** Cannot get information using ESummary.", e);
            }
        }
        return nbElement;
    }

    @Override
    public String elementURLString(String elementLocalID) {
        // removing non-digit character from elementLocalID
        return OMIM_ELT_URL + elementLocalID.replaceAll(NON_DIGIT_REGEX, EMPTY_STRING);
    }

    @Override
    public String mainContextDescriptor() {
        String OMIM_MAIN_ITEMKEY = "title";
        return OMIM_MAIN_ITEMKEY;
    }

}
