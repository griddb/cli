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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencsv.CSVWriter;
import com.toshiba.mwcloud.gs.AggregationResult;
import com.toshiba.mwcloud.gs.ColumnInfo;
import com.toshiba.mwcloud.gs.CompressionMethod;
import com.toshiba.mwcloud.gs.Container;
import com.toshiba.mwcloud.gs.ContainerInfo;
import com.toshiba.mwcloud.gs.ContainerType;
import com.toshiba.mwcloud.gs.FetchOption;
import com.toshiba.mwcloud.gs.GSException;
import com.toshiba.mwcloud.gs.GSType;
import com.toshiba.mwcloud.gs.GridStore;
import com.toshiba.mwcloud.gs.GridStoreFactory;
import com.toshiba.mwcloud.gs.IndexInfo;
import com.toshiba.mwcloud.gs.IndexType;
import com.toshiba.mwcloud.gs.PartitionController;
import com.toshiba.mwcloud.gs.Query;
import com.toshiba.mwcloud.gs.QueryAnalysisEntry;
import com.toshiba.mwcloud.gs.Row;
import com.toshiba.mwcloud.gs.Row.Key;
import com.toshiba.mwcloud.gs.RowSet;
import com.toshiba.mwcloud.gs.TimeSeriesProperties;
import com.toshiba.mwcloud.gs.TimeUnit;
import com.toshiba.mwcloud.gs.TimestampUtils;
import com.toshiba.mwcloud.gs.TriggerInfo;
import com.toshiba.mwcloud.gs.experimental.ContainerAttribute;
import com.toshiba.mwcloud.gs.experimental.DatabaseInfo;
import com.toshiba.mwcloud.gs.experimental.ExperimentalTool;
import com.toshiba.mwcloud.gs.experimental.ExtendedContainerInfo;
import com.toshiba.mwcloud.gs.experimental.PrivilegeInfo;
import com.toshiba.mwcloud.gs.experimental.UserInfo;
import com.toshiba.mwcloud.gs.tools.common.GSNode;
import com.toshiba.mwcloud.gs.tools.common.GridDBJdbcUtils;
import com.toshiba.mwcloud.gs.tools.common.GridStoreCommandException;
import com.toshiba.mwcloud.gs.tools.common.GridStoreCommandUtils;
import com.toshiba.mwcloud.gs.tools.common.data.ConnectionInfo;
import com.toshiba.mwcloud.gs.tools.common.data.EventInfo;
import com.toshiba.mwcloud.gs.tools.common.data.ExpirationInfo;
import com.toshiba.mwcloud.gs.tools.common.data.MetaContainerFileIO;
import com.toshiba.mwcloud.gs.tools.common.data.SqlInfo;
import com.toshiba.mwcloud.gs.tools.common.data.SqlInfoBinder;
import com.toshiba.mwcloud.gs.tools.common.data.TablePartitionProperty;
import com.toshiba.mwcloud.gs.tools.common.data.ToolConstants;
import com.toshiba.mwcloud.gs.tools.common.data.ToolContainerInfo;
import com.toshiba.mwcloud.gs.tools.shell.AbstractCommandClass;
import com.toshiba.mwcloud.gs.tools.shell.GridStoreShell;
import com.toshiba.mwcloud.gs.tools.shell.ShellCluster;
import com.toshiba.mwcloud.gs.tools.shell.ShellException;
import com.toshiba.mwcloud.gs.tools.shell.annotation.GSCommand;
import com.toshiba.mwcloud.gs.tools.shell.annotation.GSNullable;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.ScriptContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Data command class contains some commands to execute data operation. */
public class DataCommandClass extends AbstractCommandClass {
  private static final String FAILOVER_TIMEOUT_DEFAULT = "30";
  private static final String TRANSACTION_TIMEOUT_DEFAULT = "300";
  private static final int FETCH_SIZE = 1000;
  private static final Charset CSV_ENCODING = Charset.forName("UTF-8");

  private static final String BLOB_SHOW_STRING = "(BLOB)";

  private static final String GS_NULL_STDOUT_DEFAULT = "(NULL)";
  private static final String GS_NULL_CSV_DEFAULT = null;

  private static final String GS_LOGIN_TIMEOUT_DEFAULT = "15";
  private static final String GS_TQL_FETCH_MODE_DEFAULT = "SIZE";

  private static final String PROP_USER = "user";
  private static final String PROP_PASSWORD = "password";
  private static final String PROP_APPLICATION_NAME = "applicationName";
  private static final String PROP_TIMEZONE = "timeZone";
  private static final String PROP_AUTHENTICATION_METHOD = "authentication";
  private static final String PROP_NOTIFICATION_INTERFACE_ADDRESS = "notificationInterfaceAddress";
  private static final String PROP_SSL_MODE = "sslMode";
  private static final String DOT_CHARACTER = ".";
  private static final String TYPE_GENERAL_USER = "General User";
  private static final String TYPE_ROLE = "Role";
  /** The operator in sub-command {@code setauthentication}. */
  public enum AuthenticationMethods {
    LDAP,
    INTERNAL;
  }

  private static final String APPLICATION_NAME = "gs_sh";

  // No SQL
  private GridStore gridStore;
  private String queryContainerName;
  private Container<Object, Row> queryContainer;
  private Query<?> queryObj;
  private RowSet<?> queryRowSet;
  private ShellCluster m_cluster;

  // New SQL (JDBC)
  private Connection m_jdbcCon;
  private Statement m_jdbcStmt;
  private ResultSet m_jdbcRS;
  private String m_jdbcSQL = "";

  /* Improve CLI result format */
  private static final Integer MAX_COLUMN_WIDTH_DEFAULT = 31;
  private static final Integer MIN_COLUMN_WIDTH_LIMIT = 1;
  private static final Integer MAX_COLUMN_WIDTH_LIMIT = 1_000_000;
  private Integer  m_resultMaxWidth = MAX_COLUMN_WIDTH_DEFAULT;
  private enum ResultFormat { TABLE, CSV }
  private ResultFormat m_resultFormat = ResultFormat.TABLE;

  private String m_dbName;
  private String m_connectedUser;

  private ExplainResult m_explainResult;

  private String m_connectTimeZoneVal;

  /**
   * Get gridStore attribute.
   *
   * @return gridStore
   */
  public GridStore getGridStore() {
    return gridStore;
  }

  /**
   * Get query row set.
   *
   * @return queryRowSet
   */
  public RowSet<?> getRowSet() {
    return queryRowSet;
  }

  private void checkUserIdAndPassword(String userId, String password) {
    if (userId == null || password == null) {
      throw new ShellException(getMessage("error.userPasswordNull"));
    }
  }

  private void checkConnected() {
    if (gridStore == null) {
      throw new ShellException(getMessage("error.notConnected"));
    }
  }

  private void checkConnectedSQL() {
    if (m_jdbcCon == null) {
      throw new ShellException(getMessage("error.notConnectedSQL"));
    }
  }

  private void checkQueried() {
    if (queryRowSet == null) {
      throw new ShellException(getMessage("error.notQueried"));
    }
  }

  private void checkQueriedSQL() {
    if (m_jdbcRS == null) {
      throw new ShellException(getMessage("error.notQueried"));
    }
  }

  private void checkContainerExists(String containerName, Container<Object, Row> container) {
    if (container == null) {
      throw new IllegalArgumentException(getMessage("error.containerNotFound", containerName));
    }
  }

  private void checkContainerExists(String containerName, ContainerInfo containerInfo) {
    if (containerInfo == null) {
      throw new IllegalArgumentException(getMessage("error.containerNotFound", containerName));
    }
  }

