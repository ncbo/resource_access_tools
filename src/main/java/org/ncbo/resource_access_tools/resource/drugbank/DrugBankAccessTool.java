package org.ncbo.resource_access_tools.resource.drugbank;

import org.ncbo.resource_access_tools.common.utils.UnzipUtils;
import org.ncbo.resource_access_tools.enumeration.ResourceType;
import org.ncbo.resource_access_tools.populate.Element;
import org.ncbo.resource_access_tools.populate.Element.BadElementStructureException;
import org.ncbo.resource_access_tools.populate.Structure;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * DrugBankAccessTool is responsible for getting data elements for
 * drugcards.zip. Gets the drugcards data to populate _ET table
 * using zip file.
 * zip file found at location
 * http://www.drugbank.ca/public/downloads/current/drugcards.zip
 *
 * @author Palanisamy
 * @version June 22th, 2009
 **/
public class DrugBankAccessTool extends ResourceAccessTool {

    // Home URL of the resource
    private static final String DBK_URL = "http://www.drugbank.ca/";

    // Name of the resource
    private static final String DBK_NAME = "DrugBank";

    // Short name of the resource
    private static final String DBK_RESOURCEID = "DBK";

    // Text description of the resource
    private static final String DBK_DESCRIPTION = "DrugBank is offered to the public as a freely available resource. Use and re-distribution of the data, in whole or in part, for commercial purposes requires explicit permission of the authors and explicit acknowledgment of the source material (DrugBank) and the original publication.";

    // URL that points to the logo of the resource
    private static final String DBK_LOGO = "http://www.drugbank.ca/images/drugbankcover.png";

    //Base URL that points to an element when concatenated with an local element ID
    private static final String DBK_ELT_URL = "http://www.drugbank.ca/drugs/";

    // The set of context names
    private static final String[] DBK_ITEMKEYS = {"name", "indication", "synonyms", "brandnames", "drug_category", "target_go_classification", "ChEBI_ID"};

    // Weight associated to a context
    private static final Double[] DBK_WEIGHTS = {1.0, 0.8, 0.8, 0.8, 0.8, 1.0, 1.0};

    // OntoID associated for reported annotations. Gene ontology (GO) with virtual ontology id 1070, CHeBi onotogy -1007
    private static final String[] DBK_ONTOIDS = {Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, "1070", "1007"};

    // Structure for resource Access tool
    private static final Structure DBK_STRUCTURE = new Structure(DBK_ITEMKEYS, DBK_RESOURCEID, DBK_WEIGHTS, DBK_ONTOIDS);

    //Flat file output
    private static final String DBK_FLATFILE_URL = "http://www.drugbank.ca/system/downloads/2.5/drugcards.zip";

    // A context name used to describe the associated element
    private static final String DBK_MAIN_ITEMKEY = "name";

    //String constant for ChEBI_ID 'CHEBI:' (CHEBI value to append with ChEBI_ID)
    private static final String DBK_CHEBI = "CHEBI:";

    // String constant for semicolon ':'
    private static final String COLON_STRING = ":";

    //Following keys used to find the values from text file
    private static final String STARTINGPOINT_ = "#";
    private static final String STARTDRUGCARD_ = "#BEGIN_DRUGCARD";
    private static final String PRIMARYACCESSIONNO_ = "# Primary_Accession_No";
    private static final String GENERICNAME_ = "# Generic_Name";
    private static final String INDICATION_ = "# Indication";
    private static final String SYNONYMS_ = "# Synonyms";
    private static final String BRANDNAMES_ = "# Brand_Names";
    private static final String DRUGCATEGORY_ = "# Drug_Category";
    private static final String STARTDRUGTARGET_ = "# Drug_Target_";
    private static final String ENDDRUGTARGET_ = "_GO_Classification:";
    private static final String FUNCTION_ = "Function";
    private static final String PROCESS_ = "Process";
    private static final String BIOPROCESS_ = "Biological process";
    private static final String COMPONENT_ = "Component";
    private static final String CELLCOMPONENT_ = "Cellular component";
    private static final String CHEBIID_ = "# ChEBI_ID";
    private static final String NOTAVAILABLE_ = "Not Available";
    private static final String ENDDRUGCARD_ = "#END_DRUGCAR";

    public DrugBankAccessTool() {
        super(DBK_NAME, DBK_RESOURCEID, DBK_STRUCTURE);
        try {
            this.getToolResource().setResourceURL(new URL(DBK_URL));
            this.getToolResource().setResourceLogo(new URL(DBK_LOGO));
            this.getToolResource().setResourceElementURL(DBK_ELT_URL);

        } catch (MalformedURLException e) {
            logger.error("** PROBLEM ** Setting resource URL, Logo or ElementURL ", e);
        }
        this.getToolResource().setResourceDescription(DBK_DESCRIPTION);
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.SMALL;
    }

