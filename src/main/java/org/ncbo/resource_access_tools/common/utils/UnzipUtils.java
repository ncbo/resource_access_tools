package org.ncbo.resource_access_tools.common.utils;

import org.apache.log4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * UnzipUtils is responsible for downloading data
 * Extracting that zip file as text file to local path.
 * Removing the local text file after finishing the execution.
 *
 * @author palanisamy
 * @version June 22th, 2009
 */
public class UnzipUtils {

    // Logger for UnzipUtils
    private static final Logger logger = Logger.getLogger(UnzipUtils.class);
    //text file name
    private static String extractedFileName;
    //Buffer size
    private static final int BUFFER = 1024;

    /**
     * Extracts the zip file
     */
    public static void extractZipFile(String zipFileName) {
        logger.info("Downloading and extracting Zip file from URL: " + zipFileName + " to local path...");

        //Separating zipFileName
        String[] fileName = (zipFileName.substring(zipFileName.lastIndexOf("/"))).replace("/", "").split("\\.");
        String exten;
        //checks filename extends is zip or not.
        if (fileName[1].equals("zip")) {
            exten = "txt";
        } else {
            exten = fileName[1];
        }
        //Set extracting file name as appends with file name and extends.
        setExtractedFileName(fileName[0], exten);
        logger.info("Extracting " + extractedFileName + " file to locally");
        try {
            //Create input and output streams
            URL rdfURL = new URL(zipFileName);
            HttpURLConnection connection = (HttpURLConnection) rdfURL.openConnection();
            try {
                connection.connect();
            } catch (IOException e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    logger.error("** PROBLEM ** HttpURL connection ", e1);
                }
                connection.connect();
            }
            ZipInputStream inStream = new ZipInputStream(connection.getInputStream());
            OutputStream outStream = new FileOutputStream(extractedFileName);
            byte[] buffer = new byte[BUFFER];
            int nrBytesRead;

            //Get next zip entry and start reading data
            if ((inStream.getNextEntry()) != null) {
                while ((nrBytesRead = inStream.read(buffer)) > 0) {
                    outStream.write(buffer, 0, nrBytesRead);
                }
            }

            //Finish off by closing the streams
            outStream.close();
            inStream.close();

        } catch (IOException ex) {
            logger.error("** PROBLEM ** Extracting text file ", ex);
        }
    }

    /**
     * Unzip folders and  files
     *
     * @param inFile
     * @param outFile
     * @return outfile name
     */
    public static File unzip(String inFile, String outFile) {
        File outFolder = new File(outFile);
        try {
            //Create input and output streams
            URL rdfURL = new URL(inFile);
            HttpURLConnection connection = (HttpURLConnection) rdfURL.openConnection();
            try {
                connection.connect();
            } catch (IOException e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    logger.error("** PROBLEM ** HttpURL connection ", e1);
                }
                connection.connect();
            }
            //Creates out folder directory
            outFolder.mkdir();

            //Set extracting file name as appends with file name and extends.
            extractedFileName = outFolder.getAbsolutePath();
            logger.info("Extracting " + extractedFileName + " folders and files locally.");

            BufferedOutputStream out;
            ZipInputStream in = new ZipInputStream(new BufferedInputStream(connection.getInputStream()));
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                //for each entry to be extracted
                String entryName = entry.getName();
                File newFile = new File(entryName);
                String directory = newFile.getParent();
                if (directory != null) {
                    if (!new File(directory).exists())
                        new File(outFolder.getAbsolutePath() + "/" + directory).mkdir();
                }
                if (entry.toString().contains("/") && entry.getSize() > 0) {
                    int count;
                    byte data[] = new byte[BUFFER];
                    // write the files to the disk
                    logger.info("Extracting " + outFolder.getPath() + "/" + entry.getName() + " file...");
                    out = new BufferedOutputStream(new FileOutputStream(outFolder.getPath() + "/" + entry.getName()), BUFFER);
                    while ((count = in.read(data, 0, BUFFER)) != -1) {
                        out.write(data, 0, count);
                    }
                    cleanUp(out);
                }
            }
            cleanUp(in);
            return outFolder;
        } catch (Exception e) {
            logger.error("** PROBLEM ** Extracting " + outFolder.getName() + " file ", e);
            return outFolder;
        }
    }

    /**
     * Close the inputstream object
     *
     * @param in
     * @throws Exception
     */
    private static void cleanUp(InputStream in) throws Exception {
        in.close();
    }

    /**
     * Close the outstream object
     *
     * @param out
     * @throws Exception
     */
    private static void cleanUp(OutputStream out) throws Exception {
        out.flush();
        out.close();
    }

    /**
     * set the name of extracted file name
     *
     * @param fileName
     * @param extension
     */
    private static void setExtractedFileName(String fileName, String extension) {
        extractedFileName = fileName + "." + extension;
    }

    /**
     * returns the local data file
     *
     * @return File
     */
    public static File getDataFile() {
        return new File(extractedFileName);
    }

    /**
     * Delete or remove data file from local directory.
     */
    public static void deleteDataFile() {
        logger.info("Deleting " + extractedFileName + " file from locally...");
        try {
            if (getDataFile().exists()) {
                getDataFile().delete();
            }
        } catch (Exception ex) {
            logger.error("** PROBLEM ** Deleting the local data file ", ex);
        }
    }

    /**
     * Deletes all files and sub directories under directory.
     * Returns true if all deletions were successful.
     * If a deletion fails, the method stops attempting to delete and returns false.
     *
     * @param dir
     * @return the status of file deletion
     */
    public static boolean deleteDir(File dir) {
        logger.info("Deleting " + dir.getName() + " folder or file from locally...");
        try {
            if (dir.isDirectory()) {
                String[] children = dir.list();
                for (int i = 0; i < children.length; i++) {
                    boolean success = deleteDir(new File(dir, children[i]));
                    if (!success) {
                        return false;
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("** PROBLEM ** Deleting the local data folders and files ", ex);
        }
        // The directory is now empty so delete it
        return dir.delete();
    }

}
