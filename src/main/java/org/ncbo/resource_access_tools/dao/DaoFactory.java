package org.ncbo.resource_access_tools.dao;

import org.ncbo.resource_access_tools.dao.context.ContextDao;
import org.ncbo.resource_access_tools.dao.resource.ResourceDao;
import org.ncbo.resource_access_tools.dao.execution.ExecutionDao;

/**
 * The interface {@code DaoFactory} holds singleton data access objects for obs data tables and
 * common resource index tables. It includes resource index data access object for context , resource,
 * statistics and dictionary tables. Also it holds OBS data access objects for ontology table, concept table, term table, mapping table,
 * relation table.
 * <p/>
 * <p>This interface insures the only single object is created for above tables throughout the application.
 *
 * @author Kuladip Yadav
 */
public interface DaoFactory {
    ResourceDao resourceTableDao = ResourceDao.getInstance();
    ContextDao contextTableDao = ContextDao.getInstance();
    ExecutionDao executionDao = ExecutionDao.getInstance();
}
