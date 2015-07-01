package org.ncbo.resource_access_tools.resource.pharmgkb.gene;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.ncbo.stanford.obr.util.helper.StringHelper;


/**
 * This class enables to get the list of drug (list of access id) with pharmacokinetic or pharmacodynamic significance from pharmgkb
 * by lunching the web service client specialSearch.pl 0 an 1
 * IN: COMMAND --static for now
 * OUT: list of gene accession id in an HashSet
 *
 * @author Adrien Coulet
 * @version OBR_v0.2
 * @created 10-Dec-2008
 *
 */

public class GetPgkbGeneList implements StringHelper{

	// Logger for this class
	private static Logger logger = Logger.getLogger(GetPgkbGeneList.class);

	//attributes
	private static String PERL_SCRIPT_PATH =new File(ClassLoader.getSystemResource("org/ncbo/stanford/obr/resource/pharmgkb/specialSearch.pl" ).getFile()).getAbsolutePath();
	private static String COMMAND_0 = "perl "+ PERL_SCRIPT_PATH +" 0";
	private static String COMMAND_1 = "perl "+ PERL_SCRIPT_PATH +" 1";
	HashSet<String> geneList = new HashSet<String>();	//<accession Id of gene in PharmGKB are localElementId>

	//constructor
	public GetPgkbGeneList(){

	}

	// method
	public HashSet<String> getList() {
		// TODO Auto-generated method stub
		Runtime runtime = Runtime.getRuntime();
		Process process = null;
		// GET GENE WITH pharmacokinetic significance
		try {
			process = runtime.exec(COMMAND_0);
			//InputStream results = process.getInputStream();

			BufferedReader resultReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String resultLine = EMPTY_STRING;
			try {
				Pattern genePattern = Pattern.compile("^(PA\\d*), Gene, (.*), ([0-1]{1}), ([0-1]{1}), ([0-1]{1}), .*$");
				while((resultLine = resultReader.readLine()) != null) {
					// process the line
					//System.out.println(resultLine);
					Matcher geneMatcher = genePattern.matcher(resultLine);
					if (geneMatcher.matches()){
						//System.out.println("found!");
						// we find a gene, let's create it
						/*
						boolean pheno = false;
						boolean geno  = false;
						boolean rela  = false;
						if (geneMatcher.group(3).equals("1")){
							pheno= true;
						}
						if (geneMatcher.group(4).equals("1")){
							geno = true;
						}
						if (geneMatcher.group(5).equals("1")){
							rela = true;
						}
						*/
						if(!geneList.contains(geneMatcher.group(1))){
								geneList.add(geneMatcher.group(1));
						}
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
		// GET GENE WITH pharmacodynamic significance
		try {
			process = runtime.exec(COMMAND_1);
			//InputStream results = process.getInputStream();

			BufferedReader resultReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String resultLine = EMPTY_STRING;
			try {
				Pattern genePattern = Pattern.compile("^(PA\\d*), Gene, (.*), ([0-1]{1}), ([0-1]{1}), ([0-1]{1}), .*$");
				while((resultLine = resultReader.readLine()) != null) {
					// process the line
					//System.out.println(resultLine);
					Matcher geneMatcher = genePattern.matcher(resultLine);
					if (geneMatcher.matches()){
						//System.out.println("found!");
						// we find a gene, let's create it
						/*
						boolean pheno = false;
						boolean geno  = false;
						boolean rela  = false;
						if (geneMatcher.group(3).equals("1")){
							pheno= true;
						}
						if (geneMatcher.group(4).equals("1")){
							geno = true;
						}
						if (geneMatcher.group(5).equals("1")){
							rela = true;
						}
						*/
						if(!geneList.contains(geneMatcher.group(1))){
							geneList.add(geneMatcher.group(1));
						}
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
		return geneList;
	}
}
