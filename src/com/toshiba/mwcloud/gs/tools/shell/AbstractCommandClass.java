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

import com.toshiba.mwcloud.gs.tools.common.GridStoreCommandException;
import java.io.IOException;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javax.script.ScriptContext;

/**
 * {@code AbstractCommandClass} contains some basic methods for printing message to the output.
 *
 * <p>All sub-command classes must be extended from this class.
 */
public abstract class AbstractCommandClass {

  static ScriptContext context;

  /**
   * Get command group name.
   *
   * @return name of the sub-command group
   */
  public abstract String getCommandGroupName();

  /** Release the resources used by this command class. */
  public void close() {}

  /**
   * Print a string to output.
   *
   * @param str string to print
   * @throws ShellException if an I/O error occurs
   * @see ScriptContext
   */
  public synchronized void print(String str) {
    try {
      Writer writer = getContext().getWriter();
      writer.write(str);
      writer.flush();
    } catch (IOException e) {
      throw new ShellException(e);
    }
  }

  /**
   * Print a string to output with a new-line character.
   *
   * @param str string to print
   * @throws ShellException if an I/O error occurs
   * @see ScriptContext
   */
  public void println(String str) {
    print(str + "\n");
  }

  /**
   * Print objects to output with specified format.
   *
   * @param format format to print
   * @param args an array of objects needed to print
   * @throws ShellException if an I/O error occurs
   * @see ScriptContext
   */
  public void printf(String format, Object... args) {
    print(String.format(format, args));
  }

  /**
   * Print objects to output with specified format with a new-line character.
   *
   * @param format format format to print
   * @param args an array of objects needed to print
   * @throws ShellException if an I/O error occurs
   * @see ScriptContext
   */
  public void printfln(String format, Object... args) {
    print(String.format(format, args) + "\n");
  }

  protected ScriptContext getContext() {
    return context;
  }

  protected ShellCluster updateClusterNodes(ShellCluster cluster) {
    return updateClusterNodes(cluster, true);
  }

  protected ShellCluster updateClusterNodes(ShellCluster cluster, boolean checkFlag) {

    List<ShellNode> newNodes = new ArrayList<ShellNode>(cluster.getNodes().size());
    for (ShellNode node : cluster.getNodes()) {

      Object nodeVal = context.getAttribute(((ShellNode) node).getName());
      if (nodeVal.getClass() == ShellNode.class) {
        newNodes.add((ShellNode) nodeVal);
      } else {
        ResourceBundle bundle =
            ResourceBundle.getBundle(GridStoreShell.class.getName() + "Messages");
        throw new ShellException(
            MessageFormat.format(
                bundle.getString("error.clusterNode"),
                ((ShellCluster) cluster).getClusterVariableName(),
                ((ShellNode) node).getName()));
      }
    }
    cluster.setNodes(newNodes);

    if (checkFlag) {
      try {
        cluster.checkNodes();
      } catch (GridStoreCommandException e) {
        ResourceBundle bundle =
            ResourceBundle.getBundle(GridStoreShell.class.getName() + "Messages");
        throw new ShellException(
            bundle.getString("error.clusterNode2") + ": msg=[" + e.getMessage() + "]", e);
      }
    }

    return cluster;
  }

  /**
   * Get strings formatted by the pattern which is corresponded to the {@code key} in the resource
   * bundle.
   *
   * @param key key of the pattern in the resource bundle
   * @param args objects needed to format
   * @return formatted string
   */
  public String getMessage(String key, Object... args) {
    String bundleName = getClass().getName() + "Messages";
    ResourceBundle bundle = ResourceBundle.getBundle(bundleName);
    String string = bundle.getString(key);
    return MessageFormat.format(string, args);
  }
}
