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

import com.fasterxml.jackson.databind.JsonNode;
import com.toshiba.mwcloud.gs.tools.common.CombinedStatus;
import com.toshiba.mwcloud.gs.tools.common.CompositeWatcher;
import com.toshiba.mwcloud.gs.tools.common.GSCluster;
import com.toshiba.mwcloud.gs.tools.common.GSNode;
import com.toshiba.mwcloud.gs.tools.common.GSNodeStat;
import com.toshiba.mwcloud.gs.tools.common.GridStoreCommandException;
import com.toshiba.mwcloud.gs.tools.common.GridStoreCommandUtils;
import com.toshiba.mwcloud.gs.tools.common.GridStoreWebAPI;
import com.toshiba.mwcloud.gs.tools.common.GridStoreWebAPIException;
import com.toshiba.mwcloud.gs.tools.common.NotificationMode;
import com.toshiba.mwcloud.gs.tools.common.NullWatcher;
import com.toshiba.mwcloud.gs.tools.common.Watcher;
import com.toshiba.mwcloud.gs.tools.shell.AbstractCommandClass;
import com.toshiba.mwcloud.gs.tools.shell.GridStoreShell;
import com.toshiba.mwcloud.gs.tools.shell.ShellCluster;
import com.toshiba.mwcloud.gs.tools.shell.ShellException;
import com.toshiba.mwcloud.gs.tools.shell.ShellNode;
import com.toshiba.mwcloud.gs.tools.shell.annotation.GSCommand;
import com.toshiba.mwcloud.gs.tools.shell.annotation.GSNullable;
import com.toshiba.mwcloud.gs.tools.shell.commands.BasicCommandClass.ModOperator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.script.ScriptContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import com.toshiba.mwcloud.gs.tools.common.GSCluster;
// import com.toshiba.mwcloud.gs.tools.common.GSNode;

/**
 * The cluster command class contains some commands to manage GridDB cluster.
 *
 * @see AbstractCommandClass
 */
public class ClusterCommandClass extends AbstractCommandClass {
  private static final int THREAD_COUNT = 8;
  private static Logger logger = LoggerFactory.getLogger(ClusterCommandClass.class);

  private void checkUserIdAndPassword(String userId, String password) {
    if (userId == null || password == null) {
      throw new ShellException(getMessage("error.userPasswordNull"));
    }
  }

  private void checkUserIdAndPassword(String userId, String password, String osPassword) {
    if (userId == null || password == null || osPassword == null) {
      throw new ShellException(getMessage("error.userPasswordOspasswordNull"));
    }
  }

  private void checkClusterNode(ShellCluster cluster) {
    if (cluster.getNodes().isEmpty()) {
      throw new ShellException(
          getMessage("error.clusterNodeNull") + " (" + cluster.getClusterVariableName() + ")");
    }
  }

  private void checkVarName(String name) {
    if (!name.matches("^[0-9a-zA-Z_]+$")) {
      throw new ShellException(getMessage("error.invalidVarName", name));
    }
  }

  /**
   * Get command group name (cluster).
   *
   * @return command group name (cluster)
   */
  @Override
  public String getCommandGroupName() {
    return "cluster";
  }

  /**
   * The main method for sub-command {@code joincluster}.<br>
   * Join a node that is temporarily left from the cluster by {@code leavecluster} sub-command or
   * failure into the cluster. Need to run with administrator user.
   *
   * @param cluster cluster variable
   * @param node node variable
   * @param waitSeconds waiting time (in second))
   * @throws GridStoreCommandException exception when an expected error occurs
   * @throws IllegalStateException exception when node variable is not in cluster definition
   * @throws ShellException of user name/password variable is not set or node is not defined in the
   *     cluster variable
   * @see GridStoreCommandUtils#joinCluster
   * @see ScriptContext
   */
  @GSCommand
  public void joinCluster(ShellCluster cluster, ShellNode node, @GSNullable Integer waitSeconds)
      throws GridStoreCommandException {
    String userId = (String) getContext().getAttribute(GridStoreShell.USER);
    String password = (String) getContext().getAttribute(GridStoreShell.PASSWORD);
    checkUserIdAndPassword(userId, password);
    checkClusterNode(cluster);
    int waitSecondsVal = getWaitTime(waitSeconds);
    if (isSystemSSL()) {
      node.setSystemSSL(true);
    }
    Watcher watcher = GridStoreCommandUtils.joinCluster(cluster, node, userId, password);

    if (watcher == NullWatcher.INSTANCE) {
      println(getMessage("warning.joincluster"));
      return;
    }

    if (0 < waitSecondsVal) {
      println(getMessage("message.waitNodeJoining"));
      boolean completed = watcher.waitCompletion(waitSecondsVal);
      if (completed) {
        println(getMessage("message.nodeJoined"));
      } else {
        println(getMessage("error.timeout"));
      }
    }
  }

