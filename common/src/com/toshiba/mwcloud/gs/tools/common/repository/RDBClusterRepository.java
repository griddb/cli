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

package com.toshiba.mwcloud.gs.tools.common.repository;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import com.toshiba.mwcloud.gs.tools.common.GSCluster;
import com.toshiba.mwcloud.gs.tools.common.GSNode;
import com.toshiba.mwcloud.gs.tools.common.GSUserInfo;
import com.toshiba.mwcloud.gs.tools.common.NotificationMode;
import com.toshiba.mwcloud.gs.tools.common.Repository;


public class RDBClusterRepository extends ClusterRepository {

	/** Oracle接続用のURLフォーマット */
	private static final String ORACLE_CONN_STRING_FORMAT = "jdbc:oracle:thin:@%1$s:%2$s:%3$s";
	private static final String PGSQL_CONN_STRING_FORMAT = "jdbc:postgresql://%1$s:%2$s/%3$s";

	// テーブル定義
	private static final String TABLE_CLUSTERS						= "GRIDDB_CLUSTERS";
	private static final String TABLE_CLUSTERS_COLUMN_NAME			= "name";
	private static final String TABLE_CLUSTERS_COLUMN_MODE			= "notificationMode";
	private static final String TABLE_CLUSTERS_COLUMN_ADDRESS		= "notificationAddress";
	private static final String TABLE_CLUSTERS_COLUMN_PORT			= "notificationPort";
	private static final String TABLE_CLUSTERS_COLUMN_SQL_ADDRESS	= "sqlNotificationAddress";
	private static final String TABLE_CLUSTERS_COLUMN_SQL_PORT		= "sqlNotificationPort";
	private static final String TABLE_CLUSTERS_COLUMN_PROVIDER_URL	= "notificationProviderUrl";
	private static final String TABLE_CLUSTERS_COLUMN_MEMBER		= "transactionMember";
	private static final String TABLE_CLUSTERS_COLUMN_MEMBER_SQL	= "sqlMember";

	private static final String TABLE_NODES							= "GRIDDB_NODES";
	private static final String TABLE_NODES_COLUMN_ADDRESS			= "address";
	private static final String TABLE_NODES_COLUMN_PORT				= "port";
	private static final String TABLE_NODES_COLUMN_SSHPORT			= "sshPort";
	private static final String TABLE_NODES_COLUMN_CLUSTER_NAME		= "clusterName";
	private static final String TABLE_NODES_COLUMN_OSPASSWORD		= "ospassword";
	private static final String TABLE_NODES_COLUMN_SNAPSHOT			= "snapshot";

	private static final String TABLE_USERS							= "GRIDDB_USERS";
	private static final String TABLE_USERS_COLUMN_CLUSTER_NAME		= "clusterName";
	private static final String TABLE_USERS_COLUMN_ID				= "id";
	private static final String TABLE_USERS_COLUMN_PASSWORD			= "password";
	private static final String TABLE_USERS_COLUMN_ROLE				= "role";

	// テーブル検索用クエリ
	private static final String SQL_CLUSTERS	= "select * from " + TABLE_CLUSTERS;
	private static final String SQL_NODES		= "select * from " + TABLE_NODES;
	private static final String SQL_USERS		= "select * from " + TABLE_USERS;
	private static final String SQL_USERS_ROLE	= "select " + TABLE_USERS_COLUMN_ROLE + " from " + TABLE_USERS;


	private String m_host;
	private String m_port;
	private String m_database;
	private String m_user;
	private String m_password;
	private String m_connString;
	private String m_connStringFormat;
	private String m_driverPath;

	private Driver m_driver;

	private String m_connectionPoolJNDI;

	public RDBClusterRepository(ToolProperties prop) throws Exception {
		this(prop.getMessage("rdb.driver"),
				prop.getMessage("rdb.host"),
				prop.getMessage("rdb.port"),
				prop.getMessage("rdb.database"),
				prop.getMessage("rdb.user"),
				prop.getMessage("rdb.password"),
				prop.getMessage("rdb.jndi")
				);

		init();
	}

	public RDBClusterRepository(String driverPath, String host, String port, String database, String user, String password, String connectionPoolJNDI) throws Exception {
		m_driverPath = driverPath;
		m_host = host;
		m_port = port;
		m_database = database;
		m_user = user;
		m_password = password;
		m_connectionPoolJNDI = connectionPoolJNDI;
		init();
	}


