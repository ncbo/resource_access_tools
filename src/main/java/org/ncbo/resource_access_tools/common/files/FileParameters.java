package org.ncbo.resource_access_tools.common.files;

import org.apache.log4j.Logger;

import java.io.File;
import java.net.InetAddress;

public class FileParameters {

    // Logger for this class
    private static final Logger logger = Logger.getLogger(FileParameters.class);

    // Remote host name
    private static final String REMOTE_HOST_DEV2 = "ncbolabs-dev2";

    // Local & remote paths
    private static final String LOCAL_FOLDER = "files/";
    private static final String NCBODATA_FOLDER = "/ncbodata/OBR/";

    // Folder names
    public static final String RESULT_FOLDER = "results/";
    public static final String BLACK_LIST_FOLDER = "blacklists/";
    private static final String ONTOLOGY_FOLDER = "ontologies/";

    private static final String DICTIONARY_FOLDER = "dictionaries/";
    private static final String RESOURCE_FOLDER = "resources/";
    private static final String MGREP_FOLDER = "mgrep/mgrep3.0/";
    private static final String MGREP_INPUT_FOLDER = "mgrep/mgrepInputs/";
    private static final String MGREP_OUTPUT_FOLDER = "mgrep/mgrepOutputs/";
    private static final String OBR_LOG_FOLDER = "logs/obr/";
    private static final String OBS_LOG_FOLDER = "logs/obs/";

    // Functions to return the right folder
    public static String ontologyFolder() {
        return selectRightFolder(ONTOLOGY_FOLDER);
    }

    public static String dictionaryFolder() {
        return selectRightFolder(DICTIONARY_FOLDER);
    }

    public static String resourceFolder() {
        return selectRightFolder(RESOURCE_FOLDER);
    }

    public static String mgrepFolder() {
        return selectRightFolder(MGREP_FOLDER);
    }

    public static String mgrepInputFolder() {
        return selectRightFolder(MGREP_INPUT_FOLDER);
    }

    public static String mgrepOutputFolder() {
        return selectRightFolder(MGREP_OUTPUT_FOLDER);
    }

    public static String obrLogFolder() {
        return selectRightFolder(OBR_LOG_FOLDER);
    }

    public static String obsLogFolder() {
        return selectRightFolder(OBS_LOG_FOLDER);
    }

    // Stopwords must be in lower case (the OpenBiomedicalAnnotator.annotate() function handles case)
    public static String[] STOP_WORDS = {"i", "a", "above", "after", "against", "all", "alone",
            "always", "am", "amount",
            "an", "and", "any", "are", "around", "as", "at", "back", "be", "before", "behind",
            "below", "between", "bill", "both", "bottom", "by", "call", "can", "co", "con",
            "de", "detail", "do", "done", "down", "due", "during", "each", "eg", "eight",
            "eleven", "empty", "ever", "every", "few", "fill", "find", "fire", "first",
            "five", "for", "former", "four", "from", "front", "full", "further", "get",
            "give", "go", "had", "has", "hasnt", "he", "her", "hers", "him", "his", "ie",
            "if", "in", "into", "is", "it", "last", "less", "ltd", "many", "may", "me",
            "mill", "mine", "more", "most", "mostly", "must", "my", "name", "next", "nine",
            "no", "none", "nor", "not", "nothing", "now", "of", "off", "often", "on", "once",
            "one", "only", "or", "other", "others", "out", "over", "part", "per", "put", "re",
            "same", "see", "serious", "several", "she", "show", "side", "since", "six", "so",
            "some", "sometimes", "still", "take", "ten", "then", "third", "this", "thick",
            "thin", "three", "through", "to", "together", "top", "toward", "towards", "twelve",
            "two", "un", "under", "until", "up", "upon", "us", "very", "via", "was", "we",
            "well", "when", "while", "who", "whole", "will", "with", "within", "without", "you",
            "yourself", "yourselves"};


    private static String selectRightFolder(String folderName) {
        String folder = folderName;
        try {
            InetAddress addr = InetAddress.getLocalHost();
            String hostname = addr.getHostName();
            if (hostname.equals(REMOTE_HOST_DEV2)) {
                folder = NCBODATA_FOLDER + folderName;
            } else {
                folder = LOCAL_FOLDER + folderName;
            }
        } catch (Exception e) {
            logger.error("** PROBLEM ** Cannot get access to remoteFolder: " + folderName, e);
        }
        File folderFile = new File(folder);
        if (!folderFile.exists()) {
            folderFile.mkdirs();
        }
        return folder;
    }

}
