package org.ncbo.resource_access_tools.common.beans;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.ncbo.resource_access_tools.common.files.FileParameters;
import org.ncbo.resource_access_tools.common.service.NcboObsOntology;

import com.hp.hpl.jena.ontology.OntModel;
import com.thoughtworks.xstream.XStream;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;

/**
 * This class is a JavaBean representation of an OBA/OBR result.
 *  
 * A ResultBean has the following fields:
 * <li>
 * 	<ul> a resultId.</ul>
 * 	<ul> a dictionary {@link DictionaryBean} which represents the dictionary version in the OBS DB used.</ul>
 * 	<ul> a set of resultStatistics {@link StatisticsBean} that represent the number of annotation for each contexts.</ul>
 * 	<ul> parameters {@link ParamtersBean} which defines the parameters specified to get this result.</ul>
 * 	<ul> a set of ontologies {@link OntologyUsedBean} which represents the ontologies used for this result (i.e., the one 
 * for which at least one annotation exists). 
 * </li>
 * 
 * @author Clement Jonquet
 * @version 1.0
 * @created 24-Sep-2008 2:42:12 PM
 */
public abstract class ResultBean {

	// Logger for this class
	private static Logger logger = Logger.getLogger(ResultBean.class);
	
	private String resultId;
	private DictionaryBean dictionary;
	private HashSet<StatisticsBean> resultStatistics;
	private ParametersBean parameters;
	
	private static XStream resultBeanXStream = new XStream();
		
	public ResultBean(String resultId, DictionaryBean dictionary, HashSet<StatisticsBean> resultStatistics, ParametersBean parameters) {
		super();
		this.resultId = resultId;
		this.dictionary = dictionary;
		this.resultStatistics = resultStatistics;
		this.parameters = parameters;
	}

	public String getResultId() {
		return resultId;
	}
	
	public DictionaryBean getDictionary() {
		return dictionary;
	}

	public HashSet<StatisticsBean> getResultStatistics() {
		return resultStatistics;
	}

	public ParametersBean getParameters() {
		return parameters;
	}

	/**
	 * Returns a string that contains only the annotations of the ResultBean as tab delimited lines to be written in a file.
	 */
	public abstract String toTabDelimited();
	
	/**
	 *	Returns a string that contains all the result content in a XML pretty-printed form.
	 *	(XML serialization done with <a href="http://xstream.codehaus.org/">XStream API</a>) 
	 */
	public String toXML(){
		String xmlContent = "";
		try{
			xmlContent = resultBeanXStream.toXML(this);
		}
		catch (Exception e) {
			logger.error("** PROBLEM ** Cannot export ResultBean as XML file. Empty string '' returned.", e);
		}
		return xmlContent;
	}

	/**
	 *	Returns a string that contains all the result content in an OWL ontology populated with instances.
	 *	http://obs.bioontology.org/ontologies/NCBO_OBS_ontology.owl
	 */
	public String toOWL(NcboObsOntology obsOntology){
		String owlContent = "";
		try{
			// creates a new Result individual
			this.getOWLIndividual(obsOntology);
			
			// writes the corresponding populated ontology to a stream
			OntModel ontModel = obsOntology.owlModel.getOntModel();
			StringWriter fstream = new StringWriter();
			fstream.write("<?xml version=\"1.0\"?>\n");
			ontModel.write(fstream, "RDF/XML-ABBREV");
			fstream.close();
			owlContent = fstream.toString();
			
			// removes instances from the static ontology (for next use)
			obsOntology.removeInstances();
			
			/*			
			//save as a file here
			String fileName = FileParameters.ontologyFolder()+ NcboObsOntology.getFILE_NAME()+"."+ resultId +".owl";
		    Collection<Object> errors = new ArrayList<Object>();
		    obsOntology.owlModel.save(new File(fileName).toURI(), FileUtils.langXMLAbbrev, errors);
		    System.out.println("File saved with " + errors.size() + " errors.");
		    */
		}
		catch (Exception e) {
			e.printStackTrace();
			System.out.println("** PROBLEM ** Cannot export ResultBean as OWL file. Empty file returned.");
		}
		return owlContent;
	}
	
	/**
	 * Returns a OWL individual for that object and creates the necessary other individuals, in the given ontology. 
	 */
	public abstract OWLIndividual getOWLIndividual(NcboObsOntology obsOntology);
	  	
	protected void assignPropertyValue(OWLIndividual individual, NcboObsOntology obsOntology){
		individual.addPropertyValue(obsOntology.owlModel.getOWLObjectProperty(NcboObsOntology.OWL_HAS_DICTIONARY), this.dictionary.getOWLIndividual(obsOntology, individual));
		for (StatisticsBean statistic: this.resultStatistics){
			individual.addPropertyValue(obsOntology.owlModel.getOWLObjectProperty(NcboObsOntology.OWL_HAS_STATISTICS), statistic.getOWLIndividual(obsOntology, individual));
		}
		individual.addPropertyValue(obsOntology.owlModel.getOWLObjectProperty(NcboObsOntology.OWL_HAS_PARAMETERS), this.parameters.getOWLIndividual(obsOntology, individual));
	} 
	
	/**
	 * Writes the given String (usually with the result content) in a file in the RESULT folder.
	 */
	public static void writeStringToFile(String content, String fileName){
		File file = new File(FileParameters.LOCAL_FOLDER + FileParameters.RESULT_FOLDER + fileName);
		try {
			if(!file.createNewFile()){
				file.delete();
			}
			FileWriter fstream = new FileWriter(file);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(content);
			out.close();
			fstream.close();
		}
		catch (IOException e) {
			logger.error("Problem in writing string to file ", e);
		}
	}

	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString(){
	    final String TAB = "\r\n";
	    StringBuffer retValue = new StringBuffer();
	    retValue.append("result [ ").append(TAB)
	        .append("\tresultId = ").append(this.resultId).append(TAB)
	        .append("\tdictionary = ").append(this.dictionary).append(TAB)
	        .append("\tresultStatistics = ").append(this.resultStatistics).append(TAB)
	        .append("\tparameters = ").append(this.parameters)
	        .append("]");
	    return retValue.toString();
	}
	
}