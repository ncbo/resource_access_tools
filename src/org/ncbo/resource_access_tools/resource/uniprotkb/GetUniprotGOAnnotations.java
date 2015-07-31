package org.ncbo.resource_access_tools.resource.uniprotkb;

import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.ncbo.resource_access_tools.populate.Element;
import org.ncbo.resource_access_tools.populate.Resource;
import org.ncbo.resource_access_tools.populate.Structure;
import org.ncbo.resource_access_tools.populate.Element.BadElementStructureException;
import org.ncbo.resource_access_tools.util.helper.StringHelper;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.interfaces.uniprot.dbx.go.Go;
import uk.ac.ebi.kraken.interfaces.uniprot.features.FeatureType;
import uk.ac.ebi.kraken.model.uniprot.features.MutagenFeatureImpl;
import uk.ac.ebi.kraken.model.uniprot.features.VariantFeatureImpl;
import uk.ac.ebi.kraken.uuw.services.remoting.EntryIterator;
import uk.ac.ebi.kraken.uuw.services.remoting.Query;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtJAPI;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtQueryBuilder;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtQueryService;

/**
 * This class enables to get the list of all annotations between human proteins (gene symboles indeed) and GO concept.
 * Resource UniProtKB
 * This annotation are extracted from the UniProtKb database directly.
 *
 * @author Adrien Coulet
 * @version OBR_v0.2
 * @created 21-Nov-2008
 *
 */
public class GetUniprotGOAnnotations implements StringHelper{

	// Logger for this class
	private static Logger logger = Logger.getLogger(GetUniprotGOAnnotations.class);

	//No need of bellow attributes, As per new changes we are directly indexing data from UniProtKb database.
	//private static String SHELL_SCRIPT_PATH = new File(GetUniprotGOAnnotations.class.getResource( "getGoUniprotKbAnnot.sh" ).getFile()).getAbsolutePath();
	//private static String COMMAND = "sh "+ SHELL_SCRIPT_PATH;

	HashSet<Element>  ProteinAnnotList            = new HashSet<Element>() ;
    Hashtable<String, Hashtable<String, String>> allProtAnnot     = new Hashtable<String, Hashtable<String, String>>();	//<protAccession, Hashtable of attribut-values couple>
	Hashtable<String, String> protAnnotAttribute  = new Hashtable<String, String>();   	//<attributName, value> (a value could be a map)
	Hashtable<String, String> protAnnotAttribute2 = new Hashtable<String, String>();   	//<attributName, value> (a value could be a map)
	Resource resource;
	Structure basicStructure = null;
	String resourceID = EMPTY_STRING;

	//constructor
	public GetUniprotGOAnnotations(Resource myResource){
		this.resource = myResource;
		this.basicStructure = myResource.getResourceStructure();
		this.resourceID     = myResource.getResourceId();
	}

