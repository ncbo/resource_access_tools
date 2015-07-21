package org.ncbo.resource_access_tools.util.mgrep;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;
import org.ncbo.resource_access_tools.util.FileResourceParameters;

public class ConceptRecognitionTools {
	
	// Logger for this class
	private static Logger logger = Logger.getLogger(ConceptRecognitionTools.class);
	/* cf old version
	// Mgrep remote version
	public static File mgrepRemote(File dictionaryFile, File resourceFile) throws Exception {
		UmichRemoteCall remoteCall = new UmichRemoteCall(dictionaryFile, resourceFile);
		remoteCall.umichAnnotationFunctionRemoteCall();
		File umichOutputFile = new File("files/resources/" + resourceFile.getName() + ".umich");	
		return umichOutputFile;
	}
	*/

	// Mgrep local version 
	public static File mgrepLocal(File dictionaryFile, File resourceFile) throws Exception {
		StringBuffer mgrepCmdb = new StringBuffer();
		File mgrepFile = new File(FileResourceParameters.mgrepOutputFolder() + resourceFile.getName() + ".mgrep");	
		
		/* For the new Mgrep	*/	
		mgrepCmdb.append(FileResourceParameters.mgrepFolder());
		mgrepCmdb.append("mgrep -w -i -f ");
		mgrepCmdb.append(FileResourceParameters.dictionaryFolder());
		mgrepCmdb.append(dictionaryFile.getName());
		mgrepCmdb.append(" < ");
		mgrepCmdb.append(FileResourceParameters.mgrepInputFolder());
		mgrepCmdb.append(resourceFile.getName());
		mgrepCmdb.append(" > ");
		mgrepCmdb.append(FileResourceParameters.mgrepOutputFolder());
		mgrepCmdb.append(resourceFile.getName());
		mgrepCmdb.append(".mgrep");
		
		/* For the old mgrep */
		/*
		commandb.append(FileResourceParameters.MGREP_FOLDER);
		commandb.append("mgrep -w -i ");
		commandb.append(FileResourceParameters.REMOTE_DICTIONARY_FOLDER);
		commandb.append(dictionaryFile.getName());
		commandb.append(" ");
		commandb.append(FileResourceParameters.REMOTE_RESOURCE_FOLDER);
		commandb.append(resourceFile.getName());
		commandb.append(" > ");
		commandb.append(FileResourceParameters.MGREP_RESULT_FOLDER);
		commandb.append(resourceFile.getName());
		commandb.append(".mgrep");
		*/
		
		// Bug with java when using the ">" redirection character, so we need to do like that:
		String[] mgrepCommand = {"/bin/sh", "-c", mgrepCmdb.toString()};
				
		//Unix command execution
		logger.info("Local Mgrep execution...");
		logger.info("command: " + mgrepCmdb.toString());

		Process p = Runtime.getRuntime().exec(mgrepCommand);

		int i = p.waitFor();
		if (i != 0){
			logger.info("Problem during the Mgrep execution...");
			BufferedReader stdErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			// read the output from the command
			String s;
			while ((s = stdErr.readLine()) != null) {
				logger.error(s);
			}
		}
		
		return mgrepFile;
	}

}
