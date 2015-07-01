package org.ncbo.resource_access_tools.resource.goldminer;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;

import obs.common.files.FileParameters;
import obs.obr.populate.Structure;

import org.ncbo.stanford.obr.enumeration.ResourceType;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;

public class GoldminerAccessTool extends ResourceAccessTool {

	private static final String GM_FILE 		= "OBR_RESOURCE_Golminer_X1Sample.txt";
	private static final String GM_URL 			= "http://goldminer.arrs.org/";
	private static final String GM_NAME 		= "ARRS GoldMiner";
	private static final String GM_RESOURCEID 	= "GM";
	private static final String GM_DESCRIPTION 	= "ARRS GoldMiner provides instant access to images published in selected peer-reviewed radiology journals. This new, web-based system allows viewers to search for images by findings, anatomy, imaging technique, and patient age and sex.";
	private static final String GM_LOGO 		= "http://goldminer.arrs.org/image/goldminer.gif";
	private static final String GM_ELT_URL 		= "http://goldminer.arrs.org/link.php?id=";

	private static final String[] GM_ITEMKEYS 	= {"title", "caption"};
	private static final Double[] GM_WEIGHTS 	= {1.0, 0.8};

	private static Structure GM_STRUCTURE = new Structure(GM_ITEMKEYS, GM_RESOURCEID, GM_WEIGHTS);
	private static String GM_MAIN_ITEMKEY = "title";

	public GoldminerAccessTool(){
		super(GM_NAME, GM_RESOURCEID, GM_STRUCTURE);
		try {
			this.getToolResource().setResourceURL(new URL(GM_URL));
			this.getToolResource().setResourceLogo(new URL(GM_LOGO));
			this.getToolResource().setResourceElementURL(GM_ELT_URL);
		} catch (MalformedURLException e) {
			logger.error(EMPTY_STRING, e);
		}
		this.getToolResource().setResourceDescription(GM_DESCRIPTION);
	}

	@Override
	public void updateResourceInformation() {
		// TODO See if it can be implemented for this resource.
	}

	@Override
	public int updateResourceContent() {
		int nbElement = 0;
		File resourceFile = new File(FileParameters.resourceFolder() + GM_FILE);
		try {
			resourceFile.createNewFile();
			nbElement = resourceUpdateService.updateResourceContentFromFile(resourceFile);
		} catch (Exception e) {
			logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName() + " with file " + GM_FILE, e);
		}
		return nbElement;
	}

	@Override
	public ResourceType  getResourceType() {
		return ResourceType.SMALL;
	}

	@Override
	public String elementURLString(String elementLocalID) {
		return GM_ELT_URL + elementLocalID;
	}

	@Override
	public String mainContextDescriptor() {
		return GM_MAIN_ITEMKEY;
	}

	@Override
	public HashSet<String> queryOnlineResource(String query) {
		// Not done because it is not a public inferface
		//http://goldminer.arrs.org/search.php?query=cell+melanoma for the request
		return new HashSet<String>();
	}

}
