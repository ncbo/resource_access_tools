package org.ncbo.resource_access_tools.populate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.thoughtworks.xstream.XStream;

/**
 * This class represents the structure of an element for a given resource. 
 * A Structure has a contexts HashMap<String,String> in which the values of the map 
 * contain the element information. These contexts are constructed from an array of itemKeys.
 * For example, a structure may contain at a title and a description, that can respectively be accessed in the 
 * contexts HashMap with the key "title" and "description".
 * When a key need to be more precise we can create a new key with the "::" and the precision.
 * Ex:[[title, ... ][definition::part1, ... ][definition::part2, ...]]
 * 
 * A Structure may also contain weights & ontoIds.
 * 
 * @author Clement Jonquet, Adrien Coulet
 * @version OBR v_0.2
 * @created 02-Oct-2007 11:47:46 AM, 19-Nov-2008 
 */
public class Structure {
	
	// Logger for this class
	private static Logger logger = Logger.getLogger(Structure.class);
	
	private static final Double DEFAULT_WEIGHT = 1.0;
	public static final String FOR_CONCEPT_RECOGNITION = "null";
	public static final String NOT_FOR_ANNOTATION = "-1";

	private String resourceId;
	
	private HashMap<String,String> contexts;
	private String[] itemKeys;
	
	private HashMap<String,Double> weights;
	
	private HashMap<String,String> ontoIds;
	
	private static XStream xStream = new XStream();
	
	/**
	 * Constructs a new Structure with context names constructed with the given itemKeys and 
	 * resourceId. The values of these contexts in the HashMap are initialized as null.
	 * Weights are instantiated with the default value.
	 * OntoIds are instantiated with the FOR_CONCEPT_RECOGNITION value.
	 */
	public Structure(String[] itemKeys, String resourceId){
		this.resourceId = resourceId;
		this.contexts = new HashMap<String,String>(itemKeys.length);
		this.weights = new HashMap<String,Double>(itemKeys.length);
		this.ontoIds  = new HashMap<String,String>(itemKeys.length);
		this.itemKeys = itemKeys;
		
		// traverses the itemKeys to construct the structure
		for (int i=0; i<itemKeys.length; i++){
			String contextName = generateContextName(resourceId, itemKeys[i]);
			this.contexts.put(contextName, null);
			this.weights.put(contextName, DEFAULT_WEIGHT);
			this.ontoIds.put(contextName, FOR_CONCEPT_RECOGNITION);
		}
	}
	
	/**
	 * Constructs a new Structure with context names constructed with the given itemKeys and 
	 * resourceId. The values of these contexts in the HasMap are initialized as null.
	 * Weights are instantiated with the values in the given array.
	 * OntoIds are instantiated with the default value.
	 * @throws BadStructureException 
	 */
	public Structure(String[] itemKeys, String resourceId, Double[] itemWeights){
		this(itemKeys, resourceId);
		if (itemKeys.length != itemWeights.length){
			try {
				throw new BadStructureException("itemKeys and itemWeights do not have the same size.");
			} catch (BadStructureException e) {
				logger.error("Bad Structure Exception ", e);
			}
		}
		// traverses the itemWeights to construct the structure
		for (int i=0; i<itemWeights.length; i++){
			String contextName = generateContextName(resourceId, itemKeys[i]);
			this.weights.put(contextName, itemWeights[i]);
		}
	}
	
	/**
	 * Constructs a new Structure with context names constructed with the given itemKeys and 
	 * resourceId. The values of these contexts in the HasMap are initialized as null.
	 * Weights are instantiated with the values in the given array.
	 * OntoIds are instantiated with the values in the given array.
	 * @throws BadStructureException 
	 */
	public Structure(String[] itemKeys, String resourceId, Double[] itemWeights, String[] itemOntoIds){
		this(itemKeys, resourceId, itemWeights);
		if (itemKeys.length != itemOntoIds.length){
			try {
				throw new BadStructureException("itemKeys and itemOntoIds do not have the same size.");
			} catch (BadStructureException e) {
				logger.error("Bad Structure Exception ", e);
			}
		}
		// traverses the itemOntoIds to construct the structure
		for (int i=0; i<itemOntoIds.length; i++){
			String contextName = generateContextName(resourceId, itemKeys[i]);
			this.ontoIds.put(contextName, itemOntoIds[i]);
		}
	}
	
	/**
	 * Constructs a new Structure with context names.
	 * The values of these contexts in the HasMap are initialized as null.
	 * Weights are instantiated with the default value.
	 * OntoIds are instantiated with the default value.
	 */
	public Structure(ArrayList<String> contextNames){
		this(itemKeysFromContextNames(contextNames), resourceIDFromContextNames(contextNames));
	}
	
	public void finalize() throws Throwable {
		super.finalize();
	}
	
	public static String generateContextName(String resourceId, String itemkey){
		return resourceId + "_" + itemkey;
	}
	
	/**
	 * Context name  split in two string to separate out resource id and item keys
	 *  
	 * @param contextName
	 * @return String[] of two element 1st is ResourceId and 2nd is item key 
	 */
	private static String[] degenerateContextName(String contextName){
		//Optra: This code split context name into array having more than two strings
		// the limit parameter will avoid a error if the itemKey contains itself the character "_"
		return contextName.split("_", 2);
	}
	
	private static String[] itemKeysFromContextNames(ArrayList<String> contextNames){
		String[] itemKeys = new String[contextNames.size()];
		int i=0;
		for(String contextName: contextNames){
			itemKeys[i] = degenerateContextName(contextName)[1];
			i++;
		}
		return itemKeys;
	}
	
