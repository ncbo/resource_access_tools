package org.ncbo.resource_access_tools.resource.biositemaps;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.bioontology.biositemaps.api.BioSitemap;
import org.bioontology.biositemaps.api.Resource;
import org.bioontology.biositemaps.api.ResourceDescription;
import org.ncbo.resource_access_tools.enumeration.ResourceType;
import org.ncbo.resource_access_tools.populate.Element;
import org.ncbo.resource_access_tools.populate.Structure;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;

import edu.stanford.smi.protegex.owl.model.impl.DefaultOWLNamedClass;

/**
 * BioSitemapsAccessTool is responsible for getting data elements for
 * Biositemaps.
 * It process all resource descriptions of biositemap from RDF files
 * using Biositemap API v1.4
 *
 * @author kyadav
 * @version $$
 */
public class BioSitemapsAccessTool extends ResourceAccessTool {

	// Home URL of the resource
	private static final String BSM_URL			= "http://www.ncbcs.org/biositemaps/";

	// Name of the resource
	private static final String BSM_NAME 		= "Biositemaps";

	// Short name of the resource
	private static final String BSM_RESOURCEID 	= "BSM";

	// Text description of the resource
	private static final String BSM_DESCRIPTION = "Biositemaps represent a mechanism for computational biologists and bio-informaticians to openly broadcast and retrieve meta-data about biomedical data, tools and services (i.e., biomedical resources) over the Internet.";

	// URL that points to the logo of the resource
	private static final String BSM_LOGO 		= "http://www.ncbcs.org/biositemaps/images/BioSitemaps_Logo_alone.jpg";

	// Basic URL that points to an element when concatenated with an local element ID
	private static final String BSM_ELT_URL 	= "http://biositemaps.bioontology.org/editor/BiositemapBrowser.html?file=";

	//private static final String BSM_ELT_URL_PARAM_FILE 	= "?file=";
	//private static final String BSM_ELT_URL_PARAM_RESOURCE 	= "&resource=";

	// The set of context names
	private static final String[] BSM_ITEMKEYS = {"name", "description", "keywords", "resource_type", "url"};

	// Weight associated to a context
	private static final Double[] BSM_WEIGHTS  = {1.0, 0.8, 1.0, 0.8, 0.0};

	// OntoID associated for reported annotations. 1104 -virtual ontology ID for Biomedical Resource Ontology(BRO)
	private static final String[] BSM_ONTOIDS  = {Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, "1104", Structure.NOT_FOR_ANNOTATION};

	// Structure for GEO Access tool
	private static final Structure BSM_STRUCTURE = new Structure(BSM_ITEMKEYS, BSM_RESOURCEID, BSM_WEIGHTS, BSM_ONTOIDS);

	// A context name used to describe the associated element
	private static final String BSM_MAIN_ITEMKEY = "name";

	// Relative path for temporary rdf file.
	private static final String BSM_TEMP_FILE = "temp_biositemaps.rdf";

	// Registry url containing url for biositemaps rdf files
	private static final String BSM_REGISTRY = "http://www.ncbcs.org/biositemaps/biositemap.registry";

	// Biositemap field
	private BioSitemap biositemap = null;

	/**
	 * Construct BioSitemapsAccessTool using database connection property
	 * It set properties for tool Resource
	 *
	 */
	public BioSitemapsAccessTool(){
		super(BSM_NAME, BSM_RESOURCEID, BSM_STRUCTURE);
		try {
			this.getToolResource().setResourceURL(new URL(BSM_URL));
			this.getToolResource().setResourceLogo(new URL(BSM_LOGO));
			this.getToolResource().setResourceElementURL(BSM_ELT_URL);
		} catch (MalformedURLException e) {
			logger.error(EMPTY_STRING, e);
		}
		this.getToolResource().setResourceDescription(BSM_DESCRIPTION);
	}

	@Override
	public ResourceType  getResourceType() {
		return ResourceType.SMALL;
	}

	@Override
	public void updateResourceInformation() {
		// TODO: Please add you code here
	}

	@Override
	public int updateResourceContent(){
		return updateWithRdfFiles();
	}

