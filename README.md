<img src="https://griddb.org/brand-resources/griddb-logo/png/color.png" align="center" height="240" alt="GridDB"/>

# GridDB CLI

## Overview

The GridDB CLI provides command line interface tool to manage GridDB cluster operations and data operations.

## Operating environment

Building and program execution are checked in the environment below.

    OS: CentOS 7.9(x64) / Ubuntu 20.04
    GridDB Server: V5.0 CE(Community Edition)
    Java: Java™ SE Development Kit 8

## Quick start from CLI Source Code

### Preparations

Install GridDB Server with RPM or DEB package.

When you build with ant, please take the following steps.

    Install GridDB Java Client library and JDBC Driver.  
    And place gridstore.jar(GridDB Java Client library) and gridstore-jdbc.jar(GridDB JDBC Driver) under common/lib directory on project directory.

### Build

Run the make command like the following:
    
    $ ant

Or build with gradle:

    $ ./gradlew build

and the following file is created under `release/` folder. 
    
    griddb-cli.jar

### Start GridDB CLI

  Run GridDB CLI after build with ant:

    $ CP=.
    $ CP=$CP:common/lib/commons-io-2.4.jar:release/griddb-cli.jar:common/lib/gridstore.jar:common/lib/gridstore-jdbc.jar:common/lib/jackson-annotations-2.2.3.jar:common/lib/jackson-core-2.2.3.jar:common/lib/jackson-databind-2.2.3.jar:common/lib/javax.json-1.0.jar:common/lib/jersey-client-1.17.1.jar:common/lib/jersey-core-1.17.1.jar:common/lib/orion-ssh2-214.jar:lib/commons-beanutils-1.9.3.jar:lib/commons-cli-1.2.jar:lib/commons-collections-3.2.2.jar:lib/commons-lang3-3.5.jar:lib/commons-logging-1.2.jar:lib/jline-3.17.1.jar:lib/logback-classic-1.0.13.jar:lib/logback-core-1.0.13.jar:lib/opencsv-3.9.jar:lib/slf4j-api-1.7.5.jar
    $ java -Xmx1024m -Dlogback.configurationFile=gs_sh_logback.xml -classpath "$CP:$CLASSPATH"  com.toshiba.mwcloud.gs.tools.shell.GridStoreShell $*
    gs> version
    gs_sh-ce version 5.0.0

  Run GridDB CLI after build with gradle:

    $ CP=.
    $ CP=$CP:release/griddb-cli.jar
    $ java -Xmx1024m -Dlogback.configurationFile=gs_sh_logback.xml -classpath "$CP:$CLASSPATH"  com.toshiba.mwcloud.gs.tools.shell.GridStoreShell $*
    gs> version
    gs_sh-ce version 5.0.0

## Quick start from CLI Package


```
(CentOS)
$ rpm -ivh griddb-X.X.X-linux.x86_64.rpm
$ rpm -ivh griddb-ce-cli-X.X.X-linux.x86_64.rpm
$ gs_sh
gs> version
gs_sh-ce version 5.0.0

(Ubuntu)
$ dpkg -i griddb_x.x.x_amd64.deb
$ dpkg -i griddb-cli_X.X.X_amd64.deb
$ gs_sh
gs> version
gs_sh-ce version 5.0.0

Note: - X.X.X is the GridDB version.
      - {release} is the GridDB release version.
```

- Note: 
  - We can download the last version of `griddb-X.X.X-linux.x86_64.rpm`, `griddb_x.x.x_amd64.deb` at https://github.com/griddb/griddb/releases/ .

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

