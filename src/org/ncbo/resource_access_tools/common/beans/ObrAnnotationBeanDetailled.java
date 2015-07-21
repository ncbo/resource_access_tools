package org.ncbo.resource_access_tools.common.beans;

import org.ncbo.resource_access_tools.populate.Element;

/**
 * Extension of the class ObrAnnotationBean that contains a full Element bean.
 * Used when API needs details about the original resource element (e.g., UI).
 *
 * @author Clement Jonquet
 * @version 1.0
 * @created 30-May-2009 2:42:12 PM
 */
public class ObrAnnotationBeanDetailled extends ObrAnnotationBean {

	private Element element;
	
	public ObrAnnotationBeanDetailled(ConceptBean concept, ContextBean context, Element element, float score) {
		super(concept, context, element.getLocalElementId(), score);
		this.element = element;
	}

	public ObrAnnotationBeanDetailled(ObrAnnotationBean annotation, Element element) {
		super(annotation.getConcept(), annotation.getContext(), element.getLocalElementId(), annotation.getScore());
		this.element = element;
	}
	
	public Element getElement() {
		return element;
	}

	/**
	 * Constructs a <code>String</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString() {
	    final String TAB = "\t\n";
	    StringBuffer retValue = new StringBuffer();
	    retValue.append(super.toString()).append("\t\t-->")
	        .append("element = ").append(element.toString()).append(TAB);
	    return retValue.toString();
	}
	
}