  private void closeQuitely(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (IOException e) {
        // Do nothing
      }
    }
  }

  private void checkSuperUser() {
    try {
      UserInfo userInfo = ExperimentalTool.getCurrentUser(gridStore);
      if (!userInfo.isSuperUser()) {
        throw new ShellException(getMessage("error.notSuperUser"));
      }
    } catch (GSException e) {
      throw new ShellException(
          getMessage("error.checkSuperUser") + " :msg=[" + e.getMessage() + "]", e);
    }
  }

  private void checkColumnNameMultipleValues(String[] columnNames) {
    if (columnNames == null || columnNames.length < 2) {
      throw new IllegalArgumentException(getMessage("error.columnNamesMultiple"));
    }
  }

  /**
   * 通常モードでの実行かを確認します.
   *
   * @return true:通常モード / false:メンテナンスモード
   */
  private boolean isNormalMode() {
    Object mode = getContext().getAttribute(GridStoreShell.MAINTENANCE_MODE);
    if (mode == null) {
      return true;
    } else {
      return ((GridStoreShell.EXEC_MODE) mode == GridStoreShell.EXEC_MODE.NORMAL) ? true : false;
    }
  }

  /**
   * SQLでカウントクエリを実行するかを確認します.
   *
   * @return true: する(デフォルト) / false: しない
   */
  private boolean isSqlCount() {
    Object object = getContext().getAttribute(GridStoreShell.SQL_COUNT);
    return object != null ? (Boolean) object : true;
  }

  /**
   * Get command group name (data).
   *
   * @return command group name (data)
   */
  @Override
  public String getCommandGroupName() {
    return "data";
  }

  /**
   * The main method for sub-command {@code connect}.<br>
   * Establish the connection to a GridDB cluster to execute a data operation.
   *
   * @param cluster cluster variable
   * @param dbName database name
   * @throws ShellException if it meets 1 of below conditions:
   *     <ul>
   *       <li>User name/password is not set
   *       <li>Cannot connect to the GridDB cluster
   *       <li>The connection has already been established
   *       <li>The APIs for NewSQL does not exists
   *     </ul>
   *
   * @see ScriptContext
   */
  @GSCommand
  public void connect(ShellCluster cluster, @GSNullable String dbName) {
    close();

    Exception tqlEx = null;
    Exception sqlEx = null;

    // No SQL
    if ((cluster.getAddress() != null)
        || (cluster.getTransactionMember() != null)
        || (cluster.getProviderUrl() != null)) {
      try {
        connectNoSQL(cluster, dbName);
        println(getMessage("message.connected"));
      } catch (ShellException e) {
        tqlEx = e;
      }
    }

    // New SQL
    if ((cluster.getJdbcAddress() != null)
        || (cluster.getSqlMember() != null)
        || (cluster.getSQLProvider() != null)) {
      try {
        connectNewSQL(cluster, dbName);
        println(getMessage("message.connectedSQL"));
      } catch (ShellException e) {
        sqlEx = e;
      }
    }

    if (((tqlEx == null)
            && ((cluster.getAddress() != null)
                || (cluster.getTransactionMember() != null)
                || (cluster.getProviderUrl() != null)))
        || ((sqlEx == null)
            && ((cluster.getJdbcAddress() != null)
                || (cluster.getSqlMember() != null)
                || (cluster.getSQLProvider() != null)))) {
      if (dbName == null) {
        m_dbName = ToolConstants.PUBLIC_DB;
      } else {
        m_dbName = dbName;
      }
      getContext()
          .setAttribute(GridStoreShell.CONNECTED_DBNAME, m_dbName, ScriptContext.ENGINE_SCOPE);
    }

    String msg = "";
    Exception ex = null;
    if ((tqlEx != null) || (sqlEx != null)) {
      if (tqlEx != null) {
        ex = tqlEx;
        msg += tqlEx.getMessage();
      }
      if (sqlEx != null) {
        if (tqlEx != null) {
          Logger logger = LoggerFactory.getLogger(DataCommandClass.class);
          logger.error("", tqlEx);
        }
        ex = sqlEx;
        msg += sqlEx.getMessage();
      }
      throw new ShellException(getMessage("error.connect") + " : msg=[" + msg + "]", ex);
    }
  }

  /**
   * No SQLサーバに接続します.
   *
   * @param cluster クラスタ定義
   * @param dbName DB名
   * @throws Exception Exception
   */
  private void connectNoSQL(ShellCluster cluster, String dbName) {

    if ((cluster.getAddress() == null)
        && (cluster.getTransactionMember() == null)
        && (cluster.getProviderUrl() == null)) {
      return;
    }

    tqlClosePrivate();
    closeQuitely(gridStore);
    gridStore = null;

    String userId = (String) getContext().getAttribute(GridStoreShell.USER);
    String password = (String) getContext().getAttribute(GridStoreShell.PASSWORD);
    checkUserIdAndPassword(userId, password);

    Properties prop = new Properties();
    if (cluster.getAddress() != null) {
      prop.setProperty("notificationAddress", cluster.getAddress());
      prop.setProperty("notificationPort", Integer.toString(cluster.getPort()));

    } else if (cluster.getTransactionMember() != null) {
      prop.setProperty("notificationMember", cluster.getTransactionMember());

    } else if (cluster.getProviderUrl() != null) {
      prop.setProperty("notificationProvider", cluster.getProviderUrl());
    }
    prop.setProperty("clusterName", cluster.getName());
    prop.setProperty("user", userId);
    prop.setProperty("password", password);
    if (dbName != null) {
      prop.setProperty("database", dbName);
    }
    prop.setProperty(
        "failoverTimeout",
        getAttributeValue(GridStoreShell.FAILOVER_TIMEOUT, FAILOVER_TIMEOUT_DEFAULT));
    prop.setProperty(
        "transactionTimeout",
        getAttributeValue(GridStoreShell.TRANSACTION_TIMEOUT, TRANSACTION_TIMEOUT_DEFAULT));

    prop.setProperty("experimental.metaContainerVisible", "true");
    prop.setProperty("internal.internalMetaContainerVisible", "true");

    prop.setProperty(PROP_APPLICATION_NAME, APPLICATION_NAME);

    String timeZoneVal = (String) getContext().getAttribute(GridStoreShell.TIMEZONE);
    if (timeZoneVal != null) {
      prop.setProperty(PROP_TIMEZONE, timeZoneVal);
      m_connectTimeZoneVal = timeZoneVal;
    } else {
      m_connectTimeZoneVal = null;
    }
    // V4.5 Authentication method
    setAuthenticationMethodProperty(prop);
    // V4.5 Notification Interface Address
    setNotificationInterfaceAddressProperty(prop);
    // V4.5 Set SSL Mode connection
    setSslModeProperty(prop);

    GridStore store = null;
    try {
      store = GridStoreFactory.getInstance().getGridStore(prop);

      Container<Object, Row> container = store.getContainer("DUMMY");
      if (container != null) {
        container.close();
      }
      gridStore = store;
      m_cluster = cluster;
      m_connectedUser = userId;

    } catch (GSException e) {
      closeQuitely(store);
      String msg = "(NoSQL)";
      if (cluster.getAddress() != null) {
        msg += cluster.getAddress() + ":" + cluster.getPort();

      } else if (cluster.getTransactionMember() != null) {
        msg += cluster.getTransactionMember();

      } else if (cluster.getProviderUrl() != null) {
        msg += cluster.getProviderUrl();
      }
      msg += "," + e.getMessage();
      throw new ShellException(msg, e);
    }
  }

  /**
   * New SQLサーバに接続します.
   *
   * @param cluster クラスタ定義
   * @param dbName データベース名
   */
  private void connectNewSQL(ShellCluster cluster, String dbName) {

    if ((cluster.getJdbcAddress() == null)
        && (cluster.getSqlMember() == null)
        && (cluster.getSQLProvider() == null)) {
      return;
    }

    try {
      String userId = (String) getContext().getAttribute(GridStoreShell.USER);
      String password = (String) getContext().getAttribute(GridStoreShell.PASSWORD);
      checkUserIdAndPassword(userId, password);

      Class.forName("com.toshiba.mwcloud.gs.sql.Driver");

      String jdbcUrl = null;
      String encClusterName = URLEncoder.encode(cluster.getName(), "UTF-8");

      if (cluster.getJdbcAddress() != null) {
        jdbcUrl =
            "jdbc:gs://"
                + cluster.getJdbcAddress()
                + ":"
                + cluster.getJdbcPort()
                + "/"
                + encClusterName;
      } else {
        jdbcUrl = "jdbc:gs:///" + encClusterName;
      }
      if (dbName != null) {
        String encDbName = URLEncoder.encode(dbName, "UTF-8");
        jdbcUrl += "/" + encDbName;
      }

      if (cluster.getJdbcAddress() != null) {
        // nop
      } else if (cluster.getSqlMember() != null) {
        jdbcUrl += "?notificationMember=" + URLEncoder.encode(cluster.getSqlMember(), "UTF-8");
      } else if (cluster.getSQLProvider() != null) {
        jdbcUrl += "?notificationProvider=" + URLEncoder.encode(cluster.getSQLProvider(), "UTF-8");
      }

      DriverManager.setLoginTimeout(
          Integer.parseInt(
              getAttributeValue(GridStoreShell.LOGIN_TIMEOUT, GS_LOGIN_TIMEOUT_DEFAULT)));
      Properties prop = new Properties();

      prop.setProperty(PROP_USER, userId);
      prop.setProperty(PROP_PASSWORD, password);
      prop.setProperty(PROP_APPLICATION_NAME, APPLICATION_NAME);

      String timeZoneVal = (String) getContext().getAttribute(GridStoreShell.TIMEZONE);
      if (timeZoneVal != null) {
        prop.setProperty(PROP_TIMEZONE, timeZoneVal);
        m_connectTimeZoneVal = timeZoneVal;
      } else {
        m_connectTimeZoneVal = null;
      }

      // V4.5 Authentication method
      setAuthenticationMethodProperty(prop);
      // V4.5 Notification Interface Address
      setNotificationInterfaceAddressProperty(prop);
      // V4.5 Set SSL Mode connection
      setSslModeProperty(prop);

      m_jdbcCon = DriverManager.getConnection(jdbcUrl, prop);

      m_connectedUser = userId;

    } catch (Exception e) {
      String msg = null;
      if (e instanceof ClassNotFoundException) {
        msg = " (NewSQL)" + getMessage("error.newsqlnotfounr");
      } else {
        msg = " (NewSQL)";
        if (cluster.getJdbcAddress() != null) {
          msg += cluster.getJdbcAddress() + ":" + cluster.getJdbcPort();
        } else if (cluster.getSqlMember() != null) {
          msg += cluster.getSqlMember();
        } else if (cluster.getSQLProvider() != null) {
          msg += cluster.getSQLProvider();
        }
        msg += "," + e.getMessage();
      }
      throw new ShellException(msg, e);
    }
  }

  private void setAuthenticationMethodProperty(Properties prop) {
    String authentication = (String) getContext().getAttribute(GridStoreShell.AUTHENTICATION);
    if (authentication != null) {
      prop.setProperty(PROP_AUTHENTICATION_METHOD, authentication);
    }
  }

  private void setNotificationInterfaceAddressProperty(Properties prop) {
    String notificationInterfaceAddress =
        (String) getContext().getAttribute(GridStoreShell.NOTIFICATION_INTERFACE_ADDRESS);
    if (notificationInterfaceAddress != null) {
      prop.setProperty(PROP_NOTIFICATION_INTERFACE_ADDRESS, notificationInterfaceAddress);
    }
  }

  private void setSslModeProperty(Properties prop) {
    String sslMode = (String) getContext().getAttribute(GridStoreShell.SSL_MODE);
    if (sslMode != null) {
      prop.setProperty(PROP_SSL_MODE, sslMode);
    } else {
      prop.setProperty(PROP_SSL_MODE, BasicCommandClass.SslMode.DISABLED.getValue());
      /* CE version: 145005:JC_ILLEGAL_PROPERTY_ENTRY] Unacceptable property specified because of lack of extra library (key=sslMode) */
      prop.remove(PROP_SSL_MODE);
    }
  }
  /**
   * 変数の値を返します。 変数が設定されていない場合、値がIntに変換できない場合は、デフォルト値を返します.
   *
   * @param attributeName 変数名
   * @param defaultValue デフォルト値
   * @return 値
   */
  private String getAttributeValue(String attributeName, String defaultValue) {
    String value = (String) getContext().getAttribute(attributeName);

    if ((value == null) || (value.length() == 0)) {
      value = defaultValue;
    } else {
      try {
        Integer.parseInt(value);
      } catch (NumberFormatException e) {
        value = defaultValue;
      }
    }
    return value;
  }

  /**
   * 文字列で与える変数の値を文字列で返します。 変数が設定されていない場合、デフォルト値を返します.
   *
   * @param attributeName 変数名
   * @param defaultValue デフォルト値
   * @return 値
   */
  private String getAttributeString(String attributeName, String defaultValue) {
    String str = (String) getContext().getAttribute(attributeName);
    if ((str == null) || (str.length() == 0)) {
      str = defaultValue;
    }
    return str;
  }

  /**
   * The main method for sub-command {@code close}.<br>
   * Close all search objects and disconnect.
   *
   * @see ScriptContext
   */
  @Override
  public void close() {
    queryObjClose();

    // NoSQL
    closeQuitely(gridStore);
    gridStore = null;
    m_connectTimeZoneVal = null;

    // New SQL
    if (m_jdbcCon != null) {
      try {
        m_jdbcCon.close();
        m_jdbcCon = null;
      } catch (Exception e) {
        // Do nothing
      }
    }

    m_dbName = null;
    getContext()
        .setAttribute(GridStoreShell.CONNECTED_DBNAME, m_dbName, ScriptContext.ENGINE_SCOPE);
    m_connectedUser = null;
  }

  /** TQLとSQLの検索オブジェクトをクローズします。 （接続は切断しません。). */
  private void queryObjClose() {
    // TQL
    tqlClosePrivate();

    // SQL
    sqlClosePrivate();

    // explain解析結果も消す
    m_explainResult = null;
  }

  /** TQLの検索オブジェクトをクローズします. */
  private void tqlClosePrivate() {
    closeQuitely(queryRowSet);
    queryRowSet = null;
    closeQuitely(queryObj);
    queryObj = null;
    closeQuitely(queryContainer);
    queryContainer = null;
    queryContainerName = null;
  }

  /** SQLの検索オブジェクトをクローズします. */
  private void sqlClosePrivate() {
    try {
      if (m_jdbcRS != null) {
        m_jdbcRS.close();
        m_jdbcRS = null;
      }
      if (m_jdbcStmt != null) {
        m_jdbcStmt.close();
        m_jdbcStmt = null;
      }
    } catch (Exception e) {
      // Do nothing
    }
  }

  /**
   * The main method for sub-command {@code disconnect}.<br>
   * Disconnect user from a GridDB cluster.
   */
  @GSCommand
  public void disconnect() {
    if ((gridStore == null) && (m_jdbcCon == null)) {
      println(getMessage("message.notConnected"));
      return;
    }

    close();

    println(getMessage("message.disconnected"));
  }

  /**
   * The main method for sub-command {@code createdatabase}.<br>
   * Create a database. Need to run with administrator user.
   *
   * @param dbName database name
   * @throws ShellException if it meets 1 of the following conditions:
   *     <ul>
   *       <li>The connection is closed
   *       <li>Current user is not an administrator
   *       <li>Database already existed
   *       <li>Error while checking user information
   *       <li>Unable to create database
   *     </ul>
   *
   * @see ExperimentalTool#putDatabase
   */
  @GSCommand
  public void createDatabase(String dbName) {
    checkConnected();
    checkSuperUser();

    try {
      if (existsDatabase(dbName)) {
        throw new ShellException(getMessage("error.dbAlreadyExists", dbName));
      }

      DatabaseInfo dbInfo = new DatabaseInfo();
      dbInfo.setName(dbName);
      ExperimentalTool.putDatabase(gridStore, dbName, dbInfo, false);

    } catch (GSException e) {
      throw new ShellException(getMessage("error.createDb") + " : msg=[" + e.getMessage() + "]", e);
    }
  }

  /**
   * The main method for sub-command {@code dropdatabase}.<br>
   * Delete the database. Need to run with administrator user.
   *
   * @param dbName database name
   * @throws ShellException if it meets 1 of the following conditions:
   *     <ul>
   *       <li>The connection is closed
   *       <li>Current user is not an administrator
   *       <li>Error while checking user information
   *       <li>Database is not found
   *       <li>Database is still connected
   *       <li>Unable to drop database
   *     </ul>
   *
   * @see ExperimentalTool#dropDatabase
   * @see ScriptContext
   */
  @GSCommand
  public void dropDatabase(String dbName) {
    checkConnected();
    checkSuperUser();

    try {
      if (!existsDatabase(dbName)) {
        throw new ShellException(getMessage("error.dbNotFound", dbName));
      }

      // 自分が接続しているDBは削除できない (サーバの仕様とは逆だが、外部仕様的にはこちらの方が分かりやすい)
      if (dbName.equalsIgnoreCase(m_dbName)) {
        throw new ShellException(getMessage("error.dropDb2", dbName));
      }

      // 削除対象のDBに移動
      final String oldDbName = m_dbName;
      // V4.3 タイムゾーン設定値を退避
      final String oldTimeZoneVal = (String) getContext().getAttribute(GridStoreShell.TIMEZONE);
      String oldConnectTimeZoneVal = m_connectTimeZoneVal;
      connectNoSQL(m_cluster, dbName);

      ExperimentalTool.dropDatabase(gridStore, dbName);

      // 元のDBに戻る
      // V4.3 退避していたタイムゾーン設定値で元のDBに接続
      if (oldConnectTimeZoneVal != null) {
        getContext()
            .setAttribute(
                GridStoreShell.TIMEZONE, oldConnectTimeZoneVal, ScriptContext.ENGINE_SCOPE);
      } else {
        getContext().removeAttribute(GridStoreShell.TIMEZONE, ScriptContext.ENGINE_SCOPE);
      }
      connectNoSQL(m_cluster, oldDbName);
      // V4.3 退避していたタイムゾーン設定値を復帰させる
      if (oldTimeZoneVal != null) {
        getContext()
            .setAttribute(GridStoreShell.TIMEZONE, oldTimeZoneVal, ScriptContext.ENGINE_SCOPE);
      } else {
        getContext().removeAttribute(GridStoreShell.TIMEZONE, ScriptContext.ENGINE_SCOPE);
      }

    } catch (GSException e) {
      throw new ShellException(getMessage("error.dropDb") + " : msg=[" + e.getMessage() + "]", e);
    }
  }

  /**
   * The main method for sub-command {@code showdatabase}.<br>
   * Displays database information.
   *
   * @param dbName database name
   * @throws ShellException if it meets 1 of below conditions:
   *     <ul>
   *       <li>The connection is closed
   *       <li>Database not existed
   *       <li>Error while providing database information
   *     </ul>
   *
   * @see ExperimentalTool#getDatabases
   */
  @GSCommand
  public void showDatabase(@GSNullable String dbName) {
    checkConnected();

    try {
      Map<String, DatabaseInfo> dbList = ExperimentalTool.getDatabases(gridStore);

      // DB名ソート用Comparator
      Comparator<Map.Entry<String, DatabaseInfo>> comparator =
          new Comparator<Map.Entry<String, DatabaseInfo>>() {
            @Override
            public int compare(
                Map.Entry<String, DatabaseInfo> o1, Map.Entry<String, DatabaseInfo> o2) {
              String name1 = o1.getKey();
              String name2 = o2.getKey();
              return stringCompare(name1, name2);
            }
          };
      // ユーザ名ソート用Comparator
      Comparator<Map.Entry<String, PrivilegeInfo>> usercomparator =
          new Comparator<Map.Entry<String, PrivilegeInfo>>() {
            @Override
            public int compare(
                Map.Entry<String, PrivilegeInfo> o1, Map.Entry<String, PrivilegeInfo> o2) {
              String user1 = o1.getKey();
              String user2 = o2.getKey();
              return stringCompare(user1, user2);
            }
          };

      if (dbName == null) {
        // 一覧表示
        // V4.3 表示フォーマット変更 権限を表示
        println("Name             ACL");
        println("---------------------------------");
        printfln("%-15s  %-10s", ToolConstants.PUBLIC_DB, "ALL_USER");

        List<Map.Entry<String, DatabaseInfo>> dbEntryList =
            new ArrayList<Map.Entry<String, DatabaseInfo>>();
        for (Map.Entry<String, DatabaseInfo> e : dbList.entrySet()) {
          dbEntryList.add(e);
        }
        // DB名でソート
        Collections.sort(dbEntryList, comparator);

        for (Map.Entry<String, DatabaseInfo> e : dbEntryList) {
          // DB名
          String name = e.getKey();
          DatabaseInfo info = e.getValue();
          Map<String, PrivilegeInfo> aclList = info.getPrivileges();

          List<Map.Entry<String, PrivilegeInfo>> aclEntlyList =
              new ArrayList<Map.Entry<String, PrivilegeInfo>>();
          for (Map.Entry<String, PrivilegeInfo> aclEntry : aclList.entrySet()) {
            aclEntlyList.add(aclEntry);
          }
          // ユーザ名でソート
          Collections.sort(aclEntlyList, usercomparator);

          if (aclEntlyList.size() > 0) {
            // データベースに対しアクセス権を付与されたユーザが存在する場合
            for (Map.Entry<String, PrivilegeInfo> aclEntry : aclEntlyList) {
              // ユーザ名
              String user = aclEntry.getKey();
              // 権限
              String role = "";
              PrivilegeInfo privilegeInfo = aclEntry.getValue();
              if (privilegeInfo != null) {
                role = privilegeInfo.getRole().toString();
              }
              printfln("%-15s  %-10s  %s", name, user, role);
            }
          } else {
            // データベースに対しアクセス権を付与されたユーザが存在しない場合
            printfln("%-15s  %-10s  %s", name, "", "");
          }
        }

      } else {
        // 詳細表示 (V2.7では、一覧表示と同等の表示のみ）
        // [memo] DB名は大文字小文字の区別なし
        dbList.put(ToolConstants.PUBLIC_DB, new DatabaseInfo());
        DatabaseInfo info = null;
        for (Map.Entry<String, DatabaseInfo> entry : dbList.entrySet()) {
          if (dbName.equalsIgnoreCase(entry.getKey())) {
            info = entry.getValue();
            break;
          }
        }
        if (info == null) {
          throw new ShellException(getMessage("error.dbNotFound", dbName));

        } else {
          // V4.3 表示フォーマット変更 権限を表示
          println("Name             ACL");
          println("---------------------------------");

          if (dbName.equalsIgnoreCase(ToolConstants.PUBLIC_DB)) {
            printfln("%-15s  %-10s", ToolConstants.PUBLIC_DB, "ALL_USER");
            return;
          }

          Map<String, PrivilegeInfo> aclList = info.getPrivileges();

          List<Map.Entry<String, PrivilegeInfo>> aclEntlyList =
              new ArrayList<Map.Entry<String, PrivilegeInfo>>();
          for (Map.Entry<String, PrivilegeInfo> aclEntry : aclList.entrySet()) {
            aclEntlyList.add(aclEntry);
          }
          // ユーザ名でソート
          Collections.sort(aclEntlyList, usercomparator);

          if (aclEntlyList.size() > 0) {
            // データベースに対しアクセス権を付与されたユーザが存在する場合
            for (Map.Entry<String, PrivilegeInfo> aclEntry : aclEntlyList) {
              // ユーザ名
              String user = aclEntry.getKey();
              // 権限
              String role = "";
              PrivilegeInfo privilegeInfo = aclEntry.getValue();
              if (privilegeInfo != null) {
                role = privilegeInfo.getRole().toString();
              }
              printfln("%-15s  %-10s  %s", info.getName(), user, role);
            }
          } else {
            // データベースに対しアクセス権を付与されたユーザが存在しない場合
            printfln("%-15s  %-10s  %s", info.getName(), "", "");
          }
        }
      }
    } catch (GSException e) {
      throw new ShellException(getMessage("error.showDb") + " : msg=[" + e.getMessage() + "]", e);
    }
  }

  /**
   * Compare 2 strings.
   *
   * @param s1 string to compare
   * @param s2 string to compare
   * @return 0 if equal, a value less than 0 if {@code s1} is smaller, a value greater than 0 if
   *     {@code s2} is smaller<br>
   *     {@code null} is treated as the minimum value.
   */
  public int stringCompare(String s1, String s2) {
    if (s1 == null && s2 == null) {
      return 0;
    } else if (s1 == null) {
      return -1;
    } else if (s2 == null) {
      return 1;
    }
    return s1.compareTo(s2);
  }

  /**
   * The main method for sub-command {@code getcurrentdatabase}.<br>
   * Displays the name of the currently connected database.
   *
   * @throws ShellException if the connection is closed or error while providing current database
   *     information
   * @see ExperimentalTool#getCurrentDatabase
   */
  @GSCommand
  public void getCurrentDatabase() {
    checkConnected();

    try {
      DatabaseInfo info = ExperimentalTool.getCurrentDatabase(gridStore);
      println(info.getName());

    } catch (Exception e) {
      throw new ShellException(
          getMessage("error.getCurrentDb") + " : msg=[" + e.getMessage() + "]", e);
    }
  }

  /**
   * 指定された名前のデータベースが存在するか確認します.
   *
   * @param dbName データベース名
   * @return true:存在する false:存在しない
   */
  private boolean existsDatabase(String dbName) throws GSException {
    return (getDatabaseInfo(dbName) == null) ? false : true;
  }

  /**
   * 指定された名前のデータベース情報を返します.
   *
   * @param dbName データベース名
   * @return データベース情報 (存在しない場合はnull)
   * @throws GSException GSException
   */
  private DatabaseInfo getDatabaseInfo(String dbName) throws GSException {
    // [memo] DB名は大文字小文字区別なし
    Map<String, DatabaseInfo> map = ExperimentalTool.getDatabases(gridStore);
    for (Map.Entry<String, DatabaseInfo> entry : map.entrySet()) {
      if (dbName.equalsIgnoreCase(entry.getKey())) {
        return entry.getValue();
      }
    }
    return null;
  }

  /**
   * The main method for sub-command {@code createuser}.<br>
   * Create a user. Need to run with administrator user.
   *
   * @param userName user name
   * @param password password
   * @throws ShellException if it meets 1 of the following conditions:
   *     <ul>
   *       <li>The connection is closed
   *       <li>Error while checking user information
   *       <li>Current user is not an administrator
   *       <li>User already existed
   *       <li>Unable to create user
   *     </ul>
   *
   * @see ExperimentalTool#putUser
   */
  @GSCommand
  public void createUser(String userName, String password) {
    checkConnected();
    checkSuperUser();

    try {
      if (existsUser(userName)) {
        throw new ShellException(getMessage("error.userAlreadyExists", userName));
      }

      UserInfo info = new UserInfo();
      info.setName(userName);
      info.setPassword(password);
      ExperimentalTool.putUser(gridStore, userName, info, false);

    } catch (GSException e) {
      throw new ShellException(
          getMessage("error.createUser") + " : msg=[" + e.getMessage() + "]", e);
    }
  }

  /**
   * The main method for sub-command {@code dropuser}.<br>
   * Delete a user. Need to run with administrator user.
   *
   * @param userName user name
   * @throws ShellException if it meets 1 of the following conditions:
   *     <ul>
   *       <li>The connection is closed
   *       <li>Current user is not an administrator
   *       <li>Error while checking user information
   *       <li>User not found
   *       <li>Unable to drop user
   *     </ul>
   *
   * @see ExperimentalTool#dropUser
   */
  @GSCommand
  public void dropUser(String userName) {
    checkConnected();
    checkSuperUser();

    try {
      if (!existsUser(userName)) {
        throw new ShellException(getMessage("error.userNotFound", userName));
      }

      ExperimentalTool.dropUser(gridStore, userName);

    } catch (GSException e) {
      throw new ShellException(getMessage("error.dropUser") + " : msg=[" + e.getMessage() + "]", e);
    }
  }

  /**
   * The main method for sub-command {@code setpassword}.<br>
   * Set user password.<br>
   * Need to run with administrator user.
   *
   * <ol>
   *   <li>Administrator: can change the password of general users
   *   <li>General user: can change his own password only
   * </ol>
   *
   * @param arg1 password if {@code arg2} is not specified, otherwise user name
   * @param arg2 password
   * @throws ShellException if it meets 1 of the following conditions:
   *     <ul>
   *       <li>The connection is closed
   *       <li>Change administrator password
   *       <li>General user changes other user's password
   *     </ul>
   *
   * @see ExperimentalTool#putUser
   */
  @GSCommand
  public void setPassword(String arg1, @GSNullable String arg2) {
    checkConnected();

    try {
      String userName = null;
      String password = null;

      if (arg2 == null) {
        // 一般ユーザが自分のパスワードを変更する
        userName = m_connectedUser;
        password = arg1;

        if (isSuperUser(userName)) {
          // 管理者のパスワード変更は、運用コマンドgs_passwdで実行してください。
          throw new ShellException(getMessage("error.setPasswordAdmin", userName));
        }

      } else {
        // 管理者が一般ユーザのパスワードを変更する
        userName = arg1;
        password = arg2;

        if (!isSuperUser(m_connectedUser)) {
          // 一般ユーザは他のユーザのパスワードを変更できません。
          throw new ShellException(getMessage("error.setPasswordNormalUser"));
        }

        if (isSuperUser(userName)) {
          // 管理者のパスワード変更は、運用コマンドgs_passwdで実行してください。
          throw new ShellException(getMessage("error.setPasswordAdmin", userName));
        }

        // ユーザ情報取得
        if (!existsUser(userName)) {
          throw new ShellException(getMessage("error.userNotFound", userName));
        }
      }

      // パスワード変更
      UserInfo newInfo = new UserInfo();
      newInfo.setPassword(password);
      ExperimentalTool.putUser(gridStore, userName, newInfo, true);

    } catch (GSException e) {
      throw new ShellException(
          getMessage("error.setPassword") + " : msg=[" + e.getMessage() + "]", e);
    }
  }

  /**
   * The main method for sub-command {@code showuser}.<br>
   * Displays general user or role information. Need to run with administrator user.
   *
   * @param name general user name or role name
   * @throws ShellException if the connection is closed, user or role name does not exist.
   * @see ExperimentalTool#getUsers
   * @see ExperimentalTool#getDatabases
   */
  @GSCommand
  public void showUser(@GSNullable String name) {
    checkConnected();

    try {
      Map<String, UserInfo> userList = ExperimentalTool.getUsers(gridStore);
      if (name == null) {
        // 一覧表示
        printfln("%-32s %-10s", "Name", "Type");
        println("-------------------------------------------");
        for (Map.Entry<String, UserInfo> entry : userList.entrySet()) {
          UserInfo info = entry.getValue();
          // V4.5 print name follow user name or role name
          String userName = null;
          boolean isRole = info.isRole();
          if (!isRole) {
            userName = info.getName();
            if (info.isSuperUser()) {
              userName += " (SuperUser)";
            }
          }

          printfln(
              "%-32s %-10s",
              (isRole) ? info.getRoleName() : userName, (isRole) ? TYPE_ROLE : TYPE_GENERAL_USER);
        }
      } else {
        UserInfo info = null;
        for (Map.Entry<String, UserInfo> entry : userList.entrySet()) {
          if (name.equalsIgnoreCase(entry.getKey())) {
            info = entry.getValue();
            break;
          }
        }
        if (info == null) {
          throw new ShellException(getMessage("error.userOrRoleNotFound", name));
        } else {
          // V4.5 print name follow user name or role name
          println("Name     : " + ((info.isRole()) ? info.getRoleName() : info.getName()));
          println("Type     : " + ((info.isRole()) ? TYPE_ROLE : TYPE_GENERAL_USER));
          if (info.isSuperUser()) {
            println("SuperUser: true");
          }
          println("GrantedDB: " + ToolConstants.PUBLIC_DB);
          Map<String, DatabaseInfo> dbList = ExperimentalTool.getDatabases(gridStore);
          List<Map.Entry<String, DatabaseInfo>> dbEntryList =
              new ArrayList<Map.Entry<String, DatabaseInfo>>();
          for (Map.Entry<String, DatabaseInfo> entry : dbList.entrySet()) {
            dbEntryList.add(entry);
          }
          // DB名ソート用Comparator
          Comparator<Map.Entry<String, DatabaseInfo>> comparator =
              new Comparator<Map.Entry<String, DatabaseInfo>>() {
                @Override
                public int compare(
                    Map.Entry<String, DatabaseInfo> o1, Map.Entry<String, DatabaseInfo> o2) {
                  String name1 = o1.getKey();
                  String name2 = o2.getKey();
                  return stringCompare(name1, name2);
                }
              };
          // DB名でソート
          Collections.sort(dbEntryList, comparator);
          for (Map.Entry<String, DatabaseInfo> entry : dbEntryList) {
            // DB名
            String dbName = entry.getKey();
            Map<String, PrivilegeInfo> acl = entry.getValue().getPrivileges();
            for (Map.Entry<String, PrivilegeInfo> aclEntry : acl.entrySet()) {
              if (name.equalsIgnoreCase(aclEntry.getKey())) {
                // 権限
                String role = "";
                PrivilegeInfo privilegeInfo = aclEntry.getValue();
                if (privilegeInfo != null) {
                  role = privilegeInfo.getRole().toString();
                }
                printfln("           %-15s  %s", dbName, role);
              }
            }
          }
        }
      }
    } catch (GSException e) {
      throw new ShellException(getMessage("error.showUser") + " : msg=[" + e.getMessage() + "]", e);
    }
  }

  /**
   * ユーザが存在するか確認します.
   *
   * @param userName ユーザ名
   * @return true:存在する false:存在しない
   * @throws GSException GSException
   */
  private boolean existsUser(String userName) throws GSException {
    // [memo] ユーザ名は大文字小文字区別なし
    Map<String, UserInfo> userList = ExperimentalTool.getUsers(gridStore);
    for (Map.Entry<String, UserInfo> entry : userList.entrySet()) {
      if (userName.equalsIgnoreCase(entry.getKey())) {
        return true;
      }
    }
    return false;
  }

  /**
   * The main method for sub-command {@code grantacl}.<br>
   * Grant access rights. Need to run with administrator user.
   *
   * @param role authority
   * @param dbName database name
   * @param userName user name
   * @throws ShellException if it meets 1 of the following conditions:
   *     <ul>
   *       <li>The connection is closed
   *       <li>Current user is not an administrator
   *       <li>Error while checking user information
   *       <li>Database not found
   *       <li>Manage administrator privilege
   *       <li>User name not found
   *       <li>Privilege is already granted
   *       <li>Unable to grant permission
   *     </ul>
   *
   * @see ExperimentalTool#putPrivilege
   */
  @GSCommand(name = "grantacl")
  public void grant(PrivilegeInfo.RoleType role, String dbName, String userName) {
    checkConnected();
    checkSuperUser();

    try {
      DatabaseInfo dbInfo = getDatabaseInfo(dbName);
      // DBの存在チェック
      if (dbInfo == null) {
        throw new ShellException(getMessage("error.dbNotFound", dbName));
      }
      // 管理者ユーザチェック
      if (isSuperUser(userName)) {
        throw new ShellException(getMessage("error.privSuperUser", userName));
      }
      // 指定ユーザの存在チェック
      if (!existsUser(userName)) {
        throw new ShellException(getMessage("error.userNotFound", userName));
      }
      // 権限付与済みのチェック
      if (checkPrivilege(dbInfo, userName)) {
        throw new ShellException(
            getMessage("error.privilegeAlreadyExists", dbName)
                + " role=["
                + role.toString()
                + "] dbName=["
                + dbName
                + "] user=["
                + userName
                + "]");
      }
      // [memo] V2.7では、1つのデータベースには1人のユーザしか権限付与できない。
      // V4.3 1つのデータベースに複数ユーザの権限付与が可能となったためチェックをはずす。
      // if ( dbInfo.getPrivileges().size() != 0 ){
      // throw new ShellException(getMessage("error.privilegeAlreadyExists2",
      // dbName));
      // }

      PrivilegeInfo info = new PrivilegeInfo();
      // V4.3 PrivilegeInfo に権限をセット
      info.setRole(role);

      ExperimentalTool.putPrivilege(gridStore, dbName, userName, info);

    } catch (GSException e) {
      throw new ShellException(getMessage("error.grant") + " : msg=[" + e.getMessage() + "]", e);
    }
  }

  /**
   * The main method for sub-command {@code revokeacl}.<br>
   * Strip access rights. Need to run with administrator user.
   *
   * @param role authority
   * @param dbName database name
   * @param userName user name
   * @throws ShellException if it meets 1 of the following conditions:
   *     <ul>
   *       <li>The connection is closed
   *       <li>Current user is not an administrator
   *       <li>Error while checking user information
   *       <li>Database not found
   *       <li>Manage administrator privilege
   *       <li>User not found
   *       <li>This permission is not granted
   *       <li>Unable to revoke permission
   *     </ul>
   *
   * @see ExperimentalTool#dropPrivilege
   */
  @GSCommand(name = "revokeacl")
  public void revoke(PrivilegeInfo.RoleType role, String dbName, String userName) {
    checkConnected();
    checkSuperUser();

    try {
      DatabaseInfo dbInfo = getDatabaseInfo(dbName);
      // DBの存在チェック
      if (dbInfo == null) {
        throw new ShellException(getMessage("error.dbNotFound", dbName));
      }
      // 管理者ユーザチェック
      if (isSuperUser(userName)) {
        throw new ShellException(getMessage("error.privSuperUser", userName));
      }
      // 指定ユーザの存在チェック
      if (!existsUser(userName)) {
        throw new ShellException(getMessage("error.userNotFound", userName));
      }
      // 権限付与済みのチェック
      if (!checkPrivilege(dbInfo, userName, role)) {
        throw new ShellException(
            getMessage("error.privilegeNotFound")
                + " role=["
                + role.toString()
                + "] dbName=["
                + dbName
                + "] user=["
                + userName
                + "]");
      }

      PrivilegeInfo info = new PrivilegeInfo();
      // V4.3 PrivilegeInfo に権限をセット
      info.setRole(role);

      ExperimentalTool.dropPrivilege(gridStore, dbName, userName, info);

    } catch (GSException e) {
      throw new ShellException(getMessage("error.revoke") + " : msg=[" + e.getMessage() + "]", e);
    }
  }

  /**
   * 指定されたユーザが管理者ユーザかを確認します.
   *
   * <p>・ログインせずに確認する方法として、文字列の一致で処理する system, admin, 先頭がgs#で始まる
   *
   * @param name ユーザ名
   * @return true:管理者 / false:一般ユーザ
   */
  private boolean isSuperUser(String name) {
    if (name.equalsIgnoreCase("system")
        || name.equalsIgnoreCase("admin")
        || name.equalsIgnoreCase("gs#")) {
      return true;
    }
    return false;
  }

  /**
   * ユーザの権限が付与されているかを確認します.
   *
   * @param info データベース情報
   * @param userName ユーザ名
   * @return true:付与済み false:付与されていない
   */
  private boolean checkPrivilege(DatabaseInfo info, String userName) {
    for (Map.Entry<String, PrivilegeInfo> entry : info.getPrivileges().entrySet()) {
      if (userName.equalsIgnoreCase(entry.getKey())) {
        return true;
      }
    }
    return false;
  }

  /**
   * ユーザの権限が付与されているかを確認します.
   *
   * @param info データベース情報
   * @param userName ユーザ名
   * @param role 権限
   * @return true:付与済み false:付与されていない
   */
  private boolean checkPrivilege(DatabaseInfo info, String userName, PrivilegeInfo.RoleType role) {
    for (Map.Entry<String, PrivilegeInfo> entry : info.getPrivileges().entrySet()) {
      if (userName.equalsIgnoreCase(entry.getKey()) && role.equals(entry.getValue().getRole())) {
        return true;
      }
    }
    return false;
  }

  /**
   * The main method for sub-command {@code createindex}.<br>
   * Create an index.
   *
   * @param containerName container name
   * @param columnName column name
   * @param indexType index type
   * @param additionalIndexType array of index type
   * @throws ShellException if the connection is closed
   * @throws IllegalArgumentException if container is not existed
   * @see Container#createIndex
   */
  @GSCommand
  public void createIndex(
      String containerName,
      String columnName,
      IndexType indexType,
      IndexType... additionalIndexType) {
    checkConnected();

    try {
      Container<Object, Row> container = gridStore.getContainer(containerName);
      checkContainerExists(containerName, container);
      container.createIndex(columnName, indexType);
      for (IndexType it : additionalIndexType) {
        container.createIndex(columnName, it);
      }

    } catch (GSException e) {
      throw new ShellException(
          getMessage("error.createindex") + " : msg=[" + e.getMessage() + "]", e);
    }
  }

  /**
   * The main method for sub-command {@code createcompindex}.<br>
   * Create a composite index.
   *
   * @param containerName container name
   * @param columnNames array of column names
   * @throws ShellException if the connection is closed
   * @throws IllegalArgumentException if container is not existed or column does not have multiple
   *     values
   * @see Container#createIndex
   */
  @GSCommand
  public void createCompIndex(String containerName, String... columnNames) {
    checkConnected();

    try {
      Container<Object, Row> container = gridStore.getContainer(containerName);
      checkContainerExists(containerName, container);
      checkColumnNameMultipleValues(columnNames);

      IndexInfo indexInfo = new IndexInfo();
      List<String> columnNameList = Arrays.asList(columnNames);

      indexInfo.setType(IndexType.TREE);
      indexInfo.setColumnNameList(columnNameList);
      container.createIndex(indexInfo);

    } catch (GSException e) {
      throw new ShellException(
          getMessage("error.createcompindex") + " : msg=[" + e.getMessage() + "]", e);
    }
  }

  /**
   * The main method for sub-command {@code dropindex}.<br>
   * Drop the index.
   *
   * @param containerName container name
   * @param columnName column name
   * @param indexType index type
   * @param additionalIndexType array of index type
   * @throws ShellException if the connection is closed
   * @throws IllegalArgumentException if container is not existed
   * @see Container#dropIndex
   */
  @GSCommand
  public void dropIndex(
      String containerName,
      String columnName,
      IndexType indexType,
      IndexType... additionalIndexType) {
    checkConnected();

    try {
      Container<Object, Row> container = gridStore.getContainer(containerName);
      checkContainerExists(containerName, container);
      container.dropIndex(columnName, indexType);
      for (IndexType it : additionalIndexType) {
        container.dropIndex(columnName, it);
      }

    } catch (GSException e) {
      throw new ShellException(
          getMessage("error.dropindex") + " : msg=[" + e.getMessage() + "]", e);
    }
  }

  /**
   * The main method for sub-command {@code dropcompindex}.<br>
   * Drop the composite index.
   *
   * @param containerName container name
   * @param columnNames Array of column names
   * @throws ShellException if the connection is closed
   * @throws IllegalArgumentException if container is not existed or column name does not have
   *     multiple values
   * @see Container#dropIndex
   */
  @GSCommand
  public void dropCompIndex(String containerName, String... columnNames) {
    checkConnected();

    try {
      Container<Object, Row> container = gridStore.getContainer(containerName);
      checkContainerExists(containerName, container);

      checkColumnNameMultipleValues(columnNames);

      IndexInfo indexInfo = new IndexInfo();
      List<String> columnNameList = Arrays.asList(columnNames);

      indexInfo.setType(IndexType.TREE);
      indexInfo.setColumnNameList(columnNameList);

      container.dropIndex(indexInfo);

    } catch (GSException e) {
      throw new ShellException(
          getMessage("error.dropcompindex") + " : msg=[" + e.getMessage() + "]", e);
    }
  }

  /**
   * The main method for sub-command {@code createcontainer}.<br>
   * Create a container (COLLECTION or TIMESERIES). <br>
   * Import and create a meta information file (same format as Export).
   *
   * @param metaFile meta information file
   * @param containerName container name
   * @throws ShellException if it meets 1 of the following conditions:
   *     <ul>
   *       <li>The connection is closed
   *       <li>Partition container
   *       <li>Container already existed
   *     </ul>
   *
   * @see GridStore#putContainer
   * @since NoSQL 2.7/NewSQL 1.5
   */
  @GSCommand
  public void createContainer(String metaFile, @GSNullable String containerName) {
    checkConnected();

    try {
      MetaContainerFileIO meta = new MetaContainerFileIO();
      ToolContainerInfo conInfo = meta.readMetaInfo(metaFile);

      if (!conInfo.getAttribute().equals("SINGLE")) {
        throw new ShellException(getMessage("error.notSingleIsNotSupported"));
      }

      if (conInfo.isPartitioned()) {
        throw new ShellException(getMessage("error.notSingleIsNotSupported"));
      }

      if (containerName != null) {
        conInfo.setName(containerName);
      }

      createContainer(conInfo);

    } catch (ShellException e) {
      throw e;
    } catch (Exception e) {
      throw new ShellException(
          getMessage("error.createcontainer")
              + " : file=["
              + metaFile
              + "] msg=["
              + e.getMessage()
              + "]",
          e);
    }
  }

  /**
   * The main method for sub-command {@code createcollection}.<br>
   * Create a COLLECTION container
   *
   * @throws ShellException if it meets 1 of the following conditions:
   *     <ul>
   *       <li>The connection is closed
   *       <li>The column name or type is incorrect
   *       <li>Container already existed
   *     </ul>
   *
   * @param containerName container name
   * @param args combination of column name and column type
   * @see GridStore#putContainer
   * @since NoSQL 2.7/NewSQL 1.5
   */
  @GSCommand
  public void createCollection(String containerName, String... args) {
    checkConnected();

    ToolContainerInfo conInfo = new ToolContainerInfo();
    conInfo.setName(containerName);
    conInfo.setType(ContainerType.COLLECTION);

    List<Integer> rowKeyColumnList = new ArrayList<Integer>();
    rowKeyColumnList.add(Integer.valueOf(0));
    conInfo.setRowKeyColumnList(rowKeyColumnList);

    setColumnInfo(conInfo, args);
    createContainer(conInfo);
  }

  /**
   * The main method for sub-command {@code createtimeSeries}.<br>
   * Create a TIMESERIES container
   *
   * @param containerName container name
   * @param compMethod compression method
   * @param args combination of column name and column type
   * @throws ShellException if it meets 1 of the following conditions:
   *     <ul>
   *       <li>The connection is closed
   *       <li>Compression mode is incorrect or HI
   *       <li>The column name or type is incorrect
   *       <li>Container already existed
   *     </ul>
   *
   * @since NoSQL 2.7/NewSQL 1.5
   */
  @GSCommand
  public void createTimeSeries(String containerName, String compMethod, String... args) {
    checkConnected();

    ToolContainerInfo conInfo = new ToolContainerInfo();
    conInfo.setName(containerName);
    conInfo.setType(ContainerType.TIME_SERIES);

    List<Integer> rowKeyColumnList = new ArrayList<Integer>();
    rowKeyColumnList.add(Integer.valueOf(0));
    conInfo.setRowKeyColumnList(rowKeyColumnList);

    TimeSeriesProperties timeProp = new TimeSeriesProperties();
    CompressionMethod comMethod = null;
    try {
      comMethod = CompressionMethod.valueOf(compMethod.toUpperCase());
    } catch (Exception e) {
      throw new ShellException(
          getMessage("error.compositeMethodInvalid")
              + " : container=["
              + containerName
              + "] msg=["
              + e.getMessage()
              + "]",
          e);
    }
    if (comMethod == CompressionMethod.HI) {
      throw new ShellException(getMessage("error.createTimeseriesCompMethod"));
    }
    timeProp.setCompressionMethod(comMethod);
    conInfo.setTimeSeriesProperties(timeProp);

    setColumnInfo(conInfo, args);
    createContainer(conInfo);
  }

  /**
   * コンテナ情報オブジェクトに、カラム情報をセットします.
   *
   * @param conInfo コンテナ情報オブジェクト
   * @param args カラム名とカラムタイプの組合せ
   */
  private void setColumnInfo(ToolContainerInfo conInfo, String[] columnArray) {
    if (columnArray.length == 0) {
      throw new ShellException(getMessage("error.columnInvalid"));
    }

    String columnName = null;
    String type = null;
    try {
      for (int i = 0; i < columnArray.length; i++) {
        if (i % 2 == 0) {
          columnName = columnArray[i];
        } else {
          type = columnArray[i];
          GSType columnType = MetaContainerFileIO.convertStringToColumnType(type);
          if (MetaContainerFileIO.isTimestampStringInSeconds(type)) {
            ColumnInfo ci = new ColumnInfo(columnName, columnType);
            ColumnInfo.Builder builder = new ColumnInfo.Builder(ci);
            builder.setTimePrecision(MetaContainerFileIO.convertTimestampStringToTimeUnit(type));
            conInfo.addColumnInfo(builder.toInfo());
          }
          else {
            conInfo.addColumnInfo(columnName, MetaContainerFileIO.convertStringToColumnType(type));
          }
          columnName = null;
        }
      }
    } catch (GridStoreCommandException e) {
      throw new ShellException(
          getMessage("error.columnTypeInvalid")
              + " columnName=["
              + columnName
              + "] type=["
              + type
              + "]",
          e);
    }
    if (columnName != null) {
      throw new ShellException(
          getMessage("error.columnInvalid") + " columnName=[" + columnName + "]");
    }
  }

  /**
   * コンテナを作成します.
   *
   * @param conInfo コンテナ情報オブジェクト
   */
  private void createContainer(ToolContainerInfo conInfo) {

    String createdName = conInfo.getContainerInfo().getName();
    try {
      ContainerInfo cInfo = gridStore.getContainerInfo(createdName);
      if (cInfo != null) {
        throw new ShellException(getMessage("error.containerAlreadyExists", createdName));
      }

      ContainerInfo createInfo = conInfo.getContainerInfo();
      Container<?, Row> container = gridStore.putContainer(createInfo.getName(), createInfo, false);

      try {
        for (IndexInfo indexInfo : conInfo.getIndexInfoList()) {
          container.createIndex(indexInfo);
        }
      } catch (Exception e) {
        throw new ShellException(
            getMessage("error.createIndexInCreateContainer")
                + " : container=["
                + createdName
                + "] msg=["
                + e.getMessage()
                + "]",
            e);
      }

      try {
        for (TriggerInfo triggerInfo : createInfo.getTriggerInfoList()) {
          container.createTrigger(triggerInfo);
        }
      } catch (Exception e) {
        throw new ShellException(
            getMessage("error.createTriggerInCreateContainer")
                + " : container=["
                + createdName
                + "] msg=["
                + e.getMessage()
                + "]",
            e);
      }

    } catch (ShellException e) {
      throw e;
    } catch (Exception e) {
      String msg = getMessage("error.createcontainer2");
      if (createdName != null) {
        msg += " container=[" + createdName + "]";
      }

      msg += " msg=[" + e.getMessage() + "]";
      throw new ShellException(msg, e);
    }
  }

  /**
   * The main method for sub-command {@code dropcontainer}.<br>
   * Delete the container.
   *
   * @param containerName container name
   * @throws ShellException if it meets 1 of the following conditions:
   *     <ul>
   *       <li>The connection is closed
   *       <li>Container not found
   *       <li>Drop view
   *       <li>Drop partition table
   *     </ul>
   *
   * @see GridStore#dropContainer
   * @since NoSQL 2.7/NewSQL 1.5
   */
  @GSCommand
  public void dropContainer(String containerName) {
    checkConnected();

    try {
      ExtendedContainerInfo info =
          ExperimentalTool.getExtendedContainerInfo(gridStore, containerName);
      if (info == null) {
        throw new ShellException(getMessage("error.containerNotFound", containerName));
      } else if (info.getAttribute() == ContainerAttribute.VIEW) {
        throw new ShellException(getMessage("error.notAllowedOnView", containerName));
      } else if (info.getAttribute() != ContainerAttribute.SINGLE) {
        throw new ShellException(getMessage("error.notAllowedOnPartitioned", containerName));
      }

      gridStore.dropContainer(containerName);

    } catch (GSException e) {
      throw new ShellException(
          getMessage("error.dropcontainer")
              + " : container=["
              + containerName
              + "] msg=["
              + e.getMessage()
              + "]",
          e);
    }
  }

  private void showContainerList() throws GSException {
    PartitionController partCont = null;
    try {
      println("Database : " + m_dbName);

      println("Name                 Type        PartitionId");
      println("---------------------------------------------");

      int containerCount = 0;
      int containerErrCount = 0;
      partCont = gridStore.getPartitionController();
      int partCount = partCont.getPartitionCount();

      List<Integer> errorList = null;
      for (int partId = 0; partId < partCount; ++partId) {
        try {
          List<String> containerNames = partCont.getContainerNames(partId, 0L, null);

          for (String name : containerNames) {
            ContainerInfo contInfo = gridStore.getContainerInfo(name);
            if (contInfo == null) {
              printfln("%-20s  %s", name, "Error: Failed to get container info.");
              containerErrCount++;
            } else {
              printfln("%-20s %-11s %11d", name, contInfo.getType(), partId);
              containerCount++;
            }
          }
        } catch (GSException e) {
          Logger logger = LoggerFactory.getLogger(DataCommandClass.class);
          logger.error(
              getMessage("error.showContainerInPartition") + " partitionID=[" + partId + "]", e);

          if (errorList == null) {
            errorList = new ArrayList<Integer>();
          }
          errorList.add(partId);
        }
      }

      println("");
      print(" Total Count: " + containerCount);
      if (containerErrCount > 0) {
        print("   error: " + containerErrCount);
      }
      println("");

      if (errorList != null) {
        throw new ShellException(
            getMessage("error.showContainerInPartition") + " partitionIdList=" + errorList);
      }

      println("");

    } finally {
      closeQuitely(partCont);
    }
  }

  /**
   * The main method for sub-command {@code showcontainer}.<br>
   * Displays container information.
   *
   * @param containerName container name
   * @throws ShellException if the connection is closed or container not existed
   */
  @GSCommand
  public void showContainer(@GSNullable String containerName) {
    checkConnected();

    if (containerName == null) {
      try {
        showContainerList();
      } catch (Exception e) {
        throw new ShellException(
            getMessage("error.showcontainer") + " : msg=[" + e.getMessage() + "]", e);
      }

    } else {
      try {
        ExtendedContainerInfo extInfo =
            ExperimentalTool.getExtendedContainerInfo(gridStore, containerName);
        checkContainerExists(containerName, extInfo);
        ContainerInfo contInfo = gridStore.getContainerInfo(containerName);
        checkContainerExists(containerName, contInfo);

        // SINGLE/LARGE以外のコンテナは存在しない扱い
        if (extInfo.getAttribute() != ContainerAttribute.SINGLE
            && extInfo.getAttribute() != ContainerAttribute.LARGE) {
          throw new ShellException(getMessage("error.notNoSQLContainer", containerName));
        }

        // LARGEコンテナであれば、パーティションに関する出力を行う
        boolean isPartitioned = (extInfo.getAttribute() == ContainerAttribute.LARGE);

        switch (contInfo.getType()) {
          case COLLECTION:
            showCollectionDetail(contInfo, isPartitioned);
            break;
          case TIME_SERIES:
          default:
            showTimeSeriesDetail(contInfo, isPartitioned);
            break;
        }

      } catch (Exception e) {
        throw new ShellException(
            getMessage("error.showcontainerDetail") + " : msg=[" + e.getMessage() + "]", e);
      }
    }
  }

  /**
   * The main method for sub-command {@code showtable}.<br>
   * Displays table information.
   *
   * @param containerName container name
   * @throws ShellException if the connection is closed or container not existed
   */
  @GSCommand
  public void showtable(@GSNullable String containerName) {
    showContainer(containerName);
  }

  /**
   * コンテナの基本情報を表示します.
   *
   * @param contInfo コンテナ情報
   * @param isPartitioned パーティションコンテナか否か
   * @throws Exception Exception
   */
  private void showContainerDetail(ContainerInfo contInfo, boolean isPartitioned) throws Exception {
    PartitionController partCont = null;
    partCont = gridStore.getPartitionController();
    final int partId = partCont.getPartitionIndexOfContainer(contInfo.getName());

    println("Database    : " + m_dbName);
    println("Name        : " + contInfo.getName());
    String dataAffinity = contInfo.getDataAffinity() == null ? "-" : contInfo.getDataAffinity();

    println("Type        : " + contInfo.getType());
    println("Partition ID: " + partId);
    println("DataAffinity: " + dataAffinity);
    if (isPartitioned) {
      showTablePartitioningDetail(contInfo.getName());
    }

    if ((contInfo.getTriggerInfoList() != null) && (contInfo.getTriggerInfoList().size() != 0)) {
      println("TriggerCount: " + contInfo.getTriggerInfoList().size());
    }

    println("");
  }

  /**
   * テーブルパーティショニング情報の詳細を表示します.
   *
   * @param name name
   * @throws Exception Exception
   */
  private void showTablePartitioningDetail(String name) {
    // SQL接続が無い場合、パーティションテーブルである旨のみ表示
    if (m_jdbcCon == null) {
      println("Partitioned : true (need SQL connection for details)");
      return;
    }
    List<TablePartitionProperty> props = null;
    ExpirationInfo expInfo = null;

    try {
      props = GridDBJdbcUtils.getTablePartitionProperties(m_jdbcCon, name);

      expInfo = GridDBJdbcUtils.getExpirationInfo(m_jdbcCon, name);

    } catch (Exception e) {
      Logger logger = LoggerFactory.getLogger(DataCommandClass.class);
      logger.error(e.getMessage(), e);
    }
    if (props == null || props.isEmpty()) {
      println("Partitioned : true (failed to get details)");
      return;
    }

    println("Partitioned : true");
    printTablePartitionProperty(props.get(0), false);
    if (props.size() == 2) {
      printTablePartitionProperty(props.get(1), true);
    }

    if (expInfo != null) {
      printExpirationInfo(expInfo);
    }
  }

  private void printTablePartitionProperty(TablePartitionProperty prop, boolean isSub) {
    String preStr = isSub ? "Sub Partition " : "Partition ";
    String type = prop.getType();
    println(preStr + "Type           : " + type);
    println(preStr + "Column         : " + prop.getColumn());
    if (type.equals(ToolConstants.TABLE_PARTITION_TYPE_HASH)) {
      println(preStr + "Division Count : " + prop.getDivisionCount());
    } else if (type.equals(ToolConstants.TABLE_PARTITION_TYPE_INTERVAL)) {
      println(preStr + "Interval Value : " + prop.getIntervalValue());
      String itvUnit = prop.getIntervalUnit() == null ? "-" : prop.getIntervalUnit();
      println(preStr + "Interval Unit  : " + itvUnit);
    }
  }

  private void printExpirationInfo(ExpirationInfo expInfo) {
    println("Expiration Type      : " + expInfo.getType());
    println("Expiration Time      : " + expInfo.getTime());
    println("Expiration Time Unit : " + expInfo.getTimeUnit().toString());
  }

  /**
   * コレクションの詳細情報を表示します.
   *
   * @param contInfo コンテナ情報
   * @param maintenance メンテナンスモード
   * @throws GSException GSException
   */
  private void showCollectionDetail(ContainerInfo contInfo, boolean isPartitioned)
      throws Exception {
    showContainerDetail(contInfo, isPartitioned);

    println("Columns:");
    println("No  Name                  Type            CSTR  RowKey");
    println("------------------------------------------------------------------------------");
    int colCount = contInfo.getColumnCount();
    List<IndexInfo> gsIndices = contInfo.getIndexInfoList();
    List<Integer> rowKeyColumnList = contInfo.getRowKeyColumnList();

    for (int colNo = 0; colNo < colCount; ++colNo) {
      ColumnInfo colInfo = contInfo.getColumnInfo(colNo);
      String colName = colInfo.getName();

      String cstr = "";
      if (!colInfo.getNullable()) {
        cstr = "NN";
      }

      String rowKey = "";
      if (isRowKeyColumn(colNo, rowKeyColumnList)) {
        rowKey = "[RowKey]";
      }

      printfln("%2d  %-20s  %-15s %-5s %s", colNo, colName, formatColumnType(colInfo), cstr, rowKey);
    }

    if (gsIndices.size() > 0) {
      println("");
      println("Indexes:");
      for (IndexInfo gsIndex : gsIndices) {
        printfln("Name        : %s", nullToEmpty(gsIndex.getName()));
        printfln("Type        : %s", gsIndex.getType());
        println("Columns:");
        println("No  Name                  ");
        println("--------------------------");
        List<String> indexColNameList = gsIndex.getColumnNameList();
        int indexColCount = indexColNameList.size();
        for (int indexColNo = 0; indexColNo < indexColCount; indexColNo++) {
          printfln("%2d  %s", indexColNo, indexColNameList.get(indexColNo));
        }
        println("");
      }
    }
  }

  @Deprecated
  private ExtendedContainerInfo getPartitionTableDetail(ExtendedContainerInfo contInfo)
      throws Exception {
    checkConnectedSQL();

    ResultSet rs = null;
    try {
      List<ColumnInfo> columns = new ArrayList<ColumnInfo>();
      Set<IndexType> columnIndexs = new HashSet<IndexType>();

      // メタデータからプライマリキーの情報を取得する。
      // テーブル名以外の条件は無視されるが、null指定はできない
      DatabaseMetaData dbmd = m_jdbcCon.getMetaData();
      rs = dbmd.getPrimaryKeys("", "", contInfo.getName());

      // RKAは1つめのカラムにPRIMARY KEY指定があった場合にtrueとなる(V3.0)
      // ロウキーは最初のカラム以外に設定できない(V3.0)
      // データが存在すれば、RKA=trueであると分かる。
      if (rs.next()) {
        contInfo.setRowKeyAssigned(true);
      } else {
        contInfo.setRowKeyAssigned(false);
      }
      rs.close();
      rs = null;

      // メタデータからテーブルのカラム情報を取得する。
      // テーブル名以外の条件は無視されるが、null指定はできない
      rs = dbmd.getColumns("", "", contInfo.getName(), "");
      while (rs.next()) { // 1行目は無視。メタデータのカラム名は取る必要なし
        String columnName = rs.getString("COLUMN_NAME");
        String columnTypeStr = rs.getString("TYPE_NAME");
        GSType columnType = null;
        if (columnTypeStr != null && columnTypeStr.length() != 0) {
          columnType = GSType.valueOf(columnTypeStr.toUpperCase());
        }

        ColumnInfo colInfo = new ColumnInfo(columnName, columnType, columnIndexs);
        columns.add(colInfo);
      }
      rs.close();
      rs = null;

      // カラム情報を上書きする。
      contInfo.setColumnInfoList(columns);

      return contInfo;

    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
      } catch (Exception e) {
        // Do nothing
      }
    }
  }

  /**
   * 時系列の詳細情報を表示します.
   *
   * @param contInfo コンテナ情報
   * @param isPartitioned パーティションコンテナか否か
   * @throws Exception Exception
   */
  private void showTimeSeriesDetail(ContainerInfo contInfo, boolean isPartitioned)
      throws Exception {
    showContainerDetail(contInfo, isPartitioned);

    TimeSeriesProperties prop = contInfo.getTimeSeriesProperties();
    println("Compression Method : " + prop.getCompressionMethod());
    print(
        "Compression Window : "
            + ((prop.getCompressionWindowSize() == -1) ? "-" : prop.getCompressionWindowSize()));
    println(
        (prop.getCompressionWindowSizeUnit() == null)
            ? ""
            : " [" + prop.getCompressionWindowSizeUnit() + "]");
    print(
        "Row Expiration Time: "
            + ((prop.getRowExpirationTime() == -1) ? "-" : prop.getRowExpirationTime()));
    println(
        (prop.getRowExpirationTimeUnit() == null)
            ? ""
            : " [" + prop.getRowExpirationTimeUnit() + "]");
    println(
        "Row Expiration Division Count: "
            + ((prop.getExpirationDivisionCount() == -1)
                ? "-"
                : prop.getExpirationDivisionCount()));
    println("");

    println("Columns:");
    println("No  Name                  Type            CSTR  RowKey   Compression   ");
    println("------------------------------------------------------------------------------");
    int colCount = contInfo.getColumnCount();
    Set<String> specCols = prop.getSpecifiedColumns();
    List<IndexInfo> gsIndices = contInfo.getIndexInfoList();
    List<Integer> rowKeyColumnList = contInfo.getRowKeyColumnList();

    for (int colNo = 0; colNo < colCount; ++colNo) {
      ColumnInfo colInfo = contInfo.getColumnInfo(colNo);
      String colName = colInfo.getName();

      String cstr = "";
      if (!colInfo.getNullable()) {
        cstr = "NN";
      }

      String comp = "";
      if (specCols.contains(colName)) {
        if (prop.isCompressionRelative(colName)) {
          comp =
              String.format(
                  "REL(rate=%f,span=%f)",
                  prop.getCompressionRate(colName), prop.getCompressionSpan(colName));
        } else {
          comp = String.format("ABS(width=%f)", prop.getCompressionWidth(colName));
        }
      }
      String rowKey = "";
      if (isRowKeyColumn(colNo, rowKeyColumnList)) {
        rowKey = "[RowKey]";
      }

      printfln(
          "%2d  %-20s  %-15s %-5s %-9s %s", colNo, colName, formatColumnType(colInfo), cstr, rowKey, comp);
    }

    if (gsIndices.size() > 0) {
      println("");
      println("Indexes:");
      for (IndexInfo gsIndex : gsIndices) {
        printfln("Name        : %s", nullToEmpty(gsIndex.getName()));
        printfln("Type        : %s", gsIndex.getType());
        printfln("Columns:");
        println("No  Name                  ");
        println("--------------------------");
        List<String> indexColNameList = gsIndex.getColumnNameList();
        int indexColCount = indexColNameList.size();
        for (int indexColNo = 0; indexColNo < indexColCount; indexColNo++) {
          printfln("%2d  %s", indexColNo, indexColNameList.get(indexColNo));
        }
        println("");
      }
    }
  }

  /**
   * Display format for column type
   * @param colInfo The column information
   * @return Column type
   * @throws GridStoreCommandException
   */
  private Object formatColumnType(ColumnInfo colInfo) throws GridStoreCommandException {
    if (colInfo.getType().equals(GSType.TIMESTAMP) && MetaContainerFileIO.isTimestampUnit(colInfo.getTimePrecision())) {
      return MetaContainerFileIO.convertTimeunitToTimestampType(colInfo.getTimePrecision());
    }
    return colInfo.getType();
  }

  /**
   * ロウキーのカラムであるかを返します.
   *
   * @param colNo カラム番号
   * @param rowKeyColumnList ロウキーのカラム番号のリスト
   * @return colNoがロウキーのカラム番号である場合true
   */
  private boolean isRowKeyColumn(int colNo, List<Integer> rowKeyColumnList) {
    boolean ret = false;
    if (rowKeyColumnList != null && rowKeyColumnList.contains(Integer.valueOf(colNo))) {
      ret = true;
    }
    return ret;
  }

  /**
   * テーブルの詳細情報を表示します.
   *
   * @param contInfo コンテナ情報
   * @param maintenance メンテナンスモード
   * @throws GSException GSException
   */
  @Deprecated
  private void showTableDetail(ExtendedContainerInfo contInfo, boolean maintenance)
      throws Exception {
    // V2.7では、New SQLのコンテナはCollectionタイプのみ。
    showCollectionDetail(contInfo, maintenance);
  }

  /**
   * The main method for sub-command {@code tql}.<br>
   * Execute a search and retain the search results.
   *
   * @param containerName container name
   * @param query query string
   * @throws ShellException if it meets 1 of below conditions:
   *     <ul>
   *       <li>The connection is closed
   *       <li>Unable to get the container
   *       <li>Failed to execute query
   *     </ul>
   *
   * @throws IllegalArgumentException error when container not existed
   */
  @GSCommand(multiline = true)
  public void tql(String containerName, String query) {
    checkConnected();

    queryObjClose();

    try {
      queryContainer = gridStore.getContainer(containerName);
      checkContainerExists(containerName, queryContainer);
      queryObj = queryContainer.query(query, null);

      @SuppressWarnings("deprecation")
      FetchOption fetchOption =
          FetchOption.valueOf(GS_TQL_FETCH_MODE_DEFAULT);

      // V4.0 パーティショニングテーブルにはPARTIAL_EXECUTIONのみ指定可
      ExtendedContainerInfo exInfo =
          ExperimentalTool.getExtendedContainerInfo(gridStore, containerName);
      if (exInfo != null && exInfo.getAttribute() == ContainerAttribute.LARGE) {
        fetchOption = FetchOption.PARTIAL_EXECUTION;
      }

      String fetchMode = getAttributeString(GridStoreShell.TQL_FETCH_MODE, "").toUpperCase();
      try {
        fetchOption = FetchOption.valueOf(fetchMode);
      } catch (IllegalArgumentException e) {
      }

      // オプション指定
      // (設計メモ)オプションによって第2引数の型が変わることに注意する。
      if (fetchOption.equals(FetchOption.SIZE) || fetchOption.equals(FetchOption.LIMIT)) {
        // LIMITまたはSIZEの場合、フェッチサイズをintで指定する
        queryObj.setFetchOption(fetchOption, getFetchSize());
      } else {
        // PARTIAL_EXECUTIONの場合、trueを指定すると部分実行が有効になる
        queryObj.setFetchOption(fetchOption, true);
      }

      long start = System.currentTimeMillis();
      queryRowSet = queryObj.fetch();
      long end = System.currentTimeMillis();

      if (fetchOption.equals(FetchOption.SIZE) || fetchOption.equals(FetchOption.LIMIT)) {
        println(getMessage("message.hitCount", queryRowSet.size(), end - start));
      } else {
        println(getMessage("message.selectOnly", end - start));
      }


    } catch (GSException e) {
      queryObjClose();
      throw new ShellException(getMessage("error.tql") + " : msg=[" + e.getMessage() + "]", e);

    } catch (IllegalArgumentException e) {
      queryObjClose();
      throw e;
    }
  }

  /**
   * TQL/SQLのフェッチサイズを返します.
   *
   * @return フェッチサイズ
   */
  private int getFetchSize() {
    int fetchSize = FETCH_SIZE;
    try {
      String fetchSizeStr = (String) getContext().getAttribute(GridStoreShell.NAME_FETCH_SIZE);
      int tmp = Integer.parseInt(fetchSizeStr);
      fetchSize = tmp;
    } catch (Exception e) {
      // Do nothing
    }

    return fetchSize;
  }

  /**
   * NULLの標準出力用文字列を返します.
   *
   * <p>隠し変数GS_NULL_STDOUTの文字列を取得して返します。 値が無かった場合はデフォルト値を返します。
   *
   * @return 文字列
   */
  private String getNullStdOut() {
    return getAttributeString(GridStoreShell.NAME_NULL_STDOUT, GS_NULL_STDOUT_DEFAULT);
  }

  /**
   * NULLのCSV出力用文字列を返します.
   *
   * <p>隠し変数GS_NULL_CSVの文字列を取得して返します。 値が無かった場合はデフォルト値を返します。
   *
   * @return 文字列
   */
  private String getNullCsv() {
    return getAttributeString(GridStoreShell.NAME_NULL_CSV, GS_NULL_CSV_DEFAULT);
  }

  /**
   * The main method for sub-command {@code sql}.<br>
   * Execute the SQL.
   *
   * @param sql SQL string
   * @throws ShellException if it meets 1 of the below conditions:
   *     <ul>
   *       <li>SQL connection is closed
   *       <li>Query is not set
   *       <li>Error when executing the query
   *     </ul>
   *
   * @since NoSQL 2.7/NewSQL 1.5
   */
  @GSCommand(multiline = true)
  public void sql(String sql) {
    checkConnectedSQL();

    queryObjClose();

    try {
      sql = sql.trim();
      if (sql.length() == 0) {
        throw new ShellException(getMessage("error.sqlIsNull"));
      }

      m_jdbcStmt = m_jdbcCon.createStatement();
      m_jdbcSQL  = sql;

      String sqlCheckStr = sql.replaceAll("/\\*[^\\*]*\\*/", " ");
      sqlCheckStr = (sqlCheckStr.replaceAll("--.*(\r\n|\n)", " ")).trim();
      String[] tmp = sqlCheckStr.split("\\s");

      boolean isExplain = false;

      if (tmp[0].equalsIgnoreCase("explain")) {
        isExplain = true;
        sqlCheckStr = (sqlCheckStr.substring("explain".length())).trim();
        tmp = sqlCheckStr.split("\\s");
        if (tmp[0].equalsIgnoreCase("analyze")) {
          sqlCheckStr = (sqlCheckStr.substring("analyze".length())).trim();
          tmp = sqlCheckStr.split("\\s");
        }
      }

      if (tmp[0].equalsIgnoreCase("select") || isExplain) {
        // SELECT
        m_jdbcStmt.setFetchSize(getFetchSize());
        long start = System.currentTimeMillis();
        m_jdbcRS = m_jdbcStmt.executeQuery(sql);
        long end = System.currentTimeMillis();

        int count = -1;
        if (isSqlCount()) {
          count = getSqlResultCount(sql);
        }
        if (count != -1) {
          println(getMessage("message.hitCount", count, end - start));
        } else {
          println(getMessage("message.selectOnly", end - start));
        }

      } else if (tmp[0].equalsIgnoreCase("insert")
          || tmp[0].equalsIgnoreCase("delete")
          || tmp[0].equalsIgnoreCase("update")) {
        // INSERT/DELETE/UPDATE
        int result = m_jdbcStmt.executeUpdate(sql);

        if (tmp[0].equalsIgnoreCase("insert")) {
          println(getMessage("message.insertcount", result));
        } else if (tmp[0].equalsIgnoreCase("delete")) {
          println(getMessage("message.deletecount", result));
        } else if (tmp[0].equalsIgnoreCase("update")) {
          println(getMessage("message.updatecount", result));
        }

        m_jdbcStmt.close();
        m_jdbcStmt = null;

      } else {
        // DDL
        m_jdbcStmt.executeUpdate(sql);
        m_jdbcStmt.close();
        m_jdbcStmt = null;
      }


    } catch (Exception e) {
      queryObjClose();
      throw new ShellException(getMessage("error.sql") + " : msg=[" + e.getMessage() + "]", e);
    }
  }

  /**
   * SQLのSELECT文のヒット件数を返します.
   *
   * @param sql SQL文(SELECT)
   * @return ヒット件数 (-1の場合は失敗)
   */
  private int getSqlResultCount(String sql) {
    int count = -1;
    Statement stmt = null;
    ResultSet rs = null;
    try {
      String countSQL = "select count(*) from ( " + sql + " ) dummy";
      stmt = m_jdbcCon.createStatement();
      stmt.setFetchSize(1);
      rs = stmt.executeQuery(countSQL);
      if (rs.next()) {
        count = rs.getInt(1);
      }
    } catch (Exception e) {
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (stmt != null) {
          stmt.close();
        }
      } catch (Exception e) {
        // Do nothing
      }
    }

    return count;
  }

  /* improve query result: display pretty format as table instead of CSV */
  private class ResultTable {
    private final String                  CELL_PADDING;
    private final Integer                 MAX_CELL_WIDTH;
    private final Integer                 columnCount;
    private final ArrayList<String>       columnHeader;
    private final Integer[]               cellWidthList;
    private ArrayList<ArrayList<String>>  resultRows;

    /**
     * Create a result table
     *
     * @param columnNames name of columns
     * @param maxColumnWidth maximum width for all columns
     */
    public ResultTable(ArrayList<String> columnNames, Integer maxColumnWidth) {
      this.columnHeader   = columnNames;
      this.columnCount    = columnNames.size();
      this.cellWidthList  = columnNames.stream()
                                .map(column -> Math.min(column.length(), maxColumnWidth))
                                .toArray(Integer[]::new);
      this.resultRows     = new ArrayList<ArrayList<String>>();
      this.MAX_CELL_WIDTH = maxColumnWidth;
      this.CELL_PADDING   = new String(new char[maxColumnWidth])
                                      .replace("\0", " ");
    }

    /**
     * Create a result table
     *
     * @param columnNames name of columns
     */
    public ResultTable(ArrayList<String> columnNames) {
      this(columnNames, MAX_COLUMN_WIDTH_DEFAULT);
    }

    /**
     * Get the width of a given column
     *
     * @param cloumnIndex the position of column in table
     * @return a interger value
     */
    public Integer getColumnWidth(int cloumnIndex) {
      return cellWidthList[cloumnIndex];
    }

    /**
     * Set width to a column
     *
     * @param cloumnIndex index of column to set width
     * @param width the value to set
     */
    public void setColumnWidth(int cloumnIndex, Integer width) {
      cellWidthList[cloumnIndex] = width;
    }

    /**
     * Add a new row into the ResultTable
     *
     * @param row the row that represent by list of cell values
     */
    public void addRow(ArrayList<String> row) {
      if (row.size() != columnCount) {
        throw new ShellException(getMessage("error.getrow") + " : msg=[" + getMessage("message.getrow.mismatch", row.size(), columnCount) + "]");
      }
      resultRows.add(row);
      /* update column width */
      for (int colNo = 0; colNo < columnCount; ++colNo) {
        int cellWidth = getConsoleTextLength(row.get(colNo));
        /* limit cell width if it exceeds MAX_CELL_WIDTH */
        if (MAX_CELL_WIDTH > 0 && cellWidth > MAX_CELL_WIDTH) {
            cellWidth = MAX_CELL_WIDTH;
        }
        /* expand cell width */
        if (cellWidth > cellWidthList[colNo]) {
          cellWidthList[colNo] = cellWidth;
        }
      }
    }

    /**
     * Get width of text when it display on command line console
     *
     * @param text the text to measure
     * @return width of text in integer value
     */
    public int getConsoleTextLength(String text) {
      return text.codePoints()
                 .map(c -> getCharacterDisplayWidth(c))
                 .reduce(0, (sum, x) -> sum + x);
    }

    /**
     * Print border of table
     */
    private void printBorder() {
      System.out.println("+" + String.join("+",
          Stream.of(this.cellWidthList)
                .map(width -> new String(new char[width+2])
                                  .replace("\0", "-"))
                .toArray(String[]::new)) + "+");
    }

    /**
     * Print a row in table
     *
     * @param row the row to print
     */
    private void printRow(ArrayList<String> row) {
      String line = "|";
      for (int i = 0; i < columnCount; ++i) {
        String value = row.get(i);
        int width = getConsoleTextLength(value);
        /* fill padding to cell by spaces */
        String padding = "";
        if (width < cellWidthList[i]) {
          padding = CELL_PADDING.substring(0, cellWidthList[i] - width);
        }
        String cell = value;
        if (width > MAX_CELL_WIDTH) {
          String  cutInfo  = "...";
          if (MAX_CELL_WIDTH < cutInfo.length()) {
            cutInfo = cutInfo.substring(0, MAX_CELL_WIDTH);
          }
          /* in case of display width of all charactes is 1  */
          if(value.length() == width) {
            cell = cell.substring(0, MAX_CELL_WIDTH - cutInfo.length()) + cutInfo;
          } else {
            /* calculate the display width of mixed CJK and latin characters,
              display width of them are different */
            final int[] charCodes  = value.codePoints().toArray();
            int displayWidth = 0;
            int k = 0;
            for(; k < charCodes.length; k++) {
              int charWidth = getCharacterDisplayWidth(charCodes[k]);
              if (displayWidth + charWidth + cutInfo.length() > MAX_CELL_WIDTH) {
                break;
              }
              displayWidth += charWidth;
            }
            /* shownChars: the number of characters of display part */
            final int shownChars = k;
            /* Extra Dots:
            Assuming max-width=6, then 'aa米米a' has width = 7 (米=2),
            will display 'aa....' (6 chars: 2 displayed + 1 extraDots + 3 cutInfo's dots) */
            String extraDots = "";
            if (displayWidth < MAX_CELL_WIDTH) {
              extraDots = new String(new char[MAX_CELL_WIDTH - cutInfo.length() - displayWidth])
                                .replace("\0", ".");
            }
            cell = cell.substring(0, shownChars) + extraDots + cutInfo;
          }
        }
        line += " " + cell + padding + " |";
      }
      System.out.println(line);
    }

    /**
     * Display the result table
     */
    public void display() {
      printBorder();
      printRow(columnHeader);
      printBorder();
      resultRows.forEach(row -> {
        printRow(row);
      });
      printBorder();
    }
  }
  
  /** Acquire and display query result. */
  private abstract class RowGetter {

    /**
     * 表示処理.
     *
     * @param rowNo 表示する行の番号（ヘッダ行の場合は0、データ行は1から連番）
     * @param line 表示する行
     */
    protected abstract void printLine(int rowNo, String... line);

    /**
     * 日付時刻のフォーマット処理.
     *
     * @param date Dateオブジェクト
     * @return 日付時刻を文字列化したもの
     */
    protected abstract String formatDate(Date date);

   /**
    * Convert timestamp to string base on its precision
    *
    * @param timestamp the timestamp to convert
    * @param precisionUnit unit of timestamp
    * @return formatted string of timestamp
    */
    protected String formatTimestamp(Timestamp timestamp, TimeUnit precisionUnit) {
      ZonedDateTime zdt = null;
      if (m_connectTimeZoneVal == null) {
        // UTC if there is no time zone settings
        zdt = ZonedDateTime.ofInstant(timestamp.toInstant(), ZoneId.of("UTC"));
      } else if ("auto".equals(m_connectTimeZoneVal)) {
        // If the time zone setting is Auto, the system default
        zdt = ZonedDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault());
      } else {
        // If there is a time zone settings, reflect the setting value
        zdt = ZonedDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.of(m_connectTimeZoneVal));
      }
      return zdt.format(MetaContainerFileIO.getDateTimeFormatter(precisionUnit));
    }

    /**
     * オブジェクトの文字列化.
     *
     * @param data オブジェクト
     * @param replaceNull NULL値を文字列に置換するか否か
     * @return オブジェクトを文字列化したもの
     */
    private String stringify(Object data, boolean replaceNull) {
      if (data == null) {
        return replaceNull ? getNullStdOut() : null;
      } else if (data instanceof Blob) {
        return BLOB_SHOW_STRING;
      } else if (data instanceof Date) {
        return formatDate((Date) data);
      } else if (data instanceof Date[]) {
        Date[] dateArray = (Date[]) data;
        String[] tmp = new String[dateArray.length];
        for (int k = 0; k < dateArray.length; ++k) {
          tmp[k] = formatDate(dateArray[k]);
        }
        return Arrays.toString(tmp);
      } else if (data instanceof Object[]) {
        return Arrays.toString((Object[]) data);
      } else if (data instanceof boolean[]) {
        return Arrays.toString((boolean[]) data);
      } else if (data instanceof byte[]) {
        return Arrays.toString((byte[]) data);
      } else if (data instanceof double[]) {
        return Arrays.toString((double[]) data);
      } else if (data instanceof float[]) {
        return Arrays.toString((float[]) data);
      } else if (data instanceof int[]) {
        return Arrays.toString((int[]) data);
      } else if (data instanceof long[]) {
        return Arrays.toString((long[]) data);
      } else if (data instanceof short[]) {
        return Arrays.toString((short[]) data);
      } else {
        return data.toString();
      }
    }

    /**
     * Convert timestamp to string
     * 
     * @param data the timestamp date
     * @param precisionUnit the precision of timestamp in TimeUnit
     * @return string of timestamp
     */
    private String stringify(Timestamp data, TimeUnit precisionUnit) {
      return formatTimestamp(data, precisionUnit);
    }

    /**
     * Get rows and display.
     *
     * @param count maximum number of records to retrieve
     * @param replaceNull whether to replace NULL values with strings
     * @return number acquired rows
     */
    public int getRow(Integer count, boolean replaceNull) {
      checkConnected();
      checkQueried();

      int countVal = (count == null) ? Integer.MAX_VALUE : count;

      try {
        int rowNo;
        for (rowNo = 0; rowNo < countVal; ++rowNo) {
          if (!queryRowSet.hasNext()) {
            queryRowSet.close();
            queryRowSet = null;
            break;
          }
          Object obj = queryRowSet.next();

          if (obj instanceof Row) {
            Row row = (Row) obj;
            ContainerInfo schema = row.getSchema();
            int colCount = schema.getColumnCount();
            String[] line = new String[colCount];

            if (rowNo == 0) {
              for (int colNo = 0; colNo < colCount; ++colNo) {
                line[colNo] = schema.getColumnInfo(colNo).getName();
              }
              printLine(0, line);
            }

            for (int colNo = 0; colNo < colCount; ++colNo) {
              ColumnInfo ci = schema.getColumnInfo(colNo);
              Object data   = row.getValue(colNo);
              if(data instanceof Timestamp) {
                line[colNo] = stringify((Timestamp)   data, ci.getTimePrecision());
              } else {
                line[colNo] = stringify(data, replaceNull);
              }
            }
            printLine(1 + rowNo, line);

          } else if (obj instanceof AggregationResult) {
            AggregationResult agg = (AggregationResult) obj;

            if (rowNo == 0) {
              printLine(0, "Result");
            }

            Double number = agg.getDouble();
            if (number != null) {
              printLine(1 + rowNo, stringify(number, replaceNull));
            } else {
              ContainerInfo contInfo =  queryRowSet.getSchema();
              // AggregationResult has only 1 column
              TimeUnit precision = contInfo.getColumnInfo(0).getTimePrecision();
              String line;
              if (precision == TimeUnit.MICROSECOND || precision == TimeUnit.NANOSECOND) {
                line = stringify(agg.getPreciseTimestamp(), precision);
              } else {
                line = stringify(agg.getTimestamp(), replaceNull);
              }
              printLine(1 + rowNo, line);
            }

          } else if (obj instanceof QueryAnalysisEntry) {
            QueryAnalysisEntry entry = (QueryAnalysisEntry) obj;

            if (rowNo == 0) {
              printLine(0, "Id", "Depth", "Type", "ValueType", "Value", "Statement");
            }

            printLine(
                1 + rowNo,
                stringify(entry.getId(), replaceNull),
                stringify(entry.getDepth(), replaceNull),
                entry.getType(),
                entry.getValueType(),
                entry.getValue(),
                entry.getStatement());
          }
        }

        if (getResultFormat() == ResultFormat.TABLE) {
          displayAsTable();
        }
        
        return rowNo;

      } catch (GSException | IllegalArgumentException e) {
        throw new ShellException(getMessage("error.getrow") + " : msg=[" + e.getMessage() + "]", e);
      }
    }

    /**
     * Acquires the execution result of the SQL SELECT statement.
     *
     * @param count maximum number of records to retrieve
     * @param replaceNull whether to replace NULL values with strings
     * @return number acquired rows
     */
    public int getRowSQL(Integer count, boolean replaceNull) {
      checkConnectedSQL();
      checkQueriedSQL();
      

      String[] sqlTokens        = simplifySQL(m_jdbcSQL);
      boolean  isExplain        = "EXPLAIN".equals(sqlTokens[0]);
      ResultFormat originFormat = m_resultFormat;
      /* Change to result format to CSV if SQL is explain */
      if(isExplain) {
        m_resultFormat = ResultFormat.CSV;
      }

      int countVal = (count == null) ? Integer.MAX_VALUE : count;

      try {
        int rowNo;
        ResultSetMetaData rsMeta = m_jdbcRS.getMetaData();
        int colCount = rsMeta.getColumnCount();
        String[] line = new String[colCount];

        for (int colNo = 0; colNo < colCount; ++colNo) {
          line[colNo] = rsMeta.getColumnName(colNo + 1);
        }
        printLine(0, line);

        for (rowNo = 0; rowNo < countVal; ++rowNo) {
          if (!m_jdbcRS.next()) {
            m_jdbcRS.close();
            m_jdbcRS = null;
            break;
          }

          for (int colNo = 0; colNo < colCount; ++colNo) {
            Object obj = m_jdbcRS.getObject(colNo + 1);

            if (obj == null || m_jdbcRS.wasNull()) {
              line[colNo] = replaceNull ? getNullStdOut() : null;
            } else if (obj instanceof Blob) {
              line[colNo] = BLOB_SHOW_STRING;
            } else {
              line[colNo] = m_jdbcRS.getString(colNo + 1);
            }
          }
          printLine(1 + rowNo, line);
        }
        
        if (getResultFormat() == ResultFormat.TABLE) {
          displayAsTable();
        }

        /* Restore orginal display mode after display SQL explain result */
        if(isExplain) {
          m_resultFormat = originFormat;
        }

        return rowNo;

      } catch (Exception e) {
        throw new ShellException(getMessage("error.getrow") + " : msg=[" + e.getMessage() + "]", e);
      }
    }
    
    protected void displayAsTable() {};

  }

  /**
   * The main method for sub-command {@code get}.<br>
   * Display the results obtained in a standard output.
   *
   * @param count number of search results to be acquired
   * @throws ShellException if the query has not been executed yet
   */
  @GSCommand(name = "get")
  public void getRow(@GSNullable Integer count) {
    final DateTimeFormatter dateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    RowGetter rowgetter =
        new RowGetter() {
          private ResultTable resultTable = null;
          @Override
          protected void printLine(int rowNumber, String... line) {
            if (getResultFormat() == ResultFormat.CSV) {
              StringBuilder builder = new StringBuilder();
              for (int i = 0; i < line.length; ++i) {
                if (i != 0) {
                  builder.append(',');
                }
                builder.append(line[i]);
              }
              println(builder.toString());
            } else {
              if (this.resultTable == null) {
                ArrayList<String> columnNames = new ArrayList<String>(Arrays.asList(line));
                this.resultTable = new ResultTable(columnNames, getResultMaxColumnWidth());
              }
              else {
                this.resultTable.addRow(new ArrayList<String>(Arrays.asList(line)));
              }
            }
          }

          @Override
          protected String formatDate(Date date) {
            ZonedDateTime zdt = null;
            if (m_connectTimeZoneVal == null) {
              zdt = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"));
            } else if ("auto".equals(m_connectTimeZoneVal)) {
              zdt = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
            } else {
              zdt = ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.of(m_connectTimeZoneVal));
            }
            return zdt.format(dateTimeFormatter);
          }

          @Override
          protected void displayAsTable() {
            if (this.resultTable != null) {
              this.resultTable.display();
            }
          }

        };

    int gotCount = 0;
    if (queryRowSet != null) {
      gotCount = rowgetter.getRow(count, true);
    } else if (m_jdbcRS != null) {
      gotCount = rowgetter.getRowSQL(count, true);
    } else {
      throw new ShellException(getMessage("error.noResultSet"));
    }

    println(getMessage("message.getCount", gotCount));
  }

  /**
   * The main method for sub-command {@code getcsv}.<br>
   * Save the results obtained in a file in the CSV format.
   *
   * @param filename CSV file name
   * @param count number of search results to be acquired
   * @throws ShellException if it meets 1 of the below conditions:
   *     <ul>
   *       <li>The query has not been executed yet
   *       <li>error when saving to CSV file
   *       <li>The connection is closed (NewSQL or NoSQL)
   *     </ul>
   */
  @GSCommand(name = "getcsv")
  public void getRowToCsv(String filename, @GSNullable Integer count) {
    final DateTimeFormatter dateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    CSVWriter writer = null;
    try {
      if (queryRowSet != null) {
        checkConnected();
      } else if (m_jdbcRS != null) {
        checkConnectedSQL();
      } else {
        throw new ShellException(getMessage("error.noResultSet"));
      }

      writer =
          new CSVWriter(
              new BufferedWriter(
                  new OutputStreamWriter(new FileOutputStream(filename), CSV_ENCODING)));

      final CSVWriter csvWriter = writer;
      RowGetter rowgetter =
          new RowGetter() {
            @Override
            protected void printLine(int rowNo, String... line) {
              if (rowNo == 0) {
                line[0] = "#" + line[0];
                csvWriter.writeNext(line);
                csvWriter.writeNext(new String[] {"$", queryContainerName});
              } else {
                csvWriter.writeNext(line);
                if (rowNo % 1000 == 0) {
                  println(getMessage("message.gotCountProgress", rowNo));
                }
              }
            }

            @Override
            protected String formatDate(Date date) {
              ZonedDateTime zdt = null;
              if (m_connectTimeZoneVal == null) {
                zdt = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"));
              } else if ("auto".equals(m_connectTimeZoneVal)) {
                zdt = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
              } else {
                zdt =
                    ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.of(m_connectTimeZoneVal));
              }
              return zdt.format(dateTimeFormatter);
            }
          };

      int gotCount = 0;
      if (queryRowSet != null) {
        gotCount = rowgetter.getRow(count, false);
      } else if (m_jdbcRS != null) {
        gotCount = rowgetter.getRowSQL(count, false);
      }

      println(getMessage("message.getCount", gotCount));

    } catch (FileNotFoundException e) {
      throw new ShellException(getMessage("error.cannotSave", filename), e);

    } finally {
      closeQuitely(writer);
    }
  }

  /**
   * The main method for sub-command {@code getnoprint}.<br>
   * Results obtained will not be output.
   *
   * @param count number of search results to be acquired
   * @throws ShellException if NoSQL/NewSQL connection is closed or the query has not been executed
   *     yet
   */
  @GSCommand(name = "getnoprint")
  public void getRowNoPrint(@GSNullable Integer count) {

    int countVal = (count == null) ? Integer.MAX_VALUE : count;
    try {
      int rowNo;
      if (queryRowSet != null) {
        checkConnected();
        for (rowNo = 0; rowNo < countVal; ++rowNo) {
          if (!queryRowSet.hasNext()) {
            queryRowSet.close();
            queryRowSet = null;
            break;
          }
          queryRowSet.next();
        }
        println(getMessage("message.getCount", rowNo));
      } else if (m_jdbcRS != null) {
        checkConnectedSQL();
        for (rowNo = 0; rowNo < countVal; ++rowNo) {
          if (!m_jdbcRS.next()) {
            m_jdbcRS.close();
            m_jdbcRS = null;
            break;
          }
        }
        println(getMessage("message.getCount", rowNo));
      } else {
        throw new ShellException(getMessage("error.noResultSet"));
      }

    } catch (ShellException e) {
      throw e;
    } catch (Exception e) {
      throw new ShellException(
          getMessage("error.getnoprint") + " : msg=[" + e.getMessage() + "]", e);
    }
  }

  /**
   * The main method for sub-command {@code tqlexplain}.<br>
   * Execute the specified TQL command and display the execution plan and actual measurement values
   * such as the number of cases processed etc.<br>
   * Search is not executed.
   *
   * @param containerName container name
   * @param query query string
   * @throws ShellException if it meets 1 of the following conditions:
   *     <ul>
   *       <li>The connection is closed
   *       <li>Partition container
   *       <li>Extended container information is {@code null}
   *       <li>Error when executing TQL explain
   *     </ul>
   *
   * @throws IllegalArgumentException if container not existed
   */
  @GSCommand(multiline = true)
  public void tqlexplain(String containerName, String query) {
    checkConnected();

    Container<Object, Row> container = null;
    Query<QueryAnalysisEntry> queryObj = null;
    RowSet<QueryAnalysisEntry> rowSet = null;

    try {
      // パーティションコンテナ(LARGE)には実行不可
      ExtendedContainerInfo info =
          ExperimentalTool.getExtendedContainerInfo(gridStore, containerName);

      if (info == null || info.getAttribute() == ContainerAttribute.VIEW) {
        throw new ShellException(getMessage("error.containerNotFound", containerName));
      }

      container = gridStore.getContainer(containerName);
      checkContainerExists(containerName, container);
      queryObj = container.query("EXPLAIN " + query, QueryAnalysisEntry.class);
      rowSet = queryObj.fetch();
      while (rowSet.hasNext()) {
        QueryAnalysisEntry entry = rowSet.next();
        print(entry.getId() + "\t");
        print(entry.getDepth() + "\t");
        print(entry.getType() + "\t");
        print(entry.getValueType() + "\t");
        print(entry.getValue() + "\t");
        print(entry.getStatement());
        println("");
      }

    } catch (GSException e) {
      throw new ShellException(
          getMessage("error.tqlexplain") + " : msg=[" + e.getMessage() + "]", e);
    } finally {
      closeQuitely(rowSet);
      closeQuitely(queryObj);
      closeQuitely(container);
    }
  }

  /**
   * The main method for sub-command {@code tqlanalyze}.<br>
   * Execute the specified TQL command and display actual measurement values such as the number of
   * processing rows etc.
   *
   * @param containerName container name
   * @param query query string
   * @throws ShellException if the connection is closed or error when executing TQL analyze
   * @throws IllegalArgumentException if container not existed
   */
  @GSCommand(multiline = true)
  public void tqlanalyze(String containerName, String query) {
    checkConnected();

    Container<Object, Row> container = null;
    Query<QueryAnalysisEntry> queryObj = null;
    RowSet<QueryAnalysisEntry> rowSet = null;

    try {
      container = gridStore.getContainer(containerName);
      checkContainerExists(containerName, container);
      queryObj = container.query("EXPLAIN ANALYZE " + query, QueryAnalysisEntry.class);
      rowSet = queryObj.fetch();
      while (rowSet.hasNext()) {
        QueryAnalysisEntry entry = rowSet.next();
        print(entry.getId() + "\t");
        print(entry.getDepth() + "\t");
        print(entry.getType() + "\t");
        print(entry.getValueType() + "\t");
        print(entry.getValue() + "\t");
        print(entry.getStatement());
        println("");
      }

    } catch (GSException e) {
      throw new ShellException(
          getMessage("error.tqlanalyze") + " : msg=[" + e.getMessage() + "]", e);
    } finally {
      closeQuitely(rowSet);
      closeQuitely(queryObj);
      closeQuitely(container);
    }
  }

  /**
   * The main method for sub-command {@code tqlclose}.<br>
   * Close the TQL and discard the search results saved.
   *
   * @throws ShellException if the connection is closed
   */
  @GSCommand
  public void tqlclose() {
    checkConnected();
    tqlClosePrivate();
  }

  /**
   * The main method for sub-command {@code queryclose}.<br>
   * Close the query and discard the search results saved.
   *
   * @throws ShellException if the connection is closed
   */
  @GSCommand
  public void queryclose() {
    checkConnected();
    queryObjClose();
  }

  /**
   * The main method for sub-command {@code getplantxt}.<br>
   * Display an SQL analysis result (global plan) in text format.
   *
   * @param fileName text file name
   * @throws ShellException if it meets 1 of the following conditions:
   *     <ul>
   *       <li>The connection is closed
   *       <li>The query has not been executed
   *       <li>Error while providing explain information
   *       <li>Error while outputting file
   *       <li>The executed query is not EXPLAIN/ANALYZE
   *     </ul>
   */
  @GSCommand
  public void getplantxt(@GSNullable String fileName) {

    if (m_explainResult != null) {
      // 既にある m_explainResult から出力
      outputPlanTxt(m_explainResult, fileName);
    } else if (m_jdbcRS != null) {
      // m_explainResult を作成
      createExplainResult();
      // 作成した m_explainResult から出力
      outputPlanTxt(m_explainResult, fileName);
    } else {
      throw new ShellException(getMessage("error.noResultSet"));
    }
  }

  private void createExplainResult() {
    List<JsonNode> explainJsonList = getExplainRowSQLJson();
    if (explainJsonList.isEmpty()) {
      // 0件の場合
      throw new ShellException(getMessage("error.getplantxtNotExplain"));
    }
    List<ExplainInfo> explainInfoList = new ArrayList<ExplainInfo>();
    for (JsonNode jsonNode : explainJsonList) {
      ExplainInfo explainInfo = new ExplainInfo(jsonNode);
      explainInfoList.add(explainInfo);
    }

    ExplainInfo resultExplainInfo = ExplainInfoUtil.findResltExplainInfo(explainInfoList);
    if (resultExplainInfo == null) {
      throw new ShellException(getMessage("error.getplantxt"));
    }
    ExplainInfoUtil.createInputExplainInfoList(explainInfoList);
    ExplainInfoUtil.createInputdepth(explainInfoList, resultExplainInfo, 0);
    ExplainInfoUtil.sortInputExplainInfoList(explainInfoList);
    ExplainInfoUtil.createDisplayOrderDisplayDepth(explainInfoList, resultExplainInfo);
    ExplainInfoUtil.sortDisplayOrder(explainInfoList);
    ExplainInfoUtil.prepareDisplay(explainInfoList);

    m_explainResult = new ExplainResult();
    m_explainResult.setExplainJsonList(explainJsonList);
    m_explainResult.setExplainInfoList(explainInfoList);
  }

  private List<JsonNode> getExplainRowSQLJson() {
    checkConnectedSQL();

    ArrayList<JsonNode> explainJsonList = new ArrayList<JsonNode>();

    if (m_jdbcRS == null) {
      throw new ShellException(getMessage("error.getplantxtNotExplain"));
    }

    try {
      int rowNo;
      ResultSetMetaData rsMeta = m_jdbcRS.getMetaData();
      int colCount = rsMeta.getColumnCount();
      String[] line = new String[colCount];

      int countVal = Integer.MAX_VALUE;
      for (rowNo = 0; rowNo < countVal; ++rowNo) {
        if (!m_jdbcRS.next()) {
          m_jdbcRS.close();
          m_jdbcRS = null;
          break;
        }

        for (int colNo = 0; colNo < colCount; ++colNo) {
          Object obj = m_jdbcRS.getObject(colNo + 1);

          if (obj == null || m_jdbcRS.wasNull()) {
            line[colNo] = null;
          } else if (obj instanceof Blob) {
            line[colNo] = BLOB_SHOW_STRING;
          } else {
            line[colNo] = m_jdbcRS.getString(colNo + 1);
          }
        }

        String explainJson = line[0];
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = null;
        try {
          root = mapper.readTree(explainJson);
          explainJsonList.add(root);
        } catch (Exception e) {
          m_jdbcRS.close();
          m_jdbcRS = null;
          throw new ShellException(getMessage("error.getplantxtNotExplain"));
        }

        if (root == null || root.get("id") == null) {
          m_jdbcRS.close();
          m_jdbcRS = null;
          throw new ShellException(getMessage("error.getplantxtNotExplain"));
        }
      }
    } catch (ShellException e) {
      throw e;
    } catch (Exception e) {
      throw new ShellException(getMessage("error.getrow") + " : msg=[" + e.getMessage() + "]", e);
    }

    return explainJsonList;
  }

  private void outputPlanTxt(ExplainResult explainResult, String fileName) {
    List<ExplainInfo> explainInfoList = explainResult.getExplainInfoList();

    ExplainInfo resultExplainInfo = ExplainInfoUtil.findResltExplainInfo(explainInfoList);
    if (resultExplainInfo == null) {
      throw new ShellException(getMessage("error.getplantxt"));
    }
    boolean isAnalyze = ExplainInfoUtil.isExplainAnalyze(resultExplainInfo);

    List<String> outputStrList = ExplainInfoUtil.getDisplayValue(explainInfoList, isAnalyze);

    if (fileName == null) {
      for (String str : outputStrList) {
        println(str);
      }
    } else {
      try {
        File file = new File(fileName);
        BufferedWriter bw = new BufferedWriter(new FileWriter(file));
        for (String str : outputStrList) {
          bw.write(str);
          bw.newLine();
        }
        bw.close();
      } catch (IOException e) {
        throw new ShellException(
            getMessage("error.getplantxtFileErr") + " : msg=[" + e.getMessage() + "]", e);
      }
    }
  }

  private class ExplainResult {
    List<JsonNode> explainJsonList;
    List<ExplainInfo> explainInfoList;

    /**
     * Get list of JSON node.
     *
     * @return list of JSON node
     */
    public List<JsonNode> getExplainJsonList() {
      return explainJsonList;
    }

    /**
     * Set list of JSON node for {@code ExplainResult} object.
     *
     * @param explainJsonList list of JSON node
     */
    public void setExplainJsonList(List<JsonNode> explainJsonList) {
      this.explainJsonList = explainJsonList;
    }

    /**
     * Get list of {@code ExplainInfo}.
     *
     * @return list of {@code ExplainInfo}
     */
    public List<ExplainInfo> getExplainInfoList() {
      return explainInfoList;
    }

    /**
     * Set list of {@code ExplainInfo} for {@code ExplainResult} object.
     *
     * @param explainInfoList list of {@code ExplainInfo}
     */
    public void setExplainInfoList(List<ExplainInfo> explainInfoList) {
      this.explainInfoList = explainInfoList;
    }
  }

  private static class ExplainInfoUtil {
    private static final String HEADER_ID = "Id";
    private static final String HEADER_TYPE = "Type";
    private static final String HEADER_INPUT = "Input";
    private static final String HEADER_ROWS = "Rows";
    private static final String HEADER_LEADTIME = "Lead time";
    private static final String HEADER_ACTUALTIME = "Actual time";
    private static final String HEADER_NODE = "Node";
    private static final String HEADER_ANDMORE = "And more..";

    private static final int INPUT_DISP_MAX_NUM = 5;
    private static final int ROWS_DISP_MAX_NUM = 5;
    private static final String STR_OMISSION = "...";

    /**
     * Set the ExplainInfo pointed to by input.
     *
     * @param explainInfoList list of {@code ExplainInfo}
     */
    public static void createInputExplainInfoList(List<ExplainInfo> explainInfoList) {
      for (ExplainInfo targetExplainInfo : explainInfoList) {
        List<ExplainInfo> inputExplainInfoList = null;
        List<String> inputList = targetExplainInfo.getInputList();
        if (inputList != null) {
          inputExplainInfoList = new ArrayList<ExplainInfo>();
          for (String intputId : inputList) {
            ExplainInfo inputExplainInfo = findExplainInfoById(explainInfoList, intputId);
            if (inputExplainInfo != null) {
              inputExplainInfoList.add(inputExplainInfo);
            }
          }
        }
        targetExplainInfo.setInputExplainInfoList(inputExplainInfoList);
      }
    }

    /**
     * Specify and return the {@code ExplainInfo} of executed result.
     *
     * @param explainInfoList list of {@code ExplainInfo}
     * @return {@code ExplainInfo} of executed result
     */
    public static ExplainInfo findResltExplainInfo(List<ExplainInfo> explainInfoList) {
      ExplainInfo resultExplainInfo = null;
      for (ExplainInfo explainInfo : explainInfoList) {
        if (explainInfo.isResultNode()) {
          resultExplainInfo = explainInfo;
        }
      }
      return resultExplainInfo;
    }

    /**
     * Set the input path depth for {@code ExplainInfo} of {@code startExplainInfo}.<br>
     * If {@code startExplainInfo} has a list of {@code ExplainInfo} pointed to by input, also set
     * the input path depth for {@code ExplainInfo} in the list
     *
     * @param explainInfoList list of {@code ExplainInfo}
     * @param startExplainInfo start {@code ExplainInfo}
     * @param inputPathDepth input path depth
     */
    public static void createInputdepth(
        List<ExplainInfo> explainInfoList, ExplainInfo startExplainInfo, int inputPathDepth) {
      startExplainInfo.setInputPathDepth(inputPathDepth);

      List<ExplainInfo> inputExplainInfoList = startExplainInfo.getInputExplainInfoList();
      if (inputExplainInfoList != null && inputExplainInfoList.size() > 0) {
        for (ExplainInfo inputExplainInfo : inputExplainInfoList) {
          ExplainInfoUtil.createInputdepth(explainInfoList, inputExplainInfo, inputPathDepth + 1);
        }
      }
    }

    /**
     * Sort {@code ExplainInfo} in {@code explainInfoList}.<br>
     * Sort condition: {@code MaxInputPathDepth} is ascending and {@code id} is descending
     *
     * @param explainInfoList list of {@code ExplainInfo}
     */
    public static void sortInputExplainInfoList(List<ExplainInfo> explainInfoList) {
      Comparator<ExplainInfo> comparator =
          new Comparator<ExplainInfo>() {
            @Override
            public int compare(ExplainInfo o1, ExplainInfo o2) {
              int o1depth = o1.getMaxInputPathDepth();
              int o2depth = o2.getMaxInputPathDepth();
              if (o1depth < o2depth) {
                return -1;
              } else if (o1depth > o2depth) {
                return 1;
              } else {
                Integer o1Id = str2int(o1.getId());
                Integer o2Id = str2int(o2.getId());
                if (o2Id < o1Id) {
                  return -1;
                } else if (o2Id > o1Id) {
                  return 1;
                } else {
                  return 0;
                }
              }
            }
          };

      for (ExplainInfo explainInfo : explainInfoList) {
        List<ExplainInfo> inputExplainInfoList = explainInfo.getInputExplainInfoList();
        if (inputExplainInfoList != null && inputExplainInfoList.size() > 0) {
          ExplainInfoUtil.explainInfosort(inputExplainInfoList, comparator);
        }
      }
    }

    /**
     * explainInfoList 内のExplainInfoのソートを行う.
     *
     * @param explainInfoList explainInfoList
     * @param comparator comparator
     */
    private static void explainInfosort(
        List<ExplainInfo> explainInfoList, Comparator<ExplainInfo> comparator) {
      Collections.sort(explainInfoList, comparator);
    }

    /**
     * Set the display order and indentation for screen output.
     *
     * @param explainInfoList list of {@code ExplainInfo}
     * @param resultExplainInfo {@code ExplainInfo} result
     */
    public static void createDisplayOrderDisplayDepth(
        List<ExplainInfo> explainInfoList, ExplainInfo resultExplainInfo) {
      int explainListSize = explainInfoList.size();
      int maxInputPathDepth = resultExplainInfo.getMaxInputPathDepth();

      createDisplayOrderDisplayDepth(resultExplainInfo, explainListSize, maxInputPathDepth);
    }

    /**
     * Sort display order for {@code explainInfoList}.<br>
     * Sort condition: ascending of display order
     *
     * @param explainInfoList list of {@code ExplainInfo}
     */
    public static void sortDisplayOrder(List<ExplainInfo> explainInfoList) {
      Comparator<ExplainInfo> comparator =
          new Comparator<ExplainInfo>() {
            @Override
            public int compare(ExplainInfo o1, ExplainInfo o2) {
              return Integer.valueOf(o1.getDisplayOrder())
                  .compareTo(Integer.valueOf(o2.getDisplayOrder()));
            }
          };
      ExplainInfoUtil.explainInfosort(explainInfoList, comparator);
    }

    /**
     * Set the screen display value.
     *
     * @param explainInfoList list of {@code ExplainInfo}
     */
    public static void prepareDisplay(List<ExplainInfo> explainInfoList) {
      for (ExplainInfo explainInfo : explainInfoList) {
        String id = explainInfo.getId();
        if (id != null) {
          explainInfo.setIdDisplayValue(id);
        } else {
          explainInfo.setIdDisplayValue("-");
        }

        String typeDisplay = explainInfo.getType();
        int typeDepthDisplayLength = typeDisplay.length() + explainInfo.getDisplayDepth() * 2;
        typeDisplay = String.format("%" + typeDepthDisplayLength + "s", typeDisplay);
        explainInfo.setTypeDisplayValue(typeDisplay);

        List<String> inputList = explainInfo.getInputList();
        if (inputList != null && inputList.size() > 0) {
          if (inputList.size() > INPUT_DISP_MAX_NUM) {
            String dispValue = joinStr(inputList.subList(0, INPUT_DISP_MAX_NUM), ",");
            dispValue += STR_OMISSION;
            explainInfo.setInputListDisplayValue(dispValue);
          } else {
            explainInfo.setInputListDisplayValue(joinStr(inputList, ","));
          }
        } else {
          explainInfo.setInputListDisplayValue("-");
        }

        List<Long> rowsList = explainInfo.getRows();
        if (rowsList != null && rowsList.size() > 0) {
          StringBuilder sb = new StringBuilder();
          boolean first = true;
          if (rowsList.size() > ROWS_DISP_MAX_NUM) {
            List<Long> subRowsList = rowsList.subList(0, ROWS_DISP_MAX_NUM);
            for (Long rows : subRowsList) {
              if (!first) {
                sb.append(",");
              }
              sb.append(rows.toString());
              first = false;
            }
            sb.append(STR_OMISSION);
          } else {
            for (Long rows : rowsList) {
              if (!first) {
                sb.append(",");
              }
              sb.append(rows.toString());
              first = false;
            }
          }
          explainInfo.setRowsDisplayValue(sb.toString());
        } else {
          explainInfo.setRowsDisplayValue("-");
        }

        Long leadTime = explainInfo.getLeadTime();
        if (leadTime != null) {
          explainInfo.setLeadTimeDisplayValue(leadTime.toString());
        } else {
          explainInfo.setLeadTimeDisplayValue("");
        }

        Long actualTime = explainInfo.getActualTime();
        if (actualTime != null) {
          explainInfo.setActualTimeTimeDisplayValue(actualTime.toString());
        } else {
          explainInfo.setActualTimeTimeDisplayValue("");
        }

        String node = explainInfo.getNode();
        if (node != null) {
          explainInfo.setNodeDisplayValue(node);
        } else {
          explainInfo.setNodeDisplayValue("-");
        }

        explainInfo.setOtherDisplayValue(getOtherDisplayValuStr(explainInfo));
      }
    }

    /**
     * And more.. の画面表示値を返す
     *
     * @param explainInfo explainInfo
     * @return
     */
    private static String getOtherDisplayValuStr(ExplainInfo explainInfo) {
      prepareTableNameDisplay(explainInfo);

      prepareUseIndexInfo(explainInfo);

      prepareProfileOpTypeInfo(explainInfo);

      prepareLimitInfo(explainInfo);

      prepareUnionTypeInfo(explainInfo);

      ArrayList<String> otherValueList = new ArrayList<String>();
      otherValueList.add(explainInfo.getTableNameDisplayValue());
      if (explainInfo.isUseIndex()) {
        otherValueList.add("INDEX SCAN");
      }
      otherValueList.add(explainInfo.getUnionTypeValue());
      otherValueList.add(explainInfo.getProfileOpTypeValue());
      otherValueList.add(explainInfo.getLimiteValue());

      StringBuilder sb = new StringBuilder();
      boolean first = true;
      for (String otherValue : otherValueList) {
        if (otherValue != null && !otherValue.isEmpty()) {
          if (!first) {
            sb.append(" ");
          }
          sb.append(otherValue);
          first = false;
        }
      }

      return sb.toString();
    }

    /**
     * テーブル名の表示値を作成し引数のExplainInfoにセットする.
     *
     * @param explainInfo explainInfo
     */
    private static void prepareTableNameDisplay(ExplainInfo explainInfo) {
      String type = explainInfo.getType();
      JsonNode json = explainInfo.getJsonNode();

      ArrayList<String> tableNameList = new ArrayList<String>();
      if ("SCAN".equals(type)) {
        if (json.has("outputList") && json.get("outputList").isArray()) {
          ArrayList<String> tmpTableNameList = new ArrayList<String>();
          int size = json.get("outputList").size();
          for (int i = 0; i < size; i++) {
            try {
              JsonNode outputJson = json.get("outputList").get(i);
              String qNameTable = getQnameTable(outputJson);
              if (qNameTable != null) {
                tmpTableNameList.add(qNameTable);
              } else {
                int nSize = outputJson.get("next").size();
                for (int j = 0; j < nSize; j++) {
                  JsonNode nextOutputJson = outputJson.get("next").get(j);
                  qNameTable = getQnameTable(nextOutputJson);
                  if (qNameTable != null) {
                    tmpTableNameList.add(qNameTable);
                  }
                }
              }

            } catch (Exception e) {
              // Do nothing
            }
          }

          for (String tmpTableName : tmpTableNameList) {
            if (!tableNameList.contains(tmpTableName)) {
              tableNameList.add(tmpTableName);
            }
          }
        }
      } else if ("INSERT".equals(type) || "UPDATE".equals(type) || "DELETE".equals(type)) {
        try {
          String tableName = json.get("partitioningInfo").get("tableName").textValue();
          tableNameList.add(tableName);
        } catch (Exception e) {
          // Do nothing
        }
      } else if ("DDL".equals(type)) {
        try {
          String qNameTable = json.get("qName").get("table").textValue();
          tableNameList.add(qNameTable);
        } catch (Exception e) {
          // Do nothing
        }
      }
      prepareTableNameDisplayValuStr(explainInfo, tableNameList);
    }

    /**
     * 引数のJSONより、qName/tableの値を返す 見つからない場合はnullが返る.
     *
     * @param profileOpJson profileOpJson
     * @return
     */
    private static String getQnameTable(JsonNode outputJson) {
      String ret = null;
      try {
        ret = outputJson.get("qName").get("table").textValue();
      } catch (Exception e) {
        // Do nothing
      }
      return ret;
    }

    private static void prepareTableNameDisplayValuStr(
        ExplainInfo explainInfo, ArrayList<String> tableNameList) {
      String tableNameDisplayVal = "";

      if (tableNameList.size() > 0) {
        StringBuilder sb = new StringBuilder();
        sb.append("table: {");
        sb.append(joinStr(tableNameList, ", "));
        sb.append("}");
        tableNameDisplayVal = sb.toString();
      }
      explainInfo.setTableNameDisplayValue(tableNameDisplayVal);
    }

    /**
     * INDEXを使った処理か判断し結果を引数のExplainInfoにセットする.
     *
     * @param explainInfo explainInfo
     */
    private static void prepareUseIndexInfo(ExplainInfo explainInfo) {
      JsonNode json = explainInfo.getJsonNode();

      boolean useIndex = false;
      String columnName = null;

      // profile/op/index があれば INDEXを使用したと判断
      try {
        if (json.get("profile").get("op").has("index")) {
          useIndex = true;
          JsonNode indexJson = json.get("profile").get("op").get("index");
          columnName = getIndexColumnName(indexJson);
        }
      } catch (Exception e) {
        // Do nothing
      }

      // profile/op/index がない場合
      // profile/op[n]/op[n]/index があれば INDEXを使用したと判断
      try {
        int sizeOp = json.get("profile").get("op").size();
        for (int i = 0; i < sizeOp; i++) {
          JsonNode jsonOp = json.get("profile").get("op").get(i);
          if (!useIndex && jsonOp.has("op")) {
            int size = jsonOp.get("op").size();
            ArrayList<String> columnNames = new ArrayList<String>();
            for (int j = 0; j < size; j++) {
              if (jsonOp.get("op").get(j).has("index")) {
                useIndex = true;
                JsonNode indexJson = jsonOp.get("op").get(j).get("index");
                String tmpColumnName = getIndexColumnName(indexJson);
                if (tmpColumnName != null) {
                  columnNames.add(tmpColumnName);
                }
              }
            }
            columnName = joinStr(columnNames, ", ");
          }
        }

      } catch (Exception e) {
        // Do nothing
      }

      explainInfo.setUseIndex(useIndex);
      if (columnName == null) {
        columnName = "";
      }
      explainInfo.setUseIndexColumnName(columnName);
    }

    private static String getIndexColumnName(JsonNode indexJson) {
      String ret = null;
      ArrayList<String> nameList = new ArrayList<String>();
      try {
        int size = indexJson.size();
        for (int i = 0; i < size; i++) {
          String columnName = indexJson.get(i).get("column").textValue();
          nameList.add(columnName);
        }

        ret = joinStr(nameList, ", ");

      } catch (Exception e) {
        // Do nothing
      }

      return ret;
    }

    /**
     * profileより処理内容の情報を取得し引数のExplainInfoにセットする.
     *
     * @param explainInfo explainInfo
     */
    private static void prepareProfileOpTypeInfo(ExplainInfo explainInfo) {
      JsonNode json = explainInfo.getJsonNode();

      boolean typeInfoFound = false;
      String typeInfoStr = null;

      // profile/op[n]/op[n]/type があれば、その値を取得

      try {
        int sizeOp = json.get("profile").get("op").size();
        for (int i = 0; i < sizeOp; i++) {
          JsonNode jsonOp = json.get("profile").get("op").get(i);
          if (jsonOp.get("op").isArray()) {
            int size = jsonOp.get("op").size();
            ArrayList<String> typeInfoStrAry = new ArrayList<String>();
            for (int j = 0; j < size; j++) {
              JsonNode profileOpSubJson = jsonOp.get("op").get(j);
              String tmpType = getProfileOpType(profileOpSubJson);
              if (tmpType != null) {
                typeInfoStrAry.add(tmpType);
              }
            }
            if (!typeInfoStrAry.isEmpty()) {
              typeInfoStr = joinStr(typeInfoStrAry, " ");
              typeInfoFound = true;
            }
          }
        }
      } catch (Exception e) {
        // Do nothing
      }

      if (!typeInfoFound) {
        try {
          JsonNode profileOpJson = json.get("profile").get("op");
          String tmpType = getProfileOpType(profileOpJson);
          if (tmpType != null) {
            typeInfoStr = tmpType;
            typeInfoFound = true;
          }
        } catch (Exception e) {
          // Do nothing
        }
      }

      if (typeInfoStr == null) {
        typeInfoStr = "";
      }
      explainInfo.setProfileOpTypeValue(typeInfoStr);
    }

    /**
     * 引数のJSONより、typeの値を返す 見つからない場合はnullが返る.
     *
     * @param profileOpJson profileOpJson
     * @return
     */
    private static String getProfileOpType(JsonNode profileOpJson) {
      String ret = null;
      try {
        ret = profileOpJson.get("type").textValue();
      } catch (Exception e) {
        // Do nothing
      }
      return ret;
    }

    /**
     * limitの情報を取得し引数のExplainInfoにセットする.
     *
     * @param explainInfo explainInfo
     */
    private static void prepareLimitInfo(ExplainInfo explainInfo) {
      JsonNode json = explainInfo.getJsonNode();

      String limitInfo = "";
      try {
        long limit = json.get("limit").longValue();
        limitInfo = "LIMIT: " + Long.toString(limit);
      } catch (Exception e) {
        // Do nothing
      }

      explainInfo.setLimiteValue(limitInfo);
    }

    /**
     * unionTypeの情報を取得し引数のExplainInfoにセットする.
     *
     * @param explainInfo explainInfo
     */
    private static void prepareUnionTypeInfo(ExplainInfo explainInfo) {
      JsonNode json = explainInfo.getJsonNode();

      String unionType = "";
      try {
        unionType = json.get("unionType").textValue();
      } catch (Exception e) {
        // Do nothing
      }

      explainInfo.setUnionTypeValue(unionType);
    }

    /**
     * Get the display value.
     *
     * @param explainInfoList list of {@code ExplainInfo}
     * @param isExplainAnalyze whether it is EXPLAIN/ANALYZE query
     * @return list of display value
     */
    public static List<String> getDisplayValue(
        List<ExplainInfo> explainInfoList, boolean isExplainAnalyze) {
      final List<String> ret = new ArrayList<String>();

      int idMaxLength = 0;
      int typeMaxLength = 0;
      int inputMaxLength = 0;
      int rowsMaxLength = 0;
      int leadTimeMaxLength = 0;
      int actualTimeMaxLength = 0;
      int nodeMaxLength = 0;
      int andmoreMaxLength = 0;
      for (ExplainInfo explainInfo : explainInfoList) {
        idMaxLength = Math.max(idMaxLength, explainInfo.getIdDisplayValue().length());
        typeMaxLength = Math.max(typeMaxLength, explainInfo.getTypeDisplayValue().length());
        inputMaxLength = Math.max(inputMaxLength, explainInfo.getInputListDisplayValue().length());
        rowsMaxLength = Math.max(rowsMaxLength, explainInfo.getRowsDisplayValue().length());
        leadTimeMaxLength =
            Math.max(leadTimeMaxLength, explainInfo.getLeadTimeDisplayValue().length());
        actualTimeMaxLength =
            Math.max(actualTimeMaxLength, explainInfo.getActualTimeTimeDisplayValue().length());
        nodeMaxLength = Math.max(nodeMaxLength, explainInfo.getNodeDisplayValue().length());
        andmoreMaxLength = Math.max(andmoreMaxLength, explainInfo.getOtherDisplayValue().length());
      }
      idMaxLength = Math.max(idMaxLength, HEADER_ID.length());
      typeMaxLength = Math.max(typeMaxLength, HEADER_TYPE.length());
      inputMaxLength = Math.max(inputMaxLength, HEADER_INPUT.length());
      rowsMaxLength = Math.max(rowsMaxLength, HEADER_ROWS.length());
      leadTimeMaxLength = Math.max(leadTimeMaxLength, HEADER_LEADTIME.length());
      actualTimeMaxLength = Math.max(actualTimeMaxLength, HEADER_ACTUALTIME.length());
      nodeMaxLength = Math.max(nodeMaxLength, HEADER_NODE.length());
      andmoreMaxLength = Math.max(andmoreMaxLength, HEADER_ANDMORE.length());

      StringBuilder headerSb = new StringBuilder();
      headerSb.append(String.format("%-" + idMaxLength + "s", HEADER_ID));
      headerSb.append(" ");
      headerSb.append(String.format("%-" + typeMaxLength + "s", HEADER_TYPE));
      headerSb.append(" ");
      headerSb.append(String.format("%-" + inputMaxLength + "s", HEADER_INPUT));
      headerSb.append(" ");
      if (isExplainAnalyze) {
        headerSb.append(String.format("%-" + rowsMaxLength + "s", HEADER_ROWS));
        headerSb.append(" ");
        headerSb.append(String.format("%-" + leadTimeMaxLength + "s", HEADER_LEADTIME));
        headerSb.append(" ");
        headerSb.append(String.format("%-" + actualTimeMaxLength + "s", HEADER_ACTUALTIME));
        headerSb.append(" ");
        headerSb.append(String.format("%-" + nodeMaxLength + "s", HEADER_NODE));
        headerSb.append(" ");
      }
      headerSb.append(String.format("%-" + andmoreMaxLength + "s", HEADER_ANDMORE));
      headerSb.append(" ");
      String headerStr = headerSb.toString();
      ret.add(headerStr);

      StringBuilder separateSb = new StringBuilder();
      for (int i = 0; i < headerStr.length(); i++) {
        separateSb.append("-");
      }
      ret.add(separateSb.toString());

      for (ExplainInfo explainInfo : explainInfoList) {
        StringBuilder lineSb = new StringBuilder();
        lineSb.append(String.format("%" + idMaxLength + "s", explainInfo.getIdDisplayValue()));
        lineSb.append(" ");
        lineSb.append(String.format("%-" + typeMaxLength + "s", explainInfo.getTypeDisplayValue()));
        lineSb.append(" ");
        lineSb.append(
            String.format("%-" + inputMaxLength + "s", explainInfo.getInputListDisplayValue()));
        lineSb.append(" ");
        if (isExplainAnalyze) {
          lineSb.append(
              String.format("%-" + rowsMaxLength + "s", explainInfo.getRowsDisplayValue()));
          lineSb.append(" ");
          lineSb.append(
              String.format("%" + leadTimeMaxLength + "s", explainInfo.getLeadTimeDisplayValue()));
          lineSb.append(" ");
          lineSb.append(
              String.format(
                  "%" + actualTimeMaxLength + "s", explainInfo.getActualTimeTimeDisplayValue()));
          lineSb.append(" ");
          lineSb.append(
              String.format("%-" + nodeMaxLength + "s", explainInfo.getNodeDisplayValue()));
          lineSb.append(" ");
        }
        lineSb.append(
            String.format("%-" + andmoreMaxLength + "s", explainInfo.getOtherDisplayValue()));
        ret.add(lineSb.toString());
      }

      return ret;
    }

    /**
     * Check whether the result is EXPLAIN/ANALYZE.
     *
     * @param targetExplainInfo target {@code ExplainInfo}
     * @return {@code true} if it is EXPLAIN/ANALYZE, otherwise {@code false}
     */
    public static boolean isExplainAnalyze(ExplainInfo targetExplainInfo) {
      boolean ret = false;
      if (targetExplainInfo.getActualTime() != null) {
        ret = true;
      }
      return ret;
    }

    /**
     * 画面出力時の表示順の設定 画面出力時のインデントの設定.
     *
     * @param targetExplainInfo targetExplainInfo
     * @param displayOrder displayOrder
     * @param displayDepth displayDepth
     * @return
     */
    private static int createDisplayOrderDisplayDepth(
        ExplainInfo targetExplainInfo, int displayOrder, int displayDepth) {
      targetExplainInfo.setDisplayOrder(displayOrder);
      displayOrder--;
      targetExplainInfo.setDisplayDepth(displayDepth);
      List<ExplainInfo> inputExplainInfoList = targetExplainInfo.getInputExplainInfoList();
      if (inputExplainInfoList != null && inputExplainInfoList.size() > 0) {
        for (ExplainInfo inputExplainInfo : inputExplainInfoList) {
          displayOrder =
              createDisplayOrderDisplayDepth(inputExplainInfo, displayOrder, displayDepth - 1);
        }
      }
      return displayOrder;
    }

    /**
     * 数字の文字列をintにして返す.
     *
     * @param str input string
     * @return
     */
    private static int str2int(String str) {
      try {
        return Integer.parseInt(str);
      } catch (NumberFormatException e) {
        return -1;
      }
    }

    /**
     * EXPLAIN/ANALYZE which returns {@code ExplainInfo} with id is specified by findId from {@code
     * explainInfoList}.
     *
     * @param explainInfoList list of {@code ExplainInfo}
     * @param findId find ID
     * @return {@code ExplainInfo} object
     */
    public static ExplainInfo findExplainInfoById(
        List<ExplainInfo> explainInfoList, String findId) {
      if (findId == null) {
        return null;
      }
      for (ExplainInfo info : explainInfoList) {
        if (findId.equals(info.getId())) {
          return info;
        }
      }
      return null;
    }

    private static String joinStr(List<String> strList, String sep) {
      String ret = "";
      if (strList == null || sep == null) {
        return ret;
      }

      int sepLength = sep.length();
      StringBuilder sb = new StringBuilder();
      for (String str : strList) {
        sb.append(str);
        sb.append(sep);
      }

      if (sb.length() > sepLength) {
        ret = sb.substring(0, sb.length() - sepLength);
      }

      return ret;
    }
  }

  /** 実行計画表示時の1行分の情報に相当. */
  private class ExplainInfo {
    private static final String EXEC_RESULT = "RESULT";

    private String id;
    private String idDisplayValue;
    private String type;
    private String typeDisplayValue;
    private List<String> inputList;
    private String inputListDisplayValue;
    private List<Long> rows;
    private String rowsDisplayValue;
    private Long leadTime;
    private String leadTimeDisplayValue;
    private Long actualTime;
    private String actualTimeTimeDisplayValue;
    private String node;
    private String nodeDisplayValue;
    private String otherDisplayValue;
    private JsonNode jsonNode;

    private String tableNameDisplayValue;
    private boolean useIndex;
    private String useIndexColumnName;
    private String profileOpTypeValue;
    private String limiteValue;
    private String unionTypeValue;

    private List<ExplainInfo> inputExplainInfoList;
    /**
     * inputのパスの深さ （例） id:3 type:EXEC_RESULT inputList:[2] id:2 type:EXEC_JOIN inputList:[0,1] id:1.
     * type:EXEC_SCAN inputList:[] id:0 type:EXEC_SCAN inputList:[] となっているとき id:3 type:EXEC_RESULT
     * inputList:[2] inputのパスの深さ:0 id:2 type:EXEC_JOIN inputList:[0,1] inputのパスの深さ:1 id:1
     * type:EXEC_SCAN inputList:[] inputのパスの深さ:2 id:0 type:EXEC_SCAN inputList:[] inputのパスの深さ:2
     */
    private int inputPathDepth;

    /**
     * 実行計画画面出力時のインデントの深さ （例） 0 SCAN 1 SCAN 2 JOIN 3 RESULT と画面出力するとき、 0 インデントの深さ:0 1 インデントの深さ:0 2.
     * インデントの深さ:1 3 インデントの深さ:2
     */
    private int displayDepth;
    /**
     * 実行計画画面出力時の表示順 （例） 0 SCAN 1 SCAN 2 JOIN 3 RESULT と画面出力するとき、 0 表示順:1 1 表示順:2 2 表示順:3 3 表示順:4.
     */
    private int displayOrder = -1;

    /**
     * Constructor for {@code ExplainInfo}.
     *
     * @param json a {@code JsonNode} object
     */
    public ExplainInfo(JsonNode json) {
      initializeId(json);
      initializeType(json);
      initializeInputList(json);
      initializeRows(json);
      initializeLeadTime(json);
      initializeActualTime(json);
      initializeNode(json);
      this.jsonNode = json;
    }

    /**
     * Check whether {@code EXEC_RESULT} is node result.
     *
     * @return {@code true} if it is, otherwise {@code false}
     */
    public boolean isResultNode() {
      boolean ret = false;
      if (EXEC_RESULT.equals(type)) {
        ret = true;
      }
      return ret;
    }

    /**
     * - If input information is available, returns the maximum input path depth in the {@code
     * ExplainInfo} pointed to by input.<br>
     * - If it doesn't have the input information, it returns its own input path depth.
     *
     * <p>Example:<br>
     *
     * <ul>
     *   <li>id:5 type:EXEC_RESULT inputList:[4] input path depth:0
     *   <li>id:4 type:EXEC_JOIN inputList:[2,3] input path depth:1
     *   <li>id:3 type:EXEC_SCAN inputList:[] input path depth:2
     *   <li>id:2 type:EXEC_JOIN inputList:[0,1] input path depth:2
     *   <li>id:1 type:EXEC_SCAN inputList:[] input path depth:3
     *   <li>id:0 type:EXEC_SCAN inputList:[] input path depth:3
     * </ul>
     *
     * So the maximum input path depth of id=3 is 2 and the maximum input path depth of id=2 is 3.
     *
     * @return max input path depth
     */
    public int getMaxInputPathDepth() {
      int maxInputPathDepth = 0;

      List<ExplainInfo> inputExplainInfoList = getInputExplainInfoList();
      if (inputExplainInfoList != null && inputExplainInfoList.size() > 0) {
        for (ExplainInfo inputExplainInfo : inputExplainInfoList) {
          maxInputPathDepth = Math.max(maxInputPathDepth, inputExplainInfo.getMaxInputPathDepth());
        }
      } else {
        maxInputPathDepth = getInputPathDepth();
      }

      return maxInputPathDepth;
    }

    private void initializeId(JsonNode json) {
      // Id
      String id = null;
      try {
        id = Long.toString(json.get("id").longValue());
      } catch (Exception e) {
        // Do nothing
      }
      setId(id);
    }

    private void initializeType(JsonNode json) {
      // Type
      String type = null;
      try {
        type = json.get("type").textValue();
      } catch (Exception e) {
        // Do nothing
      }
      setType(type);
    }

    private void initializeInputList(JsonNode json) {
      // Input
      List<String> inputList = null;

      try {
        int size = json.get("inputList").size();
        inputList = new ArrayList<String>();
        for (int i = 0; i < size; i++) {
          String input = Long.toString(json.get("inputList").get(i).longValue());
          inputList.add(input);
        }
      } catch (Exception e) {
        inputList = null;
      }
      setInputList(inputList);
    }

    private void initializeRows(JsonNode json) {
      // Rows
      List<Long> rows = null;

      try {
        int size = json.get("profile").get("rows").size();
        rows = new ArrayList<Long>();
        for (int i = 0; i < size; i++) {
          Long rowsVal = Long.valueOf(json.get("profile").get("rows").get(i).longValue());
          rows.add(rowsVal);
        }
      } catch (Exception e) {
        rows = null;
      }
      setRows(rows);
    }

    private void initializeLeadTime(JsonNode json) {
      // Lead time
      Long leadTime = null;
      try {
        leadTime = Long.valueOf(json.get("profile").get("leadTime").longValue());
      } catch (Exception e) {
        // Do nothing
      }
      setLeadTime(leadTime);
    }

    private void initializeActualTime(JsonNode json) {
      // Actual time
      Long actualTime = null;
      try {
        actualTime = Long.valueOf(json.get("profile").get("actualTime").longValue());
      } catch (Exception e) {
        // Do nothing
      }
      setActualTime(actualTime);
    }

    private void initializeNode(JsonNode json) {
      // Node
      String node = null;
      try {
        node = json.get("profile").get("address").textValue();
      } catch (Exception e) {
        // Do nothing
      }
      setNode(node);
    }

    /**
     * Get ID of {@code ExplainInfo}.
     *
     * @return ID of {@code ExplainInfo}
     */
    public String getId() {
      return id;
    }

    /**
     * Set ID for {@code ExplainInfo}.
     *
     * @param id id
     */
    public void setId(String id) {
      this.id = id;
    }

    /**
     * Get ID display value of {@code ExplainInfo}.
     *
     * @return ID display value
     */
    public String getIdDisplayValue() {
      return idDisplayValue;
    }

    /**
     * Set ID display value for {@code ExplainInfo}.
     *
     * @param idDsiplayValue id display value
     */
    public void setIdDisplayValue(String idDsiplayValue) {
      this.idDisplayValue = idDsiplayValue;
    }

    /**
     * Get type of {@code ExplainInfo}.
     *
     * @return type of {@code ExplainInfo}
     */
    public String getType() {
      return type;
    }

    /**
     * Set type for {@code ExplainInfo}.
     *
     * @param type type
     */
    public void setType(String type) {
      this.type = type;
    }

    /**
     * Get type display value of {@code ExplainInfo}.
     *
     * @return type display value of {@code ExplainInfo}
     */
    public String getTypeDisplayValue() {
      return typeDisplayValue;
    }

    /**
     * Set type display value for {@code ExplainInfo}.
     *
     * @param typeDsiplayValue type display value
     */
    public void setTypeDisplayValue(String typeDsiplayValue) {
      this.typeDisplayValue = typeDsiplayValue;
    }

    /**
     * Get input list of {@code ExplainInfo}.
     *
     * @return input list of {@code ExplainInfo}
     */
    public List<String> getInputList() {
      return inputList;
    }

    /**
     * Set input list for {@code ExplainInfo}.
     *
     * @param inputList input list
     */
    public void setInputList(List<String> inputList) {
      this.inputList = inputList;
    }

    /**
     * Get input list display value of {@code ExplainInfo}.
     *
     * @return input list display value of {@code ExplainInfo}
     */
    public String getInputListDisplayValue() {
      return inputListDisplayValue;
    }

    /**
     * Set input list display value for {@code ExplainInfo}.
     *
     * @param inputListDsiplayValue input list display value
     */
    public void setInputListDisplayValue(String inputListDsiplayValue) {
      this.inputListDisplayValue = inputListDsiplayValue;
    }

    /**
     * Get rows of {@code ExplainInfo}.
     *
     * @return rows of {@code ExplainInfo}
     */
    public List<Long> getRows() {
      return rows;
    }

    /**
     * Set rows for {@code ExplainInfo}.
     *
     * @param rows list of rows
     */
    public void setRows(List<Long> rows) {
      this.rows = rows;
    }

    /**
     * Get row display value of {@code ExplainInfo}.
     *
     * @return row display value of {@code ExplainInfo}
     */
    public String getRowsDisplayValue() {
      return rowsDisplayValue;
    }

    /**
     * Set row display value for {@code ExplainInfo}.
     *
     * @param rowsDsiplayValue row display value
     */
    public void setRowsDisplayValue(String rowsDsiplayValue) {
      this.rowsDisplayValue = rowsDsiplayValue;
    }

    /**
     * Get lead time of {@code ExplainInfo}.
     *
     * @return lead time of {@code ExplainInfo}
     */
    public Long getLeadTime() {
      return leadTime;
    }

    /**
     * Set lead time for {@code ExplainInfo}.
     *
     * @param leadTime lead time
     */
    public void setLeadTime(Long leadTime) {
      this.leadTime = leadTime;
    }

    /**
     * Get lead time display value of {@code ExplainInfo}.
     *
     * @return lead time display value of {@code ExplainInfo}
     */
    public String getLeadTimeDisplayValue() {
      return leadTimeDisplayValue;
    }

    /**
     * Set lead time display value for {@code ExplainInfo}.
     *
     * @param leadTimeDsiplayValue lead time display value
     */
    public void setLeadTimeDisplayValue(String leadTimeDsiplayValue) {
      this.leadTimeDisplayValue = leadTimeDsiplayValue;
    }

    /**
     * Get actual time of {@code ExplainInfo}.
     *
     * @return actual time of {@code ExplainInfo}
     */
    public Long getActualTime() {
      return actualTime;
    }

    /**
     * Set actual time for {@code ExplainInfo}.
     *
     * @param actualTime actual time
     */
    public void setActualTime(Long actualTime) {
      this.actualTime = actualTime;
    }

    /**
     * Get actual time display value of {@code ExplainInfo}.
     *
     * @return actual time display value of {@code ExplainInfo}
     */
    public String getActualTimeTimeDisplayValue() {
      return actualTimeTimeDisplayValue;
    }

    /**
     * Set actual time display value for {@code ExplainInfo}.
     *
     * @param actualTimeTimeDsiplayValue actual time display value
     */
    public void setActualTimeTimeDisplayValue(String actualTimeTimeDsiplayValue) {
      this.actualTimeTimeDisplayValue = actualTimeTimeDsiplayValue;
    }

    /**
     * Get node of {@code ExplainInfo}.
     *
     * @return node of {@code ExplainInfo}
     */
    public String getNode() {
      return node;
    }

    /**
     * Set node for {@code ExplainInfo}.
     *
     * @param node node
     */
    public void setNode(String node) {
      this.node = node;
    }

    /**
     * Get node display value of {@code ExplainInfo}.
     *
     * @return node display value of {@code ExplainInfo}
     */
    public String getNodeDisplayValue() {
      return nodeDisplayValue;
    }

    /**
     * Set node display value for {@code ExplainInfo}.
     *
     * @param nodeDisplayValue node display value
     */
    public void setNodeDisplayValue(String nodeDisplayValue) {
      this.nodeDisplayValue = nodeDisplayValue;
    }

    /**
     * Get display depth of {@code ExplainInfo}.
     *
     * @return display depth of {@code ExplainInfo}
     */
    public int getDisplayDepth() {
      return displayDepth;
    }

    /**
     * Set display depth for {@code ExplainInfo}.
     *
     * @param displayDepth display depth
     */
    public void setDisplayDepth(int displayDepth) {
      this.displayDepth = displayDepth;
    }

    /**
     * Get display order of {@code ExplainInfo}.
     *
     * @return display order of {@code ExplainInfo}
     */
    public int getDisplayOrder() {
      return displayOrder;
    }

    /**
     * Set display order for {@code ExplainInfo}.
     *
     * @param displayOrder display order
     */
    public void setDisplayOrder(int displayOrder) {
      this.displayOrder = displayOrder;
    }

    /**
     * Get input {@code ExplainInfo} list.
     *
     * @return input {@code ExplainInfo} list
     */
    public List<ExplainInfo> getInputExplainInfoList() {
      return inputExplainInfoList;
    }

    /**
     * Set input {@code ExplainInfo} list.
     *
     * @param inputExplainInfoList list of {@code ExplainInfo}
     */
    public void setInputExplainInfoList(List<ExplainInfo> inputExplainInfoList) {
      this.inputExplainInfoList = inputExplainInfoList;
    }

    /**
     * Get input path depth of {@code ExplainInfo}.
     *
     * @return input path depth of {@code ExplainInfo}
     */
    public int getInputPathDepth() {
      return inputPathDepth;
    }

    /**
     * Set input path depth for {@code ExplainInfo}.
     *
     * @param inputPathDepth input path depth
     */
    public void setInputPathDepth(int inputPathDepth) {
      this.inputPathDepth = inputPathDepth;
    }

    /**
     * Get other display value of {@code ExplainInfo}.
     *
     * @return other display value of {@code ExplainInfo}
     */
    public String getOtherDisplayValue() {
      return otherDisplayValue;
    }

    /**
     * Set other display value for {@code ExplainInfo}.
     *
     * @param otherDisplayValue other display value
     */
    public void setOtherDisplayValue(String otherDisplayValue) {
      this.otherDisplayValue = otherDisplayValue;
    }

    /**
     * Get JsonNode of {@code ExplainInfo}.
     *
     * @return JsonNode of {@code ExplainInfo}
     */
    public JsonNode getJsonNode() {
      return jsonNode;
    }

    /**
     * Set JsonNode for {@code ExplainInfo}.
     *
     * @param jsonNode a {@code JsonNode}
     */
    public void setJsonNode(JsonNode jsonNode) {
      this.jsonNode = jsonNode;
    }

    /**
     * Get table name display value of {@code ExplainInfo}.
     *
     * @return table name display value of {@code ExplainInfo}
     */
    public String getTableNameDisplayValue() {
      return tableNameDisplayValue;
    }

    /**
     * Set table name display value for {@code ExplainInfo}.
     *
     * @param tableNameDisplayValue table name display value
     */
    public void setTableNameDisplayValue(String tableNameDisplayValue) {
      this.tableNameDisplayValue = tableNameDisplayValue;
    }

    /**
     * Check index usage.
     *
     * @return index usage
     */
    public boolean isUseIndex() {
      return useIndex;
    }

    /**
     * Set index usage.
     *
     * @param useIndex {@code true} if yes, otherwise {@code false}
     */
    public void setUseIndex(boolean useIndex) {
      this.useIndex = useIndex;
    }

    /**
     * Get column name that uses index.
     *
     * @return column name that uses index
     */
    public String getUseIndexColumnName() {
      return useIndexColumnName;
    }

    /**
     * Set column name that uses index.
     *
     * @param useIndexColumnName column name
     */
    public void setUseIndexColumnName(String useIndexColumnName) {
      this.useIndexColumnName = useIndexColumnName;
    }

    /**
     * Get profile of {@code ExplainInfo}.
     *
     * @return profile of {@code ExplainInfo}
     */
    public String getProfileOpTypeValue() {
      return profileOpTypeValue;
    }

    /**
     * Set profile for {@code ExplainInfo}.
     *
     * @param profileOpTypeValue profile
     */
    public void setProfileOpTypeValue(String profileOpTypeValue) {
      this.profileOpTypeValue = profileOpTypeValue;
    }

    /**
     * Get limited value of {@code ExplainInfo}.
     *
     * @return limited value of {@code ExplainInfo}
     */
    public String getLimiteValue() {
      return limiteValue;
    }

    /**
     * Set limited value for {@code ExplainInfo}.
     *
     * @param limiteValue limited value
     */
    public void setLimiteValue(String limiteValue) {
      this.limiteValue = limiteValue;
    }

    /**
     * Get union type value of {@code ExplainInfo}.
     *
     * @return union type value of {@code ExplainInfo}
     */
    public String getUnionTypeValue() {
      return unionTypeValue;
    }

    /**
     * Set union type value for {@code ExplainInfo}.
     *
     * @param unionTypeValue union type value
     */
    public void setUnionTypeValue(String unionTypeValue) {
      this.unionTypeValue = unionTypeValue;
    }
  }

  /**
   * The main method for sub-command {@code gettaskplan}.<br>
   * Display the detailed information of an SQL analysis result in JSON format.
   *
   * @param id plan ID
   * @throws ShellException if it meets 1 of the following conditions:
   *     <ul>
   *       <li>The task does not exist
   *       <li>The query has not been executed
   *       <li>An error occurred while providing explain information
   *       <li>The executed query is not EXPLAIN/ANALYZE
   *     </ul>
   */
  @GSCommand
  public void gettaskplan(String id) {
    if (m_explainResult != null) {
      outputTaskPlan(m_explainResult, id);
    } else if (m_jdbcRS != null) {
      createExplainResult();
      outputTaskPlan(m_explainResult, id);
    } else {
      throw new ShellException(getMessage("error.noResultSet"));
    }
  }

  private void outputTaskPlan(ExplainResult explainResult, String id) {
    List<ExplainInfo> explainInfoList = explainResult.getExplainInfoList();

    ExplainInfo explainInfo = ExplainInfoUtil.findExplainInfoById(explainInfoList, id);
    if (explainInfo == null) {
      throw new ShellException(getMessage("error.gettaskplanNotFound", "id=[" + id + "]"));
    }
    JsonNode json = explainInfo.getJsonNode();

    ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    try {
      println(mapper.writeValueAsString(json));
    } catch (JsonProcessingException e) {
      throw new ShellException(getMessage("error.gettaskplan"));
    }
  }

  /**
   * The main method for sub-command {@code getplanjson}.<br>
   * Display an SQL analysis result (global plan) in JSON format.
   *
   * @param fileName JSON file name
   * @throws ShellException if it meets 1 of the following conditions:
   *     <ul>
   *       <li>The query has not been executed
   *       <li>An error occurred while providing explain information
   *       <li>The executed query is not EXPLAIN/ANALYZE
   *       <li>Error when outputting file
   *     </ul>
   */
  @GSCommand
  public void getplanjson(@GSNullable String fileName) {
    if (m_explainResult != null) {
      outputPlanJson(m_explainResult, fileName);
    } else if (m_jdbcRS != null) {
      createExplainResult();
      outputPlanJson(m_explainResult, fileName);
    } else {
      throw new ShellException(getMessage("error.noResultSet"));
    }
  }

  private void outputPlanJson(ExplainResult explainResult, String fileName) {
    List<JsonNode> explainJsonList = explainResult.getExplainJsonList();

    ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    ObjectNode root = mapper.createObjectNode();
    ArrayNode explains = mapper.createArrayNode();

    for (JsonNode explainJson : explainJsonList) {
      explains.add(explainJson);
    }
    root.put("nodeList", explains);

    String outputStr = "";
    try {
      outputStr = mapper.writeValueAsString(root);
    } catch (JsonProcessingException e) {
      throw new ShellException(getMessage("error.getplanjson"));
    }

    if (fileName == null) {
      println(outputStr);
    } else {
      try {
        File file = new File(fileName);
        BufferedWriter bw = new BufferedWriter(new FileWriter(file));
        bw.write(outputStr);
        bw.newLine();
        bw.close();
      } catch (IOException e) {
        throw new ShellException(
            getMessage("error.getplanjsonFileErr") + " : msg=[" + e.getMessage() + "]", e);
      }
    }
  }

  /**
   * The main method for sub-command {@code showsql}.<br>
   * Display the SQL processing under execution.
   *
   * @param queryId query ID
   * @throws ShellException if it meets 1 of the following conditions:
   *     <ul>
   *       <li>The connection is closed
   *       <li>An error occurred while providing currently executing SQL information
   *       <li>Currently executing SQL not found
   *       <li>Query with specified ID does not exist
   *     </ul>
   */
  @GSCommand
  public void showsql(@GSNullable String queryId) {
    checkConnectedSQL();

    List<SqlInfo> sqlInfos = null;
    try {
      sqlInfos = GridDBJdbcUtils.getSqlInfo(m_jdbcCon, queryId, false);

    } catch (Exception e) {
      throw new ShellException(getMessage("error.showsql") + " : msg=[" + e.getMessage() + "]", e);
    }
    if (sqlInfos == null || sqlInfos.isEmpty()) {
      if (queryId == null) {
        throw new ShellException(getMessage("error.showsqlSqlInfoNotFound"));
      } else {
        throw new ShellException(
            getMessage("error.showsqlSqlInfoQueryIdNotFound", "query id=[" + queryId + "]"));
      }
    }

    List<SqlInfoBinder> sqlInfoBinderList = groupingSqlInfo(sqlInfos);

    boolean sqlOutputAll = true;
    if (queryId == null) {
      sqlOutputAll = false;
    }
    showsqlPrint(sqlInfoBinderList, sqlOutputAll);
  }

  /**
   * SQL処理一覧情報より、クエリ情報とそれに紐付くジョブ情報にグルーピングする.
   *
   * @param sqlInfoList SQL処理一覧情報
   * @return グルーピングされた結果のクエリ情報ごとのリスト
   */
  private List<SqlInfoBinder> groupingSqlInfo(List<SqlInfo> sqlInfoList) {
    List<SqlInfoBinder> binderList = new ArrayList<SqlInfoBinder>();

    Iterator<SqlInfo> sqlInfoIte = sqlInfoList.iterator();
    while (sqlInfoIte.hasNext()) {
      SqlInfo checkInfo = sqlInfoIte.next();
      if (isQuerySqlInfo(checkInfo)) {
        SqlInfoBinder sqlInfoBinder = new SqlInfoBinder();
        sqlInfoBinder.setQuerySqlInfo(checkInfo);
        binderList.add(sqlInfoBinder);
        sqlInfoIte.remove();
      }
    }

    Iterator<SqlInfoBinder> binderIte = binderList.iterator();
    while (binderIte.hasNext()) {
      SqlInfoBinder sqlInfoBinder = binderIte.next();
      String queryId = sqlInfoBinder.getQuerySqlInfo().getQueryId();

      Iterator<SqlInfo> jobIte = sqlInfoList.iterator();
      while (jobIte.hasNext()) {
        SqlInfo checkInfo = jobIte.next();
        if (queryId != null && queryId.equals(checkInfo.getQueryId())) {
          sqlInfoBinder.getJobSqlInfoList().add(checkInfo);
          jobIte.remove();
        }
      }
    }

    if (sqlInfoList.size() > 0) {
      SqlInfo emptySqlInfo = new SqlInfo("", "", 0, null, null, "", "", "");
      SqlInfoBinder sqlInfoBinder = new SqlInfoBinder();
      sqlInfoBinder.setQuerySqlInfo(emptySqlInfo);

      Iterator<SqlInfo> jobIte = sqlInfoList.iterator();
      while (jobIte.hasNext()) {
        sqlInfoBinder.getJobSqlInfoList().add(jobIte.next());
      }
      binderList.add(sqlInfoBinder);
    }

    return binderList;
  }

  private void showsqlPrint(List<SqlInfoBinder> sqlInfoBinderList, boolean sqlOutputAll) {

    println("=======================================================================");
    Date now = new Date();
    long nowTime = now.getTime();
    DateTimeFormatter dateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    for (SqlInfoBinder binder : sqlInfoBinderList) {
      SqlInfo querySqlInfo = binder.getQuerySqlInfo();
      printfln("query id: %s", nullToEmpty(querySqlInfo.getQueryId()));
      Date startTime = querySqlInfo.getStartTime();
      String startTimeStr = null;
      String elapsedTimeStr = null;
      if (startTime != null) {
        ZonedDateTime zdt = null;
        if (m_connectTimeZoneVal == null || "auto".equals(m_connectTimeZoneVal)) {
          zdt = ZonedDateTime.ofInstant(startTime.toInstant(), ZoneId.systemDefault());
        } else {
          zdt = ZonedDateTime.ofInstant(startTime.toInstant(), ZoneOffset.of(m_connectTimeZoneVal));
        }
        startTimeStr = zdt.format(dateTimeFormatter);

        long elapsedTime = (nowTime - startTime.getTime()) / 1000;
        elapsedTimeStr = Long.toString(elapsedTime);
      }
      printfln("start time: %s", nullToEmpty(startTimeStr));
      printfln("elapsed time: %s", nullToEmpty(elapsedTimeStr));
      printfln("database name: %s", nullToEmpty(querySqlInfo.getDatabaseName()));
      printfln("application name: %s", nullToEmpty(querySqlInfo.getApplicationName()));
      String nodeStr = null;
      String nodeAddressStr = querySqlInfo.getNodeAddress();
      if (nodeAddressStr != null && nodeAddressStr.isEmpty() == false) {
        nodeStr = String.format("%s:%s", nodeAddressStr, querySqlInfo.getNodePort());
      }
      printfln("node: %s", nullToEmpty(nodeStr));
      String sql = querySqlInfo.getSql();
      if (sqlOutputAll == false && sql != null && sql.length() > 75) {
        sql = sql.substring(0, 75);
      }
      printfln("sql: %s", nullToEmpty(sql));

      List<SqlInfo> jobInfoList = binder.getJobSqlInfoList();
      for (SqlInfo jobInfo : jobInfoList) {
        printfln("  job id: %s", nullToEmpty(jobInfo.getJobId()));
        String jobNodeStr = null;
        String jobNodeAddressStr = jobInfo.getNodeAddress();
        if (jobNodeAddressStr != null && jobNodeAddressStr.isEmpty() == false) {
          jobNodeStr = String.format("%s:%s", jobNodeAddressStr, jobInfo.getNodePort());
        }
        printfln("  node: %s", nullToEmpty(jobNodeStr));
      }
      println("#---------------------------");
    }
  }

  /**
   * SQL処理一覧情報がクエリの情報であるかを返す.
   *
   * @param sqlInfo SQL処理一覧情報
   * @return クエリの情報であるか
   */
  private boolean isQuerySqlInfo(SqlInfo sqlInfo) {
    boolean ret = false;
    if (sqlInfo.getSql() != null) {
      ret = true;
    }
    return ret;
  }

  /**
   * The main method for sub-command {@code showevent}.<br>
   * Display the event list executed by the thread in each node in a cluster.
   *
   * @throws ShellException if it meets 1 of the following conditions:
   *     <ul>
   *       <li>The connection is closed
   *       <li>Currently executing event not found
   *       <li>Error while providing currently executing event information
   *       <li>Meta container not found
   *     </ul>
   */
  @GSCommand
  public void showevent() {
    checkConnected();

    Container<Object, Row> eventContainer = null;
    Query<Row> query = null;
    RowSet<Row> rowSet = null;

    List<EventInfo> eventInfos = new ArrayList<EventInfo>();
    try {
      eventContainer = gridStore.getContainer(ToolConstants.META_EVENT_INFO);
      if (eventContainer == null) {
        throw new ShellException("meta container not found.");
      }
      query = eventContainer.query(ToolConstants.TQL_SELECT_META_EVENT_INFO);
      rowSet = query.fetch();
      while (rowSet.hasNext()) {
        Row row = rowSet.next();
        String nodeAddress = row.getString(ToolConstants.META_TABLE_EVENT_INFO_NODE_ADDRESS_IDX);
        int nodePort = row.getInteger(ToolConstants.META_TABLE_EVENT_INFO_NODE_PORT_IDX);
        Date startTime = (Date) row.getValue(ToolConstants.META_TABLE_EVENT_INFO_START_TIME_IDX);

        String applicationName =
            row.getString(ToolConstants.META_TABLE_EVENT_INFO_APPLICATION_NAME_IDX);
        String serviceType = row.getString(ToolConstants.META_TABLE_EVENT_SERVICE_TYPE_IDX);
        String eventType = row.getString(ToolConstants.META_TABLE_EVENT_EVENT_TYPE_IDX);
        String workerId =
            Integer.toString(row.getInteger(ToolConstants.META_TABLE_EVENT_WORKER_ID_IDX));
        int clusterPartitionId =
            row.getInteger(ToolConstants.META_TABLE_EVENT_CLUSTER_PARTITION_ID_IDX);

        EventInfo eventInfo =
            new EventInfo(
                nodeAddress,
                nodePort,
                startTime,
                applicationName,
                serviceType,
                eventType,
                workerId,
                clusterPartitionId);
        eventInfos.add(eventInfo);
      }
    } catch (Exception e) {
      throw new ShellException(
          getMessage("error.showevent") + " : msg=[" + e.getMessage() + "]", e);
    } finally {
      closeQuitely(rowSet);
      closeQuitely(query);
      closeQuitely(eventContainer);
    }

    if (eventInfos == null || eventInfos.isEmpty()) {
      throw new ShellException(getMessage("error.showeventEventNotFound"));
    }

    Comparator<EventInfo> comparator =
        new Comparator<EventInfo>() {
          @Override
          public int compare(EventInfo o1, EventInfo o2) {
            try {
              long o1startTimeLong = -1;
              long o2startTimeLong = -1;
              Date o1StartTime = o1.getStartTime();
              if (o1StartTime != null) {
                o1startTimeLong = o1StartTime.getTime();
              }
              Date o2StartTime = o2.getStartTime();
              if (o2StartTime != null) {
                o2startTimeLong = o2StartTime.getTime();
              }
              if (o1startTimeLong < o2startTimeLong) {
                return -1;
              } else if (o1startTimeLong > o2startTimeLong) {
                return 1;
              } else {
                return o1.getWorkerId().compareTo(o2.getWorkerId());
              }
            } catch (Exception e) {
              // Do nothing
            }
            return 0;
          }
        };
    Collections.sort(eventInfos, comparator);

    showeventPrint(eventInfos);
  }

  private void showeventPrint(List<EventInfo> eventInfoList) {

    println("=======================================================================");
    Date now = new Date();
    long nowTime = now.getTime();
    DateTimeFormatter dateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    for (EventInfo eventInfo : eventInfoList) {
      printfln("worker id: %s", nullToEmpty(eventInfo.getWorkerId()));
      Date startTime = eventInfo.getStartTime();
      String startTimeStr = null;
      String elapsedTimeStr = null;
      if (startTime != null) {
        ZonedDateTime zdt = null;
        if (m_connectTimeZoneVal == null || "auto".equals(m_connectTimeZoneVal)) {
          zdt = ZonedDateTime.ofInstant(startTime.toInstant(), ZoneId.systemDefault());
        } else {
          zdt = ZonedDateTime.ofInstant(startTime.toInstant(), ZoneOffset.of(m_connectTimeZoneVal));
        }
        startTimeStr = zdt.format(dateTimeFormatter);

        long elapsedTime = (nowTime - startTime.getTime()) / 1000;
        elapsedTimeStr = Long.toString(elapsedTime);
      }
      printfln("start time: %s", nullToEmpty(startTimeStr));
      printfln("elapsed time: %s", nullToEmpty(elapsedTimeStr));
      printfln("application name: %s", nullToEmpty(eventInfo.getApplicationName()));
      String nodeStr = null;
      String nodeAddressStr = eventInfo.getNodeAddress();
      if (nodeAddressStr != null && nodeAddressStr.isEmpty() == false) {
        nodeStr = String.format("%s:%s", nodeAddressStr, eventInfo.getNodePort());
      }
      printfln("node: %s", nullToEmpty(nodeStr));
      printfln("service type: %s", nullToEmpty(eventInfo.getServiceType()));
      printfln("event type: %s", nullToEmpty(eventInfo.getEventType()));
      printfln("cluster partition id: %s", eventInfo.getClusterPartitionId());

      println("#---------------------------");
    }
  }

  /**
   * The main method for sub-command {@code showconnection}.<br>
   * Display the list of connections.
   *
   * @throws ShellException if the connection is closed or currently executing connection not found
   *     or error occurred while providing currently executing connection information
   */
  @GSCommand
  public void showconnection() {
    checkConnected();

    Container<Object, Row> connectionContainer = null;
    Query<Row> query = null;
    RowSet<Row> rowSet = null;

    List<ConnectionInfo> connectionInfos = new ArrayList<ConnectionInfo>();
    try {
      connectionContainer = gridStore.getContainer(ToolConstants.META_CONNECTION_INFO);
      if (connectionContainer == null) {
        throw new ShellException("meta container not found.");
      }
      query = connectionContainer.query(ToolConstants.TQL_SELECT_META_CONNECTION_INFO);
      rowSet = query.fetch();
      while (rowSet.hasNext()) {
        Row row = rowSet.next();
        String serviceType =
            row.getString(ToolConstants.META_TABLE_CONNECTION_INFO_SERVICE_TYPE_IDX);
        String socketType = row.getString(ToolConstants.META_TABLE_CONNECTION_INFO_SOCKET_TYPE_IDX);
        String nodeAddress =
            row.getString(ToolConstants.META_TABLE_CONNECTION_INFO_NODE_ADDRESS_IDX);
        int nodePort = row.getInteger(ToolConstants.META_TABLE_CONNECTION_INFO_NODE_PORT_IDX);
        String remoteAddress =
            row.getString(ToolConstants.META_TABLE_CONNECTION_INFO_REMOTE_ADDRESS_IDX);
        int remotePort = row.getInteger(ToolConstants.META_TABLE_CONNECTION_INFO_REMOTE_PORT_IDX);
        String applicationName =
            row.getString(ToolConstants.META_TABLE_CONNECTION_INFO_APPLICATION_NAME_IDX);
        Date creationTime =
            (Date) row.getValue(ToolConstants.META_TABLE_CONNECTION_INFO_CREATION_TIME_IDX);
        long dispatchingEventCount =
            row.getLong(ToolConstants.META_TABLE_CONNECTION_INFO_DISPATCHING_EVENT_COUNT_IDX);
        long sendingEventCount =
            row.getLong(ToolConstants.META_TABLE_CONNECTION_INFO_SENDING_EVENT_COUNT_IDX);

        ConnectionInfo connInfo =
            new ConnectionInfo(
                serviceType,
                socketType,
                nodeAddress,
                nodePort,
                remoteAddress,
                remotePort,
                applicationName,
                creationTime,
                dispatchingEventCount,
                sendingEventCount);
        connectionInfos.add(connInfo);
      }
    } catch (Exception e) {
      throw new ShellException(
          getMessage("error.showconnection") + " : msg=[" + e.getMessage() + "]", e);
    } finally {
      closeQuitely(rowSet);
      closeQuitely(query);
      closeQuitely(connectionContainer);
    }

    if (connectionInfos == null || connectionInfos.isEmpty()) {
      throw new ShellException(getMessage("error.showconnectionConnectionNotFound"));
    }

    Comparator<ConnectionInfo> comparator =
        new Comparator<ConnectionInfo>() {
          @Override
          public int compare(ConnectionInfo o1, ConnectionInfo o2) {
            try {
              long o1creationTimeLong = -1;
              long o2creationTimeLong = -1;
              Date o1StartTime = o1.getCreationTime();
              if (o1StartTime != null) {
                o1creationTimeLong = o1StartTime.getTime();
              }
              Date o2StartTime = o2.getCreationTime();
              if (o2StartTime != null) {
                o2creationTimeLong = o2StartTime.getTime();
              }
              if (o1creationTimeLong < o2creationTimeLong) {
                return -1;
              } else if (o1creationTimeLong > o2creationTimeLong) {
                return 1;
              } else {
                return 0;
              }
            } catch (Exception e) {
              // Do nothing
            }
            return 0;
          }
        };
    Collections.sort(connectionInfos, comparator);

    showconnectionPrint(connectionInfos);
  }

  private void showconnectionPrint(List<ConnectionInfo> connectionInfoList) {

    println("=======================================================================");
    Date now = new Date();
    long nowTime = now.getTime();
    DateTimeFormatter dateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    for (ConnectionInfo connectionInfo : connectionInfoList) {
      Date startTime = connectionInfo.getCreationTime();
      String startTimeStr = null;
      String elapsedTimeStr = null;
      if (startTime != null) {
        ZonedDateTime zdt = null;
        if (m_connectTimeZoneVal == null || "auto".equals(m_connectTimeZoneVal)) {
          zdt = ZonedDateTime.ofInstant(startTime.toInstant(), ZoneId.systemDefault());
        } else {
          zdt = ZonedDateTime.ofInstant(startTime.toInstant(), ZoneOffset.of(m_connectTimeZoneVal));
        }
        startTimeStr = zdt.format(dateTimeFormatter);

        long elapsedTime = (nowTime - startTime.getTime()) / 1000;
        elapsedTimeStr = Long.toString(elapsedTime);
      }
      String nodeStr = null;
      String nodeAddressStr = connectionInfo.getNodeAddress();
      if (nodeAddressStr != null && nodeAddressStr.isEmpty() == false) {
        nodeStr = String.format("%s:%s", nodeAddressStr, connectionInfo.getNodePort());
      }
      String remoteStr = null;
      String remoteAddressStr = connectionInfo.getRemoteAddress();
      if (remoteAddressStr != null && remoteAddressStr.isEmpty() == false) {
        remoteStr = String.format("%s:%s", remoteAddressStr, connectionInfo.getRemotePort());
      }

      printfln("application name: %s", nullToEmpty(connectionInfo.getApplicationName()));
      printfln(
          "creation time: %s service type: %s",
          nullToEmpty(startTimeStr), nullToEmpty(connectionInfo.getServiceType()));
      printfln(
          "elapsed time: %s node: %s remote: %s",
          nullToEmpty(elapsedTimeStr), nullToEmpty(nodeStr), nullToEmpty(remoteStr));
      printfln(
          "dispatching event count: %s sending event count: %s",
          connectionInfo.getDispatchingEventCount(), connectionInfo.getSendingEventCount());

      println("#---------------------------");
    }
  }

  /**
   * 引数がnullの場合空文字を返します.
   *
   * @param str input string
   * @return 引数がnullの場合空文字 nullではない場合引数と同じ
   */
  private String nullToEmpty(String str) {
    String ret = str;
    if (str == null) {
      ret = "";
    }
    return ret;
  }

  /**
   * The main method for sub-command {@code killsql}.<br>
   * Cancel the SQL processing in progress. Need to run with administrator user.
   *
   * @param queryId query ID
   * @throws ShellException if it meets 1 of the following conditions:
   *     <ul>
   *       <li>User name or password is {@code null}
   *       <li>The connection is closed
   *       <li>Query with specified ID does not exist
   *       <li>An unexpected error occurred while canceling SQL
   *     </ul>
   *
   * @see GridStoreCommandUtils#killSql
   */
  @GSCommand
  public void killsql(String queryId) {
    String userId = (String) getContext().getAttribute(GridStoreShell.USER);
    String password = (String) getContext().getAttribute(GridStoreShell.PASSWORD);
    checkUserIdAndPassword(userId, password);
    checkConnectedSQL();

    List<SqlInfo> sqlInfos = null;
    try {
      sqlInfos = GridDBJdbcUtils.getSqlInfo(m_jdbcCon, queryId, true);
    } catch (Exception e) {
      throw new ShellException(getMessage("error.killsql") + " : msg=[" + e.getMessage() + "]", e);
    }
    if (sqlInfos == null || sqlInfos.isEmpty()) {
      throw new ShellException(
          getMessage("error.killsqlSqlInfoQueryIdNotFound", "query id=[" + queryId + "]"));
    }

    try {
      SqlInfo sqlInfo = sqlInfos.get(0);
      sqlInfo.getNodeAddress();
      sqlInfo.getNodePort();

      GSNode node =
          new GSNode(sqlInfo.getNodeAddress(), sqlInfo.getNodePort(), GSNode.DEFAULT_SSH_PORT);
      GridStoreCommandUtils.killSql(node, userId, password, queryId);
    } catch (Exception e) {
      throw new ShellException(getMessage("error.killsql") + " : msg=[" + e.getMessage() + "]", e);
    }
  }

  /**
   * The main method for sub-command {@code killjob}.<br>
   * Cancel the job being executed.<br>
   * It is a hidden sub-command. Do not appear in the help list and cannot use tab keyboard to
   * suggest for hidden sub-command.
   *
   * @param jobId job ID
   * @throws ShellException if it meets 1 of the following conditions:
   *     <ul>
   *       <li>User name or password is {@code null}
   *       <li>The connection is closed
   *       <li>Job with specified ID does not exist
   *       <li>An unexpected error occurred while canceling SQL
   *     </ul>
   *
   * @see GridStoreCommandUtils#killJob
   */
  @GSCommand(hidden = true)
  public void killjob(String jobId) {
    String userId = (String) getContext().getAttribute(GridStoreShell.USER);
    String password = (String) getContext().getAttribute(GridStoreShell.PASSWORD);
    checkUserIdAndPassword(userId, password);
    checkConnectedSQL();

    List<SqlInfo> sqlInfos = null;
    try {
      sqlInfos = GridDBJdbcUtils.getSqlInfoJob(m_jdbcCon, jobId);
    } catch (Exception e) {
      throw new ShellException(getMessage("error.killjob") + " : msg=[" + e.getMessage() + "]", e);
    }
    if (sqlInfos == null || sqlInfos.isEmpty()) {
      throw new ShellException(
          getMessage("error.killjobSqlInfoQueryIdNotFound", "job id=[" + jobId + "]"));
    }

    try {
      SqlInfo sqlInfo = sqlInfos.get(0);
      sqlInfo.getNodeAddress();
      sqlInfo.getNodePort();

      GSNode node =
          new GSNode(sqlInfo.getNodeAddress(), sqlInfo.getNodePort(), GSNode.DEFAULT_SSH_PORT);
      GridStoreCommandUtils.killJob(node, userId, password, jobId);

    } catch (Exception e) {
      throw new ShellException(getMessage("error.killjob") + " : msg=[" + e.getMessage() + "]", e);
    }
  }

  /**
   * The main method for sub-command {@code settimezone}.<br>
   * Set time zone
   *
   * @param timeZoneVal time zone value.
   * @throws ShellException when time zone is invalid
   * @see ScriptContext
   */
  @GSCommand
  public void settimezone(@GSNullable String timeZoneVal) {

    if (timeZoneVal != null) {
      checkTimeZoneVal(timeZoneVal);
      getContext().setAttribute(GridStoreShell.TIMEZONE, timeZoneVal, ScriptContext.ENGINE_SCOPE);
    } else {
      getContext().removeAttribute(GridStoreShell.TIMEZONE, ScriptContext.ENGINE_SCOPE);
    }
  }

  private void checkTimeZoneVal(String timeZoneVal) {
    boolean result = false;
    if (timeZoneVal.matches("^[\\+|\\-]\\d\\d:\\d\\d$")) {
      result = true;
    } else if (timeZoneVal.matches("^[\\+|\\-]\\d\\d\\d\\d$")) {
      result = true;
    } else if (timeZoneVal.matches("^Z$")) {
      result = true;
    } else if (timeZoneVal.matches("^auto$")) {
      result = true;
    }

    if (result != true) {
      throw new ShellException(getMessage("error.timezoneInvalid", timeZoneVal));
    }
  }

  /**
   * SQL shorthand command for SELECT. <br>
   * Do not appear in the help list and cannot use tab keyboard to suggest for hidden sub-command.
   * <br>
   * When the help command is executed individually, the help of the SQL sub-command is displayed.
   *
   * @param sql SQL string
   * @see DataCommandClass#sql
   */
  @GSCommand(multiline = true, hidden = true)
  public void select(String sql) {
    sql("select " + sql);
  }

  /**
   * SQL shorthand command for UPDATE. <br>
   * Do not appear in the help list and cannot use tab keyboard to suggest for hidden sub-command.
   * <br>
   * When the help command is executed individually, the help of the SQL sub-command is displayed.
   *
   * @param sql SQL string
   * @see DataCommandClass#sql
   */
  @GSCommand(multiline = true, hidden = true)
  public void update(String sql) {
    sql("update " + sql);
  }

  /**
   * SQL shorthand command for INSERT. <br>
   * Do not appear in the help list and cannot use tab keyboard to suggest for hidden sub-command.
   * <br>
   * When the help command is executed individually, the help of the SQL sub-command is displayed.
   *
   * @param sql SQL string
   * @see DataCommandClass#sql
   */
  @GSCommand(multiline = true, hidden = true)
  public void insert(String sql) {
    sql("insert " + sql);
  }

  /**
   * SQL shorthand command for REPLACE. <br>
   * Do not appear in the help list and cannot use tab keyboard to suggest for hidden sub-command.
   * <br>
   * When the help command is executed individually, the help of the SQL sub-command is displayed.
   *
   * @param sql SQL string
   * @see DataCommandClass#sql
   */
  @GSCommand(multiline = true, hidden = true)
  public void replace(String sql) {
    sql("replace " + sql);
  }

  /**
   * SQL shorthand command for DELETE. <br>
   * Do not appear in the help list and cannot use tab keyboard to suggest for hidden sub-command.
   * <br>
   * When the help command is executed individually, the help of the SQL sub-command is displayed.
   *
   * @param sql SQL string
   * @see DataCommandClass#sql
   */
  @GSCommand(multiline = true, hidden = true)
  public void delete(String sql) {
    sql("delete " + sql);
  }

  /**
   * SQL shorthand command for CREATE. <br>
   * Do not appear in the help list and cannot use tab keyboard to suggest for hidden sub-command.
   * <br>
   * When the help command is executed individually, the help of the SQL sub-command is displayed.
   *
   * @param sql SQL string
   * @see DataCommandClass#sql
   */
  @GSCommand(multiline = true, hidden = true)
  public void create(String sql) {
    sql("create " + sql);
  }

  /**
   * SQL shorthand command for DROP. <br>
   * Do not appear in the help list and cannot use tab keyboard to suggest for hidden sub-command.
   * <br>
   * When the help command is executed individually, the help of the SQL sub-command is displayed.
   *
   * @param sql SQL string
   * @see DataCommandClass#sql
   */
  @GSCommand(multiline = true, hidden = true)
  public void drop(String sql) {
    sql("drop " + sql);
  }

  /**
   * SQL shorthand command for ALTER. <br>
   * Do not appear in the help list and cannot use tab keyboard to suggest for hidden sub-command.
   * <br>
   * When the help command is executed individually, the help of the SQL sub-command is displayed.
   *
   * @param sql SQL string
   * @see DataCommandClass#sql
   */
  @GSCommand(multiline = true, hidden = true)
  public void alter(String sql) {
    sql("alter " + sql);
  }

  /**
   * SQL shorthand command for GRANT. <br>
   * Do not appear in the help list and cannot use tab keyboard to suggest for hidden sub-command.
   * <br>
   * When the help command is executed individually, the help of the SQL sub-command is displayed.
   *
   * @param sql SQL string
   * @see DataCommandClass#sql
   */
  @GSCommand(multiline = true, hidden = true)
  public void grant(String sql) {
    sql("grant " + sql);
  }

  /**
   * SQL shorthand command for REVOKE. <br>
   * Do not appear in the help list and cannot use tab keyboard to suggest for hidden sub-command.
   * <br>
   * When the help command is executed individually, the help of the SQL sub-command is displayed.
   *
   * @param sql SQL string
   * @see DataCommandClass#sql
   */
  @GSCommand(multiline = true, hidden = true)
  public void revoke(String sql) {
    sql("revoke " + sql);
  }

  /**
   * SQL shorthand command for PRAGMA. <br>
   * Do not appear in the help list and cannot use tab keyboard to suggest for hidden sub-command.
   * <br>
   * When the help command is executed individually, the help of the SQL sub-command is displayed.
   *
   * @param sql SQL string
   * @see DataCommandClass#sql
   */
  @GSCommand(multiline = true, hidden = true)
  public void pragma(String sql) {
    sql("pragma " + sql);
  }

  /**
   * SQL shorthand command for EXPLAIN. <br>
   * Do not appear in the help list and cannot use tab keyboard to suggest for hidden sub-command.
   * <br>
   * When the help command is executed individually, the help of the SQL sub-command is displayed.
   *
   * @param sql SQL string
   * @see DataCommandClass#sql
   */
  @GSCommand(multiline = true, hidden = true)
  public void explain(String sql) {
    sql("explain " + sql);
  }

  /**
   * The main method for sub-command {@code searchcontainer}.<br>
   * Search container information.
   *
   * @param containerNamePattern container name pattern
   * @throws SQLException if An unexpected error occurred while search container.
   * @see DataCommandClass#getTables
   */
  @GSCommand
  public void searchContainer(@GSNullable String containerNamePattern) {
    checkConnectedSQL();

    // Get container information from metadata.
    // Conditions other than container name pattern are ignored.
    DatabaseMetaData meta;
    try {
      meta = m_jdbcCon.getMetaData();
      String[] typeArr = {"TABLE"};
      try (ResultSet rs = meta.getTables(null, null, containerNamePattern, typeArr)) {
        // Display container information.
        while (rs.next()) {
          System.out.println(rs.getString("TABLE_NAME"));
        }
      }
    } catch (SQLException e) {
      throw new ShellException(
          getMessage("error.searchingContainer") + " : msg=[" + e.getMessage() + "]", e);
    }
  }

  /**
   * The main method for sub-command {@code searchview}.<br>
   * Search view information.
   *
   * @param viewNamePattern view name pattern
   * @throws SQLException if An unexpected error occurred while search view.
   * @see DataCommandClass#getTables
   */
  @GSCommand
  public void searchView(@GSNullable String viewNamePattern) {
    checkConnectedSQL();
    // Get view information from metadata.
    // Conditions other than view name pattern are ignored.
    DatabaseMetaData meta;
    try {
      meta = m_jdbcCon.getMetaData();
      String[] typeArr = {"VIEW"};

      try (ResultSet rs = meta.getTables(null, null, viewNamePattern, typeArr)) {
        // Display view information.
        while (rs.next()) {
          System.out.println(rs.getString("TABLE_NAME"));
        }
      }
    } catch (SQLException e) {
      throw new ShellException(
          getMessage("error.searchingView") + " : msg=[" + e.getMessage() + "]", e);
    }
  }

  /**
   * The main method for sub-command {@code putrow}.<br>
   * Put a row to specified container.
   *
   * @param containerName container name
   * @param columnValues column values
   * @throws ShellException if it meets 1 of below conditions:
   *     <ul>
   *       <li>This connection has already been closed(NoSQL).
   *       <li>This container does not exists.
   *       <li>Missing argument when using command.
   *       <li>Put row with invalid column type value.
   *       <li>Put row with redundant column.
   *       <li>Put row with key value is null.
   *       <li>For composite key, if the number of values is less than number of row key columns.
   *     </ul>
   *
   * @see ExperimentalTool#getExtendedContainerInfo
   */
  @GSCommand(assignall = true)
  public void putRow(String containerName, String... columnValues) {
    checkConnected();
    // Check missing argument
    if (columnValues.length == 0) {
      throw new ShellException(getMessage("error.missingArgument"));
    }
    List<String> listColumnValuesSplit = getListColumnValuesSplit(columnValues[0]);

    ContainerInfo contInfo = null;
    try {
      contInfo = gridStore.getContainerInfo(containerName);
      checkContainerExists(containerName, contInfo);
    } catch (GSException e) {
      throw new ShellException(
          getMessage("error.puttingRow") + " : msg=[" + e.getMessage() + "]", e);
    }

    try (Container<?, Row> container = gridStore.getContainer(containerName)) {
      // Put row with redundant column
      if (listColumnValuesSplit.size() > contInfo.getColumnCount()) {
        throw new ShellException(getMessage("error.puttingRowRedundantColumn"));
      }
      List<Integer> listKeyColumns = contInfo.getRowKeyColumnList();
      // Check the number of values is less than number of row key columns
      if (listColumnValuesSplit.size() < listKeyColumns.size()) {
        throw new ShellException(getMessage("error.putRowLessThanRowKey"));
      }
      Row row = container.createRow();
      setValueColumn(listColumnValuesSplit, contInfo, row);
      // The column has no value will be NULL
      if (listColumnValuesSplit.size() < contInfo.getColumnCount()) {
        for (int i = listColumnValuesSplit.size(); i < contInfo.getColumnCount(); i++) {
          row.setNull(i);
        }
      }
      container.put(row);
    } catch (GSException e) {
      throw new ShellException(
          getMessage("error.puttingRow") + " : msg=[" + e.getMessage() + "]", e);
    }
  }

  private void setValueColumn(List<String> listColumnValuesSplit, ContainerInfo contInfo, Row row)
      throws GSException {
    for (int i = 0; i < listColumnValuesSplit.size(); i++) {
      ColumnInfo columnInfo = contInfo.getColumnInfo(i);
      String valueColumn = listColumnValuesSplit.get(i);
      // Check key column is null
      if (valueColumn.equalsIgnoreCase("null")) {
        try {
          row.setNull(i);
        } catch (GSException e) {
          throw new ShellException(
              getMessage("error.puttingRowInvalidColumnValue") + " : msg=[" + e.getMessage() + "]",
              e);
        }

      } else {
        try {
          switch (columnInfo.getType()) {
            case STRING:
              row.setString(i, valueColumn);
              break;
            case FLOAT:
              row.setFloat(i, Float.parseFloat(valueColumn));
              break;
            case INTEGER:
              row.setInteger(i, Integer.parseInt(valueColumn));
              break;
            case DOUBLE:
              row.setDouble(i, Double.parseDouble(valueColumn));
              break;
            case BLOB:
              if (!valueColumn.equalsIgnoreCase("null")) {
                throw new ShellException(getMessage("error.puttingRowSpecifiedBlobValue"));
              }
              break;
            case BOOL:
              row.setBool(i, Boolean.parseBoolean(valueColumn));
              break;
            case LONG:
              row.setLong(i, Long.parseLong(valueColumn));
              break;
            case TIMESTAMP:
              if (columnInfo.getTimePrecision() == TimeUnit.MICROSECOND
                  || columnInfo.getTimePrecision() == TimeUnit.NANOSECOND ) {
                Timestamp columnTimeStamp = TimestampUtils.parsePrecise(valueColumn);
                row.setPreciseTimestamp(i, columnTimeStamp);
              }
              else { // MILISECOND timestamp still use Date type
                Date columnTimeStampDate = TimestampUtils.parse(valueColumn);
                row.setTimestamp(i, columnTimeStampDate);
              }
              break;
            case BYTE:
              row.setByte(i, Byte.parseByte(valueColumn));
              break;
            case SHORT:
              row.setShort(i, Short.parseShort(valueColumn));
              break;
            default:
              throw new ShellException(
                  getMessage("error.puttingRowColumnTypeNotSupport", columnInfo.getType()));
          }
        } catch (NumberFormatException | ParseException e) {
          throw new ShellException(
              getMessage("error.puttingRowInvalidColumnValue") + " : msg=[" + e.getMessage() + "]",
              e);
        }
      }
    }
  }

  private static List<String> getListColumnValuesSplit(String columnValues) {
    // Split input string to check space, single quotes, skip split single quotes
    // when has backslash
    // Reference resource from source
    // https://stackoverflow.com/questions/28789842/split-string-while-ignoring-escaped-character
    Pattern pattern = Pattern.compile("('[^']*?(?:\\\\'[^']*)*'|[^\\s]+)");
    Matcher m = pattern.matcher(columnValues);
    List<String> listColumnValueWithQuotes = new ArrayList<String>();
    while (m.find()) {
      String valueColumn = m.group(0);
      if (valueColumn.contains("\\'")) {
        listColumnValueWithQuotes.add(valueColumn.replace("\\'", "\'"));
      } else {
        listColumnValueWithQuotes.add(valueColumn);
      }
    }
    List<String> listColumnValueWithoutQuotes = new ArrayList<String>();
    for (String valueColumn : listColumnValueWithQuotes) {
      listColumnValueWithoutQuotes.add(stripLeadingAndTrailingQuotes(valueColumn));
    }
    return listColumnValueWithoutQuotes;
  }

  private static String stripLeadingAndTrailingQuotes(String str) {
    if (str.startsWith("\'")) {
      str = str.substring(1, str.length());
    }
    if (str.endsWith("\'")) {
      str = str.substring(0, str.length() - 1);
    }
    return str;
  }

  /**
   * The main method for sub-command {@code removerow}.<br>
   * Remove a row from specified container by specifying a row key.
   *
   * @param containerName container name
   * @param columnKeyValues column key values
   * @throws ShellException if it meets 1 of below conditions:
   *     <ul>
   *       <li>This connection has already been closed(NoSQL).
   *       <li>This container does not exists.
   *       <li>Missing argument when using command.
   *       <li>Remove row from container has no row key.
   *       <li>Remove row from timeseries with compression.
   *     </ul>
   *
   * @see ExperimentalTool#getExtendedContainerInfo
   */
  @GSCommand(assignall = true)
  public void removeRow(String containerName, String... columnKeyValues) {
    checkConnected();
    if (columnKeyValues.length == 0) {
      throw new ShellException(getMessage("error.missingArgument"));
    }

    List<String> listKeyValuesSplit = getListColumnValuesSplit(columnKeyValues[0]);
    ContainerInfo contInfo = null;

    try {
      contInfo = gridStore.getContainerInfo(containerName);
      checkContainerExists(containerName, contInfo);
    } catch (GSException e) {
      throw new ShellException(
          getMessage("error.removingRow") + " : msg=[" + e.getMessage() + "]", e);
    }

    try (Container<Object, Row> container = gridStore.getContainer(containerName)) {
      List<Integer> listKeyColumn = contInfo.getRowKeyColumnList();
      // Check container is no row key
      // Check container is lack row key value
      if (listKeyColumn.size() == 0) {
        throw new ShellException(getMessage("error.removingNoRowKey"));
      }
      // Check container lack row key value
      if (listKeyValuesSplit.size() < listKeyColumn.size()) {
        throw new ShellException(getMessage("error.putRowLessThanRowKey"));
      }
      // Check container key has key value null
      for (String keyColumnValue : listKeyValuesSplit) {
        if (keyColumnValue.equalsIgnoreCase("null")) {
          throw new ShellException(getMessage("error.putRowLessThanRowKey"));
        }
      }
      // Remove row
      Row row = container.createRow();
      setValueColumn(listKeyValuesSplit, contInfo, row);
      Key key = row.createKey();
      container.remove(key);
    } catch (GSException e) {
      throw new ShellException(
          getMessage("error.removingRow") + " : msg=[" + e.getMessage() + "]", e);
    }
  }

  /**
   * The main method for sub-command {@code setauthmethod}.<br>
   * Set authentication method
   *
   * @param authentication authentication method. The value variable are "ldap" or "internal".
   * @throws ShellException when authentication is invalid.
   * @see ScriptContext
   */
  @GSCommand(name = "setauthmethod")
  public void setAuthenticationMethod(@GSNullable AuthenticationMethods authentication) {
    if (authentication != null) {
      getContext()
          .setAttribute(
              GridStoreShell.AUTHENTICATION,
              AuthenticationMethods.valueOf(authentication.toString()).toString(),
              ScriptContext.ENGINE_SCOPE);
    } else {
      getContext().removeAttribute(GridStoreShell.AUTHENTICATION, ScriptContext.ENGINE_SCOPE);
    }
  }

  /**
   * The main method for sub-command {@code setntfif}.<br>
   * Set notification interface address
   *
   * @param notificationInterfaceAddress notification interface address follow IPv4 address format.
   * @throws ShellException when notification interface address invalid IPv4 format
   * @see ScriptContext
   */
  @GSCommand(name = "setntfif")
  public void setNotificationInterfaceAddress(@GSNullable String notificationInterfaceAddress) {

    if (notificationInterfaceAddress != null) {
      checkNotificationInterfaceAddressVal(notificationInterfaceAddress);
      getContext()
          .setAttribute(
              GridStoreShell.NOTIFICATION_INTERFACE_ADDRESS,
              formatValidIpAddress(notificationInterfaceAddress),
              ScriptContext.ENGINE_SCOPE);
    } else {
      getContext()
          .removeAttribute(
              GridStoreShell.NOTIFICATION_INTERFACE_ADDRESS, ScriptContext.ENGINE_SCOPE);
    }
  }

  /**
   * The main method for sub-command {@code setresultmaxwidth}.<br>
   * Set the maximum width for column in query result
   *
   * @param width The maximum width of column
   * @throws ShellException when width is invalid.
   */
  @GSCommand(name = "setresultmaxwidth")
  public void setResultMaxWidth(@GSNullable Integer width) {
    if (width != null) {
    	if (width < MIN_COLUMN_WIDTH_LIMIT || width > MAX_COLUMN_WIDTH_LIMIT) {
        throw new ShellException(getMessage(
            "error.illegalEnum", width, ": "
            + MIN_COLUMN_WIDTH_LIMIT + " -> " + MAX_COLUMN_WIDTH_LIMIT));
      }
      m_resultMaxWidth = width;
    } else {
      m_resultMaxWidth = MAX_COLUMN_WIDTH_DEFAULT;
    }
  }

  /**
   * The main method for sub-command {@code setresultformat}.<br>
   * Set the format to display the query result
   *
   * @param format The format of query result: choose TABLE or CSV.
   * @throws ShellException when format is invalid.
   */
  @GSCommand(name = "setresultformat")
  public void setResultFormat(@GSNullable String format) {
    if (format != null) {
      String resultFormat = format.toUpperCase();
      try {
        m_resultFormat = ResultFormat.valueOf(resultFormat);
      } catch(Exception e) {
        throw new ShellException(
            getMessage("error.illegalEnum", format, Arrays.toString(ResultFormat.values())));
      }
    } else {
      m_resultFormat = ResultFormat.TABLE;
    }
  }

  private int getResultMaxColumnWidth() {
    return m_resultMaxWidth != null ? m_resultMaxWidth : MAX_COLUMN_WIDTH_DEFAULT;
  }

  private ResultFormat getResultFormat() {
    return m_resultFormat != null ? m_resultFormat : ResultFormat.TABLE;
  }

  private static String[] simplifySQL(String sql) {
    String[] sqlTokens = sql.replaceAll("['][^']+[']",    "'_string_'") // simplify SQL string
                            .replaceAll("[\"][^\"]+[\"]", " _object_ ") // simplify SQL object identifier
                            .replaceAll("/\\*.*?\\*/",    "")           // remove comment
                            .replaceAll("--.*$",          "")           // remove comment
                            .toUpperCase()
                            .trim()
                            .split("\\s+");
    return sqlTokens;
  }

  private static int getCharacterDisplayWidth(int charCode) {
      return org.jline.utils.WCWidth.wcwidth(charCode);
  }


  
  private void checkNotificationInterfaceAddressVal(String notificationInterfaceAddress) {
    boolean result = false;
    if (isValidIpAddress(notificationInterfaceAddress)) {
      result = true;
    } else {
      result = false;
    }
    if (result != true) {
      throw new ShellException(
          getMessage("error.notificationInterfaceAddressInvalid", notificationInterfaceAddress));
    }
  }

  private static boolean isValidIpAddress(String ipAddress) {
    try {
      if (ipAddress == null || ipAddress.isEmpty()) {
        return false;
      }
      // Check length part is IPv4
      String[] parts = ipAddress.split("\\.");
      if (parts.length != 4) {
        return false;
      }
      // Check value each part of IP from 0 to 255 and accept when there are many zero at the
      // beginning
      for (String s : parts) {
        int i = Integer.parseInt(s);
        if ((i < 0) || (i > 255)) {
          return false;
        }
      }
      // check IPv4 invalid with end is dot
      if (ipAddress.endsWith(".")) {
        return false;
      }
      return true;
    } catch (NumberFormatException nfe) {
      return false;
    }
  }

  /**
   * Format IPv4 Address. Remove zero number when there are many zero at the beginning of each IP
   * part
   */
  private static String formatValidIpAddress(String ipAddress) {
    String[] parts = ipAddress.split("\\.");
    String outputIpAddress = "";
    for (String s : parts) {
      int i = Integer.parseInt(s);
      String part = String.valueOf(i) + DOT_CHARACTER;
      outputIpAddress += part;
    }
    return outputIpAddress.substring(0, outputIpAddress.length() - 1);
  }

}
