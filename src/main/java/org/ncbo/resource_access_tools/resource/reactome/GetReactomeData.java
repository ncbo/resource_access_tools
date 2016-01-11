package org.ncbo.resource_access_tools.resource.reactome;

import org.ncbo.resource_access_tools.populate.Resource;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.ser.*;
import org.apache.log4j.Logger;
import org.ncbo.resource_access_tools.populate.Element;
import org.ncbo.resource_access_tools.populate.Element.BadElementStructureException;
import org.ncbo.resource_access_tools.populate.Structure;
import org.ncbo.resource_access_tools.resource.ResourceAccessTool;
import org.ncbo.resource_access_tools.util.helper.StringHelper;
import org.reactome.cabig.domain.*;
import org.reactome.servlet.InstanceNotFoundException;
import org.reactome.servlet.ReactomeRemoteException;

import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;
import java.util.HashSet;

/**
 * This class enables to  - get the list of all localElementID of Pathways and Reactions (ie Events)
 * in the resource Reactome,
 * - get data related to a specific Pathway or Reaction.
 * This class use as a client the Reactome SOAP Web Service.
 * See http://www.reactome.org/download/index.html
 *
 * @author Adrien Coulet
 * @version OBR_v1
 * @created 26-Fev-2009
 */

class GetReactomeData implements StringHelper {

    // Logger for this class
    private static final Logger logger = Logger.getLogger(GetReactomeData.class);

    //attributes
    private final Object[] EMPTY_ARG = new Object[]{};
    private static final String SERVICE_URL_NAME = "http://www.reactome.org:8080/caBIOWebApp/services/caBIOService";
    private static final String SCHEMA_NAMESPACE_URI = "http://www.reactome.org/caBIOWebApp/schema";
    private static final String DEFAULT_SCHEMA_NAMESPACE_URI = "http://www.w3.org/2001/XMLSchema";

    private Service caBIOService;

    private Structure basicStructure = null;
    private String resourceID = EMPTY_STRING;
    private ResourceAccessTool tool = null;

    //constructor
    public GetReactomeData(Resource myResource, ResourceAccessTool tool) {
        this.basicStructure = myResource.getResourceStructure();
        this.resourceID = myResource.getResourceId();
        this.tool = tool;
    }

    // methods
    public HashSet<Long> getLocalElementIds() {

        HashSet<Long> localElementIDs = new HashSet<Long>();
        try {
            Call call = createCall("listObjects");
            String[] domainClsNames = new String[]{
                    "org.reactome.cabig.domain.Pathway",
                    "org.reactome.cabig.domain.Reaction"
            };
            int length = getMaxSizeInListObjects();
            for (String clsName : domainClsNames) {
                int total = 0;
                Object[] objects = null;

                while (true) {
                    objects = (Object[]) call.invoke(new Object[]{clsName, total, length});
                    if (objects == null || objects.length == 0) {
                        break;
                    }

                    for (int i = 0; i < objects.length; i++) {
                        Event myEvent = (Event) objects[i];
                        localElementIDs.add(myEvent.getId());
                    }
                    if (objects.length < length) {
                        break;
                    }
                    total += objects.length;

                }
            }
        } catch (Exception e) {
            logger.error("**PROBLEM: when querying the list of localElementID.", e);
        }
        return localElementIDs;
    }

