package org.ncbo.resource_access_tools.util;

import org.apache.log4j.Logger;
import org.ncbo.resource_access_tools.util.helper.StringHelper;

import java.io.File;

public class FileResourceParameters implements StringHelper {

    // Logger for this class
    private static final Logger logger = Logger.getLogger(FileResourceParameters.class);

    // OBR schema host name
    private static final String OBR_SCHEMA_HOST = MessageUtils.getMessage("obr.schema.host.name");

    // OBR schema name
    private static final String OBR_SCHEMA = MessageUtils.getMessage("obr.schema.name");

    // OBR version
    private static final String OBR_VERSION = MessageUtils.getMessage("obr.version");

    // SVN code path
    private static final String SVN_CODE_PATH = MessageUtils.getMessage("obr.svn.code.path");

    // Local & remote paths
    private static final String LOCAL_FOLDER = MessageUtils.getMessage("obr.local.path");
    private static final String NCBODATA_OBR_FOLDER = MessageUtils.getMessage("obr.ncbodata.path");

    // Folder names
    public static final String RESULT_FOLDER = MessageUtils.getMessage("obr.result.dir");
    private static final String BLACK_LIST_FOLDER = MessageUtils.getMessage("obr.blacklists.dir");


    private static final String DICTIONARY_FOLDER = MessageUtils.getMessage("obr.dictionary.dir");
    private static final String RESOURCE_FOLDER = MessageUtils.getMessage("obr.resource.dir");
    private static final String MGREP_FOLDER = MessageUtils.getMessage("obr.mgrep.dir");
    private static final String MGREP_INPUT_FOLDER = MessageUtils.getMessage("obr.mgrep.input.dir");
    private static final String MGREP_OUTPUT_FOLDER = MessageUtils.getMessage("obr.mgrep.output.dir");
    private static final String RESOURCE_LOG_FOLDER = MessageUtils.getMessage("obr.logs.dir");


    public static String dictionaryFolder() {
        return selectRightFolder(DICTIONARY_FOLDER + SVN_CODE_PATH + SLASH_STRING + OBR_SCHEMA_HOST + SLASH_STRING);
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

    public static String resourceLogFolder() {
        return selectRightFolder(RESOURCE_LOG_FOLDER + SVN_CODE_PATH + SLASH_STRING + OBR_SCHEMA_HOST + SLASH_STRING + OBR_SCHEMA + SLASH_STRING);
    }

    public static String blackListFolder() {
        return selectRightFolder(BLACK_LIST_FOLDER);
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
            File ncboDataObrFolder = new File(NCBODATA_OBR_FOLDER);
            if (ncboDataObrFolder.exists()) {
                folder = NCBODATA_OBR_FOLDER + OBR_VERSION + SLASH_STRING + folderName;
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
