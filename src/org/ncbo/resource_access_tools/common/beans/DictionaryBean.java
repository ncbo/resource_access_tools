package org.ncbo.resource_access_tools.common.beans;

import java.util.Calendar;

import org.ncbo.resource_access_tools.common.service.NcboObsOntology;

import com.ibm.icu.text.SimpleDateFormat;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.RDFSDatatype;
import edu.stanford.smi.protegex.owl.model.RDFSLiteral;

/**
 * This class is a JavaBean representation of a dictionary within OBS.
 * Dictionary are used for versioning the content of the OBS DB.
 * A dictionary has a version, dictionaryName and dictionaryDate.
 *    
 * @author Clement Jonquet
 * @version 1.0
 * @created 25-Sep-2008 2:42:11 PM
 */
public class DictionaryBean {

	public static final String DICO_NAME = "OBS_DICO_";
	
	private int dictionaryId;
	private String dictionaryName;
	private Calendar dictionaryDate;
	
	private static int count;
	
	public DictionaryBean(int dictionaryId, String name, Calendar date) {
		super();
		this.dictionaryId = dictionaryId;
		this.dictionaryName = name;
		this.dictionaryDate = date;
	}

	public int getDictionaryId() {
		return dictionaryId;
	}

	public String getDictionaryName() {
		return dictionaryName;
	}

	public Calendar getDictionaryDate() {
		return dictionaryDate;
	}

	protected static int count(){
		count++;
		return count;
	}
	
	/**
	 * Returns a OWL individual for that object and creates the necessary other individuals, in the given ontology. 
	 */
	public OWLIndividual getOWLIndividual(NcboObsOntology obsOntology, OWLIndividual resultInd){

		// creates the class individual
		OWLIndividual individual = obsOntology.owlModel.getOWLNamedClass(NcboObsOntology.OWL_DICTIONARY).createOWLIndividual(this.dictionaryName +"_"+ count());
		
		// assigns the individual properties
		individual.addPropertyValue(obsOntology.owlModel.getOWLObjectProperty(NcboObsOntology.OWL_HAS_RESULT), resultInd);
		
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_DICTIONARY_ID), obsOntology.convertToPositiveInteger(this.dictionaryId));
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_DICTIONARY_NAME), this.dictionaryName);
		// creates an RDFDatatype and a RDFLiteral for the date
		RDFSDatatype xsdDate = obsOntology.owlModel.getRDFSDatatypeByName("xsd:date");
		SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
		//String dateString = this.getDictionary().getDate().getTime().toString();
		RDFSLiteral dateLiteral = obsOntology.owlModel.createRDFSLiteral(f.format(this.dictionaryDate.getTime()), xsdDate);
		individual.addPropertyValue(obsOntology.owlModel.getOWLDatatypeProperty(NcboObsOntology.OWL_DICTIONARY_DATE), dateLiteral);
		
		return individual;
	}
	
	/**
	 * Constructs a <code>String</code> with all attributes in dictionaryName = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString(){
	    final String TAB = ", ";
	    StringBuffer retValue = new StringBuffer();
	    retValue.append("[")
	        .append("dictionaryId = ").append(this.dictionaryId).append(TAB)
	        .append("dictionaryName = ").append(this.dictionaryName).append(TAB)
	        .append("dictionaryDate = ").append(this.dictionaryDate.getTime().toString())
	        .append("]");
	    return retValue.toString();
	}
	
	
	
	
}