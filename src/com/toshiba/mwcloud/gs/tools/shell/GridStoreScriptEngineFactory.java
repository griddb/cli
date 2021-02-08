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

import java.util.Arrays;
import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

/**
 * Class used to describe and instantiate GridStoreScriptEngine.
 *
 * @see ScriptEngineFactory
 */
public class GridStoreScriptEngineFactory implements ScriptEngineFactory {

  /**
   * Get the script engine full name.
   *
   * @return full name of the engine
   */
  @Override
  public String getEngineName() {
    return "GridStoreScriptEngine";
  }

  /**
   * Get the script engine version.
   *
   * @return engine version
   */
  @Override
  public String getEngineVersion() {
    return "2.0";
  }

  /**
   * Get the list of supported file extensions.
   *
   * @return list of supported file extensions
   */
  @Override
  public List<String> getExtensions() {
    return Arrays.asList("gsh");
  }

  /**
   * Get the list of MIME type associated with scripts that can be executed by the engine.
   *
   * @return list of MIME types
   */
  @Override
  public List<String> getMimeTypes() {
    return Arrays.asList("text/gsscript");
  }

  /**
   * Get the script engine short name.
   *
   * @return list of short name
   */
  @Override
  public List<String> getNames() {
    return Arrays.asList("GridStoreScript");
  }

  /**
   * Get the name of the scripting language supported by GridStoreScriptEngine.
   *
   * @return script language name
   */
  @Override
  public String getLanguageName() {
    return "GridStoreScript";
  }

  /**
   * Get the version of the scripting language supported by GridStoreScriptEngine.
   *
   * @return version of GridStoreScript
   */
  @Override
  public String getLanguageVersion() {
    return "2.0";
  }

  /**
   * Get the value of an attribute by {@code key}.
   *
   * @param key name of the parameter
   * @return value for the given name. Return {@code null} if no value is assigned to the {@code
   *     key}
   */
  @Override
  public Object getParameter(String key) {
    if (ScriptEngine.ENGINE.equals(key)) {
      return getEngineName();
    } else if (ScriptEngine.ENGINE_VERSION.equals(key)) {
      return getEngineVersion();
    } else if (ScriptEngine.NAME.equals(key)) {
      return getNames();
    } else if (ScriptEngine.LANGUAGE.equals(key)) {
      return getLanguageName();
    } else if (ScriptEngine.LANGUAGE_VERSION.equals(key)) {
      return getLanguageVersion();
    } else {
      return null;
    }
  }

  /**
   * Get the string that's used to invoke a method of a Java object using the syntax of the
   * GridStoreScript.
   *
   * @param obj name representing the object whose method is to be invoked
   * @param m name of the method to invoke
   * @param args names of the arguments in the method call
   * @return string used to invoke the method in the syntax of the GridStoreScript
   */
  @Override
  public String getMethodCallSyntax(String obj, String m, String... args) {
    StringBuilder builder = new StringBuilder(m);
    for (String s : args) {
      builder.append(" ");
      builder.append(s);
    }
    return builder.toString();
  }

  /**
   * Get a string that's used as a statement to display the specified String using the syntax of the
   * GridStoreScript.
   *
   * @param toDisplay string to be displayed by the returned statement
   * @return string used to display the String in the syntax of the GridStoreScript
   */
  @Override
  public String getOutputStatement(String toDisplay) {
    return "echo " + toDisplay;
  }

  /**
   * Get a valid scripting language executable program with given statements.
   *
   * @param statements statements to be executed
   * @return the program
   */
  @Override
  public String getProgram(String... statements) {
    StringBuilder builder = new StringBuilder();
    for (String s : statements) {
      builder.append(s);
      builder.append("\n");
    }
    return builder.toString();
  }

  /**
   * Returns an instance of the ScriptEngine associated with this ScriptEngineFactory.
   *
   * @return a new GridStoreScriptEngine instance
   */
  @Override
  public ScriptEngine getScriptEngine() {
    return new GridStoreScriptEngine(this);
  }
}
