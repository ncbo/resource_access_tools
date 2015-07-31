/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ncbo.resource_access_tools.resource.nif;





import javax.ws.rs.core.MediaType;

import org.ncbo.resource_access_tools.populate.Structure;
import org.ncbo.resource_access_tools.resource.AbstractXmlResourceAccessTool;
import org.w3c.dom.Document;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

/**
 * Abstract class for all NIF resources.
 * @author s.kharat
 */
public abstract class AbstractNifResourceAccessTool extends AbstractXmlResourceAccessTool {

    private static final String SERVER = "http://nif-services.neuinfo.org/servicesv1/v1/federation/data/";
    private final WebResource resource = getClient().resource(SERVER);
    private static final int MAX_RECONNECT = 5;
    protected static final int rowCount = 100;  //100 records for each request.
    protected static final String query = "*";  //mins all records.
    // String constant for all NIF resources.
    protected static final String nodeName = "name";
    protected static final String nodeValue = "value";
    protected static final String resultCount = "resultCount";

    protected AbstractNifResourceAccessTool(String resourceName, String resourceID, Structure resourceStructure) {
        super(resourceName, resourceID, resourceStructure);
    }

    /***
     * Query the data federation
     * @param db The database name
     * @param indexable The indexable name
     * @param query The query string
     * @param offset The offset to start into the results
     * @param count The number of results to return
     * @return Document
     */
    protected Document queryFederation(String db, String indexable, String query, int offset, int count) {
        return queryFederation(db, indexable, query, offset, count, 0);
    }

    /**
     *
     * @param db
     * @param indexable
     * @param query
     * @param offset
     * @param count
     * @param reconnectNum
     * @return
     */
    private Document queryFederation(String db, String indexable, String query, int offset, int count, int reconnectNum) {
        Document dom = null;
        try {
            String response = resource.path(db).path(indexable).
                    queryParam("q", query).
                    queryParam("offset", Integer.toString(offset)).
                    queryParam("count", Integer.toString(count)).
                    accept(MediaType.APPLICATION_XML_TYPE).get(String.class);

            //added delay for 2 Seconds between two NIF service calls.
            Thread.sleep(2000);
            dom = buildDom(response);
        } catch (Exception e) {
            // Retrying to access NIF resource response for 5 more times.
            if (reconnectNum < MAX_RECONNECT) {
                reconnectNum++;
                logger.info("\n\tRetrying for " + reconnectNum + " time");
                return queryFederation(db, indexable, query, offset, count, reconnectNum);
            } else {
                logger.error("** After Retrying still their is problem in getting federation data for Offset: " + offset, e);
            }
        }
        return dom;
    }

    /***
     * Query the data federation
     * @param db The database name
     * @param indexable The indexable name
     * @param query The query string
     * @param offset The offset to start into the results
     * @param count The number of results to return
     * @return Document
     */
    protected Document queryFederation(String nifId, String query, int offset, int count) {
        return queryFederation(nifId, query, offset, count, 0);
    }

    /**
     *
     * @param db
     * @param indexable
     * @param query
     * @param offset
     * @param count
     * @param reconnectNum
     * @return
     */
    private Document queryFederation(String nifId, String query, int offset, int count, int reconnectNum) {
        Document dom = null;
        try {
            String response = resource.path(nifId).
                    queryParam("q", query).
                    queryParam("offset", Integer.toString(offset)).
                    queryParam("count", Integer.toString(count)).
                    accept(MediaType.APPLICATION_XML_TYPE).get(String.class);

            //added delay for 2 Seconds between two NIF service calls.
            Thread.sleep(2000);
            dom = buildDom(response);
        } catch (Exception e) {
            // Retrying to access NIF resource response for 5 more times.
            if (reconnectNum < MAX_RECONNECT) {
                reconnectNum++;
                logger.info("\n\tRetrying for " + reconnectNum + " time");
                return queryFederation(nifId, query, offset, count, reconnectNum);
            } else {
                logger.error("** After Retrying still their is problem in getting federation data for Offset: " + offset, e);
            }
        }
        return dom;
    }

    @Provides
    @Singleton
    Client getClient() {
        return Client.create();
    }
}
