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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.PatternSyntaxException;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.History.Entry;
import org.jline.reader.LineReader;
import org.jline.reader.LineReader.Option;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.widget.AutosuggestionWidgets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Customize the {@code eval} methods for GridStore script.
 *
 * @see AbstractScriptEngine
 */
public class GridStoreScriptEngine extends AbstractScriptEngine {
  private static final ResourceBundle bundle =
      ResourceBundle.getBundle("com.toshiba.mwcloud.gs.tools.shell.GridStoreShellMessages");

  /** Command table name. */
  public static final String COMMAND_TABLE_NAME = "__COMMANDS__";
  /** Name file history. */
  public static final String HISTORY_FILE = ".gssh_history";
  /** User home */
  public static final String USER_HOME = "user.home";

  /** Default history size */
  public static final int DEFAULT_HISTORY_SIZE = 500;

  /** Set terminal column size* */
  public static final int TERMINAL_COLUMN_SIZE = 360;

  /** Set terminal row size* */
  public static final int TERMINAL_ROW_SIZE = 120;

  private boolean invokeCommand(ScriptContext context, Command command, String parameters) {
    Object result = command.invoke(context, parameters);

    if (command.hasResult() && result != null) {
      String resultStr;

      if (result instanceof String) {
        resultStr = (String) result;
      } else {
        try {
          resultStr =
              new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (JsonProcessingException e) {
          resultStr = result.toString();
        }
      }
      Writer writer = context.getWriter();
      try {
        writer.write(resultStr + "\n");
        writer.flush();
      } catch (IOException e) {
        System.err.println(e);
      }
    }
    return true;
  }

  private final GridStoreScriptEngineFactory factory;

  /**
   * Constructor for {@code GridStoreScriptEngine}.
   *
   * @param factory an instance of {@code GridStoreScriptEngineFactory}
   * @see GridStoreScriptEngine
   */
  public GridStoreScriptEngine(GridStoreScriptEngineFactory factory) {
    this.factory = factory;
  }

  /**
   * Same as <code>eval(Reader, ScriptContext)</code> where the source of the script is a <code>
   * String</code>.
   *
   * @param script the script to be executed by GridStore script engine
   * @param context a {@code ScriptContext} exposing sets of attributes in different scopes
   * @return the value returned from the execution of the script
   * @throws ScriptException if sub-command does not exist or there is an error when executing
   *     sub-command
   * @see ScriptContext
   */
  @Override
  public Object eval(String script, ScriptContext context) throws ScriptException {
    return eval(new StringReader(script), context);
  }

  /**
   * Execute the sub-command obtained from {@code reader}.
   *
   * @param reader the source of the script to be executed by GridStore script engine
   * @param context the <code>ScriptContext</code> passed to GridStore script engine
   * @return the value returned from the execution of the script
   * @throws ShellException if sub-command does not exist
   * @throws ScriptException if there is an error when executing sub-command
   * @see ScriptContext
   */
  @Override
  public Object eval(Reader reader, ScriptContext context) throws ScriptException {
    CommandTable cmdTable = (CommandTable) context.getAttribute(COMMAND_TABLE_NAME);
    BufferedReader bufferedReader = null;
    String line = null;
    LineReader lineReader = null; // JLine 3
    String userHome = System.getProperty(USER_HOME);
    try {
      if (reader == null) {
        List<String> cmdList = new ArrayList<String>();
        for (Map.Entry<String, Map<String, Command>> clazz : cmdTable.getClassTable().entrySet()) {
          for (Command cmd : clazz.getValue().values()) {
            if (!cmd.isHidden()) {
              cmdList.add(cmd.getName());
            }
          }
        }
        TerminalBuilder builder = TerminalBuilder.builder();
        // Enable dumb terminal to avoid dumb warning from jline3
        Terminal terminal = builder.dumb(true).build();
        // Enable echo when using here document follow comment on GitHub at
        // https://github.com/jline/jline3/issues/603#issuecomment-729622565
        if (terminal.getWidth() == 0 || terminal.getHeight() == 0) {
          terminal.setSize(new Size(TERMINAL_COLUMN_SIZE, TERMINAL_ROW_SIZE));
        }
        List<Completer> completors = new LinkedList<Completer>(); // JLine 3
        completors.add(new StringsCompleter(cmdList.toArray(new String[0])));
        // Disable bracketed paste to user can implement pasting multiple lines
        lineReader =
            LineReaderBuilder.builder()
                .terminal(terminal)
                .option(Option.BRACKETED_PASTE, false)
                .option(Option.HISTORY_IGNORE_SPACE, false)
                .parser(new DefaultParser())
                .history(new DefaultHistory())
                .completer(new ArgumentCompleter(completors))
                .build();
        KeyMap<Binding> keymap = lineReader.getKeyMaps().get(LineReader.MAIN);
        // Set key map Home keyboard
        keymap.bind(new Reference("beginning-of-line"), "\033[1~");
        // Set key map End keyboard
        keymap.bind(new Reference("end-of-line"), "\033[4~");
        // Create autosuggestion widgets
        AutosuggestionWidgets autosuggestionWidgets = new AutosuggestionWidgets(lineReader);
        if (Terminal.TYPE_DUMB.equals(terminal.getType())) {
          // Disable autosuggestions when using here document
          autosuggestionWidgets.disable();
        } else {
          // Enable autosuggestions
          autosuggestionWidgets.enable();
        }
        lineReader.getHistory().save();
        lineReader.setVariable(LineReader.HISTORY_FILE, Paths.get(userHome, HISTORY_FILE));
        lineReader.getHistory().load();
      } else {
        bufferedReader = new BufferedReader(reader);
      }

      while (true) {
        String prompt = "gs";
        Object mode = getContext().getAttribute(GridStoreShell.MAINTENANCE_MODE);
        if ((mode != null)
            && ((GridStoreShell.EXEC_MODE) mode == GridStoreShell.EXEC_MODE.MAINTENANCE)) {
          prompt += "(maintenance)";
        }
        Object dbName = getContext().getAttribute(GridStoreShell.CONNECTED_DBNAME);
        if (dbName != null) {
          prompt += "[" + dbName + "]";
        }
        prompt += "> ";
        try {
          if (lineReader != null) {
            line = lineReader.readLine(prompt);
          } else {
            line = bufferedReader.readLine();
          }
          if (line == null) {
            break;
          }

          line = line.trim();
          if (line.equals("") || line.startsWith("#")) {
            continue;
          }

          boolean isInteractive = (lineReader != null);
          if (isInteractive == false && isEcho(context)) {
            System.out.print(prompt);
          }

          String[] tokens = line.split("\\s+", 2);
          String name = tokens[0].toLowerCase();
          String params = tokens.length == 1 ? "" : tokens[1];

          if (name.equals("load")) { // load command
            loadScript(params);
          } else if (name.equals("history")) { // history command
            loadHistory(params, lineReader);
          } else { // other commands
            Command command = cmdTable.get(name);
            if (command == null) {
              throw new ShellException(
                  MessageFormat.format(bundle.getString("error.commandNotFound"), name));
            }
            if (command.isMultiLine()) {
              if (lineReader != null) {
                params = readAdditionalLines(lineReader, params);
              } else {
                params = readAdditionalLines(bufferedReader, params);
              }
            }

            if (isEcho(context)) {
              System.out.println(name + " " + params);
            }

            invokeCommand(context, command, params);
            System.out.flush();
          }
        } catch (StringIndexOutOfBoundsException | PatternSyntaxException e) {
          line = "";
          Logger logger = LoggerFactory.getLogger(GridStoreScriptEngine.class);
          logger.warn("", e);
        } catch (EndOfFileException e) {
          // user cancelled application with Ctrl+D
          System.exit(0);
        } catch (UserInterruptException e) {
          // user cancelled line with Ctrl+C
          System.exit(130);
        } catch (Exception e) {
          System.out.println(e.getMessage());
          Logger logger = LoggerFactory.getLogger(GridStoreScriptEngine.class);
          logger.error("", e);
          if (isExitOnError(context)) {
            System.exit(1);
          }
        }
      }
      return null;
    } catch (IOException e) {
      throw new ScriptException(e);
    }
  }

  private String readAdditionalLines(BufferedReader bufferedReader, String firstLine)
      throws IOException {
    StringBuilder builder = new StringBuilder(firstLine);
    String line = firstLine;
    while (!line.endsWith(";")) {
      System.out.print("> ");
      line = bufferedReader.readLine();
      if (line == null) {
        throw new ShellException(bundle.getString("error.eofAtMultilineCommand"));
      }
      builder.append("\n").append(line);
    }
    return builder.substring(0, builder.length() - 1);
  }

  private String readAdditionalLines(LineReader bufferedReader, String firstLine)
      throws IOException {
    StringBuilder builder = new StringBuilder(firstLine);
    String line = firstLine;
    while (!line.endsWith(";")) {
      line = bufferedReader.readLine("> ");
      if (line == null) {
        throw new ShellException(bundle.getString("error.eofAtMultilineCommand"));
      }
      builder.append("\n").append(line);
    }
    return builder.substring(0, builder.length() - 1);
  }

  private void loadScript(String filename) throws ScriptException {
    File target;
    if (filename.equals("")) {
      String userHome = System.getProperty(USER_HOME);
      target = new File(userHome, ".gsshrc");
    } else {
      String ext = GridStoreShell.getFileExtension(filename);
      if ((ext == null) || !ext.equalsIgnoreCase("gsh")) {
        throw new ShellException(
            bundle.getString("error.scriptExtension") + ": file=[" + filename + "]");
      }
      target = new File(filename);
    }

    Reader reader = null;
    try {
      reader = new InputStreamReader(new FileInputStream(target), "UTF-8");
      eval(reader, context);

    } catch (FileNotFoundException e) {
      throw new ShellException(
          bundle.getString("error.fileNotFound")
              + " :file=["
              + target.getAbsolutePath()
              + "] msg=["
              + e.getMessage()
              + "]",
          e);
    } catch (Exception e) {
      throw new ShellException(
          bundle.getString("error.fileRead")
              + " :file=["
              + target.getAbsolutePath()
              + "] msg=["
              + e.getMessage()
              + "]",
          e);
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private void loadHistory(String params, LineReader lineReader) throws IOException {
    if (!params.equals("")) {
      throw new ShellException(bundle.getString("error.tooMuchArg"));
    }
    // check syntax history file and load
    lineReader.getHistory().load();
    // print history sub-command to console
    ListIterator<Entry> listEntryHistory = lineReader.getHistory().iterator();
    int firstIndex = lineReader.getHistory().first();
    while (listEntryHistory.hasNext()) {
      firstIndex++;
      System.out.println("  " + firstIndex + "  " + listEntryHistory.next().line());
    }
  }

  /**
   * Return an implementation of {@code Bindings} ({@code SimpleBindings}).
   *
   * @return a {@code SimpleBindings} that can be used to replace the state of GridStoreScriptEngine
   * @see SimpleBindings
   */
  @Override
  public Bindings createBindings() {
    return new SimpleBindings();
  }

  /**
   * Return a {@code GridStoreScriptEngineFactory}.
   *
   * @return a {@code GridStoreScriptEngineFactory} object
   */
  @Override
  public ScriptEngineFactory getFactory() {
    return factory;
  }

  private boolean isEcho(ScriptContext context) {
    Object object = context.getAttribute(GridStoreShell.ECHO);
    return object != null ? (Boolean) object : false;
  }

  private boolean isExitOnError(ScriptContext context) {
    Object object = context.getAttribute(GridStoreShell.EXIT_ON_ERROR);
    return object != null ? (Boolean) object : false;
  }
}
