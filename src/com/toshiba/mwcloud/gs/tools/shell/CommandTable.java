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

import com.toshiba.mwcloud.gs.tools.shell.annotation.GSCommand;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;

/** Stores command classes, provides method to get/register commands and command classes. */
public class CommandTable {
  /** A {@code Map} of command group name 
   * and {@code commandTable}. */
  Map<String, Map<String, Command>> classTable = new TreeMap<String, Map<String, Command>>();

  /** A {@code Map} of command name 
   * and {@code Command}. */
  Map<String, Command> commandTable = new TreeMap<String, Command>();

  /**
   * Get command by command name.
   *
   * @param name name of command
   * @return a command
   */
  public Command get(String name) {
    return commandTable.get(name);
  }

  /**
   * Register a command class.<br>
   * All commands in this command class will be added to 2 maps: {@code classTable} and {@code
   * commandTable}.
   *
   * @param commands command class
   */
  public void register(AbstractCommandClass commands) {
    TreeMap<String, Command> classCommandTable = new TreeMap<String, Command>();
    classTable.put(commands.getCommandGroupName(), classCommandTable);

    for (Method method : commands.getClass().getMethods()) {
      GSCommand anon = method.getAnnotation(GSCommand.class);
      if (anon != null) {
        Command command = new Command(commands, method);
        commandTable.put(command.getName(), command);
        classCommandTable.put(command.getName(), command);
      }
    }
  }

  /**
   * Get class table map.
   *
   * @return class table map
   */
  public Map<String, Map<String, Command>> getClassTable() {
    return classTable;
  }

  /**
   * Get command table map.
   *
   * @return command table map
   */
  public Map<String, Command> getCommandTable() {
    return commandTable;
  }
}