  /**
   * The main method for sub-command {@code appendcluster}.<br>
   * Add an undefined node to a pre-defined cluster. Need to run with administrator user.
   *
   * @param cluster cluster variable
   * @param node node variable
   * @param waitSeconds waiting time (in second)
   * @throws GridStoreCommandException exception when an expected error occurs
   * @throws IllegalStateException exception when it meets 1 of the following conditions
   *     <ul>
   *       <li>Master node status is not SERVICING
   *       <li>Cluster is unstable (designated node is different from active node)
   *       <li>Cluster has only 1 node (single node cluster is not expandable)
   *       <li>Node status is not STARTED
   *     </ul>
   *
   * @throws ShellException of user name/password is not set or node is not defined in the cluster
   *     variable
   * @see GridStoreCommandUtils#appendCluster
   * @see ScriptContext
   */
  @GSCommand
  public void appendCluster(ShellCluster cluster, ShellNode node, @GSNullable Integer waitSeconds)
      throws GridStoreCommandException {
    String userId = (String) getContext().getAttribute(GridStoreShell.USER);
    String password = (String) getContext().getAttribute(GridStoreShell.PASSWORD);
    checkUserIdAndPassword(userId, password);
    checkClusterNode(cluster);
    int waitSecondsVal = getWaitTime(waitSeconds);

    if (isSystemSSL()) {
      node.setSystemSSL(true);
    }
    Watcher watcher = GridStoreCommandUtils.appendCluster(cluster, node, userId, password);

    if (0 < waitSecondsVal) {
      println(getMessage("message.waitNodeAppending"));
      boolean completed = watcher.waitCompletion(waitSecondsVal);
      if (completed) {
        println(getMessage("message.nodeAppended"));
        println(
            getMessage(
                "message.nodeAppendedToClusterDef",
                cluster.getClusterVariableName(),
                node.getName()));
        BasicCommandClass basic = new BasicCommandClass();
        println("");
        basic.modifyCluster(cluster.getClusterVariableName(), ModOperator.ADD, node);
        basic.showVariable(cluster.getClusterVariableName());
      } else {
        println(getMessage("error.timeout"));
      }
    }
  }

  /**
   * The main method for sub-command {@code startcluster}.<br>
   * Start the cluster. Need to run with administrator user.
   *
   * @param cluster cluster variable
   * @param waitSeconds waiting time (in second)
   * @throws GridStoreCommandException if it meets 1 of the below conditions:
   *     <ul>
   *       <li>Current cluster configuration is mismatched with cluster definition
   *       <li>There is not enough number of STARTED/WAIT nodes to start cluster
   *       <li>It's unable to join cluster
   *     </ul>
   *
   * @throws IllegalArgumentException exception when there is no node in cluster definition or
   *     current cluster configuration is mismatched with cluster definition
   * @throws ShellException of user name/password is not set or node is not defined in the cluster
   *     variable
   * @see GridStoreCommandUtils#startCluster
   * @see ScriptContext
   */
  @GSCommand
  public void startCluster(ShellCluster cluster, @GSNullable Integer waitSeconds)
      throws GridStoreCommandException {
    String userId = (String) getContext().getAttribute(GridStoreShell.USER);
    String password = (String) getContext().getAttribute(GridStoreShell.PASSWORD);
    checkUserIdAndPassword(userId, password);
    checkClusterNode(cluster);
    int waitSecondsVal = getWaitTime(waitSeconds);
    if (isSystemSSL()) {
      cluster.setSystemSSL(true);
    }
    List<Watcher> watchers =
        GridStoreCommandUtils.startCluster((GSCluster<ShellNode>) cluster, userId, password);

    if (0 < waitSecondsVal) {
      println(getMessage("message.waitClusterStarting"));
      // boolean completed = watcher.waitCompletion(waitSecondsVal);
      boolean completed = new CompositeWatcher(watchers).waitCompletion(waitSecondsVal);
      if (completed) {
        println(getMessage("message.clusterStarted"));
      } else {
        println(getMessage("error.timeout"));
      }
    }
  }

  /**
   * Returns waiting time.
   *
   * @param waitSeconds wait Seconds
   * @return
   */
  private int getWaitTime(Integer waitSeconds) {
    if (waitSeconds == null) {
      return Watcher.WAIT_FOREVER;
    } else if (waitSeconds == 0) {
      return Watcher.WAIT_FOREVER;
    } else {
      return waitSeconds;
    }
  }

