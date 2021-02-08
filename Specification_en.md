# Cluster operation control command interpreter (gs_sh)

## Overview

The cluster operation control command interpreter (hereinafter referred to gs_sh) is a command line interface tool to manage GridDB cluster operations and data operations.

The following can be carried out by gs_sh.
-   Operation control of GridDB cluster
    -   Displaying status
-   GridDB cluster data operation
    -   Database and user management
    -   Displaying collection and trigger
    -   Index setting, deletion
    -   Search using a TQL/SQL

## Using gs_sh

### Preliminary preparations

Carry out the following preparations before using gs_sh.
-   GridDB setup
    -   Installation of GridDB node and client library
    -   User creation
    -   Network setting (GridDB cluster definition file, node definition file)

    　\* For details of the procedure, refer to ["GridDB Quick Start Guide"](https://github.com/griddb/docs-en/blob/master/manuals/GridDB_QuickStartGuide.md).

-   Remote connection setting using SSH
    -   This setting is necessary in order to connect to each GridDB node execution environment from the gs_sh execution environment as an OS user "gsadm".

    　See the manual of each OS for details on the SSH connection procedure.

### gs_sh start-up

There are two types of start modes in gs_sh.

- Startup in interactive mode
  - The interactive mode is started when gs_sh is executed without any arguments. The gs_sh prompt will appear, allowing sub-commands to be entered.

    ``` example
    $ gs_sh
    //execution of sub-command "version"
    gs> version
    gs_sh version 2.0.0
    ```

- Startup in batch mode
  - When the script file for user creation is specified in gs_sh, the system will be started in the batch mode. Batch processing of a series of sub-commands described in the script file will be carried out. gs_sh will terminate at the end of the batch processing.

    ``` example
    // specify the script file (test.gsh) and execute
    $ gs_sh test.gsh
    ```

[Memo]
- When a sub-command is started in the interactive mode,
  - a .gssh_history file is created in the home directory of the execution user and saved in the history.
  - Click the arrow key to display/execute up to 20 sub-commands started earlier.
  - Enter some of the sub-commands and click the Tab key to display a list of the sub-command input candidates.
- Execute gs_sh commands as the OS user "gsadm".
- During gs_sh startup, .gsshrc script files under the gsadm user home directory are imported automatically. The .gsshrc contents will also be imported to the destination from other script files.
- Extension of script file is gsh.
- A script file is described using the character code UTF-8.

　

## Definition of a GridDB cluster

The definition below is required in advance when executing a GridDB cluster operation control or data operation.
- Define each node data in the node variable
- Use the node variable to define the GridDB cluster configuration in the cluster variable
- Define the user data of the GridDB cluster

An explanation of node variables, cluster variables, and how to define user data is given below. An explanation of the definition of an arbitrary variable, display of variable definition details, and how to save and import variable definition details in a script file is also given below.

　

### Definition of node variable

Define the IP address and port no. of a GridDB node in the node variable.

- Sub-command

  | |
  |-|
  | setnode \<Node variable\> \<IP address\> \<Port no.\> [\<SSH port no.\>] |

- Argument

  | Argument      | Note                                                                           |
  |---------------|--------------------------------------------------------------------------------|
  | Node variable | Specify the node variable name. If the same variable name already exists, its definition will be overwritten. |
  | IP address    | Specify the IP address of the GridDB node (for connecting operation control tools).                 |
  | Port no.      | Specify the port no. of the GridDB node (for connecting operation control tools).                 |
  | SSH port no.  | Specify the SSH port number. Number 22 is used by default.                  |

- Example:

  ``` example
  //Define 4 GridDB nodes
  gs> setnode node0 192.168.0.1 10000
  gs> setnode node1 192.168.0.2 10000
  gs> setnode node2 192.168.0.3 10000
  gs> setnode node3 192.168.0.4 10000
  ```

[Memo]
- Only single-byte alphanumeric characters and the symbol "_" can be used in the node variable name.
- Check the GridDB node "IP address" and "port no. " for connecting the operation control tools in the node definition file of each tool.
  - IP address: /system/serviceAddress
  - Port no. : /system/servicePort

　　

### Definition of cluster variable

Define the GridDB cluster configuration in the cluster variable.

- Sub-command

  | | |
  |-|-|
  | Multicast method  | setcluster \<Cluster variable\> \<Cluster name\> \<Multicast address\> \<Port no.\> \[\<Node variable\> ...\] |
  | Fixed list method | setcluster \<Cluster variable\> \<Cluster name\> FIXED_LIST \<Address list of fixed list method\> \[\<Node variable\> ...\] |
  | Provider method   | setcluster \<Cluster variable\> \<Cluster name\> PROVIDER \<URL of provider method\> \[\<Node variable\> ...\]             |

- Argument

  | Argument      | Note                |
  |--------------------------------|-------------------------|
  | \<Cluster variable\> | Specify the cluster variable name. If the same variable name already exists, its definition will be overwritten.        |
  | cluster name | Specify the cluster name.                                                          |
  | Multicast address | \[For the multicast method\] Specify the GridDB cluster multicast address (for client connection).    |
  | Port no. | \[For the multicast method\] Specify the GridDB cluster multicast port no. (for client connection).  |
  | Node variable | Specify the nodes constituting a GridDB cluster with a node variable. When not performing operation management of GridDB clusters, the node variable may be omitted.               |
  | Address list of fixed list method | [For fixed list method] Specify the list of transaction addresses and ports. Example: 192.168.15.10:10001,192.168.15.11:10001<br>When the cluster configuration defined in the cluster definition file (gs_cluster.json) is a fixed list method, specify the transaction address and port list of /cluster/notificationMember in the cluster definition file. |
  | URL of provider method | [For the provider method] Specify the URL of the address provider. <br>If the cluster configuration defined in the cluster definition file (gs_cluster.json) is the provider method, specify the value of /cluster/notificationprovider/url in the cluster definition file.        |

- Example:

  ``` example
  //define the GridDB cluster configuration
  gs> setcluster cluster0 name 200.0.0.1 1000 $node0 $node1 $node2
  ```

[Memo]
- [For the provider method] Specify the URL of the address provider.Only single-byte alphanumeric characters and the symbol "_" can be used in the cluster variable name.
- Prepend a "$" to the node variable name.
- Check the "cluster name", "multicast address" and "port no." defined in a cluster variable in the cluster definition file of each GridDB node.
  - Cluster name: /cluster/clusterName
  - Multicast address: /transaction/notificationAddress
  - Port no.: /transaction/notificationPort

    *All settings in the cluster definition file of a node constituting a GridDB cluster have to be configured the same way. If the settings are configured differently, the cluster cannot be composed.

　

In addition, node variables can be added or deleted for a defined cluster variable.

- Sub-command

  | |
  |-|
  | modcluster \<Cluster variable\> add \| remove \<Node variable\> ... |

- Argument

  | Argument      | Note                                                                              |
  |----------------|-----------------------------------------------------------------------------------|
  | \<Cluster variable\> | Specify the name of a cluster variable to add or delete a node.                              |
  | add \| remove         | Specify "add" when adding node variables, and "remove" when deleting node variables. |
  | Node variable        | Specify node variables to add or delete a cluster variable.                      |

- Example:

  ``` example
  //Add a node to a defined GridDB cluster configuration
  gs> modcluster cluster0 add $node3
  //Delete a node from a defined GridDB cluster configuration
  gs> modcluster cluster0 remove $node3
  ```

[Memo]
- Prepend a "$" to the node variable name.

　

### Defining the SQL connection destination of a cluster

Define the SQL connection destination in the GridDB cluster configuration.  **This is set up only when using the GridDB NewSQL interface.**

- Sub-command

  | | |
  |-|-|
  | Multicast method  | setclustersql \<Cluster variable\> \<Cluster name\> \<SQL address\> \<SQL port no.\>             |
  | Fixed list method | setclustersql \<Cluster variable\> \<Cluster name\> FIXED_LIST \< SQL address list of fixed list method\>   |
  | Provider method   | setclustersql \<Cluster variable\> \<Cluster name\> PROVIDER \<URL of provider method\>                  |

- Argument

  | Argument      | Note                                      |
  |-----------------------------------|-------------------------------------------------------------------------------------|
  | \<Cluster variable\> | Specify the cluster variable name. If the same variable name already exists, the SQL connection data will be overwritten.      |
  | cluster name | Specify the cluster name.        |
  | SQL address | \[For multicast method\] Specify the reception address for the SQL client connection.     |
  | SQL port no. | \[For multicast method\] Specify the port no. for the SQL client connection.       |
  | SQL address list of fixed list method | \[For fixed list method\] Specify the list of transaction addresses and ports.  Example: 192.168.15.10:20001,192.168.15.11:20001<br>When the cluster configuration defined in the cluster definition file (gs_cluster.json) is a fixed list method, specify the sql address and port list of /cluster/notificationMember in the cluster definition file. |
  | URL of provider method | \[For the provider method\] Specify the URL of the address provider. <br>If the cluster configuration defined in the cluster definition file (gs_cluster.json) is the provider method, specify the value of /cluster/notificationprovider/url in the cluster definition file.            |

- Example:

  ``` example
  // Definition method when using both NoSQL interface and NewSQL interface to connect to a NewSQL server
  gs> setcluster    cluster0 name 239.0.0.1 31999 $node0 $node1 $node2
  gs> setclustersql cluster0 name 239.0.0.1 41999
  ```

[Memo]
- [For the provider method] Specify the URL of the address provider.Only single-byte alphanumeric characters and the symbol "_" can be used in the cluster variable name.
- When an existing cluster variable name is specified, only the section containing SQL connection data will be overwritten. When overwriting, the same method as the existing connection method needs to be specified.
- Execute only this command when using SQL only.
- Check the "SQL address" and "SQL port no." defined in a cluster variable in the cluster definition file of each GridDB node.
  - SQL address: /sql/notificationAddress
  - SQL port no.:/sql/notificationPort

　

### Definition of a user

Define the user and password to access the GridDB cluster.

- Sub-command

  | |
  |-|
  | setuser \<User name\> \<Password\>|

- Argument

  | Argument      | Note                                                                |
  |------------------|---------------------------------------------------------------------|
  | \<User name\>  | Specify the name of the user accessing the GridDB cluster.                  |
  | \<Password\>   | Specify the corresponding password.                                    |

- Example:

  ``` example
  //Define the user and password to access a GridDB cluster
  gs> setuser admin admin
  ```

[Memo]
- After a user is defined, the following variables are set.

  | Variable Name | Value    |
  |------------|-----------------|
  | user          | \<User name\>        |
  | password      | \<Password\>      |

- Multiple users cannot be defined. The user and password defined earlier will be overwritten. When operating multiple GridDB clusters in gs_sh, reset the user and password with the setuser sub-command every time the connection destination cluster is changed.

　　

### Definition of arbitrary variables

Define an arbitrary variable.

- Sub-command

  | |
  |-|
  | set \<Variable name\> [\<Value\>] |

- Argument

  | Argument      | Note                                                                         |
  |--------|------------------------------------------------------------------------------|
  | Variable Name | Specify the variable name.                                                         |
  | Value         | Specify the setting value. The setting value of the variable concerned can be cleared by omitting the specification. |

- Example:

  ``` example
  //Define variable
  gs> set GS_PORT 10000
  //Clear variable settings
  gs> set GS_PORT
  ```

[Memo]
- Node variable and cluster variable settings can also be cleared with the set sub-command.
- Only single-byte alphanumeric characters and the symbol "_" can be used in the variable name.

　

### Displaying the variable definition

Display the detailed definition of the specified variable.

- Sub-command

  | |
  |-|
  | show [\<Variable name\>] |

- Argument

  | Argument      | Note                                                                                    |
  |--------|-----------------------------------------------------------------------------------------|
  | Variable Name | Specify the name of the variable to display the definition details. If the name is not specified, details of all defined variables will be displayed. |


- Example:

  ``` example
  //Display all defined variables
  gs> show
  Node variable:
    node0=Node[192.168.0.1:10000,ssh=22]
    node1=Node[192.168.0.2:10000,ssh=22]
    node2=Node[192.168.0.3:10000,ssh=22]
    node3=Node[192.168.0.4:10000,ssh=22]
  Cluster variable:
    cluster0=Cluster[name=name,200.0.0.1:1000,nodes=(node0,node1,node2)]
  Other variables:
    user=admin
    password=*****
    ospassword=*****
  ```

[Memo]
- Password character string will not appear. Display replaced by "\*\*\*".

　

### Saving a variable definition in a script file

Save the variable definition details in the script file.

- Sub-command

  | |
  |-|
  | save [\<Script file name\>] |

- Argument

  | Argument      | Note                                                                                |
  |----------------------|-------------------------------------------------------------------------------------|
  | Script file name | Specify the name of the script file serving as the storage destination. Extension of script file is gsh. If the name is not specified, the data will be saved in the .gsshrc file in the gsadm user home directory.      |

- Example:

  ``` example
  //Save the defined variable in a file
  gs> save test.gsh
  ```

[Memo]
- If the storage destination script file does not exist, a new file will be created. If the storage destination script file exists, the contents will be overwritten.
- A script file is described using the character code UTF-8.
- Contents related to the user definition (user, password, gsadm password) will not be output to the script file.
- Contents in the .gsshrc script file will be automatically imported during gs_sh start-up.

　

### Executing a script file

Read and execute a script file.

- Sub-command

  | |
  |-|
  | load [\<Script file name\>] |

- Argument

  | Argument      | Note                                                                                 |
  |----------------------|--------------------------------------------------------------------------------------|
  | Script file name | Specify the script file to execute. <br>If the script file is not specified, the .gsshrc file in the gsadm user home directory will be imported again. |

- Example:

  ``` example
  //Execute script file
  gs> load test.gsh
  ```

[Memo]
- Extension of script file is gsh.
- A script file is described using the character code UTF-8.


### Synchronizing cluster and node variable definitions

Connect to the running GridDB cluster and automatically define a cluster variable and a node variable.

- Sub-command

  | |
  |-|
  | sync IP address port number　\[cluster variable name \[node variable\] \] |

- Argument

  | Argument      | Note                                                                                 |
  |----------------------|--------------------------------------------------------------------------------------|
  | IP address | Specify the IP address of a GridDB node participating in the GridDB cluster. |
  | port number | port number of a GridDB node （for connecting to the operation control tool) |
  | cluster variable name | Specify the cluster variable name. <br>If omitted, the cluster variable name is set to "scluster". |
  | node variable name | Specify the node variable name. <br>If omitted, the node variable name is set to "snodeX" where X is a sequential number. |