	public Repository readRepository() throws Exception{

		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		Repository repos = new Repository();
		try {
			conn = getConnection();
			stmt = conn.createStatement();

			// クラスタ情報を取得
			List<GSCluster<GSNode>> clusters = new ArrayList<GSCluster<GSNode>>();
			rs = stmt.executeQuery(SQL_CLUSTERS);
			while ( rs.next() ){
				GSCluster<GSNode> cluster = buildGSCluster(rs);
				clusters.add(cluster);
			}
			repos.setClusters(clusters);
			rs.close();
			rs = null;

			// ノード情報を取得
			List<GSNode> nodes = new ArrayList<GSNode>();
			rs = stmt.executeQuery(SQL_NODES);
			while ( rs.next() ){
				GSNode node = buildGSNode(rs);
				nodes.add(node);
			}
			repos.setNodes(nodes);

			return repos;

		} finally {
			if ( rs != null ){
				rs.close();
			}
			if ( stmt != null ){
				stmt.close();
			}
			if ( conn != null ){
				conn.close();
			}
		}

	}

	private GSCluster<GSNode> buildGSCluster(ResultSet rs) throws Exception{
		GSCluster<GSNode> cluster = new GSCluster<GSNode>();
		cluster.setName(rs.getString(TABLE_CLUSTERS_COLUMN_NAME));
		cluster.setMode(NotificationMode.valueOf(rs.getString(TABLE_CLUSTERS_COLUMN_MODE)));
		cluster.setAddress(rs.getString(TABLE_CLUSTERS_COLUMN_ADDRESS));
		cluster.setPort(rs.getInt(TABLE_CLUSTERS_COLUMN_PORT));
		cluster.setJdbcAddress(rs.getString(TABLE_CLUSTERS_COLUMN_SQL_ADDRESS));
		cluster.setJdbcPort(rs.getInt(TABLE_CLUSTERS_COLUMN_SQL_PORT));
		cluster.setProviderUrl(rs.getString(TABLE_CLUSTERS_COLUMN_PROVIDER_URL));
		cluster.setTransactionMember(rs.getString(TABLE_CLUSTERS_COLUMN_MEMBER));
		cluster.setSqlMember(rs.getString(TABLE_CLUSTERS_COLUMN_MEMBER_SQL));
		return cluster;
	}

	private GSNode buildGSNode(ResultSet rs) throws Exception {
		GSNode node = new GSNode();
		node.setAddress(rs.getString(TABLE_NODES_COLUMN_ADDRESS));
		node.setPort(rs.getInt(TABLE_NODES_COLUMN_PORT));
		node.setSshPort(rs.getInt(TABLE_NODES_COLUMN_SSHPORT));
		node.setClusterName(rs.getString(TABLE_NODES_COLUMN_CLUSTER_NAME));
		node.setOsPassword(rs.getString(TABLE_NODES_COLUMN_OSPASSWORD));
		return node;
	}

	/**
	 * V2.9では、RDBへの書き込みは未サポート
	 */
	public void saveRepository(Repository repository){
	}


	public GSCluster<GSNode> getGSCluster(String clusterName) throws Exception {

		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = getConnection();

			// クラスタ情報
			GSCluster<GSNode> cluster = null;
			String sql = SQL_CLUSTERS + " where " + TABLE_CLUSTERS_COLUMN_NAME + " = ?;";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, clusterName);
			rs = pstmt.executeQuery();
			while ( rs.next() ){
				cluster = buildGSCluster(rs);
			}
			if ( cluster == null ){
				throw new Exception("ClusterName not found in RDB");
			}
			rs.close();
			rs = null;
			pstmt.close();
			pstmt = null;

			// 指定されたクラスタがリポジトリに存在しなければnullを返す
			if (cluster == null) {
				return null;
			}

