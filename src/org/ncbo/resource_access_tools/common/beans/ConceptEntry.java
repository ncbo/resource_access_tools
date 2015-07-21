package org.ncbo.resource_access_tools.common.beans;
/** 
 * This class is a representation for a OBS_CT table entry.
 * 
 * @author Clement Jonquet
 * @version OBS_v1		
 * @created 18-Sept-2008
 * 
 */
public class ConceptEntry {

		private String localConceptId;
		private String localOntologyId;
		private boolean isTopLevel;

		public ConceptEntry(String localConceptId, String localOntologyId, boolean isTopLevel) {
			super();
			this.localConceptId = localConceptId;
			this.localOntologyId = localOntologyId;
			this.isTopLevel = isTopLevel;
		}

		public String getLocalConceptId() {
			return localConceptId;
		}

		public String getLocalOntologyId() {
			return localOntologyId;
		}
		
		public boolean isTopLevel() {
			return isTopLevel;
		}

		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("ConceptEntry: [");
			sb.append(this.localConceptId);
			sb.append(", ");
			sb.append(this.localOntologyId);
			sb.append(", ");
			sb.append(this.isTopLevel);
			sb.append("]");
			return sb.toString();
		}
	}