  /**
   * The main method for sub-command {@code startnode}.<br>
   * Start the specified node variable or cluster variable. Need to run with administrator user.
   *
   * @param nodes list of node variables
   * @param waitSeconds waiting time (in second)
   * @throws ShellException exception when it meets 1 of the following conditions
   *     <ul>
   *       <li>User name or password or OS password is {@code null}
   *       <li>{@code nodes} is empty
   *       <li>Some nodes are not started
   *     </ul>
   *
   * @throws IllegalStateException when node status is invalid
   * @see GridStoreCommandUtils#startNode
   * @see ScriptContext
   */
  @GSCommand
  public void startNode(ShellNode[] nodes, @GSNullable Integer waitSeconds) {
    final String userId = (String) getContext().getAttribute(GridStoreShell.USER);
    final String password = (String) getContext().getAttribute(GridStoreShell.PASSWORD);
    final String osPassword = (String) getContext().getAttribute(GridStoreShell.OSPASSWORD);
    checkUserIdAndPassword(userId, password, osPassword);
    final int waitSecondsVal = getWaitTime(waitSeconds);

    if (nodes.length == 0) {
      throw new ShellException(getMessage("error.clusterNodeNull"));
    }
    ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
    List<Future<Watcher>> futures = new ArrayList<Future<Watcher>>();
    try {
      for (final ShellNode node : nodes) {
        futures.add(
            pool.submit(
                new Callable<Watcher>() {
                  @Override
                  public Watcher call() throws Exception {
                    try {
                      println(
                          getMessage("message.nodeStarting", node.getName())); // ノード {0} を起動します。
                      if (isSystemSSL()) {
                        node.setSystemSSL(true);
                      }
                      Watcher watcher =
                          GridStoreCommandUtils.startNode(
                              node, userId, password, osPassword, waitSecondsVal);
                      return watcher;
                    } catch (Exception e) {
                      String message =
                          getMessage("error.nodeStarting", node.getName(), e.getMessage());
                      println(message);
                      logger.error(message, e);
                      throw e;
                    }
                  }
                }));
      }
      pool.shutdown();
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      pool.shutdownNow();
    }

    final List<Watcher> watchers = new ArrayList<Watcher>();
    for (Future<Watcher> future : futures) {
      try {
        watchers.add(future.get());
      } catch (InterruptedException e) {
        assert false;
      } catch (ExecutionException e) {
      }
    }
    if (watchers.size() < futures.size()) {
      int errorCount = futures.size() - watchers.size();
      throw new ShellException(
          getMessage("error.someNodesNotStarted", errorCount, watchers.size()));
    }

    if (0 < waitSecondsVal) {
      // println(getMessage("message.waitNodeStarting"));
      boolean completed = new CompositeWatcher(watchers).waitCompletion(waitSecondsVal);
      if (completed) {
        println(getMessage("message.allNodesStarted"));
      } else {
        println(getMessage("error.timeout"));
      }
    }
  }

  private void stopNodeImpl(ShellNode[] nodes, Integer waitSeconds, boolean force)
      throws GridStoreCommandException {
    String userId = (String) getContext().getAttribute(GridStoreShell.USER);
    String password = (String) getContext().getAttribute(GridStoreShell.PASSWORD);
    checkUserIdAndPassword(userId, password);
    int waitSecondsVal = getWaitTime(waitSeconds);

    if (nodes.length == 0) {
      throw new ShellException(getMessage("error.clusterNodeNull"));
    }

    List<Watcher> watchers = new ArrayList<Watcher>(nodes.length);
    for (ShellNode node : nodes) {
      try {
        println(getMessage("message.nodeStopping", node.getName()));
        if (isSystemSSL()) {
          node.setSystemSSL(true);
        }
        watchers.add(GridStoreCommandUtils.stopNode(node, userId, password, force));
        // println(getMessage("message.nodeStopped", node.getName()));
      } catch (Exception e) {
        String message = getMessage("error.nodeStopping", node.getName(), e.getMessage());
        println(message);
        logger.error(message, e);
      }
    }

    if ((0 < waitSecondsVal) && (0 < watchers.size())) {
      // println(getMessage("message.waitNodeStopping"));
      boolean completed = new CompositeWatcher(watchers).waitCompletion(waitSecondsVal);
      if (completed) {
        if (watchers.size() < nodes.length) {
          int errorCount = nodes.length - watchers.size();
          throw new ShellException(
              getMessage("error.someNodesNotStopped", errorCount, watchers.size()));
        } else {
          println(getMessage("message.allNodesStopped"));
        }
      } else {
        println(getMessage("error.timeout"));
      }
    } else {
      if (watchers.size() < nodes.length) {
        int errorCount = nodes.length - watchers.size();
        throw new ShellException(
            getMessage("error.someNodesNotStopped", errorCount, watchers.size()));
      }
    }
  }

