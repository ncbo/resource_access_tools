package org.ncbo.resource_access_tools.resource.micad;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.StringTokenizer;

import obs.obr.populate.Resource;
import obs.obr.populate.Structure;

import org.ncbo.resource_access_tools.resource.ResourceAccessTool;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;
import org.ncbo.stanford.obr.util.helper.StringHelper;


public class MicadReadCsv {

// Main method for testing MicadReadCsv independently
/*	public static void main(String[] args){
		String[][] text = getLocalElementIds();
		System.out.println(text[3][18]);
		System.out.println(text.length);
	}
*/

	// Logger for this class
	//private Logger logger = LoggerUtils.getCurrentLogger(MicadReadCsv.class);

	//attributes
	Resource  resource       = null;
	Structure basicStructure = null;
	String    resourceID     = StringHelper.EMPTY_STRING;
	ResourceAccessTool tool  = null;

	//constructor
	public MicadReadCsv(Resource myResource, ResourceAccessTool tool){
		this.resource       = myResource;
		this.basicStructure = myResource.getResourceStructure();
		this.resourceID     = myResource.getResourceId();
		this.tool           = tool;
		//this.logger = LoggerUtils.getCurrentLogger(MicadReadCsv.class);
	}

	// methods
	public String[][] getLocalElementIds() {

		String [][] text = new String [2000][50];
		URL url = null;
		URLConnection urlConn = null;
		InputStreamReader inStream = null;

		try
		{

		url = new URL("http://www.ncbi.nlm.nih.gov/bookshelf/picrender.fcgi?book=micad&part=contrib&blobname=micad.csv");
		urlConn = url.openConnection();
		inStream = new InputStreamReader(urlConn.getInputStream());
		BufferedReader bufRdr  = new BufferedReader(inStream);

		String line = null;
		int row = 0;
		int col = 0;

		//read each line of text file
		while((line = bufRdr.readLine()) != null)
		{
			// change ",""," to ","" ""," and to tab""tab
			line = line.replace("\"\"", "\"\" \"\"");
			line = line.replace("\",\"", "\t");
			StringTokenizer st = new StringTokenizer(line,"\t\"");
			while (st.hasMoreTokens())
			{
				//get next token and store it in the array
				text[row][col] = st.nextToken();
				col++;
			}
			col = 0;
			row++;
		}

		bufRdr.close();
		}
		catch(IOException e)
	    {
	        System.out.println("Error -- " + e.toString());
	    }
		return text;
	}


	//To be updated
	public String[] getElement(String localElementID, String[][] csvElements) {

		String [] participants = new String [8];
		try {
			participants = getParticipants(localElementID,csvElements);
		}catch(Exception e){
			//logger.error("Error in getParticipants of getElement in MicadReadCsv (csvElement)", e);
		}
		return participants;
	}

	 /**
     * Get an HashSet of the name of all participating molecules for a given pathway.
     * @param id
     * @return
     * @throws Exception
     */
    public String[] getParticipants(String id, String[][] csvElements) throws Exception {

    	String [] participant = new String [8];
    	// Initialize participants with empty strings
    	for(int j=0;j<=7;j++)
			participant[j]=" ";
    	String [] temp;

    	// To fill up the table from csv data
    	for(int i=1;i<=csvElements.length-1;i++)
		{
			//Split the element in 18'th column in csv
			if (csvElements[i][18]!=null)
			{
				temp=csvElements[i][18].split("micad&part=");
				if (temp.length==2)
					if (id.equals(temp[1]))
					{
						for(int j=0;j<=7;j++)
							participant[j]=csvElements[i][j];
						break;
					}
			}
		}
        return participant;
    }
}
