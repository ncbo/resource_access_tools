
package org.ncbo.resource_access_tools.resource.pharmgkb.drug;

import java.io.File;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import obs.obr.populate.Element;
import obs.obr.populate.Resource;
import obs.obr.populate.Structure;
import obs.obr.populate.Element.BadElementStructureException;

import org.apache.log4j.Logger;
import org.ncbo.resource_access_tools.resource.pharmgkb.gene.GetPgkbGeneData;
import org.ncbo.resource_access_tools.resource.pharmgkb.disease.GetPgkbDiseaseData;
import org.ncbo.resource_access_tools.resource.pharmgkb.gene.GetPgkbGeneData;
import org.ncbo.stanford.obr.util.ProcessExecutor;
import org.ncbo.stanford.obr.util.helper.StringHelper;

/**
 * This class enables to get pharmgkb data related to a drug
 * by lunching the web service client drugs.pl
 * IN: drug accession id (ex: PA452624)
 * OUT: related data enclosed in an Element
 *
 * @author Adrien Coulet
 * @version OBR_v0.2
 * @created 14-May-2008
 *
 */

public class GetPgkbDrugData implements StringHelper{

	// Logger for this class
	private static Logger logger = Logger.getLogger(GetPgkbDrugData.class);
	//attributes
	private static String PERL_SCRIPT_PATH =new File(GetPgkbDrugData.class.getResource("drugs.pl" ).getFile()).getAbsolutePath();
	private static String COMMAND                = "perl " +PERL_SCRIPT_PATH;
	Hashtable<String, Hashtable<String, Hashtable<Integer, String>>> drugData        = new Hashtable<String, Hashtable<String, Hashtable<Integer, String>>>();	//<geneAccession, Hashtable of attribut-value couple>
	Hashtable<String, Hashtable<Integer, String>> drugAttribute   = new Hashtable<String, Hashtable<Integer, String>>();	//<attributName, value> (a value could be a map)
	Hashtable<Integer, String>   attributeValues = new Hashtable<Integer, String>();

	Structure basicStructure = null;
	String    resourceID = EMPTY_STRING;

	//constructor
	public GetPgkbDrugData(Resource myResource){
		this.basicStructure = myResource.getResourceStructure();
		this.resourceID     = myResource.getResourceId();
	}

	public GetPgkbDrugData(){
	}

