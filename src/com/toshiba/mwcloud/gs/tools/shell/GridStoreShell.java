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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class of gs shell, used to initialize GridStore script engine, read and execute sub-command
 * sequence.
 */
public class GridStoreShell {
  private static GridStoreShell instance;

  private static final ResourceBundle bundle =
      ResourceBundle.getBundle("com.toshiba.mwcloud.gs.tools.shell.GridStoreShellMessages");

  private static final String COMMAND_VARNAME_PREFIX = "__";

  /** Version of gs shell. */
  public static final String VERSION_INFO = bundle.getString("version");

  // 変数名
  /** Variable that stores user name of GridDB cluster. */
  public static final String USER = "user";

  /** Variable that stores password of GridDB cluster. */
  public static final String PASSWORD = "password";

  /** Variable that stores password of user 'gsadm'. */
  public static final String OSPASSWORD = "ospassword";

  /** Variable that stores the flag whether to exit when error occurs. */
  public static final String EXIT_ON_ERROR = "__exitonerror";

  /**
   * Variable that stores the flag whether to print the sub-command to the output after executing a
   * sub-command.
   */
  public static final String ECHO = "__echo";

  /**
   * Variable that store the flag whether to print the number of acquired rows after executing a
   * SQL.
   */
  public static final String SQL_COUNT = "__sqlcount";

  /** Variable that store the value of mode of gs shell (maintenance mode or normal mode). */
  public static final String MAINTENANCE_MODE = "__maintenance";

  /** Variable that store the connected database name. */
  public static final String CONNECTED_DBNAME = "__connectedDbName";

  /** Variable that store the value of fail-over timeout. */
  public static final String FAILOVER_TIMEOUT = "GS_FAILOVER_TIMEOUT";

  /** Variable that store the value of transaction timeout. */
  public static final String TRANSACTION_TIMEOUT = "GS_TRANSACTION_TIMEOUT";

  /** Variable that store the value of login timeout. */
  public static final String LOGIN_TIMEOUT = "GS_LOGIN_TIMEOUT";

  /** Variable that store the value of fetch size. */
  public static final String NAME_FETCH_SIZE = "GS_FETCH_SIZE";

  /** Variable that store null standard output string. */
  public static final String NAME_NULL_STDOUT = "GS_NULL_STDOUT";

  /** Variable that store null CSV standard. */
  public static final String NAME_NULL_CSV = "GS_NULL_CSV";

  /** Variable that store the value of TQL fetch mode. */
  public static final String TQL_FETCH_MODE = "GS_TQL_FETCH_MODE";

  private static final String OPTION_HELP2 = "help2";
  private static final String OPTION_HISTORY_COUNT = "historyCount";
  private static final String OPTION_CMDLINE_TYPE = "cmdLineType";

  /** Variable that store the value of timezone. */
  public static final String TIMEZONE = "timezone";

  /** Variable that stores value of authentication method. */
  public static final String AUTHENTICATION = "authentication";

  /** Variable that store value of notification interface address. */
  public static final String NOTIFICATION_INTERFACE_ADDRESS = "notificationInterfaceAddress";

  /** Variable that store value of ssl mode */
  public static final String SSL_MODE = "sslMode";

  /** Variable that stores the flag whether to SSL connection */
  public static final String SYSTEM_SSL = "__systemssl";

  private static COMMANDLINE_TYPE cmdlineType = COMMANDLINE_TYPE.EXPAND;

  private enum COMMANDLINE_TYPE {
    STANDARD,
    EXPAND;
  }

  /** gs shell execute mode. */
  public enum EXEC_MODE {
    NORMAL,
    MAINTENANCE;
  }

  /** Number of saved commands (command history), default is 20. */
  public static int m_historyCount = 20;

  private List<AbstractCommandClass> commandClassList = new ArrayList<AbstractCommandClass>();

  /**
   * Get the extension of a file.
   *
   * @param filename file name
   * @return the extension of the file or empty string if file has no extension
   */
  public static String getFileExtension(String filename) {
    String basename = new File(filename).getName();
    int dotPos = basename.lastIndexOf('.');
    if (dotPos == -1) {
      return "";
    }
    return basename.substring(dotPos + 1);
  }