  /**
   * The main method for sub-command {@code stopnode}.<br>
   * Stop the specified node variable or cluster variable. Need to run with administrator user.
   *
   * @param nodes list of nodes
   * @param waitSeconds waiting time (in second)
   * @throws GridStoreCommandException when an unexpected error occurs
   * @throws ShellException when it meets 1 of the following conditions:
   *     <ul>
   *       <li>user name or password is {@code null}
   *       <li>{@code nodes} is empty
   *       <li>Some nodes are not stopped
   *     </ul>
   *
   * @throws IllegalStateException when it meets 1 of the following conditions:
   *     <ul>
   *       <li>The node of status 'STARTING' cannot stop
   *       <li>The node joined cluster cannot stop
   *       <li>Node status is invalid
   *     </ul>
   *
   * @see GridStoreCommandUtils#stopNode
   * @see ScriptContext
   */
  @GSCommand
  public void stopNode(ShellNode[] nodes, @GSNullable Integer waitSeconds)
      throws GridStoreCommandException {
    stopNodeImpl(nodes, waitSeconds, false);
  }

  /**
   * The main method for sub-command {@code stopnodeforce}.<br>
   * Force to stop the specified nodes. Need to run with administrator user.
   *
   * @param nodes list of nodes
   * @param waitSeconds waiting time (in second)
   * @throws GridStoreCommandException when an unexpected error occurs
   * @throws ShellException when it meets 1 of the following conditions:
   *     <ul>
   *       <li>user name or password is {@code null}
   *       <li>{@code nodes} is empty
   *       <li>Some nodes are not stopped
   *     </ul>
   *
   * @throws IllegalStateException when it meets 1 of the following conditions:
   *     <ul>
   *       <li>The node of status 'STARTING' cannot stop
   *       <li>The node joined cluster cannot stop
   *       <li>Node status is invalid
   *     </ul>
   *
   * @see GridStoreCommandUtils#stopNode
   * @see ScriptContext
   */
  @GSCommand
  public void stopNodeForce(ShellNode[] nodes, @GSNullable Integer waitSeconds)
      throws GridStoreCommandException {
    stopNodeImpl(nodes, waitSeconds, true);
  }

  /**
   * The main method for sub-command {@code stopcluster}.<br>
   * Stop GridDB cluster. Need to run with administrator user.
   *
   * @param cluster cluster variable
   * @param waitSeconds waiting time (in second)
   * @throws GridStoreCommandException when it meets 1 of the following conditions:
   *     <ul>
   *       <li>Current cluster configuration is mismatched with cluster definition
   *       <li>An error occurred while stopping cluster
   *     </ul>
   *
   * @throws ShellException when user name/password is not set or node is not defined in the cluster
   *     variable
   * @see GridStoreCommandUtils#stopCluster
   * @see ScriptContext
   */
  @GSCommand
  public void stopCluster(ShellCluster cluster, @GSNullable Integer waitSeconds)
      throws GridStoreCommandException {
    String userId = (String) getContext().getAttribute(GridStoreShell.USER);
    String password = (String) getContext().getAttribute(GridStoreShell.PASSWORD);
    checkUserIdAndPassword(userId, password);
    checkClusterNode(cluster);
    int waitSecondsVal = getWaitTime(waitSeconds);

    if (isSystemSSL()) {
      cluster.setSystemSSL(true);
    }
    List<Watcher> watchers = GridStoreCommandUtils.stopCluster(cluster, userId, password);

    if (0 < waitSecondsVal) {
      println(getMessage("message.waitClusterStopping"));
      boolean completed = new CompositeWatcher(watchers).waitCompletion(waitSecondsVal);
      if (completed) {
        println(getMessage("message.clusterStopped"));
      } else {
        println(getMessage("error.timeout"));
      }
    }
  }

  /**
   * The main method for sub-command {@code leavecluster}.<br>
   * Detach the specified node from the cluster. Need to run with administrator user.
   *
   * @param node node variable
   * @param waitSeconds waiting time (in second)
   * @throws GridStoreCommandException when it meets 1 of the following conditions:
   *     <ul>
   *       <li>Some data in node will be unavailable
   *       <li>Failed to check node status
   *       <li>An error occurred while leaving from cluster
   *     </ul>
   *
   * @throws ShellException if user name or password is {@code null}
   * @throws IllegalStateException if node status is invalid
   * @see GridStoreCommandUtils#leaveCluster
   * @see ScriptContext
   */
  @GSCommand
  public void leaveCluster(GSNode node, @GSNullable Integer waitSeconds)
      throws GridStoreCommandException {
    leaveClusterImpl(node, waitSeconds, false);
  }

  /**
   * The main method for sub-command {@code leaveclusterforce}.<br>
   * Force to detach the specified node from the cluster. Need to run with administrator user.
   *
   * @param node node variable
   * @param waitSeconds waiting time (in second)
   * @throws GridStoreCommandException when an error occurred while leaving from cluster
   * @throws ShellException if user name or password is {@code null}
   * @throws IllegalStateException if node status is invalid
   * @see GridStoreCommandUtils#leaveCluster
   * @see ScriptContext
   */
  @GSCommand
  public void leaveClusterForce(GSNode node, @GSNullable Integer waitSeconds)
      throws GridStoreCommandException {
    leaveClusterImpl(node, waitSeconds, true);
  }

