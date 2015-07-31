package org.ncbo.resource_access_tools.oba;


/**
 * This class is used as a set of parameters to score annotations for OBA.
 * When creating a new scoring, the user indicates the weight to use for each type of annotations
 * in the scoring algorithm. 
 * 
 * @author Clement Jonquet
 * @version OBS_v1
 * @created 01-Oct-2008
 *
 */
 public class ObaWeight {

	 private int preferredNameDA;
	 private int synonymDA;
	 private byte isaEA;
	 private double isaFactor;
	 private int mappingEA;
	 
	/**
	 * Constructs a new object with the given parameters for the weight function.
	 *  
	 * @param preferredNameDA
	 * Represents the weight given to a direct annotation done with the concept preferred name.
	 * Must be between 0 and 10. 
	 * @param synonymDA
	 * Represents the weight given to a direct annotation done with a concept synonym.
	 * Must be between 0 and 10. 
	 * @param isaEA
	 * Represents the function chosen to give weight to an expanded annotation done with isa
	 * relations according to a given level.
	 * Must match to a static function number in this class.
	 * @param isafactor
	 * Represents the factor used in function #3. Must not be null if isaEA=3.
	 * @param mappingEA
	 * Represents the weight given to an expanded annotation done with the mappings of the concept.
	 * Must be between 0 and 10. 
	 * @throws NonValidParameterException 
	 */
	 public ObaWeight(int preferredNameDA, int synonymDA, byte isaEA, int mappingEA) throws NonValidParameterException {
		super();
		this.preferredNameDA = preferredNameDA;
		this.synonymDA = synonymDA;
		if (isaEA>2){
			throw new NonValidParameterException(isaEA);
		}
		else{
			this.isaEA = isaEA;
		}
		this.mappingEA = mappingEA;
	}
	 
	 /**
	  * @param isafactor
	  * Represents the factor used in function #3. isaEA will be 3.
	  * Should be betweem 0 and 1. 
	  */
	 public ObaWeight(int preferredNameDA, int synonymDA, double isaFactor, int mappingEA) {
			super();
			this.preferredNameDA = preferredNameDA;
			this.synonymDA = synonymDA;
			this.isaEA = 3;
			this.isaFactor = isaFactor;
			this.mappingEA = mappingEA;
		} 
	 
	public int getPreferredNameDA() {
		return preferredNameDA;
	}

	public int getSynonymDA() {
		return synonymDA;
	}
	
	public byte getIsaEA() {
		return isaEA;
	}
	
	public double getIsaFactor() {
		return isaFactor;
	}

	public int getMappingEA() {
		return mappingEA;
	}

	public int getIsaEAByLevel(int level){
		switch (this.isaEA) {
		case 1: return function1(level);
		case 2: return function2(level);
		case 3: return function3(level, this.isaFactor);
		default: return function1(level);
		}
	}
	
	private static int function1(int level){
		if(level<2){
			return 8;
		}
		if(level<3){
			return 7;
		}
		if(level<5){
			return 6;
		}
		if(level<7){
			return 5;
		}
		if(level<15){
			return 3;
		}
		else {
			return 1;
		}
	}
	
	private static int function2(int level){
		 return level/30;
	}
	 
	private static int function3(int level, double factor){
		Double score = new Double(Math.floor(10*Math.exp(-factor*level)+1));
		return score.intValue();
	}
	
// ****************************** EXCEPTIONS *********************************************
	
	public class NonValidParameterException extends Exception {
		private static final long serialVersionUID = 1L;
		public NonValidParameterException(byte isaEA){
			super("Use other constructor with isaEA >2.");
		}
	}

}
