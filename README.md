# Resource Access Tools

Resource Access Tools (RATs) are used to generate data as the first step in the NCBO Resource Index Workflow. The code was originally written by Optra as a part of a larger [project](https://github.com/ncbo/resource_index_workflow) that contained both the RATs and the Population (Annotation) code.  The code base has since been split into two separate projects:

- Population code: [ncbo/resource_index](https://github.com/ncbo/resource_index)
- RAT code: [ncbo/resource_access_tools](https://github.com/ncbo/resource_access_tools) (this repo)

When the code base was separated, we attempted to remove anything that didn't directly pertain to RATs from this repository. There are still some questions around where the source data for the RAT process lives, e.g., the SOAP services for data retrieval from NCBI and PharmGKB are retired.

The PubMed RAT was reimplemented in October, 2016 and is now located in a separate repository ([pubmed_xml2rdbms](https://github.com/ncbo/pubmed_xml2rdbms)).

## Config

There is a configuration file located in `src/main/filters` that allows you to designate the Resource to process and database username and password information. You shouldn't need to tweak this much, mainly just these two settings unless you hit problems.

## Local Maven External Libraries

There are many specialized libraries that the RATs use to access biomedical resources which aren't available in a hosted Maven repository. Though it's against the "Maven spirit", those libraries have been included in the git repo. However, to make them accessible to Maven you will need to run a bash script (see below) that installs them into your local Maven repo (typically located in `~/.m2`).

- cd external_lib
- ./maven_local_install.sh

## Build

You can use Maven to compile and package a runnable jar file. There is a jar produced that contains all dependencies to be used for distribution.

- mvn package

## Run

Running the project requires access to a MySQL database (configured as above) and the source files for the RATs you are going to process. Check the source files for each RAT for more information.

- java -jar target/resource-access-tools-1.0-SNAPSHOT-jar-with-dependencies.jar