	// method
	public Element getDrugElement(String drugAccession) {

		Structure elementStructure = basicStructure;
		Element myDrug = null;
		try {
			//logger.info("get data for "+drugAccession+"... ");

			HashMap<Integer, String> lines = ProcessExecutor.executeCommand(COMMAND, drugAccession);
			try {
				drugData = new Hashtable<String, Hashtable<String, Hashtable<Integer, String>>>();
				Integer attributeNumber = 0;
				Pattern setPattern  = Pattern.compile("^\\t(.*)$");
				Pattern dataPattern = Pattern.compile("^(.+):(.*)$");
				String attributeName = null;
				if(!lines.keySet().isEmpty()){
					for(int i=0; i<lines.keySet().size();i++) {
						String resultLine=lines.get(i);
						// if resultLine is null then skip processing.
						if(resultLine== null){
							continue;
						}
						// process the line
						Matcher setMatcher = setPattern.matcher(resultLine);
						// line with an attribute name =====================
						if (!setMatcher.matches()){
							//new attribute
							Matcher dataMatcher = dataPattern.matcher(resultLine);
							if(dataMatcher.matches()){
								//first we put in the hashtable last things
								if (attributeName!=null && attributeValues!=null){
									drugAttribute.put(attributeName, attributeValues);
								}
								// then initialization
								attributeName   = dataMatcher.group(1);
								attributeValues = new Hashtable<Integer, String>();

								if(!dataMatcher.group(2).equals(EMPTY_STRING)){ // simple case in which we have atributeName: value on one line
									String value = null;
									value = dataMatcher.group(2).replaceFirst(" ", EMPTY_STRING);
									attributeValues.put(1, value);
								}else{
									attributeNumber = 0;
								}
							}
						// non header line => value ========================
						}else{
							if (attributeName!=null){
								attributeNumber++;
								String value = null;
								value = setMatcher.group(1);
								attributeValues.put(attributeNumber, value);
							}
						}
					}
				}
				if(attributeName!=null && attributeValues!=null){
					drugAttribute.put(attributeName, attributeValues); //update of drugAttribute content
					//update the drugData
					drugData.put(drugAccession, drugAttribute);//update of drugData content
				}else{
					logger.info("PROBLEM when getting data with the web service for " + drugAccession);
				}
				// PUT DATA INTO AN ELEMENT++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
				//System.out.println(drugData.get(drugAccession).toString());
				// for each attribute
				GetPgkbGeneData myGeneExtractor    = new GetPgkbGeneData();
				GetPgkbDiseaseData myDiseaseExtractor = new GetPgkbDiseaseData();
				String attInString = EMPTY_STRING;

				for (String contextName: elementStructure.getContextNames()){
					boolean attributeHasValue = false;
					for (String att : drugAttribute.keySet()){
						if (contextName.equals(this.resourceID+"_"+att)){
							attributeHasValue = true;
							// transform repetitive element (hashtables) in a string with > as a separator.
							attInString = EMPTY_STRING;
							Hashtable<Integer, String> valueTable = drugAttribute.get(att);
							for (Integer valueNb :valueTable.keySet()){
								if (!attInString.equals(EMPTY_STRING)){ // not the first of the list
									// specific case of gene => we want to store gene symbol and not the PharmGKB localElementID
									if(att.equals("drugRelatedGenes")){
										attInString = attInString+"> "+myGeneExtractor.getGeneSymbolByGenePgkbLocalID(valueTable.get(valueNb));
									}else if(att.equals("drugRelatedDiseases")){
										attInString = attInString+"> "+myDiseaseExtractor.getDiseaseNameByDiseaseLocalID(valueTable.get(valueNb));
									}else if(att.equals("drugTradeNames")){
										if(valueTable.get(valueNb).indexOf("(")!=-1){
											attInString = attInString+"> "+valueTable.get(valueNb).substring(0,valueTable.get(valueNb).indexOf("("));
										}else{
											attInString = attInString+"> "+valueTable.get(valueNb);
										}
									}else{
										attInString = attInString+"> "+valueTable.get(valueNb);
									}
								}else{ // first of the list
									if(att.equals("drugRelatedGenes")){
										attInString=myGeneExtractor.getGeneSymbolByGenePgkbLocalID(valueTable.get(valueNb));
									}else if(att.equals("drugRelatedDiseases")){
										attInString = myDiseaseExtractor.getDiseaseNameByDiseaseLocalID(valueTable.get(valueNb));
									}else if(att.equals("drugTradeNames")){
										if(valueTable.get(valueNb).indexOf("(")!=-1){
											attInString = valueTable.get(valueNb).substring(0,valueTable.get(valueNb).indexOf("("));
										}else{
											attInString = valueTable.get(valueNb);
										}
									}else{
										attInString=valueTable.get(valueNb);
									}
								}
							}
							elementStructure.putContext(contextName,attInString);
						}
					}
					// to avoid null value in the structure
					if (!attributeHasValue){
						elementStructure.putContext(contextName,EMPTY_STRING);
					}
				}
				//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

			} finally {
			}
        }catch(Throwable t){
        	 logger.error("Problem in processing element", t);
        }

		// creation of the element
		try{
			myDrug = new Element(drugAccession, elementStructure);

		}catch(BadElementStructureException e){
			logger.error(EMPTY_STRING, e);
		}
		return myDrug;
	}

	public String getDrugNameByDrugLocalID(String drugLocalID) {
		String drugName = EMPTY_STRING;
		try {
			HashMap<Integer, String> lines = ProcessExecutor.executeCommand(COMMAND, drugLocalID);

			Pattern dataPattern  = Pattern.compile("^drugName: (.*)$");
			if(!lines.keySet().isEmpty()){
				for(int i=0; i<lines.keySet().size();i++) {
					String resultLine=lines.get(i);
					// if resultLine is null then skip processing.
					if(resultLine== null){
						continue;
					}
					// process the line
					Matcher dataMatcher = dataPattern.matcher(resultLine);
					// line with the geneSymbol ===========================
					if (dataMatcher.matches()){
							drugName   = dataMatcher.group(1);
							//System.out.println(genePgkbLocalID+" => "+geneSymbol);
					}
				}
			}
        }catch(Throwable t){
        	logger.error("Problem in getting drug name for drugLocalID : " + drugLocalID, t);
        }
		return drugName;
	}
}
