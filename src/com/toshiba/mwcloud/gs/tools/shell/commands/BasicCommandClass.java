/*
 	Copyright (c) 2021 TOSHIBA Digital Solutions Corporation.
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package com.toshiba.mwcloud.gs.tools.shell.commands;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.script.Bindings;
import javax.script.ScriptContext;

import com.toshiba.mwcloud.gs.tools.common.GridStoreCommandException;
import com.toshiba.mwcloud.gs.tools.common.NotificationMode;
import com.toshiba.mwcloud.gs.tools.shell.AbstractCommandClass;
import com.toshiba.mwcloud.gs.tools.shell.Command;
import com.toshiba.mwcloud.gs.tools.shell.CommandTable;
import com.toshiba.mwcloud.gs.tools.shell.GridStoreScriptEngine;
import com.toshiba.mwcloud.gs.tools.shell.GridStoreShell;
import com.toshiba.mwcloud.gs.tools.shell.ShellCluster;
import com.toshiba.mwcloud.gs.tools.shell.ShellException;
import com.toshiba.mwcloud.gs.tools.shell.ShellNode;
import com.toshiba.mwcloud.gs.tools.shell.annotation.GSCommand;
import com.toshiba.mwcloud.gs.tools.shell.annotation.GSNullable;

/**
 * The basic command class contains some basic commands.
 *
 * @see AbstractCommandClass
 */
public class BasicCommandClass extends AbstractCommandClass {
  /** The operator in sub-command {@code modcluster}. */
  public enum ModOperator {
    ADD,
    REMOVE
  }

  /** The operator in sub-command {@code setsslmode}. */
  public enum SslMode {
    DISABLED("DISABLED"),
    REQUIRED("PREFERRED"),
    VERIFY("VERIFY");
    private String value;

