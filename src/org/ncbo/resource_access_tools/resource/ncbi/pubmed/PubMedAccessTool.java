package org.ncbo.resource_access_tools.resource.ncbi.pubmed;

import gov.nih.nlm.ncbi.www.soap.eutils.efetch_pubmed.AbstractType;
import gov.nih.nlm.ncbi.www.soap.eutils.efetch_pubmed.DateCreatedType;
import gov.nih.nlm.ncbi.www.soap.eutils.efetch_pubmed.EFetchPubmedServiceLocator;
import gov.nih.nlm.ncbi.www.soap.eutils.efetch_pubmed.EFetchRequest;
import gov.nih.nlm.ncbi.www.soap.eutils.efetch_pubmed.EFetchResult;
import gov.nih.nlm.ncbi.www.soap.eutils.efetch_pubmed.EUtilsServiceSoap;
import gov.nih.nlm.ncbi.www.soap.eutils.efetch_pubmed.KeywordType;
import gov.nih.nlm.ncbi.www.soap.eutils.efetch_pubmed.MeshHeadingType;
import gov.nih.nlm.ncbi.www.soap.eutils.efetch_pubmed.OtherAbstractType;
import gov.nih.nlm.ncbi.www.soap.eutils.efetch_pubmed.PubmedArticleType;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.xml.rpc.ServiceException;

import obs.common.utils.ExecutionTimer;
import obs.obr.populate.Element;
import obs.obr.populate.Structure;
import obs.obr.populate.Element.BadElementStructureException;

import org.ncbo.stanford.obr.enumeration.ResourceType;
import org.ncbo.resource_access_tools.resource.ncbi.AbstractNcbiResourceAccessTool;
import org.ncbo.stanford.obr.util.FileResourceParameters;
import org.ncbo.stanford.obr.util.MessageUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.aliasi.medline.Abstract;
import com.aliasi.medline.KeywordList;
import com.aliasi.medline.MedlineCitation;
import com.aliasi.medline.MedlineHandler;
import com.aliasi.medline.MedlineParser;
import com.aliasi.medline.MeshHeading;
import com.aliasi.medline.OtherAbstract;
import com.aliasi.medline.Topic;
import com.aliasi.util.Files;

/**
 * PubMedAccessTool is responsible for getting data elements for
 * Pub Med. The latest files available for the particular year
 * http://www.nlm.nih.gov/bsd/licensee/2010_stats/baseline_med_filecount.html
 *
 * FTP location ftp://ftp.nlm.nih.gov/nlmdata/.medleasebaseline/zip/
 *
 * This tool fetch data using xml files and E-Utilities.
 * <p>
 * To fetch data using E-utilities  use method <code>updateResourceContent</code>
 * and to fetch data using xml files use method <code>downloadResourceContent</code>
 *
 * @author  Kuladip Yadav
 * @version $$
 */
public class PubMedAccessTool extends AbstractNcbiResourceAccessTool {

	private static final String PM_URL = "http://www.pubmed.gov";
	private static final String PM_NAME = "PubMed";
	private static final String PM_RESOURCEID = "PM";
	private static final String PM_DESCRIPTION = "PubMed is a service of the U.S. National Library of Medicine  that includes over 17 million citations from MEDLINE and other life science journals for biomedical articles back to the 1950s. PubMed includes links to full text articles and other related resources.";
	private static final String PM_LOGO = "http://www.ncbi.nlm.nih.gov/entrez/query/static/gifs/iconsml.gif";

	// Basic URL that points to an element when concatenated with an local element ID
	// private static final String PM_ELT_URL = "http://www.ncbi.nlm.nih.gov/sites/entrez?Db=pubmed&Cmd=ShowDetailView&TermToSearch=";
	 private static final String PM_ELT_URL = "http://www.ncbi.nlm.nih.gov/pubmed/";

	// Database for E-Utils
	private static final String PM_EUTILS_DB = "pubmed";

	// Query terms for E-Utils
	private static final String PM_EUTILS_TERM = "all[filter]";

