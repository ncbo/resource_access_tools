package org.ncbo.resource_access_tools.resource.pharmgkb.drug;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.ncbo.resource_access_tools.util.helper.StringHelper;


/**
 * This class enables to get the list of drug (list of access id) with supporting information from pharmgkb
 * by lunching the web service client specialSearch.pl 5
 * IN: COMMAND --static for now
 * OUT: list of Drug accession id in an HashSet
 *
 * @author Adrien Coulet
 * @version OBR_v0.2
 * @created 10-Dec-2008
 *
 */

public class GetPgkbDrugList implements StringHelper{

	// Logger for this class
	private static Logger logger = Logger.getLogger(GetPgkbDrugList.class);

	private static String PERL_SCRIPT_PATH =new File(ClassLoader.getSystemResource("org/ncbo/stanford/obr/resource/pharmgkb/specialSearch.pl" ).getFile()).getAbsolutePath();
	//attributes
	private static String COMMAND = "perl "+ PERL_SCRIPT_PATH +" 5";

	HashSet<String> drugList = new HashSet<String>();	//<accession Id of drug in PharmGKB are localElementId>

	//constructor
	public GetPgkbDrugList(){

	}

	// method
	public HashSet<String> getList() {
		// TODO Auto-generated method stub
		Runtime runtime = Runtime.getRuntime();
		Process process = null;
		try {
			process = runtime.exec(COMMAND);
			//InputStream results = process.getInputStream();

			BufferedReader resultReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String resultLine = EMPTY_STRING;
			try {
				Pattern drugPattern = Pattern.compile("^(PA\\d*), Drug, (.*), ([0-1]{1}), ([0-1]{1}), ([0-1]{1}), .*$");
				while((resultLine = resultReader.readLine()) != null) {
					// process the line
					//System.out.println(resultLine);
					Matcher drugMatcher = drugPattern.matcher(resultLine);
					if (drugMatcher.matches()){
						//System.out.println("found!");
						// we find a drug, let's create it
						/*
						boolean pheno = false;
						boolean geno  = false;
						boolean rela  = false;
						if (drugMatcher.group(3).equals("1")){
							pheno= true;
						}
						if (drugMatcher.group(4).equals("1")){
							geno = true;
						}
						if (drugMatcher.group(5).equals("1")){
							rela = true;
						}
						*/
						drugList.add(drugMatcher.group(1));
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
		} catch(IOException ioe) {
			logger.error(EMPTY_STRING, ioe);
		}
		return drugList;
	}
}