    SslMode(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  private static final int DEFAULT_SSH_PORT = 22;

  /**
   * Get command group name.
   *
   * @return command group name (basic)
   */
  @Override
  public String getCommandGroupName() {
    return "basic";
  }

  private void checkVarName(String name) {
    if (!name.matches("^[0-9a-zA-Z_]+$")) {
      throw new ShellException(getMessage("error.invalidVarName", name));
    }
  }

  /**
   * The main method for sub-command {@code set}.<br>
   * Define an arbitrary variable.
   *
   * @param name variable name
   * @param value variable value
   * @throws ShellException if variable name is invalid
   * @see ScriptContext
   */
  @GSCommand(name = "set", assignall = true)
  public void setVariable(String name, @GSNullable String value) {
    checkVarName(name);
    if (value != null) {
      getContext().setAttribute(name, value, ScriptContext.ENGINE_SCOPE);
    } else {
      getContext().removeAttribute(name, ScriptContext.ENGINE_SCOPE);
    }
  }

  /**
   * The main method for sub-command {@code setnode}.<br>
   * Define the IP address and port of a GridDB node in the node variable.
   *
   * @param name node variable name
   * @param addr node address
   * @param restPort node REST API port
   * @param sshPort node SSH port with default value is 22
   * @throws ShellException if variable name is invalid
   * @see ScriptContext
   */
  @GSCommand
  public void setNode(String name, String addr, int restPort, @GSNullable Integer sshPort) {
    checkVarName(name);
    if (sshPort == null) {
      sshPort = DEFAULT_SSH_PORT;
    }
    ShellNode node = new ShellNode(name, addr, restPort, sshPort);
    getContext().setAttribute(name, node, ScriptContext.ENGINE_SCOPE);
  }

  /**
   * The main method for sub-command {@code setcluster}.<br>
   * Define the GridDB cluster configuration in the cluster variable.
   *
   * @param name cluster variable name
   * @param clusterName cluster name
   * @param value1 connection method (multicast address (for MULTICAST)/FIXED_LIST/PROVIDER)
   * @param value2 the transaction member (for FIXED_LIST) or provider URL (for PROVIDER) or
   *     multicast port (MULTICAST)
   * @param nodes list of node variables
   * @throws ShellException if it meets 1 of the following conditions:
   *     <ul>
   *       <li>Variable name is invalid
   *       <li>Port is not numeric
   *       <li>The definition of the nodes in the cluster variable is invalid
   *     </ul>
   *
   * @see ScriptContext
   */
  @GSCommand
  public void setCluster(
      String name, String clusterName, String value1, String value2, ShellNode... nodes) {
    checkVarName(name);

    // 新規作成
    ShellCluster cluster = new ShellCluster(name);
    cluster.setName(clusterName);
    cluster.setNodes(Arrays.asList(nodes));

    if (value1.equalsIgnoreCase(NotificationMode.FIXED_LIST.toString())) {
      cluster.setMode(NotificationMode.FIXED_LIST);
      cluster.setTransactionMember(value2);

    } else if (value1.equalsIgnoreCase(NotificationMode.PROVIDER.toString())) {
      cluster.setMode(NotificationMode.PROVIDER);
      cluster.setProviderUrl(value2);

    } else {
      cluster.setMode(NotificationMode.MULTICAST);
      cluster.setAddress(value1);
      try {
        cluster.setPort(Integer.parseInt(value2));
      } catch (NumberFormatException e) {
        throw new ShellException(getMessage("error.setclusterPort"), e);
      }
    }

    try {
      cluster.checkNodes();
    } catch (GridStoreCommandException e) {
      throw new ShellException(
          getMessage("error.setcluster") + ": msg=[" + e.getMessage() + "]", e);
    }
    getContext().setAttribute(name, cluster, ScriptContext.ENGINE_SCOPE);
  }

  /**
   * The main method for sub-command {@code setclustersql}.<br>
   * Define the SQL connection destination in the GridDB cluster configuration.
   *
   * @param name cluster variable name
   * @param clusterName cluster name
   * @param value1 connection method
   * @param value2 SQL member/SQL provider/JDBC port
   * @throws ShellException if it meets 1 of the following conditions:
   *     <ul>
   *       <li>Variable name is invalid
   *       <li>Port is not numeric
   *       <li>The definition of the nodes in the cluster variable is invalid
   *     </ul>
   *
   * @throws IllegalArgumentException if it meets 1 of the following conditions:
   *     <ul>
   *       <li>{@code name} is not a cluster variable
   *       <li>Designated connection method is different
   *       <li>Cluster name is unmatched
   *     </ul>
   *
   * @see ScriptContext
   */
  @GSCommand
  public void setClusterSQL(String name, String clusterName, String value1, String value2) {
    checkVarName(name);

    Object clusterObj = getContext().getAttribute(name);
    if (clusterObj == null) {
      ShellCluster cluster = new ShellCluster(name);
      cluster.setName(clusterName);
      if (value1.equalsIgnoreCase(NotificationMode.FIXED_LIST.toString())) {
        cluster.setMode(NotificationMode.FIXED_LIST);
        cluster.setSqlMember(value2);

      } else if (value1.equalsIgnoreCase(NotificationMode.PROVIDER.toString())) {
        cluster.setMode(NotificationMode.PROVIDER);
        cluster.setSQLProvider(value2);

      } else {
        cluster.setMode(NotificationMode.MULTICAST);
        cluster.setJdbcAddress(value1);
        try {
          cluster.setJdbcPort(Integer.parseInt(value2));
        } catch (NumberFormatException e) {
          throw new ShellException(getMessage("error.setclusterPort"), e);
        }
      }

      getContext().setAttribute(name, cluster, ScriptContext.ENGINE_SCOPE);

    } else if (!(clusterObj instanceof ShellCluster)) {
      throw new IllegalArgumentException(getMessage("error.varIsNotCluster", name));

    } else {
      ShellCluster cluster = (ShellCluster) clusterObj;
      if (cluster.getName().equals(clusterName)) {
        if (value1.equalsIgnoreCase(NotificationMode.FIXED_LIST.toString())) {
          if (cluster.getMode() != NotificationMode.FIXED_LIST) {
            throw new IllegalArgumentException(getMessage("error.setclusterMode", name));
          }
          cluster.setSqlMember(value2);

        } else if (value1.equalsIgnoreCase(NotificationMode.PROVIDER.toString())) {
          if (cluster.getMode() != NotificationMode.PROVIDER) {
            throw new IllegalArgumentException(getMessage("error.setclusterMode", name));
          }
          cluster.setSQLProvider(value2);

        } else {
          if (cluster.getMode() != NotificationMode.MULTICAST) {
            throw new IllegalArgumentException(getMessage("error.setclusterMode", name));
          }
          try {
            cluster.setJdbcAddress(value1);
            cluster.setJdbcPort(Integer.parseInt(value2));
          } catch (NumberFormatException e) {
            throw new ShellException(getMessage("error.setclusterPort"), e);
          }
        }

      } else {
        throw new IllegalArgumentException(
            "ClusterName is unmatch. clusterVariable=["
                + name
                + "] clusterName=["
                + cluster.getName()
                + "]");
      }
    }
  }

  /**
   * The main method for sub-command {@code modcluster}.<br>
   * Change the definition of the node list of a cluster variable.
   *
   * @param name cluster variable name
   * @param op operator (ADD/REMOVE)
   * @param nodes the list of nodes
   * @throws IllegalArgumentException if it meets 1 of the following conditions:
   *     <ul>
   *       <li>Cluster variable is undefined
   *       <li>{@code name} is not cluster variable
   *       <li>{@code nodes} is empty
   *     </ul>
   *
   * @throws ShellException if it meets 1 of the following conditions:
   *     <ul>
   *       <li>Node variable is invalid
   *       <li>The definition of node in the cluster variable is incorrect
   *     </ul>
   */
  @GSCommand(name = "modcluster")
  public void modifyCluster(String name, ModOperator op, ShellNode... nodes) {
    Object clusterObj = getContext().getAttribute(name);
    if (clusterObj == null) {
      throw new IllegalArgumentException(getMessage("error.varNotDefined", name));
    }
    if (!(clusterObj instanceof ShellCluster)) {
      throw new IllegalArgumentException(getMessage("error.varIsNotCluster", name));
    }
    if (nodes.length == 0) {
      throw new IllegalArgumentException(getMessage("error.modclsNoNode", name));
    }
    ShellCluster cluster = (ShellCluster) clusterObj;
    updateClusterNodes(cluster, false);

    List<ShellNode> clusterNodes = cluster.getNodes();

    if (op == ModOperator.ADD) {
      for (ShellNode node : nodes) {
        if (clusterNodes.contains(node)) {
          if (!node.getName()
              .equals(((ShellNode) clusterNodes.get(clusterNodes.indexOf(node))).getName())) {
            println(
                getMessage(
                    "warning.setcluster",
                    node.getName(),
                    (((ShellNode) (clusterNodes.get(clusterNodes.indexOf(node)))).getName())));
          }
        } else {
          clusterNodes.add(node);
        }
      }
    } else if (op == ModOperator.REMOVE) {

      for (ShellNode node : nodes) {
        for (int i = 0; i < clusterNodes.size(); i++) {
          ShellNode clNode = (ShellNode) clusterNodes.get(i);
          if (node.getName().equals(clNode.getName())) {
            clusterNodes.remove(i);
            break;
          }
        }
      }
    }
  }

  /**
   * The main method for sub-command {@code setuser}.<br>
   * Define the user and password to access the GridDB cluster.
   *
   * @param username GridDB user
   * @param password password of GirdDB user
   * @param osPassword password of OS user 'gsadm'
   * @see ScriptContext
   */
  @GSCommand
  public void setUser(String username, String password, @GSNullable String osPassword) {
    Bindings bindings = getContext().getBindings(ScriptContext.ENGINE_SCOPE);
    bindings.put(GridStoreShell.USER, username);
    bindings.put(GridStoreShell.PASSWORD, password);
    bindings.put(GridStoreShell.OSPASSWORD, osPassword);
  }

  /**
   * The main method for sub-command {@code show}.<br>
   * Display variables.<br>
   * [Maintenance mode] All hidden variables are also displayed.
   *
   * @param name Variable name
   */
  @GSCommand(name = "show")
  public void showVariable(@GSNullable String name) {
    if (name != null) {
      Object obj = getContext().getAttribute(name);
      if (obj != null) {
        println(obj.toString());
      }
    } else {
      SortedMap<String, ShellNode> nodes = new TreeMap<String, ShellNode>();
      SortedMap<String, ShellCluster> clusters = new TreeMap<String, ShellCluster>();
      SortedMap<String, Object> others = new TreeMap<String, Object>();

      boolean isMaintenance = false;
      Object mode = getContext().getAttribute(GridStoreShell.MAINTENANCE_MODE);
      if ((mode != null)
          && ((GridStoreShell.EXEC_MODE) mode == GridStoreShell.EXEC_MODE.MAINTENANCE)) {
        isMaintenance = true;
      }

      Bindings bindings = getContext().getBindings(ScriptContext.ENGINE_SCOPE);
      for (Map.Entry<String, Object> e : bindings.entrySet()) {
        if (isMaintenance || !e.getKey().startsWith("__")) {
          if (e.getValue() instanceof ShellNode) {
            nodes.put(e.getKey(), (ShellNode) e.getValue());
          } else if (e.getValue() instanceof ShellCluster) {
            clusters.put(e.getKey(), (ShellCluster) e.getValue());
          } else {
            others.put(e.getKey(), e.getValue());
          }
        }
      }

      println(getMessage("message.nodes"));
      for (Map.Entry<String, ShellNode> e : nodes.entrySet()) {
        println("  " + e.getKey() + "=" + e.getValue());
      }
      println(getMessage("message.clusters"));
      for (Map.Entry<String, ShellCluster> e : clusters.entrySet()) {
        println("  " + e.getKey() + "=" + e.getValue());
      }

      println(getMessage("message.others"));

      println(
          "  "
              + GridStoreShell.USER
              + "="
              + (others.get(GridStoreShell.USER) == null ? "" : others.get(GridStoreShell.USER)));
      println(
          "  "
              + GridStoreShell.PASSWORD
              + "="
              + (others.get(GridStoreShell.PASSWORD) == null ? "" : "*****"));
      println(
          "  "
              + GridStoreShell.OSPASSWORD
              + "="
              + (others.get(GridStoreShell.OSPASSWORD) == null ? "" : "*****"));

      for (Map.Entry<String, Object> e : others.entrySet()) {
        if (isMaintenance
            || (!e.getKey().equals(GridStoreShell.USER)
                && !e.getKey().equals(GridStoreShell.PASSWORD)
                && !e.getKey().equals(GridStoreShell.OSPASSWORD))) {
          println("  " + e.getKey() + "=" + e.getValue());
        }
      }
    }
  }

  /**
   * The main method for sub-command {@code help}.<br>
   * If the {@code commandName} is {@code null}, list all available sub-commands, otherwise display
   * sub-command syntax and description
   *
   * @param commandName the command name
   * @throws ShellException if the command does not exist
   * @see ScriptContext
   */
  @GSCommand
  public void help(@GSNullable String commandName) {
    CommandTable cmdTable =
        (CommandTable) getContext().getAttribute(GridStoreScriptEngine.COMMAND_TABLE_NAME);
    String[] sqlAliases = {
      "select", "update", "insert", "replace", "delete", "create", "drop", "alter", "grant",
      "revoke", "pragma"
    };

    if (commandName == null) {
      println(getMessage("message.availableCommands"));

      int maxCommandLength = 0;
      for (Map.Entry<String, Map<String, Command>> clazz : cmdTable.getClassTable().entrySet()) {
        for (Command cmd : clazz.getValue().values()) {
          if (!cmd.isHidden()) {
            int length = cmd.getName().length();
            if (maxCommandLength < length) {
              maxCommandLength = length;
            }
          }
        }
      }
      int showcount = 3;
      String format = "%-" + (maxCommandLength + 2) + "s";
      for (Map.Entry<String, Map<String, Command>> clazz : cmdTable.getClassTable().entrySet()) {
        println(clazz.getKey() + ":");
        int i = 0;
        for (Command cmd : clazz.getValue().values()) {
          if (!cmd.isHidden()) {
            if (i % showcount == 0) {
              print("\t");
            }
            printf(format, cmd.getName());
            if ((i % showcount) == (showcount - 1)) {
              println("");
            }
            i++;
          }
        }
        println("");
        if ((i % showcount) != 0) {
          println("");
        }
      }

    } else if (commandName.equalsIgnoreCase("all")) {
      println(getMessage("message.availableCommands"));
      for (Map.Entry<String, Map<String, Command>> clazz : cmdTable.getClassTable().entrySet()) {
        println(clazz.getKey() + ":");
        for (Command cmd : clazz.getValue().values()) {
          if (!cmd.isHidden()) {
            println("  " + cmd.getSyntax());
            println("    " + cmd.getDescription());
          }
        }
      }

    } else if (Arrays.asList(sqlAliases).contains(commandName.toLowerCase())) {
      Command cmd = cmdTable.get("sql");
      println(cmd.getSyntax());
      println(cmd.getDescription());
      println(cmd.getDetailDescription());
    } else {
      Command cmd = cmdTable.get(commandName.toLowerCase());
      if (cmd == null) {
        throw new ShellException(getMessage("error.commandNotFound", commandName));
      }
      println(cmd.getSyntax());
      println(cmd.getDescription());
      println(cmd.getDetailDescription());
    }
  }

  /**
   * The main method for sub-command {@code help2}.<br>
   * It is a hidden sub-command to display hidden setting variables. Do not appear in the help list
   * and use tab keyboard to hint sub-command.
   */
  @GSCommand(hidden = true)
  public void help2() {
    println(getMessage("help.help2"));
  }

  /**
   * The main method for sub-command {@code version}.<br>
   * Display version of gs_sh.
   */
  @GSCommand
  public void version() {
    println(GridStoreShell.VERSION_INFO);
  }

  /**
   * The main method for sub-command {@code quit}.<br>
   * Exit gs_sh.
   */
  @GSCommand
  public void quit() {
    System.exit(0);
  }

  /**
   * The main method for sub-command {@code exit}.<br>
   * Exit gs_sh.
   */
  @GSCommand
  public void exit() {
    System.exit(0);
  }

  /**
   * The main method for sub-command {@code save}.<br>
   * Save the variable definition details to the script file. If {@code filename} is {@code null},
   * it is set to ".gsshrc"
   *
   * @param filename name of the script file
   * @throws ShellException if if it meets 1 of the following conditions:
   *     <ul>
   *       <li>Extension of script file (.gsh) is not specified
   *       <li>The specified directory does not exist
   *       <li>Cannot save file
   *       <li>An error has occurred in the process of saving into script file
   *     </ul>
   */
  @GSCommand
  public void save(@GSNullable String filename) {
    File target;
    if (filename == null) {
      String userHome = System.getProperty(GridStoreScriptEngine.USER_HOME);
      target = new File(userHome, ".gsshrc");
    } else {
      String ext = GridStoreShell.getFileExtension(filename);
      if ((ext == null) || !ext.equalsIgnoreCase("gsh")) {
        throw new ShellException(getMessage("error.saveExtension") + ": file=[" + filename + "]");
      }
      target = new File(filename);
    }

    if (target.getParentFile() != null && !target.getParentFile().exists()) {
      throw new ShellException(
          getMessage("error.saveDirNotFound")
              + " (dir=["
              + target.getParentFile().getAbsolutePath()
              + "])");
    }

    PrintWriter stream = null;
    try {
      stream =
          new PrintWriter(
              new BufferedWriter(new OutputStreamWriter(new FileOutputStream(target), "UTF-8")));

      List<ShellCluster> clusters = new ArrayList<ShellCluster>();
      Bindings bindings = getContext().getBindings(ScriptContext.ENGINE_SCOPE);
      for (Map.Entry<String, Object> e : bindings.entrySet()) {
        if (e.getKey().equals(GridStoreShell.USER)
            || e.getKey().equals(GridStoreShell.PASSWORD)
            || e.getKey().equals(GridStoreShell.OSPASSWORD)) {
          continue;
        }

        if (e.getKey().startsWith("__")) {
          continue;
        }

        if (e.getValue() instanceof ShellNode) {
          ShellNode node = (ShellNode) e.getValue();
          stream.printf(
              "setnode %s %s %d %d",
              node.getName(),
              node.getNodeKey().getAddress(),
              node.getNodeKey().getPort(),
              node.getSshPort());
          stream.println();

        } else if (e.getValue() instanceof ShellCluster) {
          ShellCluster cluster = (ShellCluster) e.getValue();
          clusters.add(cluster);

        } else if (e.getValue() instanceof String) {
          String str = (String) e.getValue();
          stream.printf("set %s %s", e.getKey(), str);
          stream.println();
        }
      }

      for (ShellCluster cluster : clusters) {
        if ((cluster.getAddress() != null)
            || (cluster.getTransactionMember() != null)
            || (cluster.getProviderUrl() != null)) {
          if (cluster.getAddress() != null) {
            stream.printf(
                "setcluster %s %s %s %d",
                cluster.getClusterVariableName(),
                cluster.getName(),
                cluster.getAddress(),
                cluster.getPort());

          } else if (cluster.getTransactionMember() != null) {
            stream.printf(
                "setcluster %s %s %s %s",
                cluster.getClusterVariableName(),
                cluster.getName(),
                NotificationMode.FIXED_LIST.toString(),
                cluster.getTransactionMember());

          } else if (cluster.getProviderUrl() != null) {
            stream.printf(
                "setcluster %s %s %s %s",
                cluster.getClusterVariableName(),
                cluster.getName(),
                NotificationMode.PROVIDER.toString(),
                cluster.getProviderUrl());
          }

          for (ShellNode node : cluster.getNodes()) {
            stream.printf(" $%s", ((ShellNode) node).getName());
          }
          stream.println();
        }

        if (cluster.getJdbcAddress() != null) {
          stream.printf(
              "setclusterSQL %s %s %s %d",
              cluster.getClusterVariableName(),
              cluster.getName(),
              cluster.getJdbcAddress(),
              cluster.getJdbcPort());
          stream.println();

        } else if (cluster.getSqlMember() != null) {
          stream.printf(
              "setclusterSQL %s %s %s %s",
              cluster.getClusterVariableName(),
              cluster.getName(),
              NotificationMode.FIXED_LIST.toString(),
              cluster.getSqlMember());
          stream.println();

        } else if (cluster.getSQLProvider() != null) {
          stream.printf(
              "setclusterSQL %s %s %s %s",
              cluster.getClusterVariableName(),
              cluster.getName(),
              NotificationMode.PROVIDER.toString(),
              cluster.getSQLProvider());
          stream.println();
        }
      }

    } catch (FileNotFoundException e) {
      throw new ShellException(
          getMessage("error.cannotSave", target) + " :msg=[" + e.getMessage() + "]", e);

    } catch (UnsupportedEncodingException e) {
      throw new ShellException(
          getMessage("error.saveScript")
              + " :file=["
              + target.getAbsolutePath()
              + "] msg=["
              + e.getMessage()
              + "]",
          e);

    } finally {
      if (stream != null) {
        stream.close();
      }
    }
  }

  /**
   * The main method for sub-command {@code errexit}.<br>
   * Set the flag whether to exit gs shell when an error occurs in the sub-command.
   *
   * @param exitOnError {@code true} to exit when an error occurs, {@code false} to not
   * @see ScriptContext
   */
  @GSCommand(name = "errexit")
  public void setExitOnError(boolean exitOnError) {
    getContext()
        .setAttribute(GridStoreShell.EXIT_ON_ERROR, exitOnError, ScriptContext.ENGINE_SCOPE);
  }

  /**
   * The main method for sub-command {@code echo}.<br>
   * Set the flag whether to display the executed sub-command in the standard output.
   *
   * @param echo {@code true} to display the executed sub-command in the standard output, {@code
   *     false} to not
   * @see ScriptContext
   */
  @GSCommand(name = "echo")
  public void setEcho(boolean echo) {
    getContext().setAttribute(GridStoreShell.ECHO, echo, ScriptContext.ENGINE_SCOPE);
  }

  /**
   * The main method for sub-command {@code sqlcount}.<br>
   * Set the flag whether to execute count query when SQL querying.
   *
   * @param sqlCount if {@code false}, gs shell does not count the number of the result when
   *     querying by sql sub-command and hit count is not displayed, {@code true} to display the hit
   *     count to the standard output
   * @see ScriptContext
   */
  @GSCommand(name = "sqlcount")
  public void setSqlCount(boolean sqlCount) {
    getContext().setAttribute(GridStoreShell.SQL_COUNT, sqlCount, ScriptContext.ENGINE_SCOPE);
  }

  /**
   * The main method for sub-command {@code print}.<br>
   * Display the definition details of the specified character string or variable.
   *
   * @param str string or variable to display.
   */
  @GSCommand(name = "print", assignall = true)
  public void printString(String str) {
    println(str);
  }

  /**
   * The main method for sub-command {@code exec}.<br>
   * Execute an external command.
   *
   * @param command the external command will be executed
   * @param arg the arguments of external command
   * @throws ShellException if specified external command can not run in gs_sh or failed to execute
   *     external command
   */
  @GSCommand(assignall = true)
  public void exec(String command, @GSNullable String arg) {
    try {
      String[] errorCommand = {"cd", "more"};
      for (int i = 0; i < errorCommand.length; i++) {
        if (command.equals(errorCommand[i])) {
          throw new ShellException(getMessage("error.externalCommand"));
        }
      }

      List<String> commandList = new ArrayList<String>();
      commandList.add("/bin/sh");
      commandList.add("-c");
      if (arg == null) {
        commandList.add(command);
      } else {
        commandList.add(command + " " + arg);
      }

      ProcessBuilder pb = new ProcessBuilder(commandList);
      pb.redirectErrorStream(true);
      Process process = pb.start();

      BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
      try {
        String line = null;
        while ((line = br.readLine()) != null) {
          System.out.println(line);
        }
      } finally {
        br.close();
      }

      process.waitFor();

    } catch (Exception e) {
      throw new ShellException(
          getMessage("error.execExternalCommand", command) + " : msg=[" + e.getMessage() + "]", e);
    }
  }

  /**
   * The main method for sub-command {@code sleep}.<br>
   * Set the time for the sleeping function.
   *
   * @param waitSeconds the number of seconds to sleep.
   */
  @GSCommand
  public void sleep(double waitSeconds) {
    try {
      Thread.sleep((long) (waitSeconds * 1000));
    } catch (InterruptedException e) {
      // Do nothing
    }
  }

  /**
   * The main method for sub-command {@code load}.<br>
   * The implementation of load exists in {@code GridStoreScriptEngine}.<br>
   * Unless the method of the command is defined, it will not be displayed in help, so define a
   * dummy.
   */
  @GSCommand
  public void load() {
    // dummy
  }

  /**
   * The main method for sub-command {@code maintenance}.<br>
   * It is a hidden sub-command to set the maintenance mode ON/OFF Do not appear in the help list
   * and cannot use tab keyboard to suggest for hidden sub-command.
   *
   * @param flag {@code true} to set maintenance mode ON, otherwise to back to normal mode
   * @see ScriptContext
   */
  @GSCommand(hidden = true)
  public void maintenance(String flag) {
    Bindings bindings = getContext().getBindings(ScriptContext.ENGINE_SCOPE);
    if (flag.equalsIgnoreCase("on")) {
      bindings.put(GridStoreShell.MAINTENANCE_MODE, GridStoreShell.EXEC_MODE.MAINTENANCE);
    } else {
      bindings.put(GridStoreShell.MAINTENANCE_MODE, GridStoreShell.EXEC_MODE.NORMAL);
    }
  }

  /**
   * The main method for sub-command {@code history}.<br>
   * The implementation of history exists in {@code GridStoreScriptEngine}.<br>
   * Unless the method of the command is defined, it will not be displayed in help, so define a
   * dummy.
   */
  @GSCommand
  public void history() {
    // dummy
  }

  /**
   * The main method for sub-command {@code setsslmode}.<br>
   * Set SSL Mode
   *
   * @param sslMode ssl mode (DISABLED, REQUIRED and VERIFY)
   * @see ScriptContext
   */
  @GSCommand(name = "setsslmode")
  public void setSSLMode(@GSNullable String sslMode) {

    if (sslMode != null) {
      try {
        SslMode.valueOf(sslMode);
      } catch (IllegalArgumentException e) {
        String valueSSLAvailable =
            "["
                + SslMode.DISABLED.getValue()
                + ", "
                + SslMode.REQUIRED.toString()
                + ", "
                + SslMode.VERIFY.getValue()
                + "]";
        throw new ShellException(getMessage("error.illegalEnum", sslMode, valueSSLAvailable));
      }
      if (sslMode.equals(SslMode.REQUIRED.toString())) {
        getContext()
            .setAttribute(
                GridStoreShell.SSL_MODE, SslMode.REQUIRED.getValue(), ScriptContext.ENGINE_SCOPE);
      } else {
        getContext().setAttribute(GridStoreShell.SSL_MODE, sslMode, ScriptContext.ENGINE_SCOPE);
      }

    } else {
      getContext()
          .setAttribute(
              GridStoreShell.SSL_MODE, SslMode.DISABLED.getValue(), ScriptContext.ENGINE_SCOPE);
    }
  }
}