	private static final String[] PM_ITEMKEYS 	= {"title", "abstract", "keywords", "meshheadings" };
	private static final Double[] PM_WEIGHTS 	= {1.0, 0.8, 0.7, 0.7 };
	private static final String[] PM_ONTOIDS  	= {Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, "1351"};
	private static Structure PM_STRUCTURE = new Structure(PM_ITEMKEYS, PM_RESOURCEID, PM_WEIGHTS, PM_ONTOIDS);
	private static String PM_MAIN_ITEMKEY = "title";

	// Absolute path for folder containing pubmed xml files
	private static final String PM_FOLDER = FileResourceParameters.resourceFolder() +"/pubmed/";

	private static final String PM_FILE_PREFIX_2010 = "medline11n";

	// Start processing xml file number
    private static int START_XML_NUMBER = Integer.parseInt(MessageUtils.getMessage("obr.pm.xml.from"));

    // End processing xml file number
	private static int END_XML_NUMBER =  Integer.parseInt(MessageUtils.getMessage("obr.pm.xml.to"));

	 // End processing xml file number
	private static boolean PROCESS_XML_FILES = Boolean.parseBoolean(MessageUtils.getMessage("obr.pm.process.xml"));

	// lingpipe parser
	private static MedlineParser medlineParser = new MedlineParser(true);

	/**
	 * Constructor with connection information as arguments.
	 *
	 * @param String[] obsConnectionInfo
	 *
	 */
	public PubMedAccessTool() {
		super(PM_NAME, PM_RESOURCEID, PM_STRUCTURE);
		try {
			this.getToolResource().setResourceURL(new URL(PM_URL));
			this.getToolResource().setResourceLogo(new URL(PM_LOGO));
			this.getToolResource().setResourceElementURL(PM_ELT_URL);
		} catch (MalformedURLException e) {
			logger.error("", e);
		}
		this.getToolResource().setResourceDescription(PM_DESCRIPTION);

		// PUBMED supports date parameter for E-UTILS.
		this.supportDate = true;
	}

	@Override
	public ResourceType  getResourceType() {
		return ResourceType.BIG;
	}

	@Override
	/**
	 * Get eutils database String
	 *
	 * return String
	 */
	protected String getEutilsDB() {
		return PM_EUTILS_DB;
	}

	@Override
	/**
	 *  Get query terms for eutils.
	 *
	 *  return String
	 */
	protected String getEutilsTerm() {
		return PM_EUTILS_TERM;
	}

    @Override
	public void updateResourceInformation() {}

	/**
	 * Method uses E-utilities to get elements for pubmed.
	 * It updates elements for last <code>numberOfDays</code>
	 *
	 * return int
	 */
	@Override
	public int updateResourceContent() {

		int nbElements = 0;
		if(PROCESS_XML_FILES){
			 nbElements = downloadResourceContent();
		}else{
			// Get number of day's since last update.
			// this.numberOfDays = new Integer(EMPTY_STRING + this.numberOfDaysSinceLastUpdate());
			 this.numberOfDays = new Integer(365);
			 nbElements += super.eutilsUpdateAll(null);
		}

		return nbElements;
	}