  /**
   * Initialize GridStore script engine then execute the sub-command.
   *
   * @param scriptFiles a files that contains some GridStore script commands which is executed right
   *     after gs shell is started
   * @throws ShellException if script file does not have '.gsh' extension or command is not found
   * @throws Exception if script file not found or encoding is not supported or an error occurs when
   *     executing script
   */
  public void execute(List<String> scriptFiles) throws Exception {
    List<Reader> readers = new ArrayList<Reader>();

    ScriptEngineManager manager = new ScriptEngineManager();
    ScriptEngine engine;
    if (scriptFiles.isEmpty()) {
      engine = manager.getEngineByName("GridStoreScript");
      String userHome = System.getProperty(GridStoreScriptEngine.USER_HOME);
      File rcScript = new File(userHome, ".gsshrc");
      if (rcScript.exists()) {
        System.out.println("Loading \"" + rcScript + "\"");
        readers.add(new InputStreamReader(new FileInputStream(rcScript), "UTF-8"));
      }
      if (cmdlineType == COMMANDLINE_TYPE.EXPAND) {
        readers.add(null);
      } else {
        readers.add(new InputStreamReader(System.in));
      }
    } else {
      String ext = getFileExtension(scriptFiles.get(0));
      engine = manager.getEngineByExtension(ext);
      if (engine == null) {
        throw new ShellException(
            bundle.getString("error.scriptExtension") + ": file=[" + scriptFiles.get(0) + "]");
      }

      if (engine instanceof GridStoreScriptEngine) {
        String userHome = System.getProperty(GridStoreScriptEngine.USER_HOME);
        File rcScript = new File(userHome, ".gsshrc");
        if (rcScript.exists()) {
          System.out.println("Loading \"" + rcScript + "\"");
          readers.add(new InputStreamReader(new FileInputStream(rcScript), "UTF-8"));
        }
      }

      for (String filename : scriptFiles) {
        if (!ext.equalsIgnoreCase(getFileExtension(filename))) {
          throw new ShellException(
              bundle.getString("error.scriptExtension") + ": file=[" + filename + "]");
        }
        try {
          readers.add(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
        } catch (FileNotFoundException e) {
          throw new ShellException(
              bundle.getString("error.fileNotFound")
                  + " (file=["
                  + new File(filename).getAbsolutePath()
                  + "], msg=["
                  + e.getMessage()
                  + "])",
              e);
        }
      }
    }
    initializeEngine(engine);

    ScriptContext context = engine.getContext();
    for (Reader reader : readers) {
      engine.eval(reader, context);
      if (reader != null) {
        reader.close();
      }
    }
  }

  private void initializeEngine(ScriptEngine engine) {
    AbstractCommandClass.context = engine.getContext();
    Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);

    CommandTable commandTable = new CommandTable();
    bindings.put(GridStoreScriptEngine.COMMAND_TABLE_NAME, commandTable);

    @SuppressWarnings("unchecked")
    ServiceLoader<AbstractCommandClass> commandClasses =
        ServiceLoader.load(AbstractCommandClass.class);
    for (AbstractCommandClass commandClass : commandClasses) {
      bindings.put(COMMAND_VARNAME_PREFIX + commandClass.getCommandGroupName(), commandClass);
      commandTable.register(commandClass);
      commandClassList.add(commandClass);
    }
  }

  /**
   * Exit the gs shell.
   *
   * @param status status code
   */
  public static void exitShell(int status) {
    for (AbstractCommandClass commandClass : instance.commandClassList) {
      commandClass.close();
    }
    System.exit(status);
  }

  private static Options createOptions() {
    return createOptions(true);
  }

  private static Options createOptions(boolean includeHiddenOpt) {
    Options options = new Options();

    options.addOption("v", "version", false, bundle.getString("help.version"));
    options.addOption("h", "help", false, bundle.getString("help.help"));

    if (includeHiddenOpt) {
      options.addOption(null, OPTION_HISTORY_COUNT, true, bundle.getString("help.historycount"));
      options.addOption(null, OPTION_CMDLINE_TYPE, true, bundle.getString("help.cmdlinetype"));
      options.addOption(null, OPTION_HELP2, false, bundle.getString("help.help2"));
    }

    return options;
  }

  /**
   * Start the gs shell.
   *
   * @param args arguments for starting gs shell
   * @throws Exception exception when starting gs shell
   */
  public static void main(String[] args) throws Exception {
    try {
      CommandLineParser parser = new PosixParser();
      Options options = createOptions();

      CommandLine cmd = null;
      try {
        cmd = parser.parse(options, args);
      } catch (ParseException e) {
        System.out.println(bundle.getString("error.option") + " (" + e.getMessage() + ")");
        printHelp(false);
        return;
      }

      if (cmd.hasOption("v")) {
        System.out.println(VERSION_INFO);
        return;
      }

      if (cmd.hasOption("h")) {
        printHelp(true);
        return;
      }

      if (cmd.hasOption(OPTION_HELP2)) {
        printHelp(true, true);
        return;
      }

      if (cmd.hasOption(OPTION_CMDLINE_TYPE)) {
        String str = cmd.getOptionValue(OPTION_CMDLINE_TYPE);
        if (str.equalsIgnoreCase(COMMANDLINE_TYPE.STANDARD.toString())) {
          cmdlineType = COMMANDLINE_TYPE.STANDARD;
        }
      }

      if (cmd.hasOption(OPTION_HISTORY_COUNT)) {
        String count = cmd.getOptionValue(OPTION_HISTORY_COUNT);
        try {
          m_historyCount = Integer.parseInt(count);
        } catch (Exception e) {
          // Do nothing
        }
      }

      @SuppressWarnings("unchecked")
      List<String> scriptFiles = cmd.getArgList();
      instance = new GridStoreShell();
      instance.execute(scriptFiles);

    } catch (ShellException e) {
      Logger logger = LoggerFactory.getLogger(GridStoreShell.class);
      logger.error("", e);
      System.out.println(e.getMessage());

    } catch (Exception e) {
      Logger logger = LoggerFactory.getLogger(GridStoreShell.class);
      logger.error(bundle.getString("error.gs_sh"), e);
      System.out.println(bundle.getString("error.gs_sh") + " (" + e.getMessage() + ")");
    }
  }

  private static void printHelp(boolean additional) {
    printHelp(additional, false);
  }

  private static void printHelp(boolean additional, boolean showHiddenOpt) {
    HelpFormatter help = new HelpFormatter();
    Options options = createOptions(showHiddenOpt);
    help.printHelp(bundle.getString("help.usage"), options, false);
    if (additional) {
      System.out.println(bundle.getString("help.additionalHelp"));
    }
  }
}
