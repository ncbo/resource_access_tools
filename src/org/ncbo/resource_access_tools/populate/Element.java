package org.ncbo.resource_access_tools.populate;

/**
 * This class is the representation for an element in OBR. OBR resources are composed from elements.
 * Elements are stored locally in OBR_XX_ET tables. The Element abstraction is used to process them
 * (annotation, user answer, etc.).<p>
 * An Element has a elementLocalId (String), which identifies the element on its original online resource
 * and a elementStructure (Structure) which contains the element content. 
 * <p>
 * 
 * @author Clement Jonquet
 * @version OBR v_0.2
 * @created 02-Oct-2007 11:47:48 AM
 */
public class Element {

	private String localElementId;
	private Structure elementStructure;
		
	/**
	 * Constructs a new Element with a given localElementId, and a given Structure.
	 * The Structure must not contains null values in the structureItems HashMap.
	 */
	public Element(String localElementId, Structure elementStructure) throws BadElementStructureException {
		super();
		this.localElementId = localElementId;
		if (elementStructure.hasNullValues()){			
			throw new BadElementStructureException("The given structure has null value(s).");
		}
		else {
			this.elementStructure = elementStructure;
		}
	}
		
	public void finalize() throws Throwable {
		super.finalize();
	}
	
	/**
	 * Returns the localElementId (i.e., ID of the element on the corresponding online resource).
	 */
	public String getLocalElementId() {
		return localElementId;
	}
	
	/**
	 * Returns the Element Structure.
	 */
	public Structure getElementStructure() {
		return elementStructure;
	}

	/**
	 * Returns the Element title.
	 */
	public String getElementTitle() {
		return this.elementStructure.getText("title");
	}

	/**
	 * Returns the Element description.
	 */
	public String getElementDescription() {
		return this.elementStructure.getText("description");
	}

	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString(){
	    final String TAB = "\r\n";
	    StringBuffer retValue = new StringBuffer();
	    retValue.append("element [ ").append(TAB)
	        //.append(super.toString()).append(TAB)
	        .append("\t\t\tlocalElementId = ").append(this.localElementId).append(TAB)
	        .append("\t\t\telementStructure = ").append(this.elementStructure).append(TAB)
	        .append("]");
	    return retValue.toString();
	}
	
	//************************************* EXCEPTIONS *******************************************
	
	public class BadElementStructureException extends Exception {
		private static final long serialVersionUID = 1L;
		public BadElementStructureException(String s){
			super(s);
		}
	}
	
}