	/**
	 * This method populate data for given set of UIDs.
	 * It uses {@code EFetchRequest} to get pubmed element.
	 * Stub classes (Client ) are created using wsdl
	 * http://www.ncbi.nlm.nih.gov/entrez/eutils/soap/v2.0/efetch_pubmed.wsdl
	 * and placed in efetchPubmed.jar
	 *
	 * @param HashSet<String> UIDs
	 * @throws Exception BadElementStructureException
	 * @return int number of elements processed
	 */
	@Override
	protected int updateElementTableWithUIDs(HashSet<String> UIDs)
			throws BadElementStructureException {
		int nbElement = 0;

		// Creating E-fetch request
		EFetchRequest efetchRequest = new EFetchRequest();
		efetchRequest.setEmail(EUTILS_EMAIL);
		efetchRequest.setTool(EUTILS_TOOL);

		EFetchResult efetchResult;
		StringBuffer UIDlist;
		PubmedArticleType[] resultArticles;
		PubmedArticleType article;
		AbstractType articleAT;
		String articleTitle;
		String articleAbstract;
		String articlePMID;
		ExecutionTimer timer = new ExecutionTimer();

		StringBuffer abstrtext;
		OtherAbstractType[] otherAbstracts;
		StringBuffer keywordstext;
		KeywordType[][] keywordlist;
		MeshHeadingType[] meshheadingtab;
		PubMedElement pubMedElement;

		String[] UIDsTab = new String[UIDs.size()];
		UIDsTab = UIDs.toArray(UIDsTab);
		int max;

		EFetchPubmedServiceLocator toolService = new EFetchPubmedServiceLocator();
		EUtilsServiceSoap toolEutils = null;
		try {
			toolEutils = toolService.geteUtilsServiceSoap();
		} catch (ServiceException e2) {
			logger.error("Problem creating eUtils service", e2);
		}


		/*
		 * This loop process 500 UIDs for each iteration.
		 * It fires E-Fetch request for each request.
		 *
		 */
		for (int step = 0; step < UIDsTab.length; step += EUTILS_MAX) {
			timer.reset();
			timer.start();
			max = step + EUTILS_MAX;
			UIDlist = new StringBuffer();
			if (max > UIDsTab.length) {
				max = UIDsTab.length;
			}

			// Append 500 UIDs with comma as separator
			for (int u = step; u < max; u++) {
				UIDlist.append(UIDsTab[u]);
				if (u < max - 1) {
					UIDlist.append(COMMA_STRING);
				}
			}
		    efetchRequest.setId(UIDlist.toString());
			try {

				// Fire E- Fetch request
				efetchResult = toolEutils.run_eFetch(efetchRequest);
				resultArticles = efetchResult.getPubmedArticleSet();

				// Process each Articles
				for (int i = 0; i < resultArticles.length; i++) {
					article = resultArticles[i];

					// Extracting title for Articles
					articleTitle = article.getMedlineCitation().getArticle().getArticleTitle();
					if (articleTitle == null) {
						articleTitle = BLANK_SPACE;
					}

					// Extracting abstract
					articleAT = article.getMedlineCitation().getArticle().get_abstract();
					abstrtext = new StringBuffer();
					if (articleAT != null) {
						abstrtext.append(articleAT.getAbstractText());
					}

					// Append other abstract to abstract
					otherAbstracts = article.getMedlineCitation().getOtherAbstract();
					if (otherAbstracts != null && otherAbstracts.length >= 0) {
						for (int j = 0; j < otherAbstracts.length; j++) {
							abstrtext.append(BLANK_SPACE);
							abstrtext.append(otherAbstracts[j]
									.getAbstractText());
						}
					}
					articleAbstract = abstrtext.toString();

					// Extracting keywords
					keywordstext = new StringBuffer();
					keywordlist = article.getMedlineCitation().getKeywordList();
					if (keywordlist != null && keywordlist.length >= 0) {
						for (int k = 0; k < keywordlist.length; k++) {
							for (int j = 0; j < keywordlist[k].length; j++) {
								keywordstext.append(keywordlist[k][j]
										.get_value());
								keywordstext.append(COMMA_SEPARATOR);
							}
						}
					}

					// Getting PubMed ID
					articlePMID = article.getMedlineCitation().getPMID();

					// Extracting mesh headings
					meshheadingtab = article.getMedlineCitation().getMeshHeadingList();
					String concepts = EMPTY_STRING;
					if(meshheadingtab!= null && meshheadingtab.length>0){
						concepts= mapMeshHeadingsToLocalConceptIDs(meshheadingtab);
						 // if mapping concepts are null or empty then log message for it.
						if(concepts== null || concepts.trim().length()==0){
							logger.error("Cannot map Mesh headings to local concept ID's for element with ID " + articlePMID +".");
					 	}
					}

         			// Creating PUBMED element
					pubMedElement = new PubMedElement(this, articlePMID, articleTitle, articleAbstract , keywordstext.toString(), concepts);

					// Add element to ET table
		            if(resourceUpdateService.addElement(pubMedElement.getElement()))
		            	   nbElement++;

				}
				timer.end();
				Thread.sleep(Math.abs(3001 - timer.duration()));
			} catch (RemoteException e) {
				logger.error("** PROBLEM ** Cannot get information using EFetch.",e);
			} catch (InterruptedException e1) {
				logger.error("** PROBLEM ** Cannot wait to 3 seconds.", e1);
			}
		}

		return nbElement;
	}

