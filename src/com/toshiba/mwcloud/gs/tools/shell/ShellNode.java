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

import com.toshiba.mwcloud.gs.tools.common.GSNode;
import com.toshiba.mwcloud.gs.tools.common.NodeKey;

/**
 * Object that represents a GridDB node.
 *
 * @see GSNode (from gs.tools.common project)
 */
public class ShellNode extends GSNode {

  private String name = "";

  /**
   * Constructor for {@code ShellNode}. Constructor to define a shell node with specific NodeKey
   * which represents address and port of a GridDB node.
   *
   * @param name name of the {@code ShellNode}
   * @param nodeKey object that represents address and port of a GridDB node
   * @param sshPort port of secure shell for GridDB node
   */
  public ShellNode(String name, NodeKey nodeKey, int sshPort) {
    super(nodeKey, sshPort);
    this.name = name;
  }

  /**
   * Constructor for {@code ShellNode}. Constructor to define a shell node with specific address of
   * the GridDB node.
   *
   * @param name name name of the GridDB node
   * @param address address of the GridDB node
   * @param port port of the GridDB node
   * @param sshPort port of secure shell for GridDB node
   */
  public ShellNode(String name, String address, int port, int sshPort) {
    super(address, port, sshPort);
    this.name = name;
  }

  /**
   * Get name of the {@code ShellNode}.
   *
   * @return name of the {@code ShellNode}
   */
  public String getName() {
    return name;
  }
}