    @Override
    public String elementURLString(String elementLocalID) {
        return DBK_ELT_URL + elementLocalID;
    }

    @Override
    public String mainContextDescriptor() {
        return DBK_MAIN_ITEMKEY;
    }

    @Override
    public HashSet<String> queryOnlineResource(String query) {
        return new HashSet<String>();
    }

    @Override
    public int updateResourceContent() {
        int nbElement = 0;
        try {
            // Download and extracts zip file
            UnzipUtils.extractZipFile(DBK_FLATFILE_URL);
            nbElement = this.updateResourceContentFromFile(UnzipUtils.getDataFile());
        } catch (Exception e) {
            logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName(), e);
        } finally {
            UnzipUtils.deleteDataFile();
        }
        return nbElement;
    }

    /**
     * Update the resource content according to a given tab delimited text file.
     * The first column of the file must be the elementLocalID.
     * This file must contain the same number of itemKey columns than the associated resource structure.
     * Returns the number of elements updated. Can be used for updateResourceContent.
     *
     * @throws BadElementStructureException
     */
    private int updateResourceContentFromFile(File resourceFile) throws BadElementStructureException {
        int nbElement = 0;
        logger.info("Updating resource content with local file " + resourceFile.getName() + "...");
        try {
            //create file reader object
            FileReader fileReader = new FileReader(resourceFile);
            //create buffered reader object
            BufferedReader bufferReader = new BufferedReader(fileReader);
            //read first line
            String line = bufferReader.readLine();
            //create elementCompleteInfo and number columns presents in DBK_ITEMKEYS array with local element id
            String[] elementCompleteInfo = new String[DBK_ITEMKEYS.length + 1];
            String targets_classfication;
            String category;
            //Gets all table column names
            ArrayList<String> contextNames = this.getToolResource().getResourceStructure().getContextNames();
            Element element;
            Structure eltStructure = new Structure(contextNames);
            int i;
            HashSet<String> allElementLocalIDs = resourceUpdateService.getAllLocalElementIDs();

            while (line != null) {
                //continues up to end of the text file
                outerloop:
                while (line != null) {
                    //checks start with #START_DRUGCAR
                    if ((line.startsWith(STARTDRUGCARD_))) {
                        //initialize all the elementCompleteInfo to ""(empty String)
                        for (int e = 0; e < elementCompleteInfo.length; e++) {
                            elementCompleteInfo[e] = EMPTY_STRING;
                        }
                        targets_classfication = EMPTY_STRING;
                        while (line != null) {
                            if (line.startsWith(PRIMARYACCESSIONNO_)) {
                                //checks primary access no
                                elementCompleteInfo[0] = bufferReader.readLine().toString().trim();
                                //reads next line
                                line = bufferReader.readLine().toString().trim();
                                // If database already contains localElementID then break to outerloop.
                                if (allElementLocalIDs.contains(elementCompleteInfo[0])) {
                                    break outerloop;
                                }

                            } else if (line.startsWith(GENERICNAME_)) {
                                //checks generic names
                                String value = bufferReader.readLine().toString().trim();
                                //checks not equal to start with Not Available
                                if (!value.equals(NOTAVAILABLE_)) {
                                    elementCompleteInfo[1] = value;
                                }
                                //reads next line
                                line = bufferReader.readLine().toString().trim();

                            } else if (line.startsWith(INDICATION_)) {
                                //checks indication
                                category = EMPTY_STRING;
                                //reads next line
                                line = bufferReader.readLine().toString().trim();
                                while (line != null) {
                                    //checks not equal to start with # and not equal to start with Not Available
                                    if (!line.startsWith(STARTINGPOINT_) && !line.equals(NOTAVAILABLE_)) {
                                        //appends value with comma separator
                                        category += line + COMMA_STRING;
                                    } else {
                                        break;
                                    }
                                    //reads next line
                                    line = bufferReader.readLine().toString().trim();
                                }
                                //checks category length greater than 0
                                if (category.length() > 0)
                                    elementCompleteInfo[2] = category.substring(0, category.length() - 2);

                            } else if (line.startsWith(SYNONYMS_)) {
                                //checks synonyms
                                category = EMPTY_STRING;
                                //reads next line
                                line = bufferReader.readLine().toString().trim();
                                while (line != null) {
                                    //checks not equal to start with # and not equal to start with Not Available
                                    if (!line.startsWith(STARTINGPOINT_) && !line.equals(NOTAVAILABLE_)) {
                                        //appends value with comma separator
                                        category += line + COMMA_STRING;
                                    } else {
                                        break;
                                    }
                                    //reads next line
                                    line = bufferReader.readLine().toString().trim();
                                }
                                //checks category length greater than 0
                                if (category.length() > 0)
                                    elementCompleteInfo[3] = category.substring(0, category.length() - 2);

                            } else if (line.startsWith(BRANDNAMES_)) {
                                //checks brand names
                                category = EMPTY_STRING;
                                //reads next line
                                line = bufferReader.readLine().toString().trim();
                                while (line != null) {
                                    //checks not equal to start with # and not equal to start with Not Available
                                    if (!line.startsWith(STARTINGPOINT_) && !line.equals(NOTAVAILABLE_)) {
                                        //appends value with comma separator
                                        category += line + COMMA_STRING;
                                    } else {
                                        break;
                                    }
                                    //reads next line
                                    line = bufferReader.readLine().toString().trim();
                                }
                                //checks category length greater than 0
                                if (category.length() > 0)
                                    elementCompleteInfo[4] = category.substring(0, category.length() - 2);

                            } else if (line.startsWith(DRUGCATEGORY_)) {
                                //checks drug category
                                category = EMPTY_STRING;
                                //reads next line
                                line = bufferReader.readLine().toString().trim();
                                while (line != null) {
                                    //checks not equal to start with # and not equal to start with Not Available
                                    if (!line.startsWith(STARTINGPOINT_) && !line.equals(NOTAVAILABLE_)) {
                                        //appends value with comma separator
                                        category += line + COMMA_STRING;
                                    } else {
                                        break;
                                    }
                                    //reads next line
                                    line = bufferReader.readLine().toString().trim();
                                }
                                //checks category length greater than 0
                                if (category.length() > 0)
                                    elementCompleteInfo[5] = category.substring(0, category.length() - 2);

                            } else if (line.startsWith(STARTDRUGTARGET_) && line.endsWith(ENDDRUGTARGET_)) {
                                //checks drug target_n_Go_classification start with drug target_ and end with _GO_Claddification:
                                category = EMPTY_STRING;
                                line = bufferReader.readLine().toString().trim();
                                while (line != null) {
                                    //checks not equal to start with # and equal to Function:, Process, Biological Process, Component or cellular Component
                                    if (!line.startsWith(STARTINGPOINT_) && (line.startsWith(FUNCTION_) || line.startsWith(PROCESS_) || line.startsWith(BIOPROCESS_) || line.startsWith(COMPONENT_) || line.startsWith(CELLCOMPONENT_))) {
                                        //appends value with comma separator
                                        if (!line.endsWith(NOTAVAILABLE_)) {
                                            category += line.split(COLON_STRING)[1].trim() + COMMA_STRING;
                                        }
                                    } else if (line.startsWith(STARTINGPOINT_)) {
                                        //checks equal to start with #
                                        break;
                                    }
                                    //reads next line
                                    line = bufferReader.readLine().toString().trim();
                                }
                                //checks category length greater than 0
                                if (category.length() > 0) {
                                    String value = category.substring(0, category.length() - 1);
                                    //Mapping target go classification from GO resource access tool.
                                    targets_classfication = resourceUpdateService.mapTermsToLocalConceptIDs(value, DBK_ONTOIDS[5], COMMA_STRING);
                                }

                            } else if (line.startsWith(CHEBIID_)) {
                                //checks ChEBI ID
                                String value = bufferReader.readLine().toString().trim();
                                //checks not equal to start with Not Available
                                if (!value.equals(NOTAVAILABLE_)) {
                                    //appends localOntologyID, /, CHEBI: and text file value
                                    String concepts = DBK_ONTOIDS[6] + SLASH_STRING + DBK_CHEBI + value;
                                    elementCompleteInfo[7] = concepts;
                                }
                                //reads next line
                                line = bufferReader.readLine().toString().trim();

                            } else if (line.startsWith(ENDDRUGCARD_)) {
                                //checks start with #END_DRUGCAR
                                elementCompleteInfo[6] = targets_classfication;
                                //replace targets_classfication to ""(empty string value)
                                targets_classfication = EMPTY_STRING;
                                break;

                            } else {
                                //reads next line
                                line = bufferReader.readLine().toString().trim();
                            }
                        }
                        i = 0;
                        for (String contextName : contextNames) {
                            eltStructure.putContext(contextName, elementCompleteInfo[i + 1]);
                            i++;
                        }
                        element = new Element(elementCompleteInfo[0], eltStructure);
                        if (resourceUpdateService.addElement(element)) {
                            nbElement++;
                        }
                    }
                    //reads next line
                    line = bufferReader.readLine();
                }

            }

            //close file reader object
            bufferReader.close();
        } catch (IOException e) {
            logger.error("** PROBLEM ** Cannot update resource " + super.getToolResource().getResourceName() + " with file " + resourceFile.getName(), e);
        }
        return nbElement;
    }

    @Override
    public void updateResourceInformation() {

    }

}