	/**
	 * The method map each mesh heading to concept id for 'MSH' ontology id.
	 * Concept id's are separated by string '> '.
	 *
	 * @param MeshHeadingType[]
	 * @return {@link String}
	 */
	public String mapMeshHeadingsToLocalConceptIDs(MeshHeadingType[] meshheadingtab) {

		HashSet<String> meshHeadings= new HashSet<String>();
		for (MeshHeadingType meshHeading : meshheadingtab) {
				meshHeadings.add(meshHeading.getDescriptorName().get_value().trim());
		}

		return mapMeshHeadingsToLocalConceptIDs (meshHeadings ).toString();
	}

	/**
	 * This method gives number of day's since last update.
	 *
	 * @return int
	 */
	public long numberOfDaysSinceLastUpdate() {

		long nbOfDays = 0; // get the last entry in the corresponding _ET
		String lastLocalID = getResourceUpdateService().getLastElementLocalID();
		if (lastLocalID != null) {
			// use efetch to get the date of the creation of this element in
			// NCBI DB

			EFetchRequest efetchRequest = new EFetchRequest();
			efetchRequest.setEmail(EUTILS_EMAIL);
			efetchRequest.setTool(EUTILS_TOOL);
			efetchRequest.setId(lastLocalID);

			EFetchResult efetchResult;
			Calendar articleCal = Calendar.getInstance();

			try {

				EFetchPubmedServiceLocator toolService = new EFetchPubmedServiceLocator();
				EUtilsServiceSoap toolEutils = toolService
						.geteUtilsServiceSoap();

				efetchResult = toolEutils.run_eFetch(efetchRequest);
				PubmedArticleType[] resultArticles = efetchResult
						.getPubmedArticleSet();
				PubmedArticleType article = resultArticles[0];
				DateCreatedType articleDate = article.getMedlineCitation()
						.getDateCreated();
				articleCal.set(Integer.parseInt(articleDate.getYear()), Integer
						.parseInt(articleDate.getMonth()), Integer
						.parseInt(articleDate.getDay()));
			} catch ( Exception e) {
				logger.error(
						"** PROBLEM ** Cannot    get the date of the last record for "
								+ this.getToolResource().getResourceName()
								+ " using Efetch.", e);
			}
			// compute the number of days between today and this date Calendar
			Calendar rightNow = Calendar.getInstance();
			nbOfDays =   (rightNow.getTimeInMillis() - articleCal.getTimeInMillis())/(1000 * 60 *60 *24);

		}
		return nbOfDays;


	}

	@Override
	/**
	 *  Method gives String url containing details of element
	 *
	 */
	public String elementURLString(String elementLocalID) {
		return PM_ELT_URL + elementLocalID;
	}

	@Override
	public String mainContextDescriptor() {
		return PM_MAIN_ITEMKEY;
	}


	/**
	 * This method uses XML files to populate element table.
	 * It process XML files with number starts from START_XML_NUMBER to END_XML_NUMBER
	 *
	 * @return int value for number of elements processed
	 */
	public int downloadResourceContent() {
		return this.updateWithXMLFiles(START_XML_NUMBER, END_XML_NUMBER);
	}