  private void leaveClusterImpl(GSNode node, Integer waitSeconds, boolean force)
      throws GridStoreCommandException {
    String userId = (String) getContext().getAttribute(GridStoreShell.USER);
    String password = (String) getContext().getAttribute(GridStoreShell.PASSWORD);
    checkUserIdAndPassword(userId, password);
    int waitSecondsVal = getWaitTime(waitSeconds);
    if (isSystemSSL()) {
      node.setSystemSSL(true);
    }
    Watcher watcher = GridStoreCommandUtils.leaveCluster(node, userId, password, force);

    if (watcher == NullWatcher.INSTANCE) {
      println(getMessage("warning.leavecluster"));
      return;
    }

    if (0 < waitSecondsVal) {
      println(getMessage("message.waitNodeLeaving"));
      boolean completed = watcher.waitCompletion(waitSecondsVal);
      if (completed) {
        println(getMessage("message.nodeLeaved"));
      } else {
        println(getMessage("error.timeout"));
      }
    }
  }

  /**
   * The main method for sub-command {@code stat}.<br>
   * Display the node configuration data. Need to run with administrator user.
   *
   * @param node node variable
   * @return configuration data of node in JSON format
   * @throws GridStoreCommandException when error occurred while getting status info
   * @throws ShellException if user name or password is {@code null}
   * @see GridStoreCommandUtils#getStat
   * @see ScriptContext
   */
  @GSCommand(name = "stat")
  public JsonNode getStat(GSNode node) throws GridStoreCommandException {
    String userId = (String) getContext().getAttribute(GridStoreShell.USER);
    String password = (String) getContext().getAttribute(GridStoreShell.PASSWORD);
    checkUserIdAndPassword(userId, password);
    if (isSystemSSL()) {
      node.setSystemSSL(true);
    }
    JsonNode stat = GridStoreCommandUtils.getStat(node, userId, password);

    return stat;
  }

  /**
   * The main method for sub-command {@code configcluster}.<br>
   * Display the status of an active GridDB cluster, and each node constituting the cluster. Need to
   * run with administrator user.
   *
   * @param cluster cluster variable
   * @return status of cluster and each node
   * @throws GridStoreCommandException when an unexpected error occurs
   * @throws ShellException if user name or password is {@code null}
   * @see GridStoreCommandUtils#getStatCluster
   * @see ScriptContext
   */
  @GSCommand
  public String configcluster(ShellCluster cluster) throws GridStoreCommandException {
    String userId = (String) getContext().getAttribute(GridStoreShell.USER);
    String password = (String) getContext().getAttribute(GridStoreShell.PASSWORD);
    checkUserIdAndPassword(userId, password);

    if (isSystemSSL()) {
      cluster.setSystemSSL(true);
    }
    final boolean result = GridStoreCommandUtils.getStatCluster(cluster, userId, password);

    StringBuilder str = new StringBuilder();
    str.append("Name                  : ");
    str.append(cluster.getClusterVariableName());
    str.append("\nClusterName           : ");
    str.append(cluster.getName());
    str.append("\nDesignated Node Count : ");
    str.append(cluster.getNodes().size());
    str.append("\nActive Node Count     : ");
    str.append(cluster.getStat().getServiceNodeCount() + cluster.getStat().getWaitNodeCount());
    str.append("\nClusterStatus         : ");
    str.append(cluster.getStat().getClusterStatus());

    str.append("\n\nNodes:");
    str.append("\n  Name    Role Host:Port              Status");
    str.append("\n-------------------------------------------------\n");

    List<Object> target = new ArrayList<Object>();
    if (cluster.getStat().getUndefinedNodes() == null) {
      target.addAll(cluster.getNodes());
    } else {
      target = new ArrayList<Object>();
      target.addAll(cluster.getNodes());
      target.addAll(cluster.getStat().getUndefinedNodes());
    }
    for (Object obj : target) {
      GSNode node = (GSNode) obj;
      GSNodeStat stat = node.getStat();

      String tmp = "";
      if (((stat.getCombinedStatus() == CombinedStatus.WAIT)
              || (stat.getCombinedStatus() == CombinedStatus.SERVICING))
          && ((stat.getDesignatedCount() != cluster.getNodes().size())
              || (!stat.getClusterName().equals(cluster.getName())))) {
        tmp = " (" + stat.getClusterName() + ", " + stat.getDesignatedCount() + ")";

        str.append("* ");
      } else if (!(obj instanceof ShellNode) || ((ShellNode) node).getName().isEmpty()) {
        str.append("* ");
      } else {
        str.append("  ");
      }

      if (obj instanceof ShellNode) {
        str.append(String.format("%-9s", ((ShellNode) node).getName()));
      } else {
        str.append(String.format("%-9s", ""));
      }
      str.append(" ");
      switch (stat.getNodeRole()) {
        case MASTER:
          str.append("M  ");
          break;
        case FOLLOWER:
          str.append("F  ");
          break;
        case SUB_MASTER:
          str.append("S  ");
          break;
        default:
          str.append("-  ");
          break;
      }
      str.append(String.format("%-22s", node.getAddress() + ":" + node.getPort()));
      str.append(" ");
      str.append(String.format("%-9s", stat.getCombinedStatus()));
      str.append(" ");
      // str.append(stat.getActiveCount());
      // str.append(" ");
      // str.append(stat.getDesignatedCount());
      str.append(tmp);

      str.append("\n");
    }

    if (!result) {
      str.append("\n");
      str.append(getMessage("warning.configcluster"));
    }

    return str.toString();
  }