	/**
	 * This method populate ET table from RDF files.
	 * Each RDF file contains resource descriptions that can be processed as elements.
	 *
	 */
	 protected int updateWithRdfFiles()  {
		int nbElement = 0;
		ArrayList<String> contextNames = this.getToolResource().getResourceStructure().getContextNames();
		Element element;
		Structure eltStructure = new Structure(contextNames);
		String name= EMPTY_STRING;
		String description= EMPTY_STRING;
		String keywords= EMPTY_STRING;
		String resource_type= EMPTY_STRING;
		String rdfID= EMPTY_STRING;
		String localElementID= EMPTY_STRING;
		String url= EMPTY_STRING;

		File tempRDFFile = new File(BSM_TEMP_FILE);

		List<String> biositesmaps= getBiositemaps();
		// Get latest local onotlogy for given  virtual ontology id

		// Process each biositemap URL
		for (Iterator<String> biositemapIterator = biositesmaps.iterator(); biositemapIterator.hasNext();) {
			String rdfURL =  biositemapIterator.next();
			logger.info("Processing biositemaps for file : "+ rdfURL);
			List<ResourceDescription>  resources=getResources(rdfURL);
		 	if(resources!= null){
				// Process each resource description.
				for (Iterator<ResourceDescription> iterator = resources.iterator(); iterator
						.hasNext();) {
					ResourceDescription resourceDescription =  iterator.next();
					try {
						// Extracting name
						if(resourceDescription.hasResourceName()){
							name = resourceDescription.getResourceName().toArray()[0].toString();
						}else{
							name = EMPTY_STRING;
						}

					    // Extraction of description
					    if(resourceDescription.hasDescription()){
					    	description = resourceDescription.getDescription().toArray()[0].toString();
					    }else{
					    	description = EMPTY_STRING;
					    }

					    // Extraction of keywords
					    if(resourceDescription.hasKeywords()){
					    	 keywords = resourceDescription.getKeywords().toArray()[0].toString();
					    }else{
					    	 keywords= EMPTY_STRING;
					    }

					    // Extracting localElementID
					    rdfID = resourceDescription.getInstanceName();
					    if(rdfID.contains(HASH_STRING)){
					    	rdfID=rdfID.split(HASH_STRING)[1];
					    }
				        localElementID= rdfURL + PLUS_STRING + rdfID;

				        //Extracting URL
				        if(resourceDescription.hasURL()){
					    	 url = resourceDescription.getURL().toArray()[0].toString();
					    }else{
					    	 url= EMPTY_STRING;
					    }

						// Creating element structure
					    eltStructure.putContext(Structure.generateContextName(BSM_RESOURCEID, BSM_ITEMKEYS[0]), name );
						eltStructure.putContext(Structure.generateContextName(BSM_RESOURCEID, BSM_ITEMKEYS[1]), description );
						eltStructure.putContext(Structure.generateContextName(BSM_RESOURCEID, BSM_ITEMKEYS[2]),  keywords);
						eltStructure.putContext(Structure.generateContextName(BSM_RESOURCEID, BSM_ITEMKEYS[3]),  resource_type);
						eltStructure.putContext(Structure.generateContextName(BSM_RESOURCEID, BSM_ITEMKEYS[4]),  url);

						// Creating element
						element = new Element(localElementID, eltStructure);

						// Persisting element into database.
						if (resourceUpdateService.addElement(element)){
								nbElement ++;
						}
					} catch (Exception e) {
						logger.error("Problem in creating Element ", e);
					}

				}
			}

		}

		// Delete temporary rdf file
		tempRDFFile.delete();
		logger.info("Number of elements processed : " +nbElement );
		return nbElement;
	}

	 /**
	  * This method create BioSitemap for given RDF file URL
	  * using Biositemap API v1.4 and return all resource descriptions
	  * present in it.
	  *
	  * @param rdfFile - <code>String</code> containing absolute file path
	  * @return Collections of <code>ResourceDescription</code>
	  */
	private List<ResourceDescription> getResources(String rdfFileURL) {
		try {
			URL rdfURL = new URL(rdfFileURL);
			HttpURLConnection  connection = (HttpURLConnection)rdfURL.openConnection();
			try {
				connection.connect();
			} catch (IOException e) {
				Thread.sleep(1000);
				connection.connect();
			}

			biositemap  = BioSitemap.open(connection.getInputStream(), BSM_TEMP_FILE);
			connection.getInputStream().close();

			return (List<ResourceDescription>)biositemap.getAllResourceDescriptions();
		} catch (Exception e) {
			logger.error("Problem in creating Biositemap for RDF file : "+ rdfFileURL , e);
		}
		return null;
	}