	/**
	 * This method accepts file number and process all the file in between two number
	 * lingpipe parser is used for parsing xml files.
	 *
	 * @param int fromNumber
	 * @param int upToNumber
	 * @return int
	 */
	private int updateWithXMLFiles(int fromNumber, int upToNumber){
		int nbElement = 0;
		File xmlFile;
		MedlineDbLoader dbLoader;

		for (int i= fromNumber; i<=upToNumber; i++){
			try{
				xmlFile = new File(PM_FOLDER + PM_FILE_PREFIX_2010 + this.numberString(i) + ".xml");
				logger.info("Updating " + this.getToolResource().getResourceId() + " elements with XML file: " + xmlFile.getName());
				dbLoader = new MedlineDbLoader(this);
				//parse using lingpipe SAX XML parser
				loadXML(dbLoader, xmlFile);
				logger.info("Number of elements in this file " + dbLoader.getNbCitation());
				nbElement += dbLoader.getNbCitation();
			}catch (IOException e) {
				 logger.error("** PROBLEM ** Cannot find XML file to parse.", e);
			}catch (SAXException e) {
				 logger.error("** PROBLEM ** Cannot parse the XML.", e);
			}
		}
		return nbElement;
	}

	/**
	 * This method create number String from given number.
	 *
	 * @param int i
	 * @return String
	 */
	private String numberString(int i){
		StringBuffer sb = new StringBuffer();
		if(i<1000){
			sb.append(0);
			if(i<100){
				sb.append(0);
				if(i<10){
					sb.append(0);
				}
			}
			sb.append(Integer.toString(i));
		}
		return sb.toString();
	}

	/**
	 * This method parse given file using lingpipe's Medline parser
	 *
	 * @param MedlineDbLoader dbLoader
	 * @param File file
	 * @throws IOException
	 * @throws SAXException
	 * @return void
	 */
	private static void loadXML(MedlineDbLoader dbLoader, File file) throws IOException, SAXException {
		String url = Files.fileToURLName(file);
		InputSource inSource = new InputSource(url);
		medlineParser.setHandler(dbLoader);
		medlineParser.parse(inSource);
	}

	/**
	 * This inner class implements <code>MedlineHandler</code>
	 *
	 * The method handle get pubmed element from <code>MedlineCitation</code> object
	 * for each article.
	 * It populates id, title, abstract, keywords and mesh headings
	 * from <code>MedlineCitation</code>
	 *
	 */
	private class MedlineDbLoader implements MedlineHandler{

		private int nbCitation;
		private PubMedAccessTool tool;

		private String pmid;
		private String title;
		private StringBuffer abstrtext;
		private OtherAbstract[] otherAbstracts;
		private Abstract abstr;
		private PubMedElement PMElt;

		MedlineDbLoader(PubMedAccessTool tool) {
        	super();
        	nbCitation = 0;
        	this.tool = tool;
        }

        // Done according to the DTD available at http://www.nlm.nih.gov/databases/dtd/
		public void handle(MedlineCitation citation) {
	        // Extracting ID
			pmid = citation.pmid();
			try {
	            // Extracting title
            	title = citation.article().articleTitleText();

            	// Extracting abstract or other abstract
            	abstrtext = new StringBuffer();
	            abstr = citation.article().abstrct();
	            if (abstr != null)
	            	abstrtext.append(abstr.text());

	            // Appending other abstract
	            otherAbstracts = citation.otherAbstracts();
	            if (otherAbstracts != null && otherAbstracts.length>=0){
	            	for (int i = 0; i<otherAbstracts.length; i++){
	            		abstrtext.append(BLANK_SPACE);
	            		abstrtext.append(otherAbstracts[i].text());
	            	}
	            }

	            // Extracting keywords
	            StringBuffer keywordstext = new StringBuffer();
	            KeywordList[] keywordlist = citation.keywordLists();
	            if (keywordlist != null && keywordlist.length>=0){
	            	for (int i = 0; i<keywordlist.length; i++){
	            		for (Iterator<Topic> it = keywordlist[i].iterator(); it.hasNext();){
	            			keywordstext.append(it.next().topic());
	            			if(it.hasNext()){
	            				keywordstext.append(COMMA_SEPARATOR);
	            			}
	            		}
	            	}
	            }


	            // Extacting mesh headings and mapping to concept id's
	            MeshHeading[] meshheadingtab = citation.meshHeadings();
	            String concepts = EMPTY_STRING;
	            if(meshheadingtab!= null && meshheadingtab.length>0){
	            	concepts=mapMeshHeadingsToLocalConceptIDs(meshheadingtab);
	            	  // if mapping concepts are null or empty then log message for it.
					if(concepts== null || concepts.trim().length()== 0){
						logger.error("Cannot map Mesh Headings to local concept ids for element with ID " + pmid +".");
					}
	            }

	           // Creating PUBMED element
	           PMElt = new PubMedElement(this.tool, pmid, title, abstrtext.toString(), keywordstext.toString(), concepts);

	            if(this.tool.resourceUpdateService.addElement(PMElt.getElement())){
	            	  this.nbCitation++;
	            }
            }
	        catch (Exception e) {
	            logger.error("** PROBLEM ** Cannot parse MedlineCitation with PMID: " + pmid + ".",  e);
	        }
	    }