	// method
	public HashSet<Element> getElements(Map<String, String> localOntologyMap,HashSet<String> allElementsInET) {

		try	{
				allProtAnnot = new Hashtable<String, Hashtable<String, String>>();

				// Create UniProt query service
	            UniProtQueryService uniProtQueryService = UniProtJAPI.factory.getUniProtQueryService();

	            // Querying to UniProtKB database to get "UniProtKB/Swiss-Prot(manually reviewed)" type of records.
	            Query query = UniProtQueryBuilder.buildQuery("reviewed");
	            EntryIterator<UniProtEntry> entryIterator = uniProtQueryService.getEntryIterator(query);

	            System.out.println("Total size ::" + entryIterator.getResultSize());

	            //Data parsing starts
	            while (entryIterator.hasNext()) {

	            	protAnnotAttribute  = new Hashtable<String, String>();
	            	UniProtEntry up = entryIterator.next();
	            	String localElementID = EMPTY_STRING;
					String proteinName    = EMPTY_STRING;
					String geneSymbol     = EMPTY_STRING;
					String organism 	  = EMPTY_STRING;
					if(up != null){
						//Uniprot Identifier
		                localElementID = up.getPrimaryUniProtAccession().getValue();

		                //if Element already present then continue.
		                if (allElementsInET.contains(localElementID.trim())){
							continue;
						}
		                //Protein Name
		                if(!up.getProteinDescription().getRecommendedName().getFields().isEmpty())
		                	proteinName = up.getProteinDescription().getRecommendedName().getFields().get(0).getValue();

		                //Gene Name(Symbol)
		                if(!up.getGenes().isEmpty())
		                	geneSymbol = up.getGenes().get(0).getGeneName().getValue();

		                //Organism
		                organism = up.getOrganism().getScientificName().getValue();

		                //Natural Variants
		                FeatureType ft = FeatureType.VARIANT;
		                Collection<VariantFeatureImpl> feList = up.getFeatures(ft);
		                Iterator<VariantFeatureImpl> it = feList.iterator();
		                StringBuffer naturalVarients = new StringBuffer();
		                while (it.hasNext()) {
		                    VariantFeatureImpl vf = it.next();
		                    //check for unique records
		                    if (vf.getVariantReport().getValue() != null && !vf.getVariantReport().getValue().equals("") && naturalVarients.indexOf(vf.getVariantReport().getValue()) == -1) {
		                        naturalVarients.append(vf.getVariantReport().getValue());
		                        if (it.hasNext()) {
		                            naturalVarients.append(",");
		                        }
		                    }
		                }

		                if (naturalVarients.toString().endsWith(",")) {
		                    naturalVarients.deleteCharAt(naturalVarients.length() - 1);
		                }

		                //Mutagenesis
		                FeatureType ftm = FeatureType.MUTAGEN;
		                Collection<MutagenFeatureImpl> mutgenList = up.getFeatures(ftm);
		                Iterator<MutagenFeatureImpl> mutgenListIt = mutgenList.iterator();
		                StringBuffer mutgens = new StringBuffer();
		                while (mutgenListIt.hasNext()) {
		                    MutagenFeatureImpl mf = mutgenListIt.next();
		                    if (mf.getMutagenReport().getValue() != null && !mf.getMutagenReport().getValue().equals("") && mutgens.indexOf(mf.getMutagenReport().getValue()) == -1) {
		                        mutgens.append(mf.getMutagenReport().getValue());
		                        if (mutgenListIt.hasNext()) {
		                            mutgens.append(",");
		                        }
		                    }
		                }
		                if (mutgens.toString().endsWith(",")) {
		                	mutgens.deleteCharAt(mutgens.length() - 1);
		                }
		                //Biological process
		                StringBuffer bp = new StringBuffer();
		                //Cellular component
		                StringBuffer cp = new StringBuffer();
		                //Molecular function
		                StringBuffer mp = new StringBuffer();
		                //goAnnotation
		                StringBuffer goAnnotation = new StringBuffer();
		                List<Go> goterms = up.getGoTerms();

		                for (Go goobj : goterms) {
		                    if (goobj.getGoTerm().getValue() != null && !goobj.getGoTerm().getValue().equals("")) {
		                        //biological
		                        if (goobj.getOntologyType().getValue().equals("P:") && bp.indexOf(goobj.getGoTerm().getValue()) == -1) {
		                            bp.append(goobj.getGoTerm().getValue());
		                            bp.append(",");

		                        } //molecular
		                        else if (goobj.getOntologyType().getValue().equals("F:") && mp.indexOf(goobj.getGoTerm().getValue()) == -1) {
		                            mp.append(goobj.getGoTerm().getValue());
		                            mp.append(",");

		                        } //cellular
		                        else if (goobj.getOntologyType().getValue().equals("C:") && cp.indexOf(goobj.getGoTerm().getValue()) == -1) {
		                            cp.append(goobj.getGoTerm().getValue());
		                            cp.append(",");

		                        }
		                    }
		                    //go Annotaions
		                    if (goobj.getGoId().getValue() != null && !goobj.getGoId().getValue().equals("") && goAnnotation.indexOf(goobj.getGoId().getValue()) == -1) {
		                        goAnnotation.append(goobj.getGoId().getValue());
		                        goAnnotation.append(GT_SEPARATOR_STRING);
		                    }
		                }
		                if (bp.toString().endsWith(",")) {
		                    bp.deleteCharAt(bp.length() - 1);
		                }
		                if (cp.toString().endsWith(",")) {
		                    cp.deleteCharAt(cp.length() - 1);
		                }
		                if (mp.toString().endsWith(",")) {
		                    mp.deleteCharAt(mp.length() - 1);
		                }
		                if (goAnnotation.toString().endsWith(GT_SEPARATOR_STRING)) {
		                    goAnnotation.deleteCharAt(goAnnotation.length() - 2);
		                }
		                protAnnotAttribute.put("geneSymbol",  geneSymbol);
						protAnnotAttribute.put("proteinName", proteinName);
						protAnnotAttribute.put("goAnnotationList", goAnnotation.toString());
						protAnnotAttribute.put("organism",  organism);
						protAnnotAttribute.put("naturalVariant", naturalVarients.toString());
						protAnnotAttribute.put("mutagenesis", mutgens.toString());
						protAnnotAttribute.put("biologicalProcess",  bp.toString());
						protAnnotAttribute.put("cellularComponent", cp.toString());
						protAnnotAttribute.put("molecularFunction", mp.toString());

		                allProtAnnot.put(localElementID, protAnnotAttribute);
	            	}
	            }
				// end of parsing

				// Second phase: creation of elements
				// The creation of element is not done during the parsing in order to get all annotation of a protein before to create the element
				for (String localElementID : allProtAnnot.keySet()){
					protAnnotAttribute2 = new Hashtable<String, String>();
					protAnnotAttribute2 = allProtAnnot.get(localElementID);

					// PUT DATA INTO A STRUCTURE++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
					Structure elementStructure = new Structure(this.basicStructure.getContextNames());
					for (String contextName: this.basicStructure.getContextNames()){
						boolean attributeHasValue = false;

						for (String att : protAnnotAttribute2.keySet()){
							if (contextName.equals(this.resourceID+UNDERSCORE_STRING+att)){
								// not an existing annotation
								if(basicStructure.getOntoID(contextName).equals(Structure.FOR_CONCEPT_RECOGNITION) ||
										basicStructure.getOntoID(contextName).equals(Structure.NOT_FOR_ANNOTATION)){
									elementStructure.putContext(contextName, protAnnotAttribute2.get(att));
									attributeHasValue = true;

								}else{ // existing annotations
									String localConceptID_leftPart  = localOntologyMap.get(contextName); //ontoID
									String localConceptID           = EMPTY_STRING;
									String localExistingAnnotations = protAnnotAttribute2.get(att).trim();
									String localConceptIDs          = EMPTY_STRING;
									String[] splittedAnnotations    = localExistingAnnotations.split(GT_SEPARATOR_STRING);
									// translate conceptIDs used in the resource in OBR localConceptID
									for (int i=0;i<splittedAnnotations.length;i++){
										try{
											String localConceptID_rightPart = splittedAnnotations[i].trim();
											if(!localConceptID_rightPart.isEmpty())
												localConceptID = localConceptID_leftPart + SLASH_STRING+ localConceptID_rightPart;//localConceptID_leftPart+"/"+localConceptID_rightPart;
										}catch (Exception e) {
											logger.error("** PROBLEM ** Problem with the management of the conceptID used in the resource to reported in the OBR", e);
										}
										if(localConceptID!=EMPTY_STRING){
											if(localConceptIDs!=EMPTY_STRING){
												localConceptIDs+=GT_SEPARATOR_STRING+localConceptID;
											}else{
												localConceptIDs+=localConceptID;
											}
										}
									}
									elementStructure.putContext(contextName, localConceptIDs);
									attributeHasValue = true;
								}// end of existing annotation
							}
						}

						// to avoid null value in the structure
						if (!attributeHasValue){
							elementStructure.putContext(contextName,EMPTY_STRING);
						}
					}
					// put the element structure in a new element
					try{
						Element  myProtAnnot = new Element(localElementID, elementStructure);
						ProteinAnnotList.add(myProtAnnot);
					}catch(BadElementStructureException e){
						logger.error(EMPTY_STRING, e);
					}
					//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
				}
		} catch(Exception ioe) {
			logger.error(EMPTY_STRING, ioe);
		}
		return ProteinAnnotList;
	}
}
