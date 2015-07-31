package org.ncbo.resource_access_tools.resource.ncbi.pubmed;

import gov.nih.nlm.ncbi.www.soap.eutils.EUtilsServiceLocator;
import gov.nih.nlm.ncbi.www.soap.eutils.EUtilsServiceSoap;
import gov.nih.nlm.ncbi.www.soap.eutils.esearch.ESearchRequest;
import gov.nih.nlm.ncbi.www.soap.eutils.esearch.ESearchResult;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.rpc.ServiceException;

import org.ncbo.resource_access_tools.populate.Element.BadElementStructureException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * This class is responsible for getting all the data elements for
 * Pub Med.
 *
 * This tool fetch data using xml files and E-Utilities and insert it into the pm_element_all table.
 *
 * @author  Sachin
 * @version $$
 */
public class AllPubMed {

    protected EUtilsServiceLocator toolService;
    protected EUtilsServiceSoap toolEutils;
    protected static final String EUTILS_EMAIL = "jonquet@stanford.edu";
    protected static final String EUTILS_TOOL = "ontrez";
    protected static final int EUTILS_MAX = 500;
    protected static final String UID_COLUMN = "uid";
    // Database for E-Utils
    private static final String PM_EUTILS_DB = "pubmed";
    // Query terms for E-Utils
    private static final String PM_EUTILS_TERM = "all[filter]";
    static Logger mylogger = Logger.getLogger("MyLog");
    static Connection con;
    static PreparedStatement stat;
    static Statement st;
    static ResultSet rs;

    //local databse settings.
    static String database = "jdbc:mysql://localhost:3306/resource_index";
    static String loginName = "root";
    static String password = "root";

    //workflow databse settings.
//  static String database = "jdbc:mysql://ncboprod-ridb1.sunet:3306/resource_index";
//  static String loginName = "optra";
//  static String password = "optra";
    /**
     * Constructor
     *
     */
    public AllPubMed() {
        toolService = new EUtilsServiceLocator();
        try {
            toolEutils = toolService.geteUtilsServiceSoap();
        } catch (ServiceException e) {
            mylogger.info("** PROBLEM ** Cannot create the EUtils Web service");
        }
    }

    /**
     * Get eutils database String
     *
     * return String
     */
    protected String getEutilsDB() {
        return PM_EUTILS_DB;
    }

    /**
     *  Get query terms for eutils.
     *
     *  return String
     */
    protected String getEutilsTerm() {
        return PM_EUTILS_TERM;
    }

    public static void main(String[] args) {
        try {
            AllPubMed apm = new AllPubMed();
            apm.updateResourceContent();
            mylogger.info("Completed Successfully!!!");
        } catch (Exception e) {
            mylogger.info("Error occured while fetching data from Pubmed site.");
            e.printStackTrace();
        }

    }

    /**
     * Method uses E-utilities to get elements for pubmed.
     * It updates elements for last <code>numberOfDays</code>
     *
     * return int
     */
    public int updateResourceContent() {

        int nbElements = 0;
        nbElements += eutilsUpdateAll(null);

        return nbElements;
    }

    protected int eutilsUpdateAll(String columnToUse) {
        int nbElement = 0;

        // gets all the UIDs with the right query on EUtils
        HashSet<String> allUIDs = this.allUIDs();

        try {
            //created log file.
            FileHandler fh = new FileHandler("./pubmedAll.log");
            mylogger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
        } catch (IOException e2) {
            mylogger.info("Problem creating eUtils service");
        }

        mylogger.info("******** All ID size :: " + allUIDs.size());

        // gets the element already in the corresponding _ET and keeps only the difference
        HashSet<String> allUIDsinET = getAlllocalUids();

        mylogger.info("******** Local ID size :: " + allUIDsinET.size());

        allUIDs.removeAll(allUIDsinET);

        mylogger.info("******** final size :: " + allUIDs.size());

        try {
            nbElement = this.updateElementTableWithUIDs(allUIDs);
        } catch (Exception e) {
            mylogger.info("** PROBLEM ** Cannot update  because of a Structure problem.");
        }
        return nbElement;
    }

