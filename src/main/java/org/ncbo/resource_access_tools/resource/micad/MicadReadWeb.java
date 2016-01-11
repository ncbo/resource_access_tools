package org.ncbo.resource_access_tools.resource.micad;

import org.ncbo.resource_access_tools.populate.Resource;
import org.apache.log4j.Logger;
import org.ncbo.resource_access_tools.populate.Structure;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;
import org.ncbo.resource_access_tools.util.helper.StringHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

class MicadReadWeb implements StringHelper {

    private static final Logger logger = Logger.getLogger(MicadReadWeb.class);
    // Constant used to specify max number of reconnect
    private static final int MAX_RECONNECT = 3;

//Main method for testing MicadReadWeb independently
/*	public static void main(String[] args){

//		HashSet<String> elementIDList = getLocalElementIds();
		HashSet<String> elementIDList = new HashSet<String>();

		elementIDList.add("MLS128-111In");

		// get data associated with each of these elements
		Iterator<String> i = elementIDList.iterator();
		String [] data = new String [7];
		while(i.hasNext())
		{
			String elementID = i.next();
			// get data of this element
			try
			{
				data = getSections(elementID);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Error in MicadReadWeb");
			}
		}
		System.out.println(data[6]);
	}
*/

    // Logger for this class
    //private Logger logger = LoggerUtils.getCurrentLogger(MicadReadCsv.class);

    //attributes
    private Resource resource = null;
    private Structure basicStructure = null;
    private String resourceID = EMPTY_STRING;
    private ResourceAccessTool tool = null;

    //constructor
    public MicadReadWeb(Resource myResource, ResourceAccessTool tool) {
        this.resource = myResource;
        this.basicStructure = myResource.getResourceStructure();
        this.resourceID = myResource.getResourceId();
        this.tool = tool;
        //this.logger = LoggerUtils.getCurrentLogger(MicadReadCsv.class);
    }

    // methods

    /**
     * Get Various Sections from the micad website
     */
    public String[] getSections(String id) {

        String[] parts = new String[6];
        String htmlText = null;
        String[] temp = new String[2];

        try {

            BufferedReader bufRdr = getResponseForURL("http://www.ncbi.nlm.nih.gov/bookshelf/br.fcgi?book=micad&part=" + id, 0);
            if (bufRdr == null) {
                logger.error(">> Anable to get sections for residing in the MICAD website for ID : " + id);
                return parts;
            }
            String line = null;

            //read each line of html file and add all of it to htmlText
            while ((line = bufRdr.readLine()) != null)
                htmlText = htmlText + line;

            //Divide into sections
            int bg, syn, inv, anim, hum, ref;

            temp = htmlText.split("<h2 class=\"sec-title\">Background</h2>");
            if (temp.length == 2) {
                bg = 1;
                htmlText = temp[1];
            } else {
                bg = 0;
                parts[0] = null; //No Background
                htmlText = temp[0];
            }
            temp = htmlText.split("<h2 class=\"sec-title\">Synthesis</h2>");
            if (temp.length == 2) {
                syn = 1;
                htmlText = temp[1];
                if (bg == 1)
                    parts[0] = temp[0]; //Background
            } else {
                syn = 0;
                parts[1] = null; //No Synthesis
                htmlText = temp[0];
            }
            temp = htmlText.split("<h2 class=\"sec-title\"><i>In Vitro</i> Studies: Testing in Cells and Tissues</h2>");
            if (temp.length == 2) {
                inv = 1;
                htmlText = temp[1];
                if (syn == 1)
                    parts[1] = temp[0]; //Synthesis
                else if (bg == 1)
                    parts[0] = temp[0]; //Background
            } else {
                inv = 0;
                parts[2] = null; //No In Vitro Studies
                htmlText = temp[0];
            }
            temp = htmlText.split("<h2 class=\"sec-title\">Animal Studies</h2>");
            if (temp.length == 2) {
                anim = 1;
                htmlText = temp[1];
                if (inv == 1)
                    parts[2] = temp[0]; //In Vitro Studies
                else if (syn == 1)
                    parts[1] = temp[0]; //Synthesis
                else if (bg == 1)
                    parts[0] = temp[0]; //Background
            } else {
                anim = 0;
                parts[3] = null; //No Animal Studies
                htmlText = temp[0];
            }
            temp = htmlText.split("<h2 class=\"sec-title\">Human Studies</h2>");
            if (temp.length == 2) {
                hum = 1;
                htmlText = temp[1];
                if (anim == 1)
                    parts[3] = temp[0]; //Animal Studies
                else if (inv == 1)
                    parts[2] = temp[0]; //In Vitro Studies
                else if (syn == 1)
                    parts[1] = temp[0]; //Synthesis
                else if (bg == 1)
                    parts[0] = temp[0]; //Background
            } else {
                hum = 0;
                parts[4] = null; //No Human Studies
                htmlText = temp[0];
            }
            temp = htmlText.split("<h2 class=\"sec-title\">References</h2>");
            if (temp.length != 2)
                temp = htmlText.split(">References<");
            if (temp.length >= 2) {
                ref = 1;
                htmlText = temp[1];
                if (hum == 1)
                    parts[4] = temp[0]; //Human Studies
                else if (anim == 1)
                    parts[3] = temp[0]; //Animal Studies
                else if (inv == 1)
                    parts[2] = temp[0]; //In Vitro Studies
                else if (syn == 1)
                    parts[1] = temp[0]; //Synthesis
                else if (bg == 1)
                    parts[0] = temp[0]; //Background
            } else {
                ref = 0;
                parts[5] = null; //No References
                htmlText = temp[0];
            }
            temp = htmlText.split("<!-- class=back-matter-section -->");
            if (temp.length == 2) {
                if (ref == 1)
                    parts[5] = temp[0]; //References
                else if (hum == 1)
                    parts[4] = temp[0]; //Human Studies
                else if (anim == 1)
                    parts[3] = temp[0]; //Animal Studies
                else if (inv == 1)
                    parts[2] = temp[0]; //In Vitro Studies
                else if (syn == 1)
                    parts[1] = temp[0]; //Synthesis
                else if (bg == 1)
                    parts[0] = temp[0]; //Background
            } else {
                //No References because we don't know the ending
            }

            //Clean HTML tags
            for (int i = 0; i <= 5; i++) {
                if (parts[i] != null) {
                    parts[i] = parts[i].replaceAll("\\<.*?>", EMPTY_STRING);
                    parts[i] = parts[i].replace("[PubMed]", EMPTY_STRING);
                    parts[i] = parts[i].replace("/div>", EMPTY_STRING);
                }
            }

            //close the file
            bufRdr.close();
        } catch (IOException e) {
            System.out.println("Error -- " + e.toString());
        }
        return parts;
    }

    private BufferedReader getResponseForURL(String urlString, int reconnectNumber) {
        BufferedReader bufRdr = null;
        URL url;

        try {
            Thread.sleep(reconnectNumber * 5000);
            url = new URL(urlString);
            URLConnection connection = url.openConnection();
            InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream());
            bufRdr = new BufferedReader(inputStreamReader);

        } catch (Exception e) {
            if (reconnectNumber < MAX_RECONNECT) {
                reconnectNumber++;
                logger.info("Trying to get content for the given URL " + urlString + " again after " + (reconnectNumber * 5000) + "ms.");
                return getResponseForURL(urlString, reconnectNumber);
            } else {
                logger.error("** PROBLEM ** Cannot get content for the given URL " + urlString + ". Null has been returned.");
            }
        }

        return bufRdr;

    }

}
