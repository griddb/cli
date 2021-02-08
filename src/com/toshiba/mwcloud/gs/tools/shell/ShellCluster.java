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

package com.toshiba.mwcloud.gs.tools.shell;

import com.toshiba.mwcloud.gs.tools.common.GSCluster;
import java.util.List;
/**
 * Object that represents a GridDB cluster.
 *
 * @see GSCluster (from gs.tools.common project)
 * @see ShellNode
 */
public class ShellCluster extends GSCluster<ShellNode> {

  private String clusterVariableName;
  private String sqlProviderUrl;

  /**
   * Constructor for {@code ShellCluster}. Constructor with only parameter name cluster variable.
   *
   * @param name name of the {@code ShellCluster}
   */
  public ShellCluster(String name) {
    super();
    clusterVariableName = name;
  }

  /**
   * Constructor for {@code ShellCluster}. Constructor to define a shell cluster follow jdbc.
   *
   * @param name name of the {@code ShellCluster}
   * @param clusterName name of the GridDB cluster
   * @param jdbcAddr JDBC address of the GridDB cluster
   * @param jdbcPort JDBC port of the GridDB cluster
   */
  public ShellCluster(String name, String clusterName, String jdbcAddr, int jdbcPort) {
    super(clusterName, jdbcAddr, jdbcPort);
    clusterVariableName = name;
  }

  /**
   * Constructor for {@code ShellCluster}. Constructor to define a shell cluster follow multicast
   * mode.
   *
   * @param name name of the {@code ShellCluster}
   * @param clusterName name of the GridDB cluster
   * @param multicast MULTICAST address of GridDB cluster
   * @param multicastPort MULTICAST port of GridDB cluster
   * @param nodes list of {@code ShellNode}
   */
  public ShellCluster(
      String name, String clusterName, String multicast, int multicastPort, List<ShellNode> nodes) {
    super(clusterName, multicast, multicastPort, nodes);
    clusterVariableName = name;
  }

  /**
   * Get name of the cluster variable.
   *
   * @return name of the cluster variable
   */
  public String getClusterVariableName() {
    return clusterVariableName;
  }

  /**
   * Set provider URL in case of provider mode.
   *
   * @param url PROVIDER URL
   */
  public void setSQLProvider(String url) {
    sqlProviderUrl = url;
  }

  /**
   * Get provider URL in case of provider mode.
   *
   * @return provider URL
   */
  public String getSQLProvider() {
    return sqlProviderUrl;
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Cluster[name=").append(getName());
    builder.append(",mode=").append(getMode());
    if (getAddress() != null) {
      builder.append(",transaction=").append(getAddress()).append(":").append(getPort());
    } else if (getTransactionMember() != null) {
      builder.append(",transaction=").append(getTransactionMember());
    } else if (getProviderUrl() != null) {
      builder.append(",transaction=").append(getProviderUrl());
    }
    if (getJdbcAddress() != null) {
      builder.append(",sql=").append(getJdbcAddress()).append(":").append(getJdbcPort());
    } else if (getSqlMember() != null) {
      builder.append(",sql=").append(getSqlMember());
    } else if (getSQLProvider() != null) {
      builder.append(",sql=").append(getSQLProvider());
    }
    builder.append(",nodes=(");
    List<ShellNode> nodeList = getNodes();
    for (int i = 0; i < nodeList.size(); i++) {
      if (i != 0) {
        builder.append(",");
      }
      builder.append("$");
      builder.append(nodeList.get(i).getName());
    }
    builder.append(")]");
    return builder.toString();
  }
}
