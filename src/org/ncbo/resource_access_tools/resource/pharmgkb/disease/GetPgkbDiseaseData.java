
package org.ncbo.resource_access_tools.resource.pharmgkb.disease;

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
import org.ncbo.resource_access_tools.resource.pharmgkb.drug.GetPgkbDrugData;
import org.ncbo.resource_access_tools.resource.pharmgkb.gene.GetPgkbGeneData;
import org.ncbo.resource_access_tools.resource.pharmgkb.drug.GetPgkbDrugData;
import org.ncbo.resource_access_tools.resource.pharmgkb.gene.GetPgkbGeneData;
import org.ncbo.stanford.obr.util.ProcessExecutor;
import org.ncbo.stanford.obr.util.helper.StringHelper;


/**
 * This class enables to get pharmgkb data related to a disease
 * by lunching the web service client diseases.pl
 * IN: disease accession id (ex: PA447230)
 * OUT: related data enclosed in a diseaseElement
 *
 * @author Adrien Coulet
 * @version OBR_v0.2
 * @created 11-Nov-2008
 *
 */

public class GetPgkbDiseaseData implements StringHelper{

	// Logger for this class
	private static Logger logger = Logger.getLogger(GetPgkbDiseaseData.class);
	//attributes
	private static String PERL_SCRIPT_PATH =new File(GetPgkbDiseaseData.class.getResource( "diseases.pl" ).getFile()).getAbsolutePath();

	private static String COMMAND                 = "perl " +PERL_SCRIPT_PATH;
	Hashtable<String, Hashtable<String, Hashtable<Integer, String>>> diseaseData      = new Hashtable<String, Hashtable<String, Hashtable<Integer, String>>>();	//<diseaseAccession, Hashtable of attribut-value couple>
	Hashtable<String, Hashtable<Integer, String>> diseaseAttribute = new Hashtable<String, Hashtable<Integer, String>>();	//<attributName, value> (a value could be a map)
	Hashtable<Integer, String>   attributeValues  = new Hashtable<Integer, String>();
	Structure basicStructure = null;
	String resourceID = EMPTY_STRING;

	//constructor
	public GetPgkbDiseaseData(Resource myResource){
		this.basicStructure = myResource.getResourceStructure();
		this.resourceID     = myResource.getResourceId();
	}

	public GetPgkbDiseaseData(){
	}

	// method
	public Element getDiseaseElement(String diseaseAccession) {

		Structure elementStructure = basicStructure;
		Element myDisease = null;

		try {
			//logger.info("get data for "+diseaseAccession+"... ");

			HashMap<Integer, String> lines = ProcessExecutor.executeCommand(COMMAND, diseaseAccession);
			try {
				diseaseData = new Hashtable<String, Hashtable<String, Hashtable<Integer, String>>>();
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
									diseaseAttribute.put(attributeName, attributeValues);
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
					diseaseAttribute.put(attributeName, attributeValues); //update of diseaseAttribute content
					//update the diseaseData
					diseaseData.put(diseaseAccession, diseaseAttribute);//update of diseaseData content
				}else{
					logger.info("PROBLEM when getting data with the web service for "+ diseaseAccession);
				}
				// PUT DATA INTO AN ELEMENT++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
				// for each attribute
				GetPgkbGeneData myGeneExtractor = new GetPgkbGeneData();
				GetPgkbDrugData myDrugExtractor = new GetPgkbDrugData();
				String attInString = EMPTY_STRING;

				for (String contextName: elementStructure.getContextNames()){
					boolean attributeHasValue = false;
					for (String att : diseaseAttribute.keySet()){
						if (contextName.equals(this.resourceID+"_"+att)){
							attributeHasValue = true;
							// transform repetitive element (hashtables) in a string with > as a separator.
							attInString = EMPTY_STRING;
							Hashtable<Integer, String> valueTable = diseaseAttribute.get(att);
							for (Integer valueNb :valueTable.keySet()){
								if (!attInString.equals(EMPTY_STRING)){
									// specific case of gene => we want to store gene symbol and not the PharmGKB localElementID
									if(att.equals("diseaseRelatedGenes")){
										attInString = attInString+"> "+myGeneExtractor.getGeneSymbolByGenePgkbLocalID(valueTable.get(valueNb));
									}else if(att.equals("diseaseRelatedDrugs")){
										attInString = attInString+"> "+myDrugExtractor.getDrugNameByDrugLocalID(valueTable.get(valueNb));
									}else{
										attInString = attInString+"> "+valueTable.get(valueNb);
									}
								}else{
									if(att.equals("diseaseRelatedGenes")){
										attInString=myGeneExtractor.getGeneSymbolByGenePgkbLocalID(valueTable.get(valueNb));
									}else if(att.equals("diseaseRelatedDrugs")){
										attInString = myDrugExtractor.getDrugNameByDrugLocalID(valueTable.get(valueNb));
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
			myDisease = new Element(diseaseAccession, elementStructure);
		}catch(BadElementStructureException e){
			logger.error(EMPTY_STRING, e);
		}
		return myDisease;
	}

	public String getDiseaseNameByDiseaseLocalID(String diseaseLocalID) {
		String diseaseName = EMPTY_STRING;
		try {
	        HashMap<Integer, String> lines = ProcessExecutor.executeCommand(COMMAND, diseaseLocalID);

			Pattern dataPattern  = Pattern.compile("^diseaseName: (.*)$");

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
							diseaseName = dataMatcher.group(1);
							//System.out.println(genePgkbLocalID+" => "+geneSymbol);
					}
				}
			}
        }catch(Throwable t){
        	logger.error("Problem in getting disease name for diseaseLocalID : " + diseaseLocalID, t);
        }
		return diseaseName;
	}


}