- Example:

  ``` example
  gs> sync 192.168.0.1 10040 mycluster mynode

  // Check the settings.
  gs> show
  Node variable:
  mynode1=Node[192.168.0.1:10040,ssh=22]
  mynode2=Node[192.168.0.2:10040,ssh=22]
  mynode3=Node[192.168.0.3:10040,ssh=22]
  mynode4=Node[192.168.0.4:10040,ssh=22]
  mynode5=Node[192.168.0.5:10040,ssh=22]
  Cluster variable:
  mycluster=Cluster[name=mycluster,mode=MULTICAST,transaction=239.0.0.20:31999,sql=239.0.0.20:41999,nodes=($mynode1,$mynode2,$mynode3,$mynode4,$mynode5)]

  // Save the settings
  gs> save
  ```

[Memo]
- This command can be run by administrative users only.
- Only single-byte alphanumeric characters and the symbol "_" can be used in the variable name.
- If you exit the command gs_sh, variables will be discarded. Save them before exiting using the subcommand save.
- If a variable exists with the same name as the one you are working on, it will be overwritten in all cases.


## GridDB cluster operation controls

The following operations can be executed as functions to manage GridDB cluster operations.
- Displaying SQL processing under execution
- Displaying executing event
- Displaying connection
- SQL cancellation

