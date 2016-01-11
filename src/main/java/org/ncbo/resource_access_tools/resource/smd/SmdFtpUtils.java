package org.ncbo.resource_access_tools.resource.smd;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.log4j.Logger;
import org.ncbo.resource_access_tools.util.helper.StringHelper;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * SmdFtpUtils is utility class for SMD FTP site
 * ftp://smd-ftp.stanford.edu/pub/smd/publications/ Which is used to navigate
 * FTP site and get all experiments ids and descriptions for all the experiment
 * sets available in SMD publications.
 *
 * @author kyadav
 * @version $$
 */
class SmdFtpUtils implements StringHelper {

    // Logger for this class
    private static final Logger logger = Logger.getLogger(SmdFtpUtils.class);

    // String constant for forward slash
    private static final String FORWARD_SLASH = "/";

    // String constant for '='
    private static final String EQUAL_STRING = "=";

    // Publication directory on FTP site.
    private static final String PUB_FTP_DIR = "pub/smd/publications/";

    // FTP site host name.
    private static final String FTP_HOSTNAME = "smd-ftp.stanford.edu";

    // Default user for FTP site.
    private static final String FTP_USERNAME = "anonymous";

    // Default password for FTP site
    private static final String FTP_PASSWORD = EMPTY_STRING;

    // Meta data file name prefix.
    private static final String META_FILE_PREFIX = "exptset_";

    // Meta data file extension.
    private static final String META_FILE_EXT = ".meta";

    // Experiment ID tag in meta data file.
    private static final String EXPT_ID_META_TAG = "!Exptid=";

    // Experiment set description tag in meta data file.
    private static final String EXPTSET_DESC_META_TAG = "!Description=";

    // Default Constructor
    public SmdFtpUtils() {

    }

    /**
     * This method navigate FTP site
     * ftp://smd-ftp.stanford.edu/pub/smd/publications/. as follows :
     * <li> Get All publication id's
     * <li> For each publication id get list of experiment sets present in it.
     * <li> For each experiment set retrieves experiment id's <BR>
     * and experiment set description to create <code>ExperimrntSet</code>.
     *
     * @return HashMap containing all ExperimentSet object with key as
     * experimentSet id.
     */
    public HashMap<String, ExperimentSet> getAllExperimentSets() {
        HashMap<String, ExperimentSet> experimentSetsMap = new HashMap<String, ExperimentSet>();
        try {
            // Connect to FTP site.
            FTPClient ftpClient = connectSMDFTPSite();

            // Change Working directory to pub/smd/publications
            ftpClient.changeWorkingDirectory(PUB_FTP_DIR);

            // Get list of publication id
            List<String> publicationIDs = getChildrenDir(ftpClient, PUB_FTP_DIR);

            // Create publication map to put experiment set id's
            HashMap<String, List<String>> publicationMap = new HashMap<String, List<String>>();
            List<String> expSetList = new ArrayList<String>();
            logger.info("Total number of publications : "
                    + publicationIDs.size());

            // Processing each publication id to get experiment sets present in
            // that.
            for (String publicationID : publicationIDs) {
                // Change directory to publicationID
                ftpClient.changeWorkingDirectory(publicationID);
                logger.info("Getting experiment set for publication id "
                        + publicationID);
                try {
                    Thread.sleep(1000);
                    // Get all experiment sets present in current directory
                    expSetList = getChildrenDir(ftpClient, PUB_FTP_DIR
                            + publicationID);
                    publicationMap.put(publicationID, expSetList);

                    // Go back to parent directory
                    ftpClient.changeToParentDirectory();
                } catch (Exception e) {
                    logger.error(
                            "Problem in getting experiment sets for publication id "
                                    + publicationID, e);
                }
            }
            // Disconnect FTP connection.
            ftpClient.disconnect();
            logger.info("Getting ExperimentSet Maps From publication map...");
            experimentSetsMap = getExperimentSets(publicationMap);

        } catch (Exception e) {
            logger.error("Problem in getting all experiment sets", e);
        }

        return experimentSetsMap;
    }

