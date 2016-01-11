package org.ncbo.resource_access_tools.util;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.RollingFileAppender;

import java.util.Enumeration;

/**
 * This utility class is used for creating log4j loggers.
 *
 * @author kyadav
 * @version $$
 */
public class LoggerUtils {

    // log files for OBS workflow.
    private static final String OBR_LOG_FILE = MessageUtils.getMessage("obr.common.log.file");

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
            if (appender instanceof RollingFileAppender) {
                try {
                    // Creates new file appender
                    ((RollingFileAppender) appender).setFile(FileResourceParameters.resourceLogFolder() + logFile);
                    ((RollingFileAppender) appender).activateOptions();
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
            if (appender instanceof RollingFileAppender) {
                try {
                    // Creates new file appender
                    RollingFileAppender newFileAppender = new RollingFileAppender(
                            appender.getLayout(), FileResourceParameters.resourceLogFolder() + OBR_LOG_FILE
                    );
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


}
