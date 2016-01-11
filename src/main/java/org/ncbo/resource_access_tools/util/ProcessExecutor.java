package org.ncbo.resource_access_tools.util;

import org.apache.log4j.Logger;
import org.ncbo.resource_access_tools.util.helper.StringHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * This is utility class for executing process in java
 *
 * @author Kuladip Yadav
 */
public class ProcessExecutor implements StringHelper {

    private final Logger logger;


    public ProcessExecutor(Logger logger) {
        super();
        this.logger = logger;
    }


    /**
     * This method execute base command with given parameter.
     *
     * @param baseCommand
     * @param parameters
     * @return
     * @throws Exception
     */
    public static HashMap<Integer, String> executeCommand(String baseCommand, String... parameters) throws Exception {
        Runtime runtime = Runtime.getRuntime();
        Process process = null;

        StringBuffer command = new StringBuffer();
        command.append(baseCommand);
        command.append(BLANK_SPACE);
        for (int i = 0; i < parameters.length; i++) {
            command.append(parameters[i]);
            command.append(BLANK_SPACE);
        }

        process = runtime.exec(command.toString());

        BufferedReader resultReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String resultLine = EMPTY_STRING;
        HashMap<Integer, String> lines = new HashMap<Integer, String>();
        int line_nb = 0;
        // Tab separated string containing id and name of organism.
        while ((resultLine = resultReader.readLine()) != null) {
            lines.put(line_nb, resultLine);
            line_nb++;
        }
        resultReader.close();

        return lines;

    }

    /**
     * This method execute master/slave synchronization script
     *
     * @param syncScriptPath
     * @param resourceIds
     */
    public void executeShellScript(String scriptPath, boolean withSudo, boolean lowerCaseParameter, boolean replicateObsTables, String... parameters) {
        Runtime runtime = Runtime.getRuntime();
        Process process = null;
        BufferedReader resultReader = null;
        BufferedReader errorReader = null;

        StringBuffer command = new StringBuffer();
        if (withSudo) {
            command.append("sudo");
            command.append(BLANK_SPACE);
        }
        command.append(scriptPath);
        command.append(BLANK_SPACE);

        if (replicateObsTables) {
            command.append("replicateObstables");
            command.append(BLANK_SPACE);
        }

        for (int i = 0; i < parameters.length; i++) {
            if (lowerCaseParameter) {
                command.append(parameters[i].toLowerCase());
            } else {
                command.append(parameters[i]);
            }

            if (i < parameters.length - 1) {
                command.append(BLANK_SPACE);
            }

        }

        try {
            process = runtime.exec(command.toString());
            resultReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String resultLine = EMPTY_STRING;

            while ((resultLine = resultReader.readLine()) != null) {
                logger.info(resultLine);
            }
            while ((resultLine = errorReader.readLine()) != null) {
                logger.error(resultLine);
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            logger.error("Problem in executing shell script ", e);
        } finally {
            if (resultReader != null) {
                try {
                    resultReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (errorReader != null) {
                try {
                    errorReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        }
    }


}