	/**
	 *
	 * This method get the list of biositemaps rdf files form <code>BSM_REGISTRY</code> url
	 *
	 * @return List of biositemaps rdf files
 	 */
	public List<String> getBiositemaps(){
		List<String> biositemaps= new ArrayList<String>();
		try {
			URL rdfURL = new URL(BSM_REGISTRY);
			HttpURLConnection  connection = (HttpURLConnection)rdfURL.openConnection();
			try {
				connection.connect();
			} catch (IOException e) {
				Thread.sleep(1000);
				connection.connect();
			}

			BufferedReader buferedReader = new BufferedReader(new InputStreamReader(
									connection.getInputStream()));
			String url;
			while ((url = buferedReader.readLine()) != null) {
				biositemaps.add(url);
			}
			buferedReader.close();

		} catch (Exception e) {
			logger.error("Problem in creating Biositemaps from registry url : "+ BSM_REGISTRY , e);
		}

		return biositemaps;
	}


	/**
	 * This method extract resource type from resource description.
	 *
	 * @param resourceDescription
	 * @return
	 */
	private String getResourceType(ResourceDescription resourceDescription, String latestLocalOntologyID) {
		StringBuffer resourceType= new StringBuffer(EMPTY_STRING);
		try{
			if(resourceDescription.getResourceType().size() >0){
				// Getting several resource types
				for (Iterator<Resource> resourceTypeIterator = resourceDescription.getResourceType().iterator(); resourceTypeIterator
						.hasNext();) {
					Resource resource = resourceTypeIterator.next();
                    //rajesh
					if(resource.getBRO_ResourceInstance().getRDFTypes()!= null
							&& resource.getBRO_ResourceInstance().getRDFTypes().size()>0){

						for (Iterator<?> rdfTypeIterator = resource.getBRO_ResourceInstance().getRDFTypes().iterator(); rdfTypeIterator
								.hasNext();) {
							DefaultOWLNamedClass  defaultOWLNamedClass= (DefaultOWLNamedClass) rdfTypeIterator.next();
						    resourceType.append(latestLocalOntologyID).append(SLASH_STRING);
							resourceType.append(defaultOWLNamedClass.getBrowserText());
							resourceType.append(GT_SEPARATOR_STRING);
						}
					}

				}

				resourceType.delete(resourceType.length()-2, resourceType.length());
			}
		}
		catch (Exception e) {
			logger.error("Problem in geting resource type for resourceDescription: "+resourceDescription+".", e);
		}

		return resourceType.toString();
	}

	@Override
	public String elementURLString(String elementLocalID) {
		/*
		//URL for biositmap-viewer
		try {
			// TODO: to be tested with both
			//return java.net.URLEncoder.encode(BSM_ELT_URL + elementLocalID.replace(PLUS_STRING, "&resource="), "UTF-8");
			String[] params = elementLocalID.split(PLUS_STRING_REG);
			//TODO check for params size
			return BSM_ELT_URL +
				BSM_ELT_URL_PARAM_FILE + java.net.URLEncoder.encode(params[0], "UTF-8") +
				BSM_ELT_URL_PARAM_RESOURCE + java.net.URLEncoder.encode(params[1], "UTF-8");
		} catch (java.io.UnsupportedEncodingException e) {
			logger.error("Problem when encoding the URL for elementLocalID: "+elementLocalID, e);
			return BSM_ELT_URL + elementLocalID.replace(PLUS_STRING, "&resource=");
		}
		*/

		// URL suing the url field in the ElementTable
		// we should not have context in ElementTable that are used like that for reconstruct the URLs
		// as the result the context BSM_url will go through, as the other contexts, the whole workflow mgrep etc. for nothing.
		// TODO: change that in the future by adapting the workflow to exclude certain fields from ET that are neither text to process with Mgrep or direct annotation
		return resourceUpdateService.getContextValueByContextName(elementLocalID, Structure.generateContextName(BSM_RESOURCEID, BSM_ITEMKEYS[4]));
	}

	@Override
	public String mainContextDescriptor() {
		return BSM_MAIN_ITEMKEY;
	}

	@Override
	public HashSet<String> queryOnlineResource(String query) {

		return new HashSet<String>();
	}

}
