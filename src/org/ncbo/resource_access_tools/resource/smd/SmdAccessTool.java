package org.ncbo.resource_access_tools.resource.smd;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.ncbo.resource_access_tools.enumeration.ResourceType;
import org.ncbo.resource_access_tools.populate.Element;
import org.ncbo.resource_access_tools.populate.Structure;
import org.ncbo.resource_access_tools.populate.Element.BadElementStructureException;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;

/**
 * SmdAccessTool is responsible for getting data elements for
 * Stanford Microarray Database(SMD). Gets the experiment data to populate _ET table
 * using FTP site ftp://smd-ftp.stanford.edu/pub/smd/publications/
 * and get details for experiments using Perl script.
 *
 * @author kyadav
 * @version $$
 */
public class SmdAccessTool extends  ResourceAccessTool {

	// Home URL of the resource
	private static final String SMD_URL			= "http://smd.stanford.edu/";

	// Name of the resource
	private static final String SMD_NAME 		= "Stanford Microarray Database";

	// Short name of the resource
	private static final String SMD_RESOURCEID 	= "SMD";

	// Text description of the resource
	private static final String SMD_DESCRIPTION = "The Stanford Microarray Database (SMD) serves as a microarray research database for Stanford investigators and their collaborators.";

	// URL that points to the logo of the resource
	private static final String SMD_LOGO 		= "http://smd.stanford.edu/images/logo_art.gif";

	//Basic URL that points to an element when concatenated with an local element ID
	private static final String SMD_ELT_URL 	= "http://smd.stanford.edu/cgi-bin/data/viewDetails.pl?exptid=";

	// The set of context names
	private static final String[] SMD_ITEMKEYS  = {"exptname", "category", "subcategory", "organism", "description" };

	// Weight associated to a context
	private static final Double[] SMD_WEIGHTS   = {1.0, 0.9, 0.8, 1.0, 0.7};

	// OntoID associated for reported annotations NCBI organismal classification(1132) for organism
	private static final String[] SMD_ONTOIDS   = {Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, "1132", Structure.FOR_CONCEPT_RECOGNITION};

	// Structure for SMD Access tool
	private static final Structure SMD_STRUCTURE = new Structure(SMD_ITEMKEYS, SMD_RESOURCEID, SMD_WEIGHTS, SMD_ONTOIDS);

	// A context name used to describe the associated element
	private static final String SMD_MAIN_ITEMKEY = SMD_ITEMKEYS[0];

	// Map for organisms with organism id as key.
	private static HashMap<String, String> organismMap;

	// Map for concept id with organism id.
	private static HashMap<String, String> conceptIDMap;

	// Absolute path of Perl script file
	private static String PERL_SCRIPT_FILE = new File(SmdAccessTool.class.getResource( "smdwebservice.pl" ).getFile()).getAbsolutePath()   ;

	// Base command to execute perl script.
	private static final String BASE_COMMAND ="perl "+ PERL_SCRIPT_FILE;

	// Command to get all organisms.
	private static final String ORGANISM_COMMAND = BASE_COMMAND + " organisms";

	// Command to get details of each experiment.
	private static final String EXP_DETAIL_COMMAND = BASE_COMMAND + " detail";

	// String constant for TAXONOMY_ENUM_ID.
	private static final String ORGANISM_ID_STRING = "TAXONOMY_ENUM_ID";

	// Experiment set description tag in meta data file.
	private static final String EXPT_ID_TAG = "EXPTID";

	// Start of experiment detail.
	private static final String EXPT_DETAIL_START ="Start";

	// End of experiment detail.
	private static final String EXPT_DETAIL_END ="End";

