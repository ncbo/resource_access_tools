package org.ncbo.resource_access_tools.dao;

import org.ncbo.resource_access_tools.dao.concept.ConceptDao;
import org.ncbo.resource_access_tools.dao.context.ContextDao;
import org.ncbo.resource_access_tools.dao.dictionary.DictionaryDao;
import org.ncbo.resource_access_tools.dao.execution.ExecutionDao;
import org.ncbo.resource_access_tools.dao.map.MapDao;
import org.ncbo.resource_access_tools.dao.ontology.ObrOntologyDao;
import org.ncbo.resource_access_tools.dao.ontology.OntologyDao;
import org.ncbo.resource_access_tools.dao.relation.RelationDao;
import org.ncbo.resource_access_tools.dao.resource.ResourceDao;
import org.ncbo.resource_access_tools.dao.semantic.LSemanticTypeDao;
import org.ncbo.resource_access_tools.dao.semantic.SemanticTypeDao;
import org.ncbo.resource_access_tools.dao.statistics.StatisticsDao;
import org.ncbo.resource_access_tools.dao.term.TermDao;

/**
 * The interface {@code DaoFactory} holds singleton data access objects for obs data tables and
 * common resource index tables. It includes resource index data access object for context , resource,
 * statistics and dictionary tables. Also it holds OBS data access objects for ontology table, concept table, term table, mapping table,
 * relation table.
 *
 * <p>This interface insures the only single object is created for above tables throughout the application.
 *
 * @author Kuladip Yadav
 */
public interface DaoFactory {

	/** Singleton data access object for dictionary table. */
	public static final DictionaryDao dictionaryDao = DictionaryDao.getInstance();

	/** Singleton data access object for ontology table. */
	public static final OntologyDao ontologyDao = OntologyDao.getInstance();

	/** Singleton data access object for concept table. */
	public static final ConceptDao conceptDao = ConceptDao.getInstance();

	/** Singleton data access object for term table. */
	public static final TermDao termDao = TermDao.getInstance();

	/** Singleton data access object for mapping table. */
	public static final MapDao mapDao = MapDao.getInstance();

	/** Singleton data access object for relation table. */
	public static final RelationDao relationDao = RelationDao.getInstance();

	/** Singleton data access object for semantic Type table. */
	public static final SemanticTypeDao semanticTypeDao = SemanticTypeDao.getInstance();

	/** Singleton data access object for l semantic Type table. */
	public static final LSemanticTypeDao lSemanticTypeDao = LSemanticTypeDao.getInstance();

	/** Singleton data access object for context table. */
	public static final ContextDao contextTableDao = ContextDao.getInstance();

	/** Singleton data access object for resource table. */
	public static final ResourceDao resourceTableDao = ResourceDao.getInstance();

	/** Singleton data access object for statistics table. */
	public static final StatisticsDao statisticsDao = StatisticsDao.getInstance();

	/** Singleton data access object for obr ontology table. */
	public static final ObrOntologyDao obrOntologyDao = ObrOntologyDao.getInstance();

	/** Singleton data access object for obr execution table. */
	public static final ExecutionDao executionDao = ExecutionDao.getInstance();

}