  /**
   * The main method for sub-command {@code config}.<br>
   * Display the cluster configuration data. Need to run with administrator user.
   *
   * @param node node variable
   * @return cluster configuration data
   * @throws GridStoreCommandException if an error occurred while getting cluster configuration info
   * @throws ShellException if user name or password is {@code null}
   * @see GridStoreCommandUtils#getConfig
   * @see ScriptContext
   */
  @GSCommand(name = "config")
  public JsonNode getConfig(GSNode node) throws GridStoreCommandException {
    String userId = (String) getContext().getAttribute(GridStoreShell.USER);
    String password = (String) getContext().getAttribute(GridStoreShell.PASSWORD);
    checkUserIdAndPassword(userId, password);
    if (isSystemSSL()) {
      node.setSystemSSL(true);
    }
    return GridStoreCommandUtils.getConfig(node, userId, password);
  }

  /**
   * The main method for sub-command {@code logs}.<br>
   * Displays the log of the specified node.
   *
   * @param node node variable
   * @return log of specified node
   * @throws ShellException if user name or password is {@code null}
   * @throws GridStoreCommandException if an error occurred while getting log
   * @see GridStoreCommandUtils#getLogs
   * @see ScriptContext
   */
  @GSCommand(name = "logs")
  public String[] getLogs(GSNode node) throws GridStoreCommandException {
    String userId = (String) getContext().getAttribute(GridStoreShell.USER);
    String password = (String) getContext().getAttribute(GridStoreShell.PASSWORD);
    checkUserIdAndPassword(userId, password);
    if (isSystemSSL()) {
      node.setSystemSSL(true);
    }
    return GridStoreCommandUtils.getLogs(node, userId, password);
  }

  /**
   * The main method for sub-command {@code logconf}.<br>
   * Display and change output level of a log Need to run with administrator user.
   *
   * @param node node variable
   * @param category log category
   * @param level log level
   * @return log level of all category or {@code null}
   * @throws GridStoreCommandException when an error occurred while getting/setting log
   *     configuration
   * @throws ShellException if user name or password is {@code null}
   * @see GridStoreCommandUtils#getLogConf
   * @see GridStoreCommandUtils#setLogConf
   * @see ScriptContext
   */
  @GSCommand(name = "logconf")
  public Map<String, String> getLogConf(
      GSNode node, @GSNullable String category, @GSNullable String level)
      throws GridStoreCommandException {
    String userId = (String) getContext().getAttribute(GridStoreShell.USER);
    String password = (String) getContext().getAttribute(GridStoreShell.PASSWORD);
    checkUserIdAndPassword(userId, password);
    if (isSystemSSL()) {
      node.setSystemSSL(true);
    }
    if (category == null) {
      return GridStoreCommandUtils.getLogConf(node, userId, password);
    } else if (level == null) {
      Map<String, String> data = GridStoreCommandUtils.getLogConf(node, userId, password, category);
      String retLevel = data.get(category);
      if (retLevel == null) {
        System.out.println(getMessage("error.logconfCategory") + " : category=[" + category + "]");
        return null;
      } else {
        Map<String, String> tmp = new HashMap<String, String>();
        tmp.put(category, retLevel);
        return tmp;
      }

    } else {
      GridStoreCommandUtils.setLogConf(node, userId, password, category, level);
      return null;
    }
  }