### Displaying SQL processing under execution

Display the SQL processing under execution.

- Sub-command

  | |
  |-|
  | showsql \<Query ID\> |

- Argument

  | Argument      | Note                                         |
  |------------|----------------------------------------------|
  | Query ID | ID to specify the SQL processing to be displayed. <br>When specified, display only the SQL processing information on specified query ID. <br>When not specified, display the list of SQL processes in progress. <br>Query ID can be obtained by displaying the SQL processing in progress. |

- Display item
  - elapsed time: Lapsed time calculated from the gs_sh start time and the system time of the terminal which is executing the gs_sh. (Unit: second)

- Example:
  ``` example
  gs[public]> showsql
    =======================================================================
    query id: e6bf24f5-d811-4b45-95cb-ecc643922149:3
    start time: 2019-04-02T06:02:36.93900
    elapsed time: 53
    database name: public
    application name: gs_admin
    node: 192.168.56.101:10040
    sql: INSERT INTO TAB_711_0101 SELECT a.id, b.longval FROM TAB_711_0001 a LEFT OU
      job id: e6bf24f5-d811-4b45-95cb-ecc643922149:3:5:0
      node: 192.168.56.101:10040
    #---------------------------
  ```
[Memo]
- For start time, the value that reflects the time zone set by the settimezone subcommand is displayed. If the time zone is not set, the value that reflects the default time zone value is displayed.

　
　
### Displaying executing event

Display the event list executed by the thread in each node in a cluster.

- Sub-command

  | |
  |-|
  | showevent |

- Display item
  - elapsed time: Lapsed time calculated from the gs_sh start time and the system time of the terminal which is executing the gs_sh. (Unit: second)

- Example:

  ``` example
  gs[public]> showevent
    =======================================================================
    worker id: 0
    start time: 2019-03-05T05:28:21.00000
    elapsed time: 1
    application name:
    node: 192.168.56.101:10040
    service type: TRANSACTION_SERVICE
    event type: PUT_MULTIPLE_ROWS
    cluster partition id: 5
    #---------------------------
  ```
[Memo]
- For start time, the value that reflects the time zone set by the settimezone subcommand is displayed. If the time zone is not set, the value that reflects the default time zone value is displayed.

　

### Displaying connection

Display the list of connections.

- Sub-command

  | |
  |-|
  | showconnection |

- Display item
  - elapsed time: Lapsed time calculated from the creation time and the system time of the terminal which is executing the gs_sh. (Unit: second)

- Example:

  ``` example
  gs[public]> showconnection
    =======================================================================
    application name: gs_admin
    creation time: 2019-04-02T06:09:42.52300 service type: TRANSACTION
    elapsed time: 106 node: 192.168.56.101:10001 remote: 192.168.56.101:56166
    dispatching event count: 5 sending event count: 5
    #---------------------------
  ```
[Memo]
- For creation time, the value reflecting the time zone set by the settimezone subcommand is displayed. If the time zone is not set, the value that reflects the default time zone value is displayed.

　

### SQL cancellation

Cancel the SQL processing in progress.

- Sub-command

  | |
  |-|
  | killsql \<query ID\> |

- Argument

  | Argument      | Note                                         |
  |------------|----------------------------------------------|
  | Query ID | ID to specify SQL processing to be canceled. <br>Can be obtained by displaying the SQL processing in progress. |

- Example:

  ``` example
  gs[public]> killsql 5b9662c0-b34f-49e8-92e7-7ca4a9c1fd4d:1
  ```

[Memo]
- Command can be executed by an administrator user only.

　

## Data operation in a database

To execute a data operation, there is a need to connect to the cluster subject to the operation.
Data in the database configured during the connection ("public" when the database name is omitted) will be subject to the operation.


### Connecting to a cluster

Establish connection to a GridDB cluster to execute a data operation.

- Sub-command

  | |
  |-|
  | connect \<Cluster variable\> [\<Database name\>] |

- Argument

  | Argument      | Note                                                     |
  |----------------|----------------------------------------------------------|
  | Cluster variable  | Specify a GridDB cluster serving as the connection destination by its cluster variable. |
  | \<Database name\> | Specify the database name.                             |

- Example:

  ``` example
  //connect to GridDB cluster
    //for NoSQL
    gs> connect $cluster1
    The connection attempt was successful(NoSQL).  
  gs[public]>
    
    gs> connect $cluster1 userDB
    The connection attempt was successful(NoSQL).  
  gs[userDB]>
    
    //For NewSQL (configure both NoSQL/NewSQL interfaces)
    gs> connect $cluster1
    The connection attempt was successful(NoSQL).  
  The connection attempt was successful(NewSQL). 
  gs[public]>
  ```

[Memo]
- Connect to the database when the database name is specified. Connect to the "public" database if the database name is omitted.
- If the connection is successful, the connection destination database name appears in the prompt.
- Prepend a "$" to the cluster variable name.
- When executing a data operation sub-command, it is necessary to connect to a GridDB cluster.
- If the SQL connection destination is specified (execution of setclustersql sub-command), SQL connection is also carried out.
- If the time zone setting is changed with the settimezone subcommand after executing the connect subcommand, the changed timezone setting is not reflected until the connect subcommand is executed again. After changing the time zone setting, execute the connect subcommand again.

　

### Search (TQL)

Execute a search and retain the search results.

- Sub-command

  | |
  |-|
  | tql \<Container name\> \<Query;\> |

- Argument

  | Argument      | Note                                                                |
  |------------|---------------------------------------------------------------------|
  | \<Container name\> | Specify the container subject to the search.                                |
  | Query;             | Specify the TQL command to execute. A semicolon (;) is required at the end of a TQL command. |

- Example:

  ``` example
  //execute a search
    gs[public]> tql c001 select *;
    5 results. (25 ms) (25 ms)
  ```

[Memo]
- When executing a data operation sub-command, it is necessary to connect to a GridDB cluster.
- A return can be inserted in the middle of a TQL command.
- Display the elapsed time of query as milliseconds.
- Retain the latest search result. Search results are discarded when a tql or sql sub-command is executed.
- See the chapter on TQL Syntax and Calculation Functions" in the ["GridDB TQL Reference"](https://github.com/griddb/docs-en/blob/master/manuals/GridDB_TQL_Reference.md) for the TQL details.

　

### SQL command execution

Execute an SQL command and retains the search result. 

- Sub-command

  | |
  |-|
  | sql <SQL command;> |

- Argument

  | Argument         | Note                                                                |
  |----------|---------------------------------------------------------------------|
  | \<SQL command;\> | Specify the SQL command to execute. A semicolon (;) is required at the end of the SQL command. |

- Example:

  ``` example
  gs[public]> sql select * from con1;          -> search for SQL
    10000 results. (52 ms)
    gs[public]> get 1                            -> display SQL results
    id,name
    ----------------------
    0,tanaka
    The 1 result has been acquired.
  ```

　

Sub-command name 'sql' can be omitted when the first word of SQL statement is one of the follows.
-   select update insert replace delete create drop alter grant revoke pragma explain

[Memo]
- Before executing a sql subcommand, there is a need to specify the SQL connection destination and perform a connection first.
- Retain the latest search result. Search results are discarded when a sql or tql sub-command is executed.
- Sub-command name 'sql' can not be omitted when specifying the 'set password' command or the command includes comments in header.
- Count the number of hits when querying.
- Display the elapsed time of query as milliseconds. It does not include the time of counting.
- The following results will appear depending on the type of SQL command.

  | Operation                   | Execution results when terminated normally                                                                  |
  |----------------------------|---------------------------------------------------------------------------------------|
  | Search SELECT               | Display the no. of search results found. Search results are displayed in sub-command get/getcsv/getnoprint. |
  | Update INSERT/UPDATE/DELETE | Display the no. of rows updated.                                                          |
  | DDL statement               | Nothing is displayed.                                                                  |