    public Element getElement(Long localElementID) {

        Structure elementStructure = basicStructure;
        Element element = null;
        try {
            Call callQueryByID = null;
            callQueryByID = createCall("queryById");

            Object obj = callQueryByID.invoke(new Object[]{localElementID});
            Event myEvent = (Event) obj;
            for (String contextName : elementStructure.getContextNames()) {
                if (elementStructure.getOntoID(contextName).equals(Structure.FOR_CONCEPT_RECOGNITION) ||
                        elementStructure.getOntoID(contextName).equals(Structure.NOT_FOR_ANNOTATION)) {
                    // get name
                    if (contextName.equals(this.resourceID + UNDERSCORE_STRING + "name")) {
                        if (myEvent.getName() != null || myEvent.getName().equals("null")) {
                            elementStructure.putContext(contextName, myEvent.getName());
                        } else {
                            elementStructure.putContext(contextName, EMPTY_STRING);
                        }
                    }
                    // get participants
                    else if (contextName.equals(this.resourceID + UNDERSCORE_STRING + "participants")) {
                        HashSet<String> participants = getParticipants(localElementID);
                        String particpantList = EMPTY_STRING;
                        if (participants != null) {
                            for (String name : participants) {
                                if (!particpantList.equals(EMPTY_STRING)) {
                                    particpantList += GT_SEPARATOR_STRING + name;
                                } else {
                                    particpantList += name;
                                }
                            }
                            elementStructure.putContext(contextName, particpantList);
                        } else {
                            elementStructure.putContext(contextName, EMPTY_STRING);
                        }
                    }
                    // get text description
                    else if (contextName.equals(this.resourceID + UNDERSCORE_STRING + "summation")) {
                        String summationList = EMPTY_STRING;
                        if (myEvent.getSummation() != null) {
                            for (Summation sum : myEvent.getSummation()) {
                                if (sum.getText() != null) {
                                    if (!summationList.equals(EMPTY_STRING)) {
                                        summationList += GT_SEPARATOR_STRING + sum.getText();
                                    } else {
                                        summationList += sum.getText();
                                    }
                                }
                            }
                            elementStructure.putContext(contextName, summationList);
                        } else {
                            elementStructure.putContext(contextName, EMPTY_STRING);
                        }
                    }
                } else {
                    // REPORTED ANNOTATIONS
                    // TODO to be re-implemented with mapStringToLocalConceptIDs(String s, String localOntologyID, boolean exactMap)
                    // handle the case where several concept ID will show up
                    // Exceptions handling to be changed and logged
                    // get GO annotations
                    if (contextName.equals(this.resourceID + UNDERSCORE_STRING + "goBiologicalProcess")) {
                        try {
                            if (myEvent.getGoBiologicalProcess() != null) {

                                String localConceptID = tool.getResourceUpdateService().getLocalConceptIdByPrefNameAndOntologyId(elementStructure.getOntoID(contextName), myEvent.getGoBiologicalProcess().getName());
                                elementStructure.putContext(contextName, localConceptID);
                            } else {
                                elementStructure.putContext(contextName, EMPTY_STRING);
                            }
                        } catch (Exception e) {
                            elementStructure.putContext(contextName, EMPTY_STRING);
                        }
                    }
                    if (contextName.equals(this.resourceID + UNDERSCORE_STRING + "goCellCompartiment")) {
                        try {
                            if (myEvent.getCompartment() != null) {
                                String localConceptID = tool.getResourceUpdateService().getLocalConceptIdByPrefNameAndOntologyId(elementStructure.getOntoID(contextName), myEvent.getCompartment().getName());
                                elementStructure.putContext(contextName, localConceptID);
                            } else {
                                elementStructure.putContext(contextName, EMPTY_STRING);
                            }
                        } catch (Exception e) {
                            elementStructure.putContext(contextName, EMPTY_STRING);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("**PROBLEM: when querying data for an element.", e);
        }
        // put the elementStructure in a new element
        try {
            element = new Element(localElementID.toString(), elementStructure);
        } catch (BadElementStructureException e) {
            logger.error(EMPTY_STRING, e);
        }
        return element;
    }

    /**
     * Get an HashSet of the name of all participating molecules for a given pathway.
     *
     * @param id
     * @return
     * @throws Exception
     */
    private HashSet<String> getParticipants(Long id) throws Exception {
        HashSet<String> participants = new HashSet<String>();

        Call callForPathwayParticipants = null;
        callForPathwayParticipants = createCall("listPathwayParticipantsForId");
        Object[] rtn = (Object[]) callForPathwayParticipants.invoke(new Object[]{id});

        Call callByObject = null;
        callByObject = createCall("queryByObjects");
        rtn = (Object[]) callByObject.invoke(new Object[]{rtn});

        for (int i = 0; i < rtn.length; i++) {
            EventEntity entity = (EventEntity) rtn[i];
            String[] nameAndCompart = entity.getName().split(" \\[");
            if (nameAndCompart.length == 2 && !participants.contains(nameAndCompart[0])) {
                participants.add(nameAndCompart[0]);
            }
        }
        return participants;
    }

    /**
     * Return the nb max of object that can be send back by the Web Service.
     * This parameter is used when we asking for the list of all Pathways and Reactions localElementIDs
     *
     * @return int : The max size
     * @throws Exception
     */
    private int getMaxSizeInListObjects() throws Exception {
        Call call = createCall("getMaxSizeInListObjects");
        return (Integer) call.invoke(EMPTY_ARG);
    }

    /*******************************Reactome Web Service******************************************/
    /**
     * Reactome Web Service createCall class
     * enables to create a Call that can then be invoked to get data from the web service.
     *
     * @param callName
     * @return
     * @throws Exception
     */
    private Call createCall(String callName) throws ServiceException {
        if (caBIOService == null) {
            caBIOService = new Service(SERVICE_URL_NAME + "?wsdl",
                    new QName(SERVICE_URL_NAME,
                            "CaBioDomainWSEndPointService"));
        }
        String portName = "caBIOService";
        Call call = (Call) caBIOService.createCall(new QName(SERVICE_URL_NAME, portName),
                callName);
        registerTypeMappings(call);
        return call;
    }

    private void registerTypeMappings(Call call) {
        QName instanceNotFoundModel = new QName(SCHEMA_NAMESPACE_URI,
                "InstanceNotFoundException");
        call.registerTypeMapping(InstanceNotFoundException.class, instanceNotFoundModel,
                new BeanSerializerFactory(InstanceNotFoundException.class, instanceNotFoundModel),
                new BeanDeserializerFactory(InstanceNotFoundException.class, instanceNotFoundModel));
        QName reactomeAxisFaultModel = new QName(SCHEMA_NAMESPACE_URI,
                "ReactomeRemoteException");
        call.registerTypeMapping(ReactomeRemoteException.class, reactomeAxisFaultModel,
                new BeanSerializerFactory(ReactomeRemoteException.class, reactomeAxisFaultModel),
                new BeanDeserializerFactory(ReactomeRemoteException.class, reactomeAxisFaultModel));
        QName CatalystActivityModel = new QName(SCHEMA_NAMESPACE_URI,
                "CatalystActivity");
        call.registerTypeMapping(CatalystActivity.class, CatalystActivityModel,
                new BeanSerializerFactory(CatalystActivity.class, CatalystActivityModel),
                new BeanDeserializerFactory(CatalystActivity.class, CatalystActivityModel));
        QName ComplexModel = new QName(SCHEMA_NAMESPACE_URI,
                "Complex");
        call.registerTypeMapping(Complex.class, ComplexModel,
                new BeanSerializerFactory(Complex.class, ComplexModel),
                new BeanDeserializerFactory(Complex.class, ComplexModel));
        QName DatabaseCrossReferenceModel = new QName(SCHEMA_NAMESPACE_URI, "DatabaseCrossReference");
        call.registerTypeMapping(DatabaseCrossReference.class, DatabaseCrossReferenceModel,
                new BeanSerializerFactory(DatabaseCrossReference.class, DatabaseCrossReferenceModel),
                new BeanDeserializerFactory(DatabaseCrossReference.class, DatabaseCrossReferenceModel));
        QName EventModel = new QName(SCHEMA_NAMESPACE_URI, "Event");
        call.registerTypeMapping(Event.class, EventModel,
                new BeanSerializerFactory(Event.class, EventModel),
                new BeanDeserializerFactory(Event.class, EventModel));
        QName EventEntityModel = new QName(SCHEMA_NAMESPACE_URI, "EventEntity");
        call.registerTypeMapping(EventEntity.class, EventEntityModel,
                new BeanSerializerFactory(EventEntity.class, EventEntityModel),
                new BeanDeserializerFactory(EventEntity.class, EventEntityModel));
        QName EventEntitySetModel = new QName(SCHEMA_NAMESPACE_URI, "EventEntitySet");
        call.registerTypeMapping(EventEntitySet.class, EventEntitySetModel,
                new BeanSerializerFactory(EventEntitySet.class, EventEntitySetModel),
                new BeanDeserializerFactory(EventEntitySet.class, EventEntitySetModel));
        QName GeneOntologyModel = new QName(SCHEMA_NAMESPACE_URI, "GeneOntology");
        call.registerTypeMapping(GeneOntology.class, GeneOntologyModel,
                new BeanSerializerFactory(GeneOntology.class, GeneOntologyModel),
                new BeanDeserializerFactory(GeneOntology.class, GeneOntologyModel));
        QName GeneOntologyRelationshipModel = new QName(SCHEMA_NAMESPACE_URI, "GeneOntologyRelationship");
        call.registerTypeMapping(GeneOntologyRelationship.class, GeneOntologyRelationshipModel,
                new BeanSerializerFactory(GeneOntologyRelationship.class, GeneOntologyRelationshipModel),
                new BeanDeserializerFactory(GeneOntologyRelationship.class, GeneOntologyRelationshipModel));
        QName GenomeEncodedEntityModel = new QName(SCHEMA_NAMESPACE_URI, "GenomeEncodedEntity");
        call.registerTypeMapping(GenomeEncodedEntity.class, GenomeEncodedEntityModel,
                new BeanSerializerFactory(GenomeEncodedEntity.class, GenomeEncodedEntityModel),
                new BeanDeserializerFactory(GenomeEncodedEntity.class, GenomeEncodedEntityModel));
        QName ModifiedResidueModel = new QName(SCHEMA_NAMESPACE_URI, "ModifiedResidue");
        call.registerTypeMapping(ModifiedResidue.class, ModifiedResidueModel,
                new BeanSerializerFactory(ModifiedResidue.class, ModifiedResidueModel),
                new BeanDeserializerFactory(ModifiedResidue.class, ModifiedResidueModel));
        QName PathwayModel = new QName(SCHEMA_NAMESPACE_URI, "Pathway");
        call.registerTypeMapping(Pathway.class, PathwayModel,
                new BeanSerializerFactory(Pathway.class, PathwayModel),
                new BeanDeserializerFactory(Pathway.class, PathwayModel));
        QName PolymerModel = new QName(SCHEMA_NAMESPACE_URI, "Polymer");
        call.registerTypeMapping(Polymer.class, PolymerModel,
                new BeanSerializerFactory(Polymer.class, PolymerModel),
                new BeanDeserializerFactory(Polymer.class, PolymerModel));
        QName PublicationSourceModel = new QName(SCHEMA_NAMESPACE_URI, "PublicationSource");
        call.registerTypeMapping(PublicationSource.class, PublicationSourceModel,
                new BeanSerializerFactory(PublicationSource.class, PublicationSourceModel),
                new BeanDeserializerFactory(PublicationSource.class, PublicationSourceModel));
        QName ReactionModel = new QName(SCHEMA_NAMESPACE_URI, "Reaction");
        call.registerTypeMapping(Reaction.class, ReactionModel,
                new BeanSerializerFactory(Reaction.class, ReactionModel),
                new BeanDeserializerFactory(Reaction.class, ReactionModel));
        QName ReferenceChemicalModel = new QName(SCHEMA_NAMESPACE_URI, "ReferenceChemical");
        call.registerTypeMapping(ReferenceChemical.class, ReferenceChemicalModel,
                new BeanSerializerFactory(ReferenceChemical.class, ReferenceChemicalModel),
                new BeanDeserializerFactory(ReferenceChemical.class, ReferenceChemicalModel));
        QName ReferenceEntityModel = new QName(SCHEMA_NAMESPACE_URI, "ReferenceEntity");
        call.registerTypeMapping(ReferenceEntity.class, ReferenceEntityModel,
                new BeanSerializerFactory(ReferenceEntity.class, ReferenceEntityModel),
                new BeanDeserializerFactory(ReferenceEntity.class, ReferenceEntityModel));
        QName ReferenceGeneModel = new QName(SCHEMA_NAMESPACE_URI, "ReferenceGene");
        call.registerTypeMapping(ReferenceGene.class, ReferenceGeneModel,
                new BeanSerializerFactory(ReferenceGene.class, ReferenceGeneModel),
                new BeanDeserializerFactory(ReferenceGene.class, ReferenceGeneModel));
        QName ReferenceProteinModel = new QName(SCHEMA_NAMESPACE_URI, "ReferenceProtein");
        call.registerTypeMapping(ReferenceProtein.class, ReferenceProteinModel,
                new BeanSerializerFactory(ReferenceProtein.class, ReferenceProteinModel),
                new BeanDeserializerFactory(ReferenceProtein.class, ReferenceProteinModel));
        QName ReferenceRNAModel = new QName(SCHEMA_NAMESPACE_URI, "ReferenceRNA");
        call.registerTypeMapping(ReferenceRNA.class, ReferenceRNAModel,
                new BeanSerializerFactory(ReferenceRNA.class, ReferenceRNAModel),
                new BeanDeserializerFactory(ReferenceRNA.class, ReferenceRNAModel));
        QName ReferenceSequenceModel = new QName(SCHEMA_NAMESPACE_URI, "ReferenceSequence");
        call.registerTypeMapping(ReferenceSequence.class, ReferenceSequenceModel,
                new BeanSerializerFactory(ReferenceSequence.class, ReferenceSequenceModel),
                new BeanDeserializerFactory(ReferenceSequence.class, ReferenceSequenceModel));
        QName RegulationModel = new QName(SCHEMA_NAMESPACE_URI, "Regulation");
        call.registerTypeMapping(Regulation.class, RegulationModel,
                new BeanSerializerFactory(Regulation.class, RegulationModel),
                new BeanDeserializerFactory(Regulation.class, RegulationModel));
        QName RegulationTypeModel = new QName(SCHEMA_NAMESPACE_URI, "RegulationType");
        call.registerTypeMapping(RegulationType.class, RegulationTypeModel,
                new EnumSerializerFactory(RegulationType.class, RegulationTypeModel),
                new EnumDeserializerFactory(RegulationType.class, RegulationTypeModel));
        QName RegulatorModel = new QName(SCHEMA_NAMESPACE_URI, "Regulator");
        call.registerTypeMapping(Regulator.class, RegulatorModel,
                new BeanSerializerFactory(Regulator.class, RegulatorModel),
                new BeanDeserializerFactory(Regulator.class, RegulatorModel));
        QName SmallMoleculeEntityModel = new QName(SCHEMA_NAMESPACE_URI, "SmallMoleculeEntity");
        call.registerTypeMapping(SmallMoleculeEntity.class, SmallMoleculeEntityModel,
                new BeanSerializerFactory(SmallMoleculeEntity.class, SmallMoleculeEntityModel),
                new BeanDeserializerFactory(SmallMoleculeEntity.class, SmallMoleculeEntityModel));
        QName SummationModel = new QName(SCHEMA_NAMESPACE_URI, "Summation");
        call.registerTypeMapping(Summation.class, SummationModel,
                new BeanSerializerFactory(Summation.class, SummationModel),
                new BeanDeserializerFactory(Summation.class, SummationModel));
        QName TaxonModel = new QName(SCHEMA_NAMESPACE_URI, "Taxon");
        call.registerTypeMapping(Taxon.class, TaxonModel,
                new BeanSerializerFactory(Taxon.class, TaxonModel),
                new BeanDeserializerFactory(Taxon.class, TaxonModel));
        QName arrayModel = new QName("http://www.reactome.org/caBIOWebApp/services/caBIOService", "ArrayOf_xsd_anyType");
        QName componentModel = new QName(DEFAULT_SCHEMA_NAMESPACE_URI, "anyType");
        call.registerTypeMapping(Object[].class, arrayModel,
                new ArraySerializerFactory(Object.class, componentModel),
                new ArrayDeserializerFactory());
        arrayModel = new QName(SCHEMA_NAMESPACE_URI, "ArrayOfAnyType");
        componentModel = new QName(DEFAULT_SCHEMA_NAMESPACE_URI, "anyType");
        call.registerTypeMapping(Object.class, arrayModel,
                new ArraySerializerFactory(Object.class, componentModel),
                new ArrayDeserializerFactory());
    }
}
