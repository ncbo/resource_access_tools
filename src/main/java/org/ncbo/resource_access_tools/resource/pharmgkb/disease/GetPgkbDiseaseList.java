package org.ncbo.resource_access_tools.resource.pharmgkb.disease;

import org.apache.log4j.Logger;
import org.ncbo.resource_access_tools.util.helper.StringHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This class enables to get the list of disease (list of access id) with supporting information from pharmgkb
 * by lunching the web service client specialSearch.pl 6
 * IN: COMMAND --static for now
 * OUT: list of disease accession id in an HashSet
 *
 * @author Adrien Coulet
 * @version OBR_v0.2
 * @created 11-Nov-2008
 */

class GetPgkbDiseaseList implements StringHelper {

    // Logger for this class
    private static final Logger logger = Logger.getLogger(GetPgkbDiseaseList.class);
    //attributes
    private static final String PERL_SCRIPT_PATH = new File(ClassLoader.getSystemResource("org/ncbo/stanford/obr/resource/pharmgkb/specialSearch.pl").getFile()).getAbsolutePath();
    private static final String COMMAND = "perl " + PERL_SCRIPT_PATH + " 6";
    private final HashSet<String> diseaseList = new HashSet<String>();    //<accession Id of disease in PharmGKB are localElementId>

    //constructor
    public GetPgkbDiseaseList() {
    }

    // method
    public HashSet<String> getList() {
        Runtime runtime = Runtime.getRuntime();
        Process process;
        try {
            process = runtime.exec(COMMAND);
            //InputStream results = process.getInputStream();

            BufferedReader resultReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String resultLine;
            try {
                Pattern diseasePattern = Pattern.compile("^(PA\\d*), Disease, (.*), ([0-1]{1}), ([0-1]{1}), ([0-1]{1}), .*$");
                while ((resultLine = resultReader.readLine()) != null) {
                    // process the line
                    //System.out.println(resultLine);
                    Matcher diseaseMatcher = diseasePattern.matcher(resultLine);
                    if (diseaseMatcher.matches()) {
                        //System.out.println("found!");
                        // we find a disease, let's create it
                        /*
						boolean pheno = false;
						boolean geno  = false;
						boolean rela  = false;
						if (diseaseMatcher.group(3).equals("1")){
							pheno= true;
						}
						if (diseaseMatcher.group(4).equals("1")){
							geno = true;
						}
						if (diseaseMatcher.group(5).equals("1")){
							rela = true;
						}
						*/
                        diseaseList.add(diseaseMatcher.group(1));
                    }
                }
            } finally {
                resultReader.close();
            }
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            // String line2 = EMPTY_STRING;
            try {
                //while((line2 = reader2.readLine()) != null) {
                // process
                //System.out.println(line2);
                //}
            } finally {
                errorReader.close();
            }
        } catch (IOException ioe) {
            logger.error(EMPTY_STRING, ioe);
        }
        return diseaseList;
    }
}
