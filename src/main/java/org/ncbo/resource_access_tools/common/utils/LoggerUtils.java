package org.ncbo.resource_access_tools.common.utils;

import org.apache.log4j.Appender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Logger;
import org.ncbo.resource_access_tools.common.files.FileParameters;

import java.util.Enumeration;

/**
 * This utility class is used for creating log4j loggers.
 *
 * @author kyadav
 * @version $$
 */
class LoggerUtils {

    // log files for OBS workflow.
    private static final String OBS_LOG_FILE = "PopulateOBSTables";

    // log files for OBS workflow.
    private static final String OBR_LOG_FILE = "PopulateOBRTables";

    /**
     * This create log4j logger for resource access tool using root logger.
     * It adds file appender with specified filename to the logger which creates separate
     * log file for each resource access tool.
     *
     * @param clazz   Class object for Resource Access Tool
     * @param logFile String containing name of log file.
     * @return log4j Logger object
     */
    @SuppressWarnings("unchecked")
    public static Logger createRATSpecificLogger(Class clazz, String logFile) {
        Enumeration<Appender> appenders = Logger.getRootLogger()
                .getAllAppenders();
        // Add all appenders of root logger
        while (appenders.hasMoreElements()) {
            Appender appender = appenders.nextElement();

            //	For file appender create new appender with different log file
            if (appender instanceof DailyRollingFileAppender) {
                try {
                    // Creates new file appender
                    ((DailyRollingFileAppender) appender).setFile(FileParameters.obrLogFolder() + logFile);
                    ((DailyRollingFileAppender) appender).activateOptions();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return Logger.getLogger(clazz);
    }

    /**
     * This create log4j logger for OBS work flow using root logger.
     * It adds file appender with specified filename to the logger which creates separate
     * log file for each resource access tool.
     *
     * @param clazz
     * @param logFile String containing name of log file.
     * @return log4j Logger object
     */
    @SuppressWarnings("unchecked")
    public static Logger createOBRLogger(Class clazz) {

        Logger logger = Logger.getLogger(clazz);

        Enumeration<Appender> appenders = Logger.getRootLogger()
                .getAllAppenders();
        // Add all appenders of root logger
        while (appenders.hasMoreElements()) {
            Appender appender = appenders.nextElement();

            //	For file appender create new appender with different log file
            if (appender instanceof DailyRollingFileAppender) {
                try {
                    // Creates new file appender
                    DailyRollingFileAppender newFileAppender = new DailyRollingFileAppender(
                            appender.getLayout(), FileParameters.obrLogFolder() + OBR_LOG_FILE,
                            ((DailyRollingFileAppender) appender)
                                    .getDatePattern());
                    newFileAppender.activateOptions();

                    logger.addAppender(newFileAppender);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                logger.addAppender(appender);
            }
        }

        logger.setAdditivity(false);

        return logger;

    }

    /**
     * This create log4j logger for OBS work flow using root logger.
     * It adds file appender with specified filename to the logger which creates separate
     * log file for each resource access tool.
     *
     * @param clazz
     * @param logFile String containing name of log file.
     * @return log4j Logger object
     */
    @SuppressWarnings("unchecked")
    public static Logger createOBSLogger(Class clazz) {

        Enumeration<Appender> appenders = Logger.getRootLogger()
                .getAllAppenders();
        // Add all appenders of root logger
        while (appenders.hasMoreElements()) {
            Appender appender = appenders.nextElement();

            //	For file appender create new appender with different log file
            if (appender instanceof DailyRollingFileAppender) {
                try {
                    // Creates new file appender
                    ((DailyRollingFileAppender) appender).setFile(FileParameters.obsLogFolder() + OBS_LOG_FILE);
                    ((DailyRollingFileAppender) appender).activateOptions();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return Logger.getLogger(clazz);
    }
}
