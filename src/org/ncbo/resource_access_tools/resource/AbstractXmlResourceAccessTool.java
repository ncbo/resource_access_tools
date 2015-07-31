package org.ncbo.resource_access_tools.resource;

import java.io.File;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.ncbo.resource_access_tools.populate.Structure;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public abstract class AbstractXmlResourceAccessTool extends ResourceAccessTool {

    private static DocumentBuilderFactory toolDBF;
    private static DocumentBuilder toolBuilder;
    //private static javax.xml.xpath.XPath toolXpath;
    // Constant used to specify max number of reconnect
    private static final int MAX_RECONNECT = 5;

    protected AbstractXmlResourceAccessTool(String resourceName, String resourceID, Structure resourceStructure) {
        super(resourceName, resourceID, resourceStructure);
        try {
            toolDBF = DocumentBuilderFactory.newInstance();
            toolBuilder = toolDBF.newDocumentBuilder();
            //toolXpath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
        } catch (ParserConfigurationException e) {
            logger.error("** PROBLEM ** Cannot create the XML parser for resource " + this.getToolResource().getResourceName(), e);
        }
    }

    protected static DocumentBuilderFactory getToolDBF() {
        return toolDBF;
    }

    protected static DocumentBuilder getToolBuilder() {
        return toolBuilder;
    }

    protected static Document parseXML(String URL) {
        Document dom = null;
        try {
            dom = toolBuilder.parse(URL);

        } catch (Exception e) {
            logger.error("** PROBLEM ** Cannot parse the given URL " + URL + ". Null has been returned.", e);
        }
        return dom;
    }

    /**
     * Parse given URL to get <code>Document</code> object.
     *
     * @param URL  - <code>String</code> having request URL.
     * @return Document
     */
    protected static Document parseXMLWithReconnect(String URL) {
        return parseXMLWithReconnect(URL, 0);
    }

    /**
     * Parse given URL to get <code>Document</code> object.
     * If it fails first time then again fire request.
     * If request fails, it try to reconnect upto <code>MAX_RECONNECT</code>
     * time with incremental delay between requests.
     *
     * @param URL   <code>String</code> containing request URL.
     * @param reconnectNumber  int
     * @return  Document
     */
    protected static Document parseXMLWithReconnect(String URL, int reconnectNumber) {
        Document dom = null;
        try {
            Thread.sleep(reconnectNumber * 1000);
            dom = toolBuilder.parse(URL);
        } catch (Exception e) {
            if (reconnectNumber < MAX_RECONNECT) {
                reconnectNumber++;
                logger.info("Trying to parse the given URL " + URL + " again after " + (reconnectNumber * 1000) + "ms.");
                return parseXMLWithReconnect(URL, reconnectNumber);
            } else {
                logger.error("** PROBLEM ** Cannot parse the given URL " + URL + ". Null has been returned.", e);
            }
        }

        return dom;
    }

    protected static Document parseXML(File file) {
        Document dom = null;
        try {
            dom = toolBuilder.parse(file);
        } catch (Exception e) {
            logger.error("** PROBLEM ** Cannot parse the given file " + file + ". Null has been returned.", e);
        }
        return dom;
    }

    protected static Document buildDom(String content) {
        Document dom = null;
        try {
            InputSource source = new InputSource();
            source.setCharacterStream(new StringReader(content));
            dom = toolBuilder.parse(source);
        } catch (Exception e) {
            logger.error("** PROBLEM ** Cannot parse the given content.", e);
        }
        return dom;
    }
    /*
    public static javax.xml.xpath.XPath getToolXpath() {
    return toolXpath;
    }

    protected static Node evaluateAsNode(String expression, Node node){
    Node returnNode = null;
    try {
    toolXpath.reset();
    returnNode = (Node)toolXpath.evaluate(expression, node, javax.xml.xpath.XPathConstants.NODE);
    } catch (javax.xml.xpath.XPathExpressionException e) {
    logger.error("** PROBLEM ** Cannot evaluate Xpath expression " + expression + " for node " + node.getNodeName() + ". Null has been returned.", e);
    }
    return returnNode;
    }

    protected static NodeList evaluateAsNodeList(String expression, Node node){
    NodeList returnNodeList = null;
    try {
    toolXpath.reset();
    returnNodeList = (NodeList)toolXpath.evaluate(expression, node, javax.xml.xpath.XPathConstants.NODESET);
    } catch (javax.xml.xpath.XPathExpressionException e) {
    logger.error("** PROBLEM ** Cannot evaluate Xpath expression " + expression + " for node " + node.getNodeName() + ". Null has been returned.", e);
    }
    return returnNodeList;
    }

    protected static String evaluateAsString(String expression, Node node){
    String s = EMPTY_STRING;
    try {
    toolXpath.reset();
    s = (String)toolXpath.evaluate(expression, node, javax.xml.xpath.XPathConstants.STRING);
    } catch (javax.xml.xpath.XPathExpressionException e) {
    logger.error("** PROBLEM ** Cannot evaluate Xpath expression " + expression + " for node " + node.getNodeName() + ". Empty string has been returned.", e);
    }
    return s;
    }
     */
}
