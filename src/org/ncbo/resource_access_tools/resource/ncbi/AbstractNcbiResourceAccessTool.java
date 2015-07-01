package org.ncbo.resource_access_tools.resource.ncbi;

import gov.nih.nlm.ncbi.www.soap.eutils.EUtilsServiceLocator;
import gov.nih.nlm.ncbi.www.soap.eutils.EUtilsServiceSoap;
import gov.nih.nlm.ncbi.www.soap.eutils.esearch.ESearchRequest;
import gov.nih.nlm.ncbi.www.soap.eutils.esearch.ESearchResult;
import gov.nih.nlm.ncbi.www.soap.eutils.esearch.WarningListType;
import gov.nih.nlm.ncbi.www.soap.eutils.esummary.ItemType;

import java.rmi.RemoteException;
import java.util.HashSet;

import javax.xml.rpc.ServiceException;

import obs.obr.populate.Structure;
import obs.obr.populate.Element.BadElementStructureException;

import org.ncbo.resource_access_tools.resource.ResourceAccessTool;

public abstract class AbstractNcbiResourceAccessTool extends ResourceAccessTool {

	private EUtilsServiceLocator toolService;
	private EUtilsServiceSoap toolEutils;

	protected static final String EUTILS_EMAIL = "jonquet@stanford.edu";
	protected static final String EUTILS_TOOL = "ontrez";
	protected static final int EUTILS_MAX = 500;

	protected static final String UID_COLUMN = "uid";

	//Optra : Flag for whether eutils support data paramater
	protected boolean supportDate;

	// Number of days from last update.
	protected Integer numberOfDays;

	protected AbstractNcbiResourceAccessTool(String resourceName, String resourceID, Structure resourceStructure ){
		super(resourceName, resourceID, resourceStructure);
		this.toolService = new EUtilsServiceLocator();
		try {
			this.toolEutils = this.toolService.geteUtilsServiceSoap();
		} catch (ServiceException e) {
			 logger.error("** PROBLEM ** Cannot create the EUtils Web service", e);
		}
	}

	protected EUtilsServiceLocator getToolService() {
		return toolService;
	}

	protected EUtilsServiceSoap getToolEutils() {
		return toolEutils;
	}

	protected abstract String getEutilsDB();

	protected abstract String getEutilsTerm();

	protected int eutilsUpdateAll(String columnToUse){
		int nbElement = 0;
		logger.info("Updating " + this.getToolResource().getResourceName() + " elements with ESearch...");
		// gets all the UIDs with the right query on EUtils
		HashSet<String> allUIDs = this.allUIDs();
		// gets the element already in the corresponding _ET and keeps only the difference
		HashSet<String> allUIDsinET;
		if(columnToUse==null){
			allUIDsinET = resourceUpdateService.getAllLocalElementIDs();
		}
		else{
			allUIDsinET = resourceUpdateService.getAllValuesByColumn(this.getToolResource().getResourceId().toLowerCase()+ "_"+columnToUse);
		}
		allUIDs.removeAll(allUIDsinET);
		try {
			nbElement = this.updateElementTableWithUIDs(allUIDs);
		} catch (BadElementStructureException e) {
			 logger.error("** PROBLEM ** Cannot update " + this.getToolResource().getResourceName() +" because of a Structure problem.", e);
		}
		return nbElement;
	}