			// ノード情報を取得
			List<GSNode> nodes = new ArrayList<GSNode>();
			sql = SQL_NODES + " where " + TABLE_NODES_COLUMN_CLUSTER_NAME + " = ?;";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, clusterName);
			rs = pstmt.executeQuery();
			while ( rs.next() ){
				GSNode node = buildGSNode(rs);
				nodes.add(node);
			}
			cluster.setNodes(nodes);

			return cluster;

		} finally {
			if ( rs != null ){
				rs.close();
			}
			if ( pstmt != null ){
				pstmt.close();
			}
			if ( conn != null ){
				conn.close();
			}
		}
	}

	public GSNode getGSNode(String clusterName, String host, int port) throws Exception {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = getConnection();

			// ノード情報
			GSNode node = null;
			String sql = SQL_NODES + " where " + TABLE_NODES_COLUMN_CLUSTER_NAME + " = ? and " + TABLE_NODES_COLUMN_ADDRESS + " = ? and " + TABLE_NODES_COLUMN_PORT + " = ?;";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, clusterName);
			pstmt.setString(2, host);
			pstmt.setInt(3, port);
			rs = pstmt.executeQuery();
			while ( rs.next() ){
				node = buildGSNode(rs);
			}
			rs.close();

			return node;

		} finally {
			if ( rs != null ){
				rs.close();
			}
			if ( pstmt != null ){
				pstmt.close();
			}
			if ( conn != null ){
				conn.close();
			}
		}
	}

	public GSUserInfo auth(String clusterName, String userId, String password) throws Exception {

		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = getConnection();

			// クラスタ情報
			String sql = SQL_USERS_ROLE + " where "
					+ TABLE_USERS_COLUMN_CLUSTER_NAME + " = ? AND "
					+ TABLE_USERS_COLUMN_ID +" = ? AND "
					+ TABLE_USERS_COLUMN_PASSWORD + " = ?;";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, clusterName);
			pstmt.setString(2, userId);
			pstmt.setString(3, password);

			rs = pstmt.executeQuery();
			while ( rs.next() ){
				GSUserInfo info = new GSUserInfo();
				info.setRole(rs.getString(1));
				return info;
			}

			return null;

		} finally {
			if ( rs != null ){
				rs.close();
			}
			if ( pstmt != null ){
				pstmt.close();
			}
			if ( conn != null ){
				conn.close();
			}
		}
	}


	public void init() throws Exception {

		if ( m_connectionPoolJNDI == null ){
			if ( m_driverPath == null ){
				throw new Exception("definition error : 'rdb.driver' isn't specified in property file.");
			}
			File file = new File(m_driverPath);
			if ( !file.exists() ){
				throw new Exception("jdbc driver not found. path=["+m_driverPath+"]");
			}
			URLClassLoader loader;
			Class<?> cl;
			try {
				loader = URLClassLoader.newInstance(new URL[]{file.toURI().toURL()});

				String type = ToolProperties.getMessage("rdb.kind");
				if ( type == null || type.equalsIgnoreCase("PostgreSQL") ){
					cl = loader.loadClass("org.postgresql.Driver");
					m_connString = String.format(PGSQL_CONN_STRING_FORMAT, m_host, m_port, m_database);
				} else {
					cl = loader.loadClass("oracle.jdbc.driver.OracleDriver");
					m_connString = String.format(ORACLE_CONN_STRING_FORMAT, m_host, m_port, m_database);
				}
				m_driver = (Driver)cl.newInstance();

			} catch (MalformedURLException e) {
				throw new Exception("Failed to load jdbc driver.", e);
			} catch (ClassNotFoundException e) {
				throw new Exception("Failed to load jdbc driver.", e);
			} catch (InstantiationException e) {
				throw new Exception("Failed to load jdbc driver.", e);
			} catch (IllegalAccessException e) {
				throw new Exception("Failed to load jdbc driver.", e);
			}

		} else {
			m_connectionPoolJNDI = "java:comp/env/" + m_connectionPoolJNDI;
		}
	}

	/**
	 * データベースに接続を行います。
	 * <p>
	 * 実行ごとに個別の接続を返します。そのため、呼び出し側でcloseする必要があります。
	 */
	public Connection getConnection() throws Exception {

		Connection conn = null;
		try {
			/*
			long threadId = Thread.currentThread().getId();
			if ( m_connectMap.containsKey(threadId)){
				conn = m_connectMap.get(threadId);
				if ( !conn.isClosed() ){
					return conn;
				} else {
					m_connectMap.remove(threadId);
				}
			}
			*/

			if ( m_connectionPoolJNDI == null ){
				// RDBユーザ定義なし、パスワード定義なしは認めない
				// パスワードなしの場合は、rdb.password=を指定する。この場合は空文字がくる。
				if ( m_user == null ){
					throw new Exception("definition error : 'rdb.user' isn't specified in property file.");
				}
				if ( m_password == null ){
					throw new Exception("definition error : 'rdb.password' isn't specified in property file.");
				}

				Properties props = new Properties();
				props.setProperty("user", m_user);
				props.setProperty("password", m_password);

				//System.out.println(m_connString);
				DriverManager.setLoginTimeout(3);
				//long startNanos = System.nanoTime();
				conn = m_driver.connect(m_connString, props);
				//conn = DriverManager.getConnection(m_connString, m_user, m_password);
				//long connectMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
				//log.debug("Connected to RDB. (connectMillis={})", connectMillis);

				//m_connectMap.put(threadId, conn);

				return conn;

			} else {
				InitialContext context = new InitialContext();
				DataSource ds = (DataSource)context.lookup(m_connectionPoolJNDI);
				return ds.getConnection();
			}

		} catch ( Exception e ){
			throw new Exception("failed to read RDB Repository."+": str=["+m_connString+"], msg=["+e.getMessage().replace("\n", " ")+"]", e);
		}
	}
}
