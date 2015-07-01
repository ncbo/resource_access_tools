package org.ncbo.resource_access_tools.resource.testresource;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;

import obs.obr.populate.Structure;

import org.ncbo.stanford.obr.enumeration.ResourceType;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;
import org.ncbo.stanford.obr.util.FileResourceParameters;

public class TestResourceAccessTool extends ResourceAccessTool {

	private static final String TR_FILE = "OBR_RESOURCE_Test_Resource.txt";

	private static final String TR_URL = "http://www.ncbi.nlm.nih.gov/geo/";
	private static final String TR_NAME = "Test Resource";
	private static final String TR_RESOURCEID = "TR";
	private static final String TR_DESCRIPTION = "A description.";
	private static final String TR_LOGO = "http://www.ncbi.nlm.nih.gov/projects/geo/img/geo_main.gif";
	private static final String TR_ELT_URL = "http://www.ncbi.nlm.nih.gov/projects/geo/gds/gds_browse.cgi?gds=";

	private static final String[] TR_ITEMKEYS	= {"title",	"description", "reported"};
	private static final Double[] TR_WEIGHTS  	= {1.0,	0.7, 0.5};
	private static final String[] TR_ONTOIDS  = {Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, "NCI"};

	private static Structure TR_STRUCTURE = new Structure(TR_ITEMKEYS, TR_RESOURCEID, TR_WEIGHTS, TR_ONTOIDS);
	private static String TR_MAIN_ITEMKEY = "title";

	public TestResourceAccessTool(){
		super(TR_NAME, TR_RESOURCEID, TR_STRUCTURE);
		try {
			this.getToolResource().setResourceURL(new URL(TR_URL));
			this.getToolResource().setResourceLogo(new URL(TR_LOGO));
			this.getToolResource().setResourceElementURL(TR_ELT_URL);
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		this.getToolResource().setResourceDescription(TR_DESCRIPTION);
	}

	@Override
	public ResourceType  getResourceType() {
		return ResourceType.SMALL;
	}

	@Override
	public void updateResourceInformation() {
	}

	@Override
	public HashSet<String> queryOnlineResource(String query) {
		return new HashSet<String>();
	}

	@Override
	public int updateResourceContent(){
		int nbElement = 0;
		File resourceFile = new File(FileResourceParameters.resourceFolder() + TR_FILE);
		try {
			resourceFile.createNewFile();
			nbElement = resourceUpdateService.updateResourceContentFromFile(resourceFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return nbElement;
	}

	@Override
	public String elementURLString(String elementLocalID) {
		return TR_ELT_URL + elementLocalID;
	}

	@Override
	public String mainContextDescriptor() {
		return TR_MAIN_ITEMKEY;
	}

}