    /**
     * Get all elements from our local database.
     * @return all unique pubmed ID's set.
     */
    protected HashSet<String> getAlllocalUids() {
        HashSet<String> allUIDsinET = new HashSet<String>();
        try {
            con = getConnection(database, loginName, password);
            String query = "select local_element_id from pm_element_all";
            st = con.createStatement();
            ResultSet r = st.executeQuery(query);

            while (r.next()) {
                allUIDsinET.add(r.getString(1));
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return allUIDsinET;
    }

    /**
     * Fetch all PubMed Identifiers from pubmed site.
     * @return
     */
    private HashSet<String> allUIDs() {
        System.out.println("******** Getting all pubmed Ids");
        HashSet<String> allUIDs = new HashSet<String>();
        ESearchRequest esearchRequest = new ESearchRequest();
        esearchRequest.setEmail(EUTILS_EMAIL);
        esearchRequest.setTool(EUTILS_TOOL);
        esearchRequest.setDb(this.getEutilsDB());
        esearchRequest.setTerm(this.getEutilsTerm());
        String[] resultTab = null;


        ESearchResult esearchResult;
        try {

            esearchResult = this.toolEutils.run_eSearch(esearchRequest);
            int resultsCount = Integer.parseInt(esearchResult.getCount());
            mylogger.info("Nb of results: " + resultsCount + " for term: " + this.getEutilsTerm());

            resultTab = esearchResult.getIdList();

            Integer max;

            boolean dup = true;
            for (Integer i = 0; i < resultsCount; i += EUTILS_MAX) {
                max = EUTILS_MAX;
                if (resultsCount - i < EUTILS_MAX) {
                    max = resultsCount - i;
                }
                esearchRequest.setRetMax(max.toString());
                esearchRequest.setRetStart(i.toString());
                esearchResult = this.toolEutils.run_eSearch(esearchRequest);
                //logger.info("ESearch call...");
                resultTab = esearchResult.getIdList();
                //  System.out.println(i.toString() + " - " + max.toString() +" id size :" + resultTab.length);

                if (resultTab != null) {
                    for (int j = 0; j < resultTab.length; j++) {
                        if (allUIDs.add(resultTab[j])) {
                        } else {
                            if (dup) {
                                dup = false;
                                mylogger.info("******************Duplicate pubmedID");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            mylogger.info("** PROBLEM ** Cannot get all UIDs for using ESearch.");
        }
        return allUIDs;
    }

    //This method is used to fetch the data using xml files
    protected int updateElementTableWithUIDs(HashSet<String> UIDs)
            throws BadElementStructureException {
        int nbElement = 0;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        String url = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id=";
        String endurl = "&rettype=xml&retmode=text";

        StringBuffer ids = new StringBuffer();
        Iterator it = UIDs.iterator();
        HashSet<PubMedElement> elementSet = new HashSet<PubMedElement>();

        try {
            int cnt = 1;

            while (it.hasNext()) {
                ids.append(it.next().toString());

                //fetching 500 elements per each request.
                if (cnt % 500 == 0) {
                    String furl = url + ids.toString() + endurl;


                    //Using factory get an instance of document builder
                    DocumentBuilder db = dbf.newDocumentBuilder();

                    //parse using builder to get DOM representation of the XML file
                    Document dom = db.parse(furl);

                    NodeList medNodeList = dom.getElementsByTagName("MedlineCitation");

                    for (int i = 0; i < medNodeList.getLength(); i++) {
                        NodeList childsMed = medNodeList.item(i).getChildNodes();
                        String pmID = "";
                        String tittle = "";
                        StringBuffer abstrtext = new StringBuffer();
                        StringBuffer kwords = new StringBuffer();
                        StringBuffer mshtext = new StringBuffer();

                        for (int j = 0; j < childsMed.getLength(); j++) {

                            if (childsMed.item(j).getNodeName().equalsIgnoreCase("PMID")) {
                                pmID = childsMed.item(j).getTextContent();
                            }

                            if (childsMed.item(j).getNodeName().equalsIgnoreCase("Article")) {
                                NodeList articalNodeList = childsMed.item(j).getChildNodes();


                                for (int a = 0; a < articalNodeList.getLength(); a++) {
                                    if (articalNodeList.item(a).getNodeName().equalsIgnoreCase("ArticleTitle")) {
                                        tittle = articalNodeList.item(a).getTextContent();
                                    }

                                    if (articalNodeList.item(a).getNodeName().equalsIgnoreCase("Abstract")) {
                                        NodeList abstractNodeList = articalNodeList.item(a).getChildNodes();
                                        for (int ab = 0; ab < abstractNodeList.getLength(); ab++) {
                                            if (abstractNodeList.item(ab).getNodeName().equalsIgnoreCase("AbstractText")) {
                                                abstrtext.append(abstractNodeList.item(ab).getTextContent());
                                                abstrtext.append(" ");
                                            }
                                        }
                                    }

                                    if (articalNodeList.item(a).getNodeName().equalsIgnoreCase("KeywordList")) {
                                        NodeList keywordNodeList = articalNodeList.item(a).getChildNodes();
                                        for (int k = 0; k < keywordNodeList.getLength(); k++) {
                                            if (keywordNodeList.item(k).getNodeName().equalsIgnoreCase("Keyword")) {
                                                kwords.append(keywordNodeList.item(k).getTextContent());
                                                kwords.append(",");
                                            }
                                        }
                                    }


                                }
                            }

                            if (childsMed.item(j).getNodeName().equalsIgnoreCase("KeywordList")) {
                                NodeList keywordNodeList = childsMed.item(j).getChildNodes();
                                for (int k = 0; k < keywordNodeList.getLength(); k++) {
                                    if (keywordNodeList.item(k).getNodeName().equalsIgnoreCase("Keyword")) {
                                        kwords.append(keywordNodeList.item(k).getTextContent());
                                        kwords.append(",");
                                    }
                                }
                            }

                            if (childsMed.item(j).getNodeName().equalsIgnoreCase("MeshHeadingList")) {
                                NodeList mshNodeList = childsMed.item(j).getChildNodes();
                                for (int k = 0; k < mshNodeList.getLength(); k++) {
                                    if (mshNodeList.item(k).getNodeName().equalsIgnoreCase("MeshHeading")) {
                                        NodeList mshDescNodeList = mshNodeList.item(k).getChildNodes();
                                        for (int x = 0; x < mshDescNodeList.getLength(); x++) {
                                            if (mshDescNodeList.item(x).getNodeName().equalsIgnoreCase("DescriptorName")) {
                                                mshtext.append(mshDescNodeList.item(x).getTextContent());
                                                mshtext.append(",");
                                            }
                                        }

                                    }
                                }
                            }
                        }

                        String kw = kwords.toString();
                        if (kw.endsWith(",")) {
                            kw = kw.substring(0, kw.length() - 1);
                        }

                        String mh = mshtext.toString();
                        if (mh.endsWith(",")) {
                            mh = mh.substring(0, mh.length() - 1);
                        }

                        elementSet.add(new PubMedElement(pmID, tittle, abstrtext.toString(), kw, mh));
                    }

                    addRecord(elementSet);
                    elementSet.clear();
                    elementSet = null;
                    elementSet = new HashSet<PubMedElement>();

                    ids = null;
                    ids = new StringBuffer();
                }
                if (ids.length() > 1) {
                    ids.append(",");
                }
                cnt++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return nbElement;
    }

    /*
     * Inner class of creating pubmed elements.
     */
    class PubMedElement {

        String eltPMID = "";
        String title = "";
        String abst = "";
        String keywords = "";
        String meshheadings = "";

        /**
         * Default Constructor
         */
        PubMedElement(String eltPMID, String titletext, String abstractext, String keywords, String meshheadings) {
            this.eltPMID = eltPMID;
            this.title = titletext;
            this.abst = abstractext;
            this.keywords = keywords;
            this.meshheadings = meshheadings;
        }

        public String getAbst() {
            return abst;
        }

        public void setAbst(String abst) {
            this.abst = abst;
        }

        public String getEltPMID() {
            return eltPMID;
        }

        public void setEltPMID(String eltPMID) {
            this.eltPMID = eltPMID;
        }

        public String getKeywords() {
            return keywords;
        }

        public void setKeywords(String keywords) {
            this.keywords = keywords;
        }

        public String getMeshheadings() {
            return meshheadings;
        }

        public void setMeshheadings(String meshheadings) {
            this.meshheadings = meshheadings;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    public void addRecord(HashSet<PubMedElement> eleList) {

        try {

            boolean records = false;
            con = getConnection(database, loginName, password);

            String query = "INSERT INTO resource_index.pm_element_all (local_element_id, pm_title, pm_abstract, pm_keywords, pm_meshheadings) values (?,?,?,?,?)";

            Iterator e = eleList.iterator();

            while (e.hasNext()) {
                records = true;
                PubMedElement pme = (PubMedElement) e.next();

                stat = con.prepareStatement(query);
                stat.setString(1, pme.getEltPMID());
                stat.setString(2, pme.getTitle());
                stat.setString(3, pme.getAbst());
                stat.setString(4, pme.getKeywords());
                stat.setString(5, pme.getMeshheadings());
                stat.executeUpdate();
            }
            con.close();

            if (records) {
                mylogger.info(eleList.size() + " more record Added ****");
            }
        } catch (Exception x) {
            mylogger.info("Error while inserting record!");
            x.printStackTrace();
        }

    }

    //not used
    public void addrecordsBatch(HashSet<PubMedElement> eleList) {

        try {
            boolean records = false;
            con = getConnection(database, loginName, password);
            StringBuffer sb = new StringBuffer();
            sb.append("INSERT INTO resource_index.pm_element_all (local_element_id, pm_title, pm_abstract, pm_keywords, pm_meshheadings) values ");

            Iterator e = eleList.iterator();

            while (e.hasNext()) {
                records = true;
                PubMedElement pme = (PubMedElement) e.next();

                sb.append("(");
                sb.append("\"");
                sb.append(pme.getEltPMID());
                sb.append("\"");
                sb.append(",");
                sb.append("\"");
                sb.append(pme.getTitle());
                sb.append("\"");
                sb.append(",");
                sb.append("\"");
                sb.append(pme.getAbst());
                sb.append("\"");
                sb.append(",");
                sb.append("\"");
                sb.append(pme.getKeywords());
                sb.append("\"");
                sb.append(",");
                sb.append("\"");
                sb.append(pme.getMeshheadings());
                sb.append("\"");
                sb.append("),");

            }

            if (records) {
                String str = sb.toString();
                if (str.endsWith(",")) {
                    str = str.substring(0, str.length() - 1);
                }
                Statement st = con.createStatement();

                st.executeUpdate(str);

                mylogger.info("**** 500 elements added successfully..");
            } else {
                mylogger.info("**** NO elements *********");
            }

            con.close();
        } catch (Exception x) {
            mylogger.info("Error while inserting record!");
            x.printStackTrace();
        }

    }

    /*
     * returns database connection
     */
    public static Connection getConnection(String url, String loginName, String Password) {

        try {
            Class.forName("com.mysql.jdbc.Driver");
            con = DriverManager.getConnection(url, loginName, Password);
        } catch (Exception x) {
            mylogger.info("Unable to load the driver class!");
            x.printStackTrace();
        }

        return con;
    }




}
