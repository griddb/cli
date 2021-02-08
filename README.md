# GridDB CLI

## Overview

The GridDB CLI provides command line interface tool to manage GridDB cluster operations and data operations.

## Operating environment

Building and program execution are checked in the environment below.

    OS: CentOS 7.6(x64) 
    GridDB CLI: V4.6.0 CE(Community Edition)
    Java: Java™ SE Development Kit 8

## Quick start

 We have confirmed the operation on CentOS 7.6 , CentOS 8.1

### Preparations

Install GridDB Server with RPM or DEB package.

Install GridDB Java Client library and JDBC Driver.  
And place gridstore.jar(GridDB Java Client library) and gridstore-jdbc.jar(GridDB JDBC Driver) under common/lib directory on project directory.

### Build

Run the make command like the following:
    
    $ ant

and the following file is created under `release/` folder. 
    
    griddb-cli.jar

### Start GridDB CLI

    $ CP=.
    $ CP=$CP:common/lib/commons-io-2.4.jar:release/griddb-cli.jar:common/lib/gridstore.jar:common/lib/gridstore-jdbc.jar:common/lib/jackson-annotations-2.2.3.jar:common/lib/jackson-core-2.2.3.jar:common/lib/jackson-databind-2.2.3.jar:common/lib/javax.json-1.0.jar:common/lib/jersey-client-1.17.1.jar:common/lib/jersey-core-1.17.1.jar:common/lib/orion-ssh2-214.jar:lib/commons-beanutils-1.9.3.jar:lib/commons-cli-1.2.jar:lib/commons-collections-3.2.2.jar:lib/commons-lang3-3.5.jar:lib/commons-logging-1.2.jar:lib/jline-3.17.1.jar:lib/logback-classic-1.0.13.jar:lib/logback-core-1.0.13.jar:lib/opencsv-3.9.jar:lib/slf4j-api-1.7.5.jar
    $ java -Xmx1024m -Dlogback.configurationFile=gs_sh_logback.xml -classpath "$CP:$CLASSTH"  com.toshiba.mwcloud.gs.tools.shell.GridStoreShell $*
    gs> version
    gs_sh-ce version 4.6.0

## Document

  Refer to the file below for more detailed information.  
  - [Specification_en.md](Specification_en.md)
  - [Specification_ja.md](Specification_ja.md)

## Community
  * Issues  
    Use the GitHub issue function if you have any requests, questions, or bug reports. 
  * PullRequest  
    Use the GitHub pull request function if you want to contribute code.
    You'll need to agree GridDB Contributor License Agreement(CLA_rev1.1.pdf).
    By using the GitHub pull request function, you shall be deemed to have agreed to GridDB Contributor License Agreement.

## License
  The GridDB CLI source license is Apache License, version 2.0.  

