package org.ncbo.resource_access_tools.util.helper;


/**
 * @author Kuladip Yadav
 */
public interface StringHelper {

    // Separator '> ' used to separator localConceptIDs for reported annotation
    String GT_SEPARATOR_STRING = "> ";
    // String constant for empty string
    String EMPTY_STRING = "";
    // String constant for blank space ' '
    String BLANK_SPACE = " ";
    // String constant for comma ','
    String COMMA_STRING = ",";
    // String constant for semicolon ';'
    String SEMICOLON_STRING = ";";
    // String constant for comma separator with blank space ', '
    String COMMA_SEPARATOR = COMMA_STRING + BLANK_SPACE;
    // Regular expression  for  non digit character
    String NON_DIGIT_REGEX = "\\D";
    // String constant for tab
    String TAB_STRING = "\t";
    // String constant for hash #
    String HASH_STRING = "#";
    // String constant for empty string
    String SLASH_STRING = "/";
    // String constant for + string
    String PLUS_STRING = "+";
    // Regex for plus string
    String PLUS_STRING_REG = "\\" + PLUS_STRING;
    // Regex for plus string
    String NEW_LINE_REGEX = "\n";
    // String constant for underscore
    String UNDERSCORE_STRING = "_";
    // String constant for 'Accession'
    String ACCESSION_STRING = "Accession";
    // String constant for 'Not Applicable'
    String NOT_APPLICABLE = "NA";
    // String constant for end tag
    String endTag = "\">";
    // String Regular expression constant for extra white spaces.
    String whitespace_regx = "\\s+";

}
