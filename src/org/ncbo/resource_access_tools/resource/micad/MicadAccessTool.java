package org.ncbo.resource_access_tools.resource.micad;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.ncbo.resource_access_tools.enumeration.ResourceType;
import org.ncbo.resource_access_tools.populate.Element;
import org.ncbo.resource_access_tools.populate.Structure;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;

/**
 * Extract data from micad.
 * Use Micad's csv file and access web to get all the data.
 * @author  Kranthi Kode
 * @version MicadAccessTool v2
 * @date    19-October-2009
 */

public class MicadAccessTool extends ResourceAccessTool {

	private static final String MICAD_URL         = "http://www.micad.nih.gov/";
	private static final String MICAD_NAME        = "MICAD";
	private static final String MICAD_RESOURCEID  = "MICAD";
	private static final String MICAD_DESCRIPTION = "Molecular Imaging and Contrast Agent Database";
	private static final String MICAD_LOGO        = "http://www.ncbi.nlm.nih.gov/corehtml/pmc/pmcgifs/bookshelf/thumbs/th-micad-lrg.png";
	private static final String MICAD_ELT_URL     = "http://www.ncbi.nlm.nih.gov/bookshelf/br.fcgi?book=micad&part=";

	private static final String[] MICAD_ITEMKEYS  = {"Name", "Abbreviated", "Synonym", "Agent_Category", "Target", "Target_Category",
		"Detection_Method", "Signal_Source", "Background", "Synthesis", "In_Vitro", "Animal_Studies", "Human_Studies", "References"};
	private static final Double[] MICAD_WEIGHTS 	 = {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
	private static final String[] MICAD_ONTOIDS 	 = {Structure.FOR_CONCEPT_RECOGNITION,	Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION,	Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION,	Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION,	Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION,	Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION,	Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION,	Structure.FOR_CONCEPT_RECOGNITION};

	private static Structure MICAD_STRUCTURE      = new Structure(MICAD_ITEMKEYS, MICAD_RESOURCEID, MICAD_WEIGHTS, MICAD_ONTOIDS);
	private static String    MICAD_MAIN_ITEMKEY   = "Name";

	// constructor
	public MicadAccessTool(){
		super(MICAD_NAME, MICAD_RESOURCEID, MICAD_STRUCTURE);
		try {
			this.getToolResource().setResourceURL(new URL(MICAD_URL));
			this.getToolResource().setResourceDescription(MICAD_DESCRIPTION);
			this.getToolResource().setResourceLogo(new URL(MICAD_LOGO));
			this.getToolResource().setResourceElementURL(MICAD_ELT_URL);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public ResourceType  getResourceType() {
		return ResourceType.SMALL;
	}

	@Override
	public void updateResourceInformation() {
		// TODO
		// can be used to update resource name, description, logo, elt_url.
	}

	@Override
	public HashSet<String> queryOnlineResource(String query) {
		// TODO
		// not used for MICADtome
		return new HashSet<String>();
	}

	@Override
	public String elementURLString(String elementLocalID) {
		return MICAD_ELT_URL + elementLocalID;
	}


	public String itemKeyForAnnotationForBP() {
		return MICAD_MAIN_ITEMKEY;
	}

	@Override
	public int updateResourceContent() {
		int nbElement = 0;
		try {
			Element myElement = null;

			/************* variable part ****************/
			// get the list of elements present in the original resource (CSV file)
			String[][] csvElements = this.getLocalElementIds();

			// get the list of element Ids from csvElements
			HashSet<String> elementIDList = new HashSet<String>();


			String [] temp;
			for(int i=1;i<=csvElements.length-1;i++)
			{
				//Split the element in 18'th column in csv
				if (csvElements[i][18]!=null)
				{
					temp=csvElements[i][18].split("micad&part=");
					if (temp.length==2)
						elementIDList.add(temp[1]);
				}
			}
			// System.out.println(elementIDList);
			logger.info(elementIDList.size()+" chemicals found. ");

			// gets the elements already in the corresponding _ET and keeps only the difference
			HashSet<String> allElementsInET = resourceUpdateService.getAllLocalElementIDs();
			//HashSet<Long> allElementsInETasLong = new HashSet<Long>(allElementsInET.size());
			HashSet<String> allElementsInETasLong = new HashSet<String>(allElementsInET.size());
			for(String elementInET: allElementsInET){
				//allElementsInETasLong.add(Long.parseLong(elementInET));
				allElementsInETasLong.add((elementInET));
			}
			elementIDList.removeAll(allElementsInETasLong);
			logger.info("Number of new elements to dump: " + elementIDList.size());

			// get data associated with each of these elements
			// and populate the ElementTable
			Iterator<String> i = elementIDList.iterator();
			while(i.hasNext()){
				String elementID = i.next();
				// get data of this element
				myElement = this.getElement(elementID, csvElements);

				// populates OBR_MICAD_ET with this element
					if(resourceUpdateService.addElement(myElement)){
						nbElement++;
					}
			}
			/*********************************************/
		} catch (Exception e) {
			logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName(), e);
		}
		logger.info(nbElement+" elements from "+MICAD_NAME+" added to the element table.");
		return nbElement;
	}

	/**
	 * Get the list of all elementID of the resource.
	 * For MICAD database, get the list of elementIDs from csv file.
	 */
	public String[][] getLocalElementIds(){
		String[][] elements = new String [2000][50];
		logger.info(" Get the list of all elmentIDs from "+MICAD_NAME+" ... ");
		try{
			MicadReadCsv myExtractor = new MicadReadCsv(this.getToolResource(), this);
			elements = myExtractor.getLocalElementIds();
		}catch(Exception e){
			logger.error("** PROBLEM ** Problem when extracting elementID from the original resource. " +
					"Check MicadReadCsv", e);
		}
		return elements;
	}

	/**
	 * Get a complete Element (Structure filled) from the resource.
	 * For now, we will just get few elements from csv file
	 * @return
	 */
	public Element getElement(String elementID, String[][] csvElement){
		Element element = null;
		String [] parts = new String [8];
		String [] data = new String [6];
		ArrayList<String> contextNames = this.getToolResource().getResourceStructure().getContextNames();
		Structure eltStructure = new Structure(contextNames);
		String Name= EMPTY_STRING;
		String Abbreviated= EMPTY_STRING;
		String Synonym= EMPTY_STRING;
		String Agent_Category= EMPTY_STRING;
		String Target= EMPTY_STRING;
		String Target_Category= EMPTY_STRING;
		String Detection_Method= EMPTY_STRING;
		String Signal_Source= EMPTY_STRING;
		String Background= EMPTY_STRING;
		String Synthesis= EMPTY_STRING;
		String In_Vitro= EMPTY_STRING;
		String Animal_Studies= EMPTY_STRING;
		String Human_Studies= EMPTY_STRING;
		String References= EMPTY_STRING;
		//System.out.println("Get data for the Element "+elementID.toString()+" ... ");
		try{
			MicadReadCsv csvExtractor = new MicadReadCsv(this.getToolResource(), this);
			parts = csvExtractor.getElement(elementID,csvElement);

			for(int i=0;i<=7;i++)
				if(parts[i]==null)
					parts[i]=EMPTY_STRING;

			Name = parts[0];
			Abbreviated= parts[1];
			Synonym= parts[2];
			Agent_Category= parts[3];
			Target= parts[4];
			Target_Category= parts[5];
			Detection_Method= parts[6];
			Signal_Source= parts[7];

			MicadReadWeb webExtractor = new MicadReadWeb(this.getToolResource(), this);
			data = webExtractor.getSections(elementID);

			for(int i=0;i<=5;i++)
				if(data[i]==null)
					data[i]=EMPTY_STRING;

			Background= data[0];
			Synthesis= data[1];
			In_Vitro= data[2];
			Animal_Studies= data[3];
			Human_Studies= data[4];
			References= data[5];

			// Creating element structure
		    eltStructure.putContext(Structure.generateContextName(MICAD_RESOURCEID, MICAD_ITEMKEYS[0]),  Name );
			eltStructure.putContext(Structure.generateContextName(MICAD_RESOURCEID, MICAD_ITEMKEYS[1]),  Abbreviated );
			eltStructure.putContext(Structure.generateContextName(MICAD_RESOURCEID, MICAD_ITEMKEYS[2]),  Synonym);
			eltStructure.putContext(Structure.generateContextName(MICAD_RESOURCEID, MICAD_ITEMKEYS[3]),  Agent_Category);
			eltStructure.putContext(Structure.generateContextName(MICAD_RESOURCEID, MICAD_ITEMKEYS[4]),  Target);
			eltStructure.putContext(Structure.generateContextName(MICAD_RESOURCEID, MICAD_ITEMKEYS[5]),  Target_Category);
			eltStructure.putContext(Structure.generateContextName(MICAD_RESOURCEID, MICAD_ITEMKEYS[6]),  Detection_Method);
			eltStructure.putContext(Structure.generateContextName(MICAD_RESOURCEID, MICAD_ITEMKEYS[7]),  Signal_Source);

			eltStructure.putContext(Structure.generateContextName(MICAD_RESOURCEID, MICAD_ITEMKEYS[8]),   Background);
			eltStructure.putContext(Structure.generateContextName(MICAD_RESOURCEID, MICAD_ITEMKEYS[9]),   Synthesis);
			eltStructure.putContext(Structure.generateContextName(MICAD_RESOURCEID, MICAD_ITEMKEYS[10]),  In_Vitro);
			eltStructure.putContext(Structure.generateContextName(MICAD_RESOURCEID, MICAD_ITEMKEYS[11]),  Animal_Studies);
			eltStructure.putContext(Structure.generateContextName(MICAD_RESOURCEID, MICAD_ITEMKEYS[12]),  Human_Studies);
			eltStructure.putContext(Structure.generateContextName(MICAD_RESOURCEID, MICAD_ITEMKEYS[13]),  References);

			// Creating element
			element = new Element(elementID.toString(), eltStructure);


		}catch(Exception e){
			logger.error("** PROBLEM ** Problem when extracting"+ elementID.toString()+" from "+MICAD_NAME+
					". Check the AccessTool", e);
		}
		return element;
	}

	@Override
	public String mainContextDescriptor() {
		// TODO Auto-generated method stub
		return MICAD_MAIN_ITEMKEY;
	}
}