    /**
     * This method get all experiment ids and description for experiment sets
     * and bind it in <code>ExperimentSet</code> object. It parse metadata
     * file for given experiment set FTP url of the form
     * ftp://smd-ftp.stanford.edu/pub/smd/publications/{publication_id}/{expset_no}/exptset_{
     * expset_no }.meta
     *
     * @param publicationMap containing <code>List</code> of experiment sets for
     *                       publication id
     * @return <code>Map</code> containing <code>ExperimentSet</code> for
     * key experiment set id
     */
    private HashMap<String, ExperimentSet> getExperimentSets(
            HashMap<String, List<String>> publicationMap) {
        // Connect to FTP site.
        FTPClient ftpClient = connectSMDFTPSite();

        HashMap<String, ExperimentSet> experimentSetsMaps = new HashMap<String, ExperimentSet>();
        try {
            // Change FTP directory to pub/smd/publications/
            ftpClient.changeWorkingDirectory(PUB_FTP_DIR);

            // Iterate publicationMap keys
            for (String publicationID : publicationMap.keySet()) {
                logger.info("Getting experiment sets maps for publication id "
                        + publicationID);
                List<String> expSetNumbers = publicationMap.get(publicationID);
                // Iterate through each experimentSet Number
                for (String expSetNumber : expSetNumbers) {
                    // Get ExprementSet for given expSetNumber
                    ExperimentSet experimentSet = getExperimentSet(ftpClient,
                            publicationID + FORWARD_SLASH + expSetNumber
                                    + FORWARD_SLASH + META_FILE_PREFIX
                                    + expSetNumber + META_FILE_EXT,
                            expSetNumber);
                    if (experimentSet != null) {
                        experimentSetsMaps.put(expSetNumber, experimentSet);
                    }
                }
            }
            // Disconnect FTP connection.
            ftpClient.disconnect();
        } catch (IOException e) {
            logger.error(
                    "Problem in getting experiment sets from publication map.",
                    e);
        }

        return experimentSetsMaps;
    }

    /**
     * This method parse experiment set meta data file to extract experiment id
     * and description and put it into <code>ExperimentSet</code> object.
     *
     * @param ftpClient      <code>FTPClient</code> to get meta data file
     * @param expSetMetaFile <code>String</code> containing experiment set meta data file
     * @param expSetNumber   <code>String</code> containing experiment set number.
     * @return <code>ExperimentSet</code>
     * @throws IOException
     */
    private ExperimentSet getExperimentSet(FTPClient ftpClient,
                                           String expSetMetaFile, String expSetNumber) {
        ExperimentSet experimentSet = null;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HashSet<String> experimentIDs = new HashSet<String>();
        String description = null;

        // Retrieve meta data file in output stream
        ftpClient.retrieveFile(expSetMetaFile, outputStream);
        // Create reader for output stream of meta data file.
        BufferedReader resultReader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(outputStream.toByteArray())));

        String resultLine;
        try {
            // Reading each line of experiment set meta file
            while ((resultLine = resultReader.readLine()) != null) {

                if (resultLine.contains(EXPT_ID_META_TAG)) {
                    // Extracting experiment IDs
                    if (resultLine.split(EQUAL_STRING).length == 2) {
                        experimentIDs.add(resultLine.split(EQUAL_STRING)[1]
                                .trim());
                    }
                } else if (resultLine.contains(EXPTSET_DESC_META_TAG)) {
                    // Extracting description
                    if (resultLine.split(EQUAL_STRING).length == 2) {
                        description = resultLine.split(EQUAL_STRING)[1].trim();
                    }
                }
            }
            if (experimentIDs.size() > 0) {
                experimentSet = new ExperimentSet(description, experimentIDs);
            } else {
                logger.info("Experement Set number " + expSetNumber
                        + " contains zero experiment.");
            }
        } catch (IOException e) {
            logger
                    .error("Problem in getting ExperimentSet for experiment set number "
                            + expSetNumber);
        }

        return experimentSet;
    }

    /**
     * This this method connect to FTP site ftp://smd-ftp.stanford.edu and log
     * in with user name and password.
     *
     * @return FTPClient
     */
    private FTPClient connectSMDFTPSite() {
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(FTP_HOSTNAME);
            ftpClient.login(FTP_USERNAME, FTP_PASSWORD);
            ftpClient.enterLocalPassiveMode();
        } catch (Exception e) {
            try {
                Thread.sleep(1000);
                ftpClient.connect(FTP_HOSTNAME);
                ftpClient.login(FTP_USERNAME, FTP_PASSWORD);
                ftpClient.enterLocalPassiveMode();
            } catch (Exception exception) {
                logger.error("Problem in connecting FTP site .", exception);
            }
        }
        return ftpClient;
    }

    /**
     * This method get list of children directories present in current
     * directory.
     *
     * @param ftpClient
     * @param currentDir <code>String</code> containing current directory.
     * @return <code>List</code> of child directories name
     */
    private List<String> getChildrenDir(FTPClient ftpClient,
                                        String currentDir) {
        List<String> childDirs = new ArrayList<String>();

        // Check whether FTP client is connected
        if (ftpClient.isConnected()) {
            FTPFile[] files = null;
            try {
                files = ftpClient.listFiles();
            } catch (IOException e) {
                // Catch exception and try to reconnect
                try {
                    ftpClient = connectSMDFTPSite();
                    ftpClient.changeWorkingDirectory(currentDir);
                    files = ftpClient.listFiles();
                } catch (IOException exception) {
                    logger
                            .error("Problem in getting child directories for parent directory "
                                    + currentDir);
                }
            }
            // Adding name of directories to list
            for (FTPFile file : files) {
                if (file.isDirectory()) {
                    childDirs.add(file.getName());
                }

            }
        }
        return childDirs;
    }

}