	/**
	 * Construct SmdAccessTool using database connection property
	 * It set properties for tool Resource.
	 *
	 */
	public SmdAccessTool(){
		super(SMD_NAME, SMD_RESOURCEID, SMD_STRUCTURE);
		try {
			this.getToolResource().setResourceURL(new URL(SMD_URL));
			this.getToolResource().setResourceLogo(new URL(SMD_LOGO));
			this.getToolResource().setResourceElementURL(SMD_ELT_URL);
		} catch (MalformedURLException e) {
			logger.error(EMPTY_STRING, e);
		}
		this.getToolResource().setResourceDescription(SMD_DESCRIPTION);
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
	public int updateResourceContent(){
		return updateAllElements( );
	}

	/**
	 * This method update experiments for SMD Resource Access Tool
	 *
	 * @return number of elements updated
	 */
	private int updateAllElements(){
		int totalNumberOfExperiments = 0;
		int nbElement = 0;
		logger.info("Updating " + this.getToolResource().getResourceName() + " elements...");

		// Get all ExperimentSet containing experiment id's in it.
		HashMap<String, ExperimentSet> experimentSets= new SmdFtpUtils().getAllExperimentSets();

		for (String expSetNumber : experimentSets.keySet()) {
			 logger.info("For experiment set number " + expSetNumber+ ", number of experimets found : " +experimentSets.get(expSetNumber).getExperimentIDs().size());
			 totalNumberOfExperiments+=experimentSets.get(expSetNumber).getExperimentIDs().size();
		}
		logger.info("Total Number of Experiments Present : " + totalNumberOfExperiments  );

		// Initialize organisms map.
		organismMap=getOrganismsMap();
		// Mapping organisms to concept ID.
		conceptIDMap = getLocalConceptIDMap(organismMap);

		HashSet<String> allElementLocalIDs = resourceUpdateService.getAllLocalElementIDs();
		HashSet<String> experimentIDs;

		// Process each experiment set and get experiment id's from it to populate ET table
		for (String expSetNumber : experimentSets.keySet()) {
			try {
			    experimentIDs= experimentSets.get(expSetNumber).getExperimentIDs();
			    if(experimentIDs== null){
			    	continue;
			    }
				experimentIDs.removeAll(allElementLocalIDs);
				nbElement += this.updateElementTableWithExperimentIDs(experimentIDs, experimentSets.get(expSetNumber).getDecription());
			} catch (BadElementStructureException e) {
				 logger.error("** PROBLEM ** Cannot update " + this.getToolResource().getResourceName() +" because of a Structure problem.", e);
			}
		}

		return nbElement;
	}

	/**
	 * This method process each experiment id using perl script
	 * and populate experiment details.Perl script returns result in key-value strings format.
	 *
	 * @param experimentIDs
	 * @return number of element updated.
	 * @throws BadElementStructureException
	 */
	private int updateElementTableWithExperimentIDs(HashSet<String> experimentIDs, String description) throws BadElementStructureException{

		int nbElement = 0;
		ArrayList<String> contextNames = this.getToolResource().getResourceStructure().getContextNames();
		Element element;
		Structure eltStructure = new Structure(contextNames);
		String localElementID= EMPTY_STRING;
		String name ;
		String category ;
		String subcategory ;
		String organism ;
		String organismID;
		String resultLine;
		HashMap<String, String> experimentDetailTemp= null;
		Process process = null;
		int MAX_EXPERIMENTS_PROCESS = 30;
		int max;
		StringBuffer expIDlist ;
		//Converting set to array
		String [] experimentIDArray = new String[experimentIDs.size()];
	    experimentIDArray=   experimentIDs.toArray(experimentIDArray);

	    // Process experiments id's
		for (int step = 0; step < experimentIDArray.length; step += MAX_EXPERIMENTS_PROCESS) {

			max = step + MAX_EXPERIMENTS_PROCESS;
			expIDlist =  new StringBuffer();
			if (max > experimentIDArray.length) {
				max = experimentIDArray.length;
			}

			// append experiment id separated by blank space
			for (int u = step; u < max; u++) {
				expIDlist.append(experimentIDArray[u]);
				if (u < max - 1) {
					expIDlist.append(BLANK_SPACE);
				}
			}
			// Execute perl command to get experiment details for maximum 50 experiment ids .
			process = executePerlCommand(EXP_DETAIL_COMMAND+  BLANK_SPACE + expIDlist);
 			// Create reader for  input steam of perl script
			BufferedReader perlProcessReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

 			List<HashMap<String, String>> experiments= new ArrayList<HashMap<String, String>>();

 			try{
 				/*
 				 * Reading each line of perl script result and
 				 * Creating list containing experiment detail map.
 				 */
 				while((resultLine = perlProcessReader.readLine()) != null) {

 					if(EXPT_DETAIL_START.equals(resultLine)){

	 					experimentDetailTemp = new HashMap<String, String>();
	 				}else if(EXPT_DETAIL_END.equals(resultLine)){

	 					if(experimentDetailTemp!= null){
	 						experiments.add(experimentDetailTemp);
	 					}
	 				}else{
	 					String [] experimentInfo=resultLine.split(TAB_STRING);
		 				// Get each line as Key --> Value pair separated by tab delimiter
		 				if(experimentInfo.length == 2){
		 					experimentDetailTemp.put(experimentInfo[0], experimentInfo[1]);
						}
	 				}
	 			}
 				perlProcessReader.close();
 			}catch (IOException e) {
				 logger.error("Problem in getting perl result", e );
			}

 			// Iterating each experiment detail map
 			for (HashMap<String, String> experimentDetail : experiments) {
 				try {

	 				// Extracting experiment name.
					name= experimentDetail.get(SMD_ITEMKEYS[0].toUpperCase());
					if(name== null){
						name= EMPTY_STRING;
					}

					// Extracting category name.
					category= experimentDetail.get(SMD_ITEMKEYS[1].toUpperCase());
					if(category== null){
						category= EMPTY_STRING;
					}

					// Extracting sub category.
					subcategory= experimentDetail.get(SMD_ITEMKEYS[2].toUpperCase());
					if(subcategory== null){
						subcategory= EMPTY_STRING;
					}

					 // Assigning localElementID as experimentID
				    localElementID=experimentDetail.get(EXPT_ID_TAG);

					// Get organism id.
					organismID= experimentDetail.get(ORGANISM_ID_STRING);
				    if(conceptIDMap.get(organismID)!= null){
				    	organism = conceptIDMap.get(organismID);
				    } else{
				    	organism= EMPTY_STRING;
				    	logger.error("Cannot map Organism with id "+ organismID +" to local concept id for element with ID " + localElementID +".");
				    }

					// Creating element structure
				    eltStructure.putContext(Structure.generateContextName(SMD_RESOURCEID, SMD_ITEMKEYS[0]), name );
					eltStructure.putContext(Structure.generateContextName(SMD_RESOURCEID, SMD_ITEMKEYS[1]), category );
					eltStructure.putContext(Structure.generateContextName(SMD_RESOURCEID, SMD_ITEMKEYS[2]),  subcategory);
					eltStructure.putContext(Structure.generateContextName(SMD_RESOURCEID, SMD_ITEMKEYS[3]),  organism);

					if(description== null){
						description = EMPTY_STRING;
					}
					eltStructure.putContext(Structure.generateContextName(SMD_RESOURCEID, SMD_ITEMKEYS[4]),  description);


					// Creating element
					element = new Element(localElementID, eltStructure);

					// Persisting element into database.
					if (resourceUpdateService.addElement(element)){
							nbElement ++;
					}
 				}catch (Exception e) {
 					logger.error("** PROBLEM ** In creating Element with localElementID " + localElementID ,e );
 				}
			}

		}

		return nbElement;
	}

	/**
	 * This method create map for local concept id for given organism id's.
	 * @param organismMap
	 * @return
	 */
	private HashMap<String, String> getLocalConceptIDMap(HashMap<String, String> organismMap)
	{
		HashMap<String, String> conceptMap= new HashMap<String, String>();
		String organismID;
		String conceptIDs;
		for (Iterator<String> iterator = organismMap.keySet().iterator(); iterator.hasNext();) {
			organismID = iterator.next();
			conceptIDs= resourceUpdateService.mapTermsToVirtualLocalConceptIDs(organismMap.get(organismID), SMD_ONTOIDS[3], null);
			conceptMap.put(organismID, conceptIDs);
		}

		return conceptMap;
	}

	/**
	 * This method get organisms map using perl command <code>ORGANISM_COMMAND</code>.
	 *
	 * @return map of organisms.
	 */
	private HashMap<String, String> getOrganismsMap(){
		HashMap<String, String> organismMap = new HashMap<String, String>();
		Process process = null;
		try {
			process = executePerlCommand(ORGANISM_COMMAND);
			BufferedReader resultReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String resultLine = EMPTY_STRING;

			// Tab separated string containing id and name of organism.
			while((resultLine = resultReader.readLine()) != null) {
				String [] organismInfo=resultLine.split(TAB_STRING);
				if(organismInfo.length == 2){
					organismMap.put(organismInfo[0], organismInfo[1]);
				}
			}
			resultReader.close();

		}catch (Exception e) {
			logger.error(EMPTY_STRING, e);
		}
		return organismMap;
	}

	/**
	 * This method execute perl command using <code>Runtime</code>
	 *
	 * @param String containing perl command
	 * @return Process
	 */
	public static Process executePerlCommand(String command){
		Runtime runtime = Runtime.getRuntime();
		Process process = null;
		try {
			process = runtime.exec(command);
		}catch (Exception e) {
			logger.error(EMPTY_STRING, e);
		}
		return process;
	}

	@Override
	public String elementURLString(String elementLocalID) {
		return SMD_ELT_URL + elementLocalID;
	}

	@Override
	public String mainContextDescriptor() {
		return SMD_MAIN_ITEMKEY;
	}

	@Override
	public HashSet<String> queryOnlineResource(String query) {
		return new HashSet<String>();
	}
}