	private static String resourceIDFromContextNames(ArrayList<String> contextNames){
		String resourceId = null;
		for(String contextName: contextNames){
			resourceId = degenerateContextName(contextName)[0];
		}
		return resourceId;
	}
	
	/**
	 * Associates the given contextName to the given value in the HashMap
	 * only if the given contextName already exists in the HashMap.
	 * Returns true if the insertion succeed, false else.
	 * If the map previously contained a mapping for this contextName, the old value is replaced.
	 */
	public boolean putContext(String contextName, String value){
		if (this.contexts.containsKey(contextName)){
			this.contexts.put(contextName, value);
			return true;
		}
		else {
			logger.error("ContextName: "+ contextName +" doesn't exits in the structure.");
			return false;
		}
	}
	
	/**
	 * Associates the given contextName to the given weight in the HashMap
	 * only if the given contextName already exists in the Hashmap.
	 * Returns true if the insertion succeed, false else.
	 * If the map previously contained a mapping for this contextName, the old value is replaced.
	 */
	public boolean putWeight(String contextName, Double weight){
		if (this.weights.containsKey(contextName)){
			this.weights.put(contextName, weight);
			return true;
		}
		else {
			logger.error("ContextName: "+ contextName +" doesn't exits in the structure.");
			return false;
		}
	}
	
	/**
	 * Associates the given contextName to the given ontoId in the HashMap
	 * only if the given contextName already exists in the Hashmap.
	 * Returns true if the insertion succeed, false else.
	 * If the map previously contained a mapping for this contextName, the old value is replaced.
	 */
	public boolean putOntoID(String contextName, String ontoId){
		if (this.ontoIds.containsKey(contextName)){
			this.ontoIds.put(contextName, ontoId);
			return true;
		}
		else {
			logger.error("ContextName: "+ contextName +" doesn't exits in the structure.");
			return false;
		}
	}
		
	/**
	 * Returns the String value to which the given contextName is mapped to in the contexts HashMap
	 * only if the given contextName already exists in the Hashmap.
	 * Else returns null.
	 */
	public String getText(String contextName){
		if (this.contexts.containsKey(contextName)){
			String itemVal = this.contexts.get(contextName);
			return itemVal;
		}
		else{
			logger.error("ContextName: "+ contextName +" doesn't exits in the structure.");
			return null;
		}
	}
	
	/**
	 * Returns the Double value to which the given contextName is mapped to in the weights HashMap
	 * only if the given contextName already exists in Hashmap.
	 * Else returns null.
	 */
	public Double getWeight(String contextName){
		if (this.weights.containsKey(contextName)){
			Double itemWeight = this.weights.get(contextName);
			return itemWeight;
		}
		else{
			logger.error("ContextName: "+ contextName +" doesn't exits in the strucutre.");
			return null;
		}
	}

	/**
	 * Returns the String value to which the given contextName is mapped in the ontoIds HashMap
	 * only if the given contextName already exists in Hashmap.
	 * Else returns null.
	 */
	public String getOntoID(String contextName){
		if (this.ontoIds.containsKey(contextName)){
			String itemOntoId = this.ontoIds.get(contextName);
			return itemOntoId;
		}
		else{
			logger.error("ContextName: "+ contextName +" doesn't exits in the structure.");
			return "";
		}
	}

	/**
	 *	Returns the resourceId of the resource for which the structure exists. 
	 */
	public String getResourceId() {
		return resourceId;
	}

	/**
	 * Returns an array of the itemkeys contained in the contexts HashMap 
	 * (the array used to construct the Structure).
	 */
	public String[] getItemKeys() {
		return itemKeys;
	}

	/**
	 *	Returns the Structure contexts HashMap. 
	 */
	public HashMap<String,String> getContexts(){
		return this.contexts;
	}
	
	/**
	 * Returns a List of the keys contained in the contexts HashMap
	 * (in the same order than in the itemKeys tab).
	 */
	public ArrayList<String> getContextNames(){
		ArrayList<String> contextNames = new ArrayList<String>(itemKeys.length);
		for(int i=0; i<itemKeys.length; i++){
			contextNames.add(generateContextName(this.resourceId, itemKeys[i]));
		}
		return contextNames;
	}

	/**
	 *	Returns the Structure weights HashMap. 
	 */
	public HashMap<String, Double> getWeights() {
		return weights;
	}

	/**
	 *	Returns the Structure ontoIds HashMap. 
	 */
	public HashMap<String, String> getOntoIds() {
		return ontoIds;
	}
	
	/**
	 * Returns true if the HashMap contains one or more null value. 
	 */
	public boolean hasNullValues(){
		return this.contexts.containsValue(null);
	}
	
	/**
	 * Returns the Structure size. 
	 */
	public int size(){
		return this.contexts.size();
	}
	
	public String toString(){
		String display = "[";
		Set<Map.Entry<String,String>> items = this.contexts.entrySet();
		for (Iterator<Map.Entry<String,String>> it = items.iterator(); it.hasNext();){
			Map.Entry<String,String> item = it.next();
			display += "[" + item.getKey() + ", " + item.getValue() + "]";
		}
		display += "]";
		return display;
	}
	
	/**
	 * 
	 * @return xml string representing Structure object
	 */
	public String toXMLString (){		
		return xStream.toXML(this) ;
	}
	
	/**
	 * This method creates Structure from xml string.
	 * 
	 * @param xmlString
	 * @return Structure object
	 */
	public static Structure createStructureFromXML(String xmlString){
		return (Structure)xStream.fromXML(xmlString);
	}
	
	//************************************* EXCEPTIONS *******************************************
	
	public class BadStructureException extends Exception {
		private static final long serialVersionUID = 1L;
		public BadStructureException(String s){
			super(s);
		}
	}
	
}