	private HashSet<String> allUIDs(){
		HashSet<String> allUIDs = new HashSet<String>();
		ESearchRequest esearchRequest = new ESearchRequest();
		esearchRequest.setEmail(EUTILS_EMAIL);
		esearchRequest.setTool(EUTILS_TOOL);
		esearchRequest.setDb(this.getEutilsDB());
		esearchRequest.setTerm(this.getEutilsTerm());

		//Optra : if supportDate is set then add rel date parameter to E search request
		if(supportDate){
			if(numberOfDays != null && numberOfDays.intValue() >0){
				esearchRequest.setReldate(numberOfDays.toString());
			}else{

				 return allUIDs;
			}

		}

		ESearchResult esearchResult;
		try {
			esearchResult = this.toolEutils.run_eSearch(esearchRequest);
			int resultsCount = Integer.parseInt(esearchResult.getCount());
			logger.info("Nb of results: " + resultsCount + " for term: " + this.getEutilsTerm());
			String[] resultTab;
			Integer max;
			for(Integer i=0; i<resultsCount; i+=EUTILS_MAX){
				max = EUTILS_MAX;
				if(resultsCount-i<EUTILS_MAX){max=resultsCount-i;}
				esearchRequest.setRetMax(max.toString());
				esearchRequest.setRetStart(i.toString());
				esearchResult = this.toolEutils.run_eSearch(esearchRequest);
				//logger.info("ESearch call...");
				resultTab = esearchResult.getIdList();
				//esearchResult.
				if (resultTab != null){
					for(int j=0; j<resultTab.length; j++){
						//logger.info("UID: " + resultTab[j]);
						allUIDs.add(resultTab[j]);
					}
				}
			}
		} catch (RemoteException e) {
			 logger.error("** PROBLEM ** Cannot get all UIDs for " + this.getToolResource().getResourceName() +" using ESearch.", e);
		}
		return allUIDs;
	}

	protected abstract int updateElementTableWithUIDs(HashSet<String> UIDs) throws BadElementStructureException;

	@Override
	public HashSet<String> queryOnlineResource(String query){
		HashSet<String> answerUIDs = new HashSet<String>();
		ESearchRequest esearchRequest = new ESearchRequest();
		esearchRequest.setEmail(EUTILS_EMAIL);
		esearchRequest.setTool(EUTILS_TOOL);
		esearchRequest.setDb(this.getEutilsDB());
		esearchRequest.setTerm(this.stringToNCBITerm(query));

		ESearchResult esearchResult;
		try {
			esearchResult = this.toolEutils.run_eSearch(esearchRequest);
			int resultsCount = Integer.parseInt(esearchResult.getCount());

			WarningListType errorlist = esearchResult.getWarningList();
			if (errorlist != null){
				String[] pnf = errorlist.getQuotedPhraseNotFound();
				if(pnf != null && pnf.length>0){
					//logger.error("quoted PhraseNotFound[0] : " + errorlist.getQuotedPhraseNotFound(0));
				}
			}
			else{
				//logger.info("Querying online for '" + query + "' returns " + resultsCount + " elements.");
				String[] resultTab;
				Integer max;
				for(Integer i=0; i<resultsCount; i+=EUTILS_MAX){
					max = EUTILS_MAX;
					if(resultsCount-i<EUTILS_MAX){max=resultsCount-i;}
					esearchRequest.setRetMax(max.toString());
					esearchRequest.setRetStart(i.toString());
					esearchResult = this.toolEutils.run_eSearch(esearchRequest);
					//logger.info("ESearch call...");
					resultTab = esearchResult.getIdList();
					if (resultTab != null){
						for(int j=0; j<resultTab.length; j++){
							//logger.info("UID: " + resultTab[j]);
							answerUIDs.add(resultTab[j]);
						}
					}
				}
			}
		} catch (RemoteException e) {
			 logger.error("** PROBLEM ** Cannot query online resource " + this.getToolResource().getResourceName() +" using ESearch for query: '" + query + "'.", e);
		}
		return answerUIDs;
	}

	protected String stringToNCBITerm(String query){
		return "%22" + query.replaceAll(" ", "+") + "%22";
	}

	protected String getItemTypeContent(ItemType itemType, String separator){
		String contentType =EMPTY_STRING;
		int index;
		if(itemType.getItem() == null){
			if(itemType.getItemContent()!= null){
				contentType = itemType.getItemContent();
			}
		}else{
			ItemType[] subItemTypes = itemType.getItem();
			index=0;
			for (ItemType subItemType : subItemTypes) {
				contentType += getItemTypeContent(subItemType, separator);
				if(index<subItemTypes.length-1){
					contentType += separator;
				}
				index++;
			}


		}
		return contentType;
	}

	protected String getItemTypeContent(ItemType itemType){
		return getItemTypeContent(itemType, BLANK_SPACE);
	}


}