  /**
   * Sync the cluster and node definitions. Need to run with administrator user.
   *
   * @param address system address of the node
   * @param restPort system port of the node
   * @param clusterVar cluster variable with default value is "scluster"
   * @param nodePrefixVar node prefix with default value is "snode"
   * @throws GridStoreCommandException when an error occurred while get status of node.
   * @throws GridStoreWebAPIException when an error occurred while get node configure from webapi
   * @throws ShellException if it meets 1 of below conditions:
   *     <ul>
   *       <li>Node is inactive.
   *       <li>Missing argument when using command.
   *       <li>There are too many arguments
   *       <li>Cluster and node prefix variable name not matches with "^[0-9a-zA-Z_]+$"
   *       <li>This argument type is incorrect
   *       <li>Only administrator users are allowed to execute this command.
   *     </ul>
   *
   * @see BasicCommandClass#setNode
   * @see BasicCommandClass#setCluster
   * @see BasicCommandClass#setClusterSQL
   * @see GridStoreCommandUtils#getStat
   * @see GridStoreCommandUtils#getConfig
   * @see GridStoreCommandUtils#getConfig
   * @see GridStoreWebAPI#getNodeConfig
   */
  @GSCommand
  public void sync(
      String address,
      int restPort,
      @GSNullable String clusterVar,
      @GSNullable String nodePrefixVar) {
    String userId = (String) getContext().getAttribute(GridStoreShell.USER);
    String password = (String) getContext().getAttribute(GridStoreShell.PASSWORD);
    checkUserIdAndPassword(userId, password);
    // Set default value is "scluster".
    if (clusterVar == null) {
      clusterVar = "scluster";
    }
    // Set default value is "snode".
    if (nodePrefixVar == null) {
      nodePrefixVar = "snode";
    }
    checkVarName(clusterVar);
    checkVarName(nodePrefixVar);
    GSNode node = new GSNode(address, restPort);
    if (isSystemSSL()) {
      node.setSystemSSL(true);
    }
    try {
      GridStoreWebAPI webapi = new GridStoreWebAPI(node, userId, password);
      final JsonNode configNode = webapi.getNodeConfig();
      JsonNode nodeStatus = GridStoreCommandUtils.getStat(node, userId, password);
      // Check node is inactive.
      if (getNodeStatus(nodeStatus).equals("INACTIVE")) {
        throw new ShellException(getMessage("error.nodeInactive"));
      }
      // Check cluster operation
      if (getClusterStatus(nodeStatus).equals("SUB_CLUSTER")) {
        throw new ShellException(getMessage("error.clusterNotOperation"));
      }

      String clusterName = GridStoreCommandUtils.getClusterName(nodeStatus);
      String masterNodeAddress = getMasterNodeAddress(nodeStatus);
      GSNode nodeMaster = new GSNode(masterNodeAddress, restPort);
      if (isSystemSSL()) {
        nodeMaster.setSystemSSL(true);
      }
      JsonNode statusMaster = GridStoreCommandUtils.getStat(nodeMaster, userId, password);
      // Get node list by get status master node
      JsonNode nodeList = getNodeList(statusMaster);
      BasicCommandClass basicCommandClass = new BasicCommandClass();
      ShellNode[] listShellNode = new ShellNode[nodeList.size()];

      // Set list node variable
      for (int i = 0; i < nodeList.size(); i++) {
        String addressNode = getAddressNode(nodeList.get(i));
        String nodeName = nodePrefixVar + (i + 1);
        basicCommandClass.setNode(nodeName, addressNode, restPort, null);
        ShellNode shellNode =
            new ShellNode(nodeName, addressNode, restPort, GSNode.DEFAULT_SSH_PORT);
        listShellNode[i] = shellNode;
      }
      String notificationMode = getNotificationMode(nodeStatus);
      setClusterNodeVariableSync(
          clusterVar,
          userId,
          password,
          clusterName,
          nodeMaster,
          nodeList,
          basicCommandClass,
          listShellNode,
          notificationMode,
          configNode,
          nodeStatus);

    } catch (GridStoreWebAPIException e) {
      throw new ShellException(
          getMessage("error.syncingClusterNode") + " : msg=[" + e.getMessage() + "]", e);
    } catch (GridStoreCommandException e1) {
      throw new ShellException(
          getMessage("error.syncingClusterNode") + " : msg=[" + e1.getMessage() + "]", e1);
    }
  }