- See ["GridDB SQL Reference"](https://github.com/griddb/docs-en/blob/master/manuals/GridDB_SQL_Reference.md) for the SQL details.

　

### Getting search results

The following command gets the inquiry results and presents them in different formats. There are 3 ways to output the results as listed below.


(A) Display the results obtained in a standard output.

- Sub-command

  | |
  |-|
  | get [\<No. of acquires\>] |

- Argument

  | Argument         | Note                                                                             |
  |----------|----------------------------------------------------------------------------------|
  | No. of acquires | Specify the number of search results to be acquired. All search results will be obtained and displayed by default. |


(B) Save the results obtained in a file in the CSV format.

- Sub-command

  | |
  |-|
  | getcsv \<CSV file name\> [\<No. of acquires\>] |

- Argument

  | Argument      | Note                                                                                       |
  |------------|--------------------------------------------------------------------------------------------|
  | CSV file name   | Specify the name of the csv file where the search results are saved.                                                 |
  | No. of acquires | Specify the number of search results to be acquired. All search results will be obtained and saved in the file by default. |


(C) Results obtained will not be output.

- Sub-command

  | |
  |-|
  | getnoprint [\<No. of acquires\>] |

- Argument

  | Argument         | Note                                                                     |
  |----------|--------------------------------------------------------------------------|
  | No. of acquires | Specify the number of search results to be acquired. All search results will be obtained by default. |


Example:

  ``` example
  //検索を実行
  gs[public]> tql c001 select *;
  5 results.  

  //Get first result and display
  gs[public]> get 1
  name,status,count
  mie,true,2
  The 1 result has been acquired.  

  //Get second and third results and save them in a file
  gs[public]> getcsv /var/lib/gridstore/test2.csv 2
  The 2 results had been acquired.  

  //Get fourth result
  gs[public]> getnoprint 1
  The 1 result has been acquired.  

  //Get fifth result and display
  gs[public]> get 1
  name,status,count
  akita,true,45
  The 1 result has been acquired.
  ```

[Memo]
- When executing a data operation sub-command, it is necessary to connect to a GridDB cluster.
- Output the column name to the first row of the search results
- An error will occur if the search results are obtained when a search has not been conducted, or after all search results have been obtained or discarded.
- A NULL value is output by each command as follows.
  - get: (NULL)
  - getcsv: an unquoted empty string
- A value of TIMESTAMP is output as following format:
  - Date and time format: ISO8601 format
  - Time zone: Time zone value set by the settimezone subcommand, if no value is set, UTC is used
  - Example: 2018-11-07T12:30:00.417Z, 2019-05-01T09:30:00.000+09:00

　

### Getting the execution plan

Execute the specified TQL command and display the execution plan and actual measurement values such as the number of cases processed etc. Search is not executed.

- Sub-command

  | |
  |-|
  | tqlexplain \<Container name\> \<Query;\> |

- Argument

  | Argument      | Note                                                                          |
  |------------|-------------------------------------------------------------------------------|
  | \<Container name\> | Specify the target container.                                              |
  | Query;             | Specify the TQL command to get the execution plan. A semicolon (;) is required at the end of a TQL command. |

- Example:

  ``` example
  //Get an execution plan
  gs[public]> tqlexplain c001 select * ;
  0       0       SELECTION       CONDITION       NULL
  1       1       INDEX   BTREE   ROWMAP
  2       0       QUERY_EXECUTE_RESULT_ROWS       INTEGER 0
  ```

In addition, the actual measurement values such as the number of processing rows etc. can also be displayed together with the executive plan by actually executing the specified TQL command.

- Sub-command

  | |
  |-|
  | tqlanalyze \<Container name\> \<Query;\> |

- Argument

  | Argument      | Note                                                                          |
  |------------|-------------------------------------------------------------------------------|
  | \<Container name\> | Specify the target container.                                              |
  | Query;             | Specify the TQL command to get the execution plan. A semicolon (;) is required at the end of a TQL command. |

- Example:

  ``` example
  //Execute a search to get an execution plan
  gs[public]> tqlanalyze c001 select *;
  0       0       SELECTION       CONDITION       NULL
  1       1       INDEX   BTREE   ROWMAP
  2       0       QUERY_EXECUTE_RESULT_ROWS       INTEGER 5
  3       0       QUERY_RESULT_TYPE       STRING  RESULT_ROW_ID_SET
  4       0       QUERY_RESULT_ROWS       INTEGER 5
  ```

[Memo]
- When executing a data operation sub-command, it is necessary to connect to a GridDB cluster.
- Since search results are not retained, search results cannot be acquired and thus there is also no need to execute a tqlclose sub-command. When the search results are required, execute a query with the tql sub-command.
- A partitioned table (container) is not supported. If executed, an error will occur.

　

### Discarding search results

Close the tql and discard the search results saved.
- Sub-command

  | |
  |-|
  | tqlclose |

Close the query and discard the search results saved.
- Sub-command

  | |
  |-|
  | queryclose |

Example:

  ``` example
  //Discard search results
  gs[public]> tqlclose

  gs[public]> queryclose
  ```

[Memo]
- Search results are discarded at the following timing.
  - When a tqlclose or query close sub-command is executed
  - When executing a new search using a tql or sql sub-command
  - When disconnecting from a GridDB cluster using a disconnect sub-command
- An error will occur if search results are acquired (get sub-command, etc.) after they have been discarded.

　

### Disconnecting from a cluster

Disconnect from a GridDB cluster.

- Sub-command

  | |
  |-|
  | disconnect |

- Example:

  ``` example
  //Disconnect from a GridDB cluster
  gs[public]> disconnect
  gs>
  ```

[Memo]
- Retained search results are discarded.
- When disconnected, the connection database name will disappear from the prompt.

　

### Hit count setting

Set whether to execute count query when SQL querying.

- Sub-command

  | |
  |-|
  | sqlcount <Boolean> |

- Argument

  | Argument      | Note                       |
  |---------|--------------------------------------------------------------------------------------------------------------------------------------|
  | Boolean  | If FALSE is specified, gs_sh does not count the number of the result when querying by sql sub-command. And hit count does not be displayed. Default is TRUE. |

- Example:

  ``` example
  gs[public]> sql select * from mycontainer;
  25550 results. (33 ms)

  gs[public]> sqlcount FALSE

  gs[public]> sql select * from mycontainer;
  A search was executed. (33 ms)
  ```

[Memo]
- If FALSE is specified, the response will be faster instead of displaying no hit count. The execution time is not affected by this setting.

　

## Container management

This section explains the available sub-commands that can be used when performing container operations.  Connect to the cluster first before performing container management (sub-command connect).  The container in the connected database will be subject to the operation.

### Creating a container

Create a container.

- Sub-command (Simple version)

  | | |
  |-|-|
  | Container (collection)           | createcollection \<Container name\> \<Column name\> \<Column type\> \[\<Column name\> \<Column type\> ...\] |
  | Container (timeseries container) | createtimeseries \<Container name\> \<Compression method\> \<Column name\> \<Column type\> \[\<Column name\> \<Column type\> ...\] |

- Description of each argument

  | Argument      | Note                                                 |
  |----------------------|-----------------------------------------------------|
  | \<Container name\>  | Specify the name of the container to be created. |
  | Column name         | Specify the column name.                                 |
  | Column type         | Specify the column type.                           |
  | Compression method  | For time series data, specify the data compression method.       |

　

**Simplified version**

Specify the container name and column data (column name and type) to create the container. The compression type can also be specified for timeseries containers only.

- Specify "NO", "SS" for the compression method. Use the API if "HI" is specified.
- The collection will be created with a specified row key. The first column will become the row key.
- To specify a composite row key, a composite index, column constraints, and index names, use the SQL.

　

### Deleting container

Delete a container

- Sub-command

  | |
  |-|
  | dropcontainer \<Container name\> |

- Argument

  | Argument      | Note                                 |
  |------------|--------------------------------------|
  | \<Container name\> | Specify the name of the container to be deleted. |

- Example:
  ``` example
  gs[public]> dropcontainer　Con001
  ```

[Memo]
- A partitioned table (container) is not supported. If executed, an error will occur.
  - To drop partitioned table (container), use SQL.


### Registering a row

Register a row in a container

- Sub-command

  | |
  |-|
  | putrow container name value [value...]|

- Argument

  | Argument      | Note                                 |
  |------------|--------------------------------------|
  | container name | Specify the name of a container where a row is to be registered. |
  | value | Specify the value of a row to be registered, |
  
- Example:
  ``` example
  gs[public]> putrow mycontainer 'key1' 1 1.0
  gs[public]> putrow mycontainer 'key2' 2 2.0
  gs[public]> putrow mycontainer 'key3' 3 null

  // Check the results.
  gs[public]> tql mycontainer select *;
  3 results. (1 ms)

  gs[public]> get
  key,val1,val2
  key1,1,1.0
  key2,2,2.0
  key3,3,(NULL)
  3 results had been acquired.
  ```


### Deleting a row

Delete a row from a container

- Sub-command

  | |
  |-|
  | removerow container name row key value [row key value...]|

- Argument

  | Argument      | Note                                 |
  |------------|--------------------------------------|
  | container name | Specify the name of a container from which a row is to be deleted. |
  | value | Specify the row key value of a row to be deleted. |
  
- Example:
  ``` example
  gs[public]> removerow mycontainer 'key1'
  gs[public]> removerow mycontainer 'key2'

  // Check the results.
  gs[public]> tql mycontainer select *;
  1 results. (1 ms)
  
  gs[public]> get
  key,val1,val2
  key3,3,(NULL)
  1 results had been acquired.
  ```

[Memo]
- If a composite row key is set in the container, all the row keys must be specified.


### Displaying a container data

Display the container data.

- Sub-command

  | |
  |-|
  | showcontainer [\<Container name\>] |

- Argument

  | Argument      | Note                                                                               |
  |------------|------------------------------------------------------------------------------------|
  | \<Container name\> | Specify the container name to be displayed. Display a list of all containers if omitted. |


- Example:

  ``` example
  //display container list
  gs[public]> showcontainer
  Database : public
  Name                 Type        PartitionId
  ---------------------------------------------
  TEST_TIME_0001       TIME_SERIES           3
  TEST_TIME_0004       TIME_SERIES          12
  TEST_TIME_0005       TIME_SERIES          26
  cont003              COLLECTION           27
  TABLE_01             COLLECTION           58
  TEST_COLLECTION_0001 COLLECTION           79

  //display data of specified container
  gs[public]> showcontainer cont003
  Database    : public
  Name        : cont003
  Type        : COLLECTION
  Partition ID: 27
  DataAffinity: -
  
  Columns:
  No  Name                  Type            CSTR  RowKey
  ------------------------------------------------------------------------------
   0  col1                  INTEGER         NN    [RowKey]
   1  col2                  STRING
   2  col3                  TIMESTAMP
  
  Indexes:
  Name        :
  Type        : TREE
  Columns:
  No  Name
  --------------------------
   0  col1
  
  Name        :
  Type        : HASH
  Columns:
  No  Name
  --------------------------
   0  col2
  
  Name        : myIndex
  Type        : TREE
  Columns:
  No  Name
  --------------------------
   0  col2
   1  col3
  ```

[Memo]
- Container data of the current database will be displayed.
- The data displayed in a container list are the "Container name", "Container type" and "Partition ID".
- The data displayed in the specified container are the "Container name", "Container type", "Partition ID", "Defined column name", "Column data type", "Column constraints" as CSTR, "Index information", and "Table partitioning data".
- In "Column constraints" column, "NOT NULL constraint" as NN is displayed.
- In the case of connecting through JDBC, the details of "Table partitioning data" are displayed. The displayed items are "Partitioning Type", "Partitioning Column", "Partition Interval Value", "Partition Interval Unit" of interval partitioning, and "Partition Division Count" of hash partitioning. For interval-hash partitioning, the items of interval partitioning and hash partitioning are both displayed.


- Example:
  ``` example
  //Display the specified container data (in the case of connecting through JDBC)
  gs[userDB]> showcontainer time018
  Database    : userDB
  Name        : time018
  Type        : TIME_SERIES
  Partition ID: 37
  DataAffinity: -
  Partitioned : true
  Partition Type           : INTERVAL
  Partition Column         : date
  Partition Interval Value : 730
  Partition Interval Unit  : DAY
  Sub Partition Type           : HASH
  Sub Partition Column         : date
  Sub Partition Division Count : 16
        :
        :

  //Display the specified container data (not in the case of connecting through JDBC)
  gs[userDB]> showcontainer time018
  Database    : userDB
  Name        : time018
  Type        : TIME_SERIES
  Partition ID: 37
  DataAffinity: -
  Partitioned : true (need SQL connection for details)
        :
        :
  ```

　

### Displaying a table data

Display the table data. It is compatible command of showcontainer.

- Sub-command

  | |
  |-|
  | showtable [\<Table name\>] |

- Argument

  | Argument      | Note                                                                               |
  |------------|------------------------------------------------------------------------------------|
  | \<Table name\> | Specify the table name to be displayed. Display a list of all tables if omitted. |

　
### Searching for container

Search for a container by specifying a container name.

- Sub-command

  | |
  |-|
  | searchcontainer \[container name\] |

- Argument

  | Argument      | Note                                                                               |
  |------------|------------------------------------------------------------------------------------|
  | container name | Specify the container name to search for. Otherwise, all containers are displayed. Wild cards (where % represents zero or more characters, and _ represents a single character) can be specified for a container name. |

- Example:
  ``` example
  gs[public]> searchcontainer mycontainer
  mycontainer
  
  gs[public]> searchcontainer my%
  my
  my_container
  mycontainer

  gs[public]> searchcontainer my\_container
  my_container
  ```

### Searching for a view

Search for a view by specifying a view name.

- Sub-command

  | |
  |-|
  | searchview \[view name\] |

- Argument

  | Argument      | Note                                                                               |
  |------------|------------------------------------------------------------------------------------|
  | view name | Specify the view name to search for. Otherwise, all views are displayed. Wild cards (where % represents zero or more characters, and _ represents a single character) can be specified for a view name. |

- Example:
  ``` example
  gs[public]> searchview myview
  myview
  
  gs[public]> searchview my%
  my
  my_view
  myview

  gs[public]> searchview my\_view
  my_view
  ```

### Creating an index

Create an index in the column of a specified container.

- Sub-command

  | |
  |-|
  | createindex \<Container name\> \<Column name\> \<Index type\> ... |

- Argument

  | Argument      | Note                                                                                          |
  |---------------|-----------------------------------------------------------------------------------------------|
  | \<Container name\> | Specify the name of container that the column subject to the index operation belongs to.                                        |
  | Column name        | Specify the name of the column subject to the index operation.                                                          |
  | Index type ...     | Specify the index type. Specify TREE, HASH or SPATIAL (or multiple) for the index type. |

- Example:

  ``` example
  //create index
  gs[public]> createindex cont003 col2 tree hash

  gs[public]> showcontainer cont003
  Database    : public
  Name        : cont003
  Type        : COLLECTION
  Partition ID: 27
  DataAffinity: -

  Columns:
  No  Name                  Type            CSTR  RowKey
  ------------------------------------------------------------------------------
   0  col1                  INTEGER         NN    [RowKey]
   1  col2                  STRING                
   2  col3                  TIMESTAMP             

  Indexes:
  Name        : 
  Type        : TREE
  Columns:
  No  Name                  
  --------------------------
   0  col1

  Name        : 
  Type        : TREE
  Columns:
  No  Name                  
  --------------------------
   0  col2

  Name        : 
  Type        : HASH
  Columns:
  No  Name                  
  --------------------------
   0  col2
  ```

[Memo]
- An error will not occur even if an index that has already been created is specified.
- An index name is not supported. In case of specifying an index name, use the SQL.

　

### Creating a compound index

Create a composite index on the column of a specified container.

- Sub-command

  | |
  |-|
  | createcompindex <Container name> <Column name> ... |

- Argument

  | Argument      | Note                                                                                          |
  |---------------|-----------------------------------------------------------------------------------------------|
  | \<Container name\> | Specify the name of container that the column subject to the index operation belongs to.                                        |
  | Column name        | Specify the name of the column subject to the index operation. Specify more than one.                                          |

- Example:

  ``` example
  //create index
  gs[public]> createcompindex cont003 col2 col3

  gs[public]> showcontainer cont003
  Database    : public
  Name        : cont003
  Type        : COLLECTION
  Partition ID: 27
  DataAffinity: -
  
  Columns:
  No  Name                  Type            CSTR  RowKey
  ------------------------------------------------------------------------------
   0  col1                  INTEGER         NN    [RowKey]
   1  col2                  STRING
   2  col3                  TIMESTAMP
  
  Indexes:
  Name        :
  Type        : TREE
  Columns:
  No  Name
  --------------------------
   0  col1
  
  Name        :
  Type        : TREE
  Columns:
  No  Name
  --------------------------
   0  col2
   1  col3
  ```
[Memo]
- An error will not occur even if an index that has already been created is specified.
- An index name is not supported. In case of specifying an index name, use the SQL.
- Operations on the partition table are not supported. An error will occur if a partition table is specified for the container name.

　

### Deleting an index

Delete the index in the column of a specified container.

- Sub-command

  | |
  |-|
  | dropindex \<Container name\> \<Column name\> \<Index type\> ... |

- Argument

  | Argument      | Note                                                                                          |
  |---------------|-----------------------------------------------------------------------------------------------|
  | \<Container name\> | Specify the name of container that the column subject to the index operation belongs to.                                        |
  | Column name        | Specify the name of the column subject to the index operation.                                                          |
  | Index type ...     | Specify the index type. Specify TREE, HASH or SPATIAL (or multiple) for the index type. |

- Example:

  ``` example
  //delete index
  gs[public]> showcontainer cont004
  Database    : public
  Name        : cont004
      :
      :
  Indexes:
  Name        : 
  Type        : TREE
  Columns:
  No  Name                  
  --------------------------
   0  id

  Name        : myIndex
  Type        : TREE
  Columns:
  No  Name                  
  --------------------------
   0  value

  gs[public]> dropindex cont004 value tree
  gs[public]> showcontainer cont004
  Database    : public
  Name        : cont004
      :
      :
  Indexes:
  Name        : 
  Type        : TREE
  Columns:
  No  Name                  
  --------------------------
   0  id
  ```

[Memo]
- An error will not occur even if an index that has not been created is specified.

　

### Deleting a compound index

Delete the compound index in the column of a specified container.

- Sub-command

  | |
  |-|
  | dropcompindex <Container name> <Column name> ... |

- Argument

  | Argument      | Note                                                                                          |
  |---------------|-----------------------------------------------------------------------------------------------|
  | \<Container name\> | Specify the name of container that the column subject to the index operation belongs to.                                        |
  | Column name        | Specify the name of the column subject to the index operation. Specify more than one.                                          |

- Example:

  ``` example
  //delete index
  gs[public]> showcontainer cont003
  Database    : public
  Name        : cont003
  Type        : COLLECTION
  Partition ID: 27
  DataAffinity: -
  
  Columns:
  No  Name                  Type            CSTR  RowKey
  ------------------------------------------------------------------------------
   0  col1                  INTEGER         NN    [RowKey]
   1  col2                  STRING
   2  col3                  TIMESTAMP
  
  Indexes:
  Name        :
  Type        : TREE
  Columns:
  No  Name
  --------------------------
   0  col1
  
  Name        :
  Type        : TREE
  Columns:
  No  Name
  --------------------------
   0  col2
   1  col3

  gs[public]> dropcompindex cont003 col2 col3

  gs[public]> showcontainer cont003
  Database    : public
  Name        : cont003
  Type        : COLLECTION
  Partition ID: 27
  DataAffinity: -
  
  Columns:
  No  Name                  Type            CSTR  RowKey
  ------------------------------------------------------------------------------
   0  col1                  INTEGER         NN    [RowKey]
   1  col2                  STRING
   2  col3                  TIMESTAMP
  
  Indexes:
  Name        :
  Type        : TREE
  Columns:
  No  Name
  --------------------------
   0  col1
  ```

[Memo]
- An error will not occur even if an index that has not been created is specified.
- Operations on the partition table are not supported. An error will occur if a partition table is specified for the container name.

　

### Deleting a trigger

Delete the trigger of a specified container.

- Sub-command

  | |
  |-|
  | droptrigger \<Container name\> \<Trigger name\> |

- Argument

  | Argument         | Note                                     |
  |----------|------------------------------------------|
  | \<Container name\> | Specify the name of the container whose trigger is going to be deleted. |
  | Trigger name       | Specify the trigger name to delete.           |

- Example:
  ``` example
  gs[public]> droptrigger con01 tri03
  ```

　

### Displaying trigger data

Display the trigger data of a specified container.

- Sub-command

  | |
  |-|
  | showtrigger \<Container name\> [\<Trigger name\>] |

- Argument

  | Argument         | Note                                                                               |
  |----------|------------------------------------------------------------------------------------|
  | \<Container name\> | Specify the container name to be displayed.                                                 |
  | Trigger name       | Specify the trigger name to be displayed. Display a list of all trigger data if omitted. |

- Example:

  ``` example
  //Display the trigger data list of the specified container
  gs[public]> showtrigger cont003
  Name                 Type  Columns              Events
  ---------------------------------------------------------------
  rtrig01              REST  [col1, col3]         [PUT]

  gs[public]> showtrigger cont003 rtrig01
  Name          : rtrig01
  Type          : REST
  Target Columns: [col1, col3]
  Target Events : [PUT]

  Destination URI: http://example.com
  ```

[Memo]
- The data displayed in a trigger list are the "Trigger name", "Notification method", "Column to be notified", "Operation to be monitored (create new or update, delete a row)".
- The data displayed in the specified trigger data are the "Trigger name", "Notification method", "Column to be notified", "Operation to be monitored" and "Notification destination URI". In addition, the "Destination name", "Destination type", "User" and "Password" are also displayed together in a JMS notification.
- See the chapter of "Trigger function" of a ["GridDB functional reference"](https://github.com/griddb/docs-en/blob/master/manuals/GridDB_FeaturesReference.md) for the details of a trigger function.

　

## Execution plan

This section explains subcommands to displays an SQL execution plan.

### Getting an SQL analysis result (global plan)

Display an SQL analysis result (global plan) in text format or in JSON format.

#### Text format

- Sub-command

  | |
  |-|
  | getplantxt [\<Text file name\>] |

- Argument

  | Argument      | Note                                  |
  |------------------|---------------------------------------|
  | Text file name | Specify the name of the file where the results are saved.   |

- Example:

  ``` example
  gs[public]> getplantxt
  Id Type         Input Rows Lead time Actual time Node                 And more..
  -----------------------------------------------------------------------------------------------------------------
   0 SCAN         -     -            0           0 192.168.56.101:10001 table: {collection_nopart_AA22} INDEX SCAN
   1 SCAN         -     -            0           0 192.168.56.101:10001 table: {collection_nopart_AA23}
   2   JOIN       0,1   1,10         1           1 192.168.56.101:20001 JOIN_EQ_HASH
   3   SCAN       -     -            0           0 192.168.56.101:10001 table: {collection_nopart_AA24}
   4     JOIN     2,3   1,10         4           1 192.168.56.101:20001 JOIN_EQ_HASH
   5       RESULT 4     1            0           0 192.168.56.101:20001           
  ```

[Memo]
- An error will occur if EXPLAIN or EXPLAIN ANALYZE is not executed in advance.
- The contents to be displayed
  - ID: Plan ID
  - Type: Type of processing
  - Input: Plan ID of the input plan
  - Rows: The number of input rows
  - Lead time: Processing time
  - Actual time: Time the thread was occupied for processing
  - Node: Node the processing was executed

#### JSON format

- Sub-command

  | |
  |-|
  | getplanjson [\<JSON file name\>] |

- Argument

  | Argument      | Note                                  |
  |------------------|---------------------------------------|
  | JSON file name | Specify the name of the file where the results are saved.   |


- Example:
  ``` example
  gs[public]> getplanjson
  {
    "nodeList" : [ {
      "cmdOptionFlag" : 65,
      "id" : 0,
      "indexInfoList" : [ 2, 0, 0 ],
      "inputList" : [ ],
      "outputList" : [ {
        "columnId" : 0,
        "columnType" : "STRING",
        "inputId" : 0,
        "op" : "EXPR_COLUMN",
        "qName" : {
          "db" : "public",
          "name" : "id",
          "table" : "collection_nopart_AA22"
        },
        "srcId" : 1
      }, {
  ・
  ・
  ・
  ```

[Memo]
- An error will occur if EXPLAIN or EXPLAIN ANALYZE is not executed in advance.

### Getting detailed information about an SQL analysis result

Display the detailed information of an SQL analysis result in JSON format.

- Sub-command

  | |
  |-|
  | gettaskplan \<plan ID\> |

- Argument

  | Argument      | Note                                  |
  |------------------|---------------------------------------|
  | Plan ID  | Specify the plan ID of the plan to display.   |


- Example:
  ``` example
  gs[public]> gettaskplan 0
  {
    "cmdOptionFlag" : 65,
    "id" : 0,
    "indexInfoList" : [ 2, 0, 0 ],
    "inputList" : [ ],
    "outputList" : [ {
      "columnId" : 0,
      "columnType" : "STRING",
      "inputId" : 0,
      "op" : "EXPR_COLUMN",
      "qName" : {
        "db" : "public",
        "name" : "id",
        "table" : "collection_nopart_AA22"
      },
      "srcId" : 1
    }, {
  ・
  ・
  ・
  ```

[Memo]
- An error will occur if EXPLAIN or EXPLAIN ANALYZE is not executed in advance.

## Other operations

This section explains the sub-commands for other operations.

### Echo back setting

Display the executed sub-command in the standard output.

- Sub-command

  | |
  |-|
  | echo <boolean> |

- Argument

  | Argument      | Note                                                                              |
  |---------|-----------------------------------------------------------------------------------|
  | Boolean  | Display the executed sub-command in the standard output when TRUE is specified. Default value is FALSE. |

- Example:

  ``` example
  //display the executed sub-command in the standard output
  gs> echo TRUE
  ```

[Memo]
- gs_sh prompt "gs\>" always appear in the standard output.

　

### Displaying a message

Display the definition details of the specified character string or variable.

- Sub-command

  | |
  |-|
  | print \<message\> |

- Argument

  | Argument      | Note                                     |
  |------------|------------------------------------------|
  | Message  | Specify the character string or variable to display. |

- Example:

  ``` example
  //display of character string
  gs> print print executed.  
  print executed.
  ```

[Memo]
- Append "$" in front of the variable name when using a variable.

　

### Sleep

Set the time for the sleeping function.

- Sub-command

  | |
  |-|
  | sleep \<No. of sec\> |

- Argument

  | Argument      | Note                           |
  |------|--------------------------------|
  | No. of sec | Specify the no. of sec to go to sleep. |

- Example:

  ``` example
  //sleep for 10 sec
  gs> sleep 10
  ```

[Memo]
- Specify a positive integer for the no. of sec number.

　

### Executing external commands

Execute an external command.

- Sub-command

  | |
  |-|
  | exec \<External command\> [\<External command arguments\>] |

- Argument

  | Argument      | Note                             |
  |------------------|----------------------------------|
  | External command           | Specify an external command.       |
  | External command arguments | Specify the argument of an external command. |

- Example:

  ``` example
  //display the file data of the current directory
  gs> exec ls -la
  ```

[Memo]
- Pipe, redirect, and hear document cannot be used.

　

### Terminating gs_sh

The above command is used to terminate gs_sh.

- Sub-command

  | |
  |-|
  | exit<br>quit |

- Example:

  ``` example
  // terminate gs_sh.  
  gs> exit
  ```

In addition, if an error occurs in the sub-command, the setting can be configured to end gs_sh.

- Sub-command

  | |
  |-|
  | errexit <boolean> |

- Argument

  | Argument      | Note                                                                                                         |
  |---------|--------------------------------------------------------------------------------------------------------------|
  | If TRUE is specified, gs_sh ends when an error occurs in the sub-command. Default is FALSE. |

- Example:

  ``` example
  //configure the setting so as to end gs_sh when an error occurs in the sub-command
  gs> errexit TRUE
  ```

[Memo]
- There is no functional difference between the exit sub-command and quit sub-command.

　

### Help

Display a description of the sub-command.

- Sub-command

  | |
  |-|
  | help [\<Sub-command name\>] |

- Argument

  | Argument      | Note                                                                                     |
  |----------------|------------------------------------------------------------------------------------------|
  | Sub-command name | Specify the sub-command name to display the description Display a list of the sub-commands if omitted. |

- Example:

  ``` example
  //display the description of the sub-command
  gs> help exit
  exit
  terminate gs_sh.
  ```

[Memo]
- A description of gs_sh can be obtained with the command "gs_sh --help".

　

### Version

Display the version of gs_sh.

- Sub-command

  | |
  |-|
  | version |

- Example:

  ``` example
  //display the version
  gs> version
  gs_sh version 2.0.0
  ```

[Memo]
- The gs_sh version data can be obtained with the command "gs_sh --version" as well.



### Setting the time zone

Set the time zone.

- Sub-command

  | |
  |-|
  | settimezone [setting value] |

- Argument

  | Argument      | Note                                                                                     |
  |----------------|------------------------------------------------------------------------------------------|
  | Value | The format of the setting value is "±hh:mm", "±hhmm", "Z", and "auto". For example, for Japan time, the setting value is "+09:00". <br>When the value is not specified, the setting value for the time zone is cleared. |

[Memo]
- If the time zone setting is changed with the settimezone subcommand after executing the connect subcommand, the changed timezone setting is not reflected until the connect subcommand is executed again. After changing the time zone setting, execute the connect subcommand again.

### Displaying history and rerunning a previous subcommand

Display previously run subcommands.

- Sub-command

  | |
  |-|
  | history |

Rerun recent subcommands from the subcommand history displayed with the history subcommand.

- Sub-command

  | |
  |-|
  |!history number|

- Argument

  | Argument      | Note                                                                                     |
  |----------------|------------------------------------------------------------------------------------------|
  | history number | Specify the history number of the subcommand you want to rerun from the subcommand history displayed with the history subcommand.|

Rerun the previously run subcommand.

- Sub-command

  | |
  |-|
  |!!|


- Example:

  ``` example
  gs> history
  　1  connect $mycluster
  　2  showcontainer
  　3  select * from mytable;
  　:
  　210  configcluster $mycluster
  　211  history

  gs> !210
  gs> configcluster $mycluster
             :

  gs> !!
  gs> configcluster $mycluster
             :
  
  ```

[Memo]
- Up to 500 most recent commands can be displayed.



## Options and sub-commands specifications

### Option

- Command list

  | |
  |-|
  | gs_sh [\<Script file\>] |
  | gs_sh -v\|--version         |
  | gs_sh -h\|--help           |

- Options

  | Options       | Required | Note                                             |
  |---------------|------|--------------------------------------------------|
  | \-v\|--version |          | Display the version of the tool.                 |
  | \-h\|--help    |          | Display the command list as a help message. |

[Memo]
- In order to batch process the gs_sh sub-command, a script file can be created. Extension of script file is gsh.
- During gs_sh startup, .gsshrc script files under the gsadm user home directory are imported automatically. The .gsshrc contents will also be imported to the destination from other script files.

　

### Sub-command list

- GridDB cluster definition sub-command list

  | Sub-command   | Argument                                                                                                      | Note                                                                | \*1 |
  |---------------|----------------------------------------|------------------------------------------------------------|-----|
  | setnode       | \<Node variable\> \<IP address\> \<Port no.\> \[\<SSH port no.\>\]                                            | Define the node variable.                          |     |
  | setcluster    | setcluster \<Cluster variable\> \<Cluster name\> \<Multicast address\> \<Port no.\> \[\<Node variable\> ...\] | Define the cluster variable.  |     |
  | setclustersql | setclustersql \<Cluster variable\> \<Cluster name\> \<SQL address\> \<SQL port no.\>                          | Define the SQL connection destination in the cluster configuration.                |     |
  | modcluster    | \<Cluster variable\> add | remove \<Node variable\> ...                                                       | Add or delete a node variable to or from the cluster variable.             |     |
  | setuser       | \<User name\> \<Password\> \[\<gsadm password\>\]                                                             | Define the user and password to access the cluster. |     |
  | set           | \<Variable name\> \[\<Value\>\]                                                                               | Define an arbitrary variable.                                            |     |
  | show          | \[\<Variable name\>\]                                                                                         | Display the detailed definition of the variable.                                         |     |
  | save          | \[\<Script file name\>\]                                                                                      | Save the variable definition in the script file.                             |     |
  | load          | \[\<Script file name\>\]                                                                                      | Execute a read script file.                               | 　 |
  | sync     | IP address port number　\[cluster variable name \[node variable\] \]       | Connect to the running GridDB cluster and automatically define a cluster variable and a node variable.                                | \*　 |

  - \*1 : Commands marked with an \* can be executed by the administrator user only.


- GridDB cluster operation sub-command list

  | Sub-command       | Argument                                                              | Note                                                                         | \*1 |
  |-----------------------|----------------------------------------------------|----------------------------------------------|-----|
  | showsql           | Query ID                                                              | Display the SQL processing under execution.                         |   |
  | showevent         |                                                                       | Display the event list under execution.                    |   |
  | showconnection    |                                                                       | Display the list of connections.                      |   |
  | killsql           | Query ID                                                              | Cancel the SQL processing in progress.                    | \* |

  - \*1 : Commands marked with an \* can be executed by the administrator user only.

　

- Data operation sub-command list

  | Sub-command       | Argument                                                              | Note                                                                         | \*1 |
  |------------|---------------------------------|------------------------------------------------------|-----|
  | connect     | \<Cluster variable\> \[\<Database name\>\] | Connect to a GridDB cluster.                                 |     |
  | tql         | \<Container name\> \<Query;\>              | Execute a search and retain the search results.                         |     |
  | get         | \[ \<No. of acquires\> \]                  | Get the search results and display them in a stdout.                    |     |
  | getcsv      | \<CSV file name\> \[\<No. of acquires\>\]  | Get the search results and save them in a file in the CSV format.             |     |
  | getnoprint  | \[ \<No. of acquires\> \]                  | Get the query results but do not display them in a stdout.          |     |
  | tqlclose    |                                            | Close the TQL and discard the search results saved.               |     |
  | tqlexplain  | \<Container name\> \<Query;\>              | Execute the specified TQL command and display the execution plan and actual measurement values such as the number of cases processed etc.                            |     |
  | tqlanalyze  | \<Container name\> \<Query;\>              | Displays the execution plan of the specified TQL command.   |     |
  | sql         | \<SQL command;\>                           | Execute an SQL command and retains the search result.                       |     |
  | sqlcount    | Boolean                                    | Set whether to execute count query when SQL querying. |     |
  | queryclose  |                                            | Close the query and discard the search results saved.            |     |
  | disconnect  |                                            | Disconnect user from a GridDB cluster.                             |   　  |

  - \*1 : Commands marked with an \* can be executed by the administrator user only.

 　
- Container management sub-command list

  | Sub-command       | Argument                                                              | Note                                                                         | \*1 |
  |------------------|--------------------------------------------------------|------------------------------------------------|-----|
  | createcollection | \<Container name\> \<Column name\> \<Column type\> \[\<Column name\> \<Column type\> ...\]                        | Create a container (collection).           |     |
  | createtimeseries | \<Container name\> \<Compression method\> \<Column name\> \<Column type\> \[\<Column name\> \<Column type\> ...\] | Create a container (timeseries container).         |     |
  | dropcontainer    | \<Container name\>                                                                                                | Delete a container                         |     |
  | putrow     | container name value [value...]       | Register a row in a container.    | 　 |
  | removerow     | container name row key value [row key value...]  | Delete a row from a container.        | 　 |
  | showcontainer    | \[ \<Container name\> \]                                                                                          | Display the container data.                     |     |
  | showtable        | \[ \<Table name\> \]                                                                                              | Display the table data.                     |     |
  | searchcontainer   | \[container name\]  | Search for a container by specifying a container name.    | 　 |
  | searchview    | \[view name\]  | Search for a view by specifying a view name.  | 　 |
  | createindex      | \<Container name\> \<Column name\> \<Index type\> ...                                                             | Create an index in the specified column.                 |     |
  | createcompindex  | \<Container name\> \<Column name\> ...                                                                            | Create a composite index on the specified column.             |     |
  | dropindex        | \<Container name\> \<Column name\> \<Index type\> ...                                                             | Delete an index of the specified column.                 |     |
  | dropcompindex    | \<Container name\> \<Column name\> ...                                                                            | Deletes the composite index of the specified column.               |     |
  | droptrigger      | \<Container name\> \<Trigger name\>                                                                               | Delete the trigger data.                       |     |
  | showtrigger      | \<Container name\> [\<Trigger name\>]                                                                           | Display the trigger data.                       |     |

  - \*1 : Commands marked with an \* can be executed by the administrator user only.

　

- Execution plan sub-command list

  | Sub-command       | Argument                                                              | Note                                                                         | \*1 |
  |------------------|--------------------------------------------------------|------------------------------------------------|-----|
  | getplantxt  | \[Text file name\] | Display an SQL analysis result in text format.        |     |
  | getplanjson | \[JSON file name\] | Display an SQL analysis result in JSON format.            |     |
  | gettaskplan | Plan ID            | Display the detailed information of an SQL analysis result in JSON format.  |     |

  - \*1 : Commands marked with an \* can be executed by the administrator user only.

　

- Other operation sub-command list

  | Sub-command       | Argument                                                              | Note                                                                         | \*1 |
  |----------|--------------------------------------|--------------------------------------------------|-----|
  | echo        | Boolean                                         | Set whether to echo back.                    |     |
  | print       | Message                                         | Display the definition details of the specified character string or variable.  |     |
  | sleep       | No. of sec                                      | Set the time for the sleeping function.                        |     |
  | exec        | External command \[External command arguments\] | Execute an external command.                            |     |
  | exit        |                                                 | The above command is used to terminate gs_sh.                               |     |
  | quit        |                                                 | The above command is used to terminate gs_sh.                               |     |
  | errexit     | Boolean                                         | Set whether to terminate gs_sh when an error occurs.        |     |
  | help        | \[ \<Sub-command name\> \]                      | Display a description of the sub-command.                    |     |
  | version     |                                                 | Display the version of gs_sh.                       |     |
  | settimezone | \[setting value\]                               | Set the time zone.                      |     |
  | history | | Display previously run subcommands. |   |
  | !\[history number \]| | Specify the history number of the subcommand you want to rerun from the subcommand history displayed with the history subcommand. |   |
  | !! | |Rerun the previously run subcommand. |   |

  - \*1 : Commands marked with an \* can be executed by the administrator user only.

 　