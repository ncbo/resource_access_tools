package org.ncbo.resource_access_tools.populate;

import org.ncbo.resource_access_tools.common.beans.AnnotationBean;
import org.ncbo.resource_access_tools.common.beans.ReportedContextBean;
import org.ncbo.resource_access_tools.oba.ObaWeight;
import org.ncbo.resource_access_tools.oba.ObaWeight.NonValidParameterException;

/**
* This class is used as a set of parameters to score annotations for OBR.
* When creating a new scoring, the user indicates the weight to use for each type of annotations
* in the scoring algorithm. 
* 
* @author Clement Jonquet
* @version OBS_v1
* @created 13-Feb-2009
*
*/
public class ObrWeight extends ObaWeight {

	 private int reportedDA;
 
	 /**
	 * Constructs a new object with the given parameters for the weight function.
	 * @param reportedDA
	 * Represents the weight given to a reported direct annotation.
	 */
	 public ObrWeight(int preferredNameDA, int synonymDA, byte isaEA, int mappingEA, int reportedDA) throws NonValidParameterException {
		 super(preferredNameDA, synonymDA, isaEA, mappingEA);
		 this.reportedDA = reportedDA;
	 }
	 
	 /**
	  * Constructs a new object with the given parameters for the weight function.
	  * @param reportedDA
	  * Represents the weight given to a reported direct annotation.
	  */
	 public ObrWeight(int preferredNameDA, int synonymDA, double isaFactor, int mappingEA, int reportedDA) {
		 super(preferredNameDA, synonymDA, isaFactor, mappingEA);
		 this.reportedDA = reportedDA;
	 }

	 public int getReportedDA() {
		 return reportedDA;
	 }
	 
	 /**
	  * Returns the weight of the given annotation according to the ObrWeight parameters.
	  */
	 @Override
	 public int weight(AnnotationBean annotation){
		 String contextType = annotation.getContext().getContextName();
		 if(contextType.equals(ReportedContextBean.REPORTED_CTX)){
			 return this.reportedDA;
		 }
		 else{
			 return super.weight(annotation);
		 }
	 }

	 /**
	  * Returns the weight of the given annotation according to the ObrWeight parameters
	  * and a given context weight.
	  */
	 public float weight(AnnotationBean annotation, float contextWeight){
		 String contextType = annotation.getContext().getContextName();
		 if(contextType.equals(ReportedContextBean.REPORTED_CTX)){
			 return this.reportedDA*contextWeight;
		 }
		 else{
			 return super.weight(annotation)*contextWeight;
		 }
	 }

}