  private void setClusterNodeVariableSync(
      String clusterVar,
      String userId,
      String password,
      String clusterName,
      GSNode nodeMaster,
      JsonNode nodeList,
      BasicCommandClass basicCommandClass,
      ShellNode[] listShellNode,
      String notificationMode,
      JsonNode configNode,
      JsonNode statusNode)
      throws GridStoreCommandException {

    // Define the cluster configuration in the cluster variable with multicast,
    // provider or fixed_list
    if (notificationMode.equalsIgnoreCase(NotificationMode.MULTICAST.toString())) {
      String notificationAddress = getAddressMulticast(configNode);
      // Get port transaction number of multicast
      String transactionPort = getTransactionPort(configNode);

      // Set variable cluster
      basicCommandClass.setCluster(
          clusterVar, clusterName, notificationAddress, transactionPort, listShellNode);
      if (!isVersionStandardEdition(statusNode)) {
        // Get port sql number of multicast
        String sqlPort = getSqlPort(configNode);
        basicCommandClass.setClusterSQL(clusterVar, clusterName, notificationAddress, sqlPort);
      }

    } else if (notificationMode.equalsIgnoreCase(NotificationMode.FIXED_LIST.toString())) {
      StringBuilder transactionMember = new StringBuilder();
      JsonNode notificationMember = getNotificationMember(configNode);
      for (int i = 0; i < nodeList.size(); i++) {
        String transactionMemberAddress = getTransactionMemberAddress(notificationMember.get(i));
        transactionMember.append(transactionMemberAddress);
        transactionMember.append(":" + getTransactionMemberPort(notificationMember.get(i)));
        if (i + 1 < nodeList.size()) {
          transactionMember.append(",");
        }
      }
      // Set variable cluster
      basicCommandClass.setCluster(
          clusterVar, clusterName, notificationMode, transactionMember.toString(), listShellNode);
      if (!isVersionStandardEdition(statusNode)) {
        StringBuilder sqlAddressFixedList = new StringBuilder();
        for (int i = 0; i < nodeList.size(); i++) {
          String sqlAddress = getSqlPortFixedListAddress(notificationMember.get(i));
          sqlAddressFixedList.append(sqlAddress);
          sqlAddressFixedList.append(":" + getSqlPortFixedListPort(notificationMember.get(i)));
          if (i + 1 < nodeList.size()) {
            sqlAddressFixedList.append(",");
          }
        }
        basicCommandClass.setClusterSQL(
            clusterVar, clusterName, notificationMode, sqlAddressFixedList.toString());
      }
    } else {
      String url = getNotificationProviderUrl(configNode);
      // Set variable cluster
      basicCommandClass.setCluster(clusterVar, clusterName, notificationMode, url, listShellNode);
      if (!isVersionStandardEdition(statusNode)) {
        basicCommandClass.setClusterSQL(clusterVar, clusterName, notificationMode, url);
      }
    }
  }

  /**
   * Get node status.
   *
   * @param stats JSON node
   * @return node status
   */
  private static String getNodeStatus(JsonNode nodeStatus) {
    return nodeStatus.path("cluster").path("nodeStatus").textValue();
  }

  /**
   * Get master node address.
   *
   * @param stats JSON node
   * @return master node address
   */
  private static String getMasterNodeAddress(JsonNode nodeStatus) {
    return nodeStatus.path("cluster").path("master").path("address").textValue();
  }

  /**
   * Get notification mode.
   *
   * @param stats JSON node
   * @return notification mode
   */
  private static String getNotificationMode(JsonNode configNode) {
    return configNode.path("cluster").path("notificationMode").textValue();
  }

  /**
   * Get address node.
   *
   * @param stats JSON node
   * @return address node
   */
  private static String getAddressNode(JsonNode nodeAddressList) {
    return nodeAddressList.path("address").textValue();
  }

  /**
   * Get node list.
   *
   * @param stats JSON node
   * @return node list
   */
  private static JsonNode getNodeList(JsonNode statusNodeMaster) {
    return statusNodeMaster.path("cluster").path("nodeList");
  }

  private static String getNotificationProviderUrl(JsonNode configNode) {
    return configNode.path("cluster").path("notificationProvider").path("url").textValue();
  }

  private static String getTransactionPort(JsonNode configNode) {
    return configNode.path("transaction").path("notificationPort").toString();
  }

  private static String getSqlPort(JsonNode configNode) {
    return configNode.path("sql").path("notificationPort").toString();
  }

  private static String getAddressMulticast(JsonNode configNode) {
    return configNode.path("cluster").path("notificationAddress").textValue();
  }

  private static JsonNode getNotificationMember(JsonNode configNode) {
    return configNode.path("cluster").path("notificationMember");
  }

  private static String getTransactionMemberPort(JsonNode notificationMember) {
    return notificationMember.path("transaction").path("port").toString();
  }

  private static String getTransactionMemberAddress(JsonNode notificationMember) {
    return notificationMember.path("transaction").path("address").textValue();
  }

  private static String getSqlPortFixedListPort(JsonNode notificationMember) {
    return notificationMember.path("sql").path("port").toString();
  }

  private static String getSqlPortFixedListAddress(JsonNode notificationMember) {
    return notificationMember.path("sql").path("address").textValue();
  }

  private static String getClusterStatus(JsonNode nodeStatus) {
    return nodeStatus.path("cluster").path("clusterStatus").textValue();
  }

  private static boolean isVersionStandardEdition(JsonNode statusNode) {
    if (statusNode.path("version").textValue().contains("SE")) {
      return true;
    } else {
      return false;
    }
  }

  private boolean isSystemSSL() {
    String sslMode = (String) getContext().getAttribute(GridStoreShell.SSL_MODE);
    if (sslMode == null) {
      return false;
    }
    if (sslMode.equals(BasicCommandClass.SslMode.DISABLED.toString())) {
      return false;
    } else {
      return true;
    }
  }
}