		public void delete(String arg0) {}

		public int getNbCitation() {
			return nbCitation;
		}

	}

	/**
	 * Element for pubmed article.
	 *
	 */
	private class PubMedElement{

		PubMedAccessTool eltPMTool;
		String eltPMID;
		HashMap<String,String> eltInfo;
		static final String ELT_TITLE = "title";
		static final String ELT_ABSTRACT = "abstract";
		static final String ELT_KEYWORDS = "keywords";
		static final String ELT_MESHHEADINGS = "meshheadings";

		/**
		 * Default Constructor
		 */
		PubMedElement(PubMedAccessTool eltPMTool, String eltPMID, String titletext, String abstractext, String keywords, String meshheadings){
			this.eltPMTool = eltPMTool;
			this.eltPMID = eltPMID;
			this.eltInfo = new HashMap<String, String>(4);
			this.eltInfo.put(ELT_TITLE, titletext);
			this.eltInfo.put(ELT_ABSTRACT, abstractext);
			this.eltInfo.put(ELT_KEYWORDS, keywords);
			this.eltInfo.put(ELT_MESHHEADINGS, meshheadings);
		}

		/**
		 * Get Elements
		 *
		 * @return Element
		 */
		Element getElement(){
			Element element = null;
			 String[] itemKeys = this.eltPMTool.getToolResource().getResourceStructure().getItemKeys();
			Structure eltStructure = new Structure(itemKeys, PM_RESOURCEID);
			for(String itemKey: itemKeys){
				eltStructure.putContext(Structure.generateContextName(PM_RESOURCEID, itemKey), this.eltInfo.get(itemKey));
			}
			try {
				element = new Element(this.eltPMID, eltStructure);
			} catch (BadElementStructureException e) {
				 logger.error("** PROBLEM ** Cannot create Element for PubMedElement with PMID: " + this.eltPMID + ". Null have been returned.", e);
			}
		return element;
		}
	}

	/**
	 * The method map each mesh heading to concept id for 'MSH' onltology id.
	 * Concept id's are separated by separator string '> '.
	 *
	 * @param meshheadingtab
	 * @return
	 */
	public String mapMeshHeadingsToLocalConceptIDs(MeshHeading[] meshheadingtab){
		HashSet<String> meshHeadings= new HashSet<String>();
		for (MeshHeading meshHeading : meshheadingtab) {
				meshHeadings.add(meshHeading.descriptor().topic().trim());
		}
		return mapMeshHeadingsToLocalConceptIDs(meshHeadings).toString();
	}


	/**
	 * @param HashSet<String> meshHeadings
	 * @return StringBuilder
	 */
	private StringBuilder mapMeshHeadingsToLocalConceptIDs(HashSet<String> meshHeadings) {

       StringBuilder conceptString=new StringBuilder();
       conceptString.append(resourceUpdateService.mapTermsToVirtualLocalConceptIDs(meshHeadings, PM_ONTOIDS[3]));

		return conceptString;
	}
}
