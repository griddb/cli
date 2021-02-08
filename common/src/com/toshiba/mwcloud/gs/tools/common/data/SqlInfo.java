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

package com.toshiba.mwcloud.gs.tools.common.data;

import java.util.Date;

/**
 * SQL処理一覧情報
 * <p>
 * SQL処理一覧の各種情報を保持します。
 * <p>
 *
 * @since 4.0
 */
public class SqlInfo {

	/**
	 * 接続データベース名
	 */
	private String databaseName;
	/**
	 * 処理実行しているノードアドレス
	 */
	private String nodeAddress;
	/**
	 * 処理実行しているノードポート
	 */
	private int nodePort;
	/**
	 * 処理開始時刻
	 */
	private Date startTime;
	/**
	 * アプリケーション名
	 */
	private String applicationName;
	/**
	 * クエリ文字列
	 */
	private String sql;
	/**
	 * クエリID
	 * キャンセル用(クエリ単位)
	 */
	private String queryId;
	/**
	 * ジョブID
	 * キャンセル用(ジョブ単位)
	 */
	private String jobId;

	/**
	 * コンストラクタ
	 *
	 * @param database_name
	 * @param node_address
	 * @param node_port
	 * @param start_time
	 * @param application_name
	 * @param sql
	 * @param query_id
	 * @param job_id
	 */
	public SqlInfo(String databaseName, String nodeAddress, int nodePort,
			Date startTime, String applicationName, String sql,
			String queryId, String jobId) {
		this.databaseName = databaseName;
		this.nodeAddress = nodeAddress;
		this.nodePort = nodePort;
		this.startTime = startTime;
		this.applicationName = applicationName;
		this.sql = sql;
		this.queryId = queryId;
		this.jobId = jobId;
	}

	/**
	 * 接続データベース名を返します。
	 * @return 接続データベース名
	 */
	public String getDatabaseName() {
		return databaseName;
	}
	/**
	 * 接続データベース名を設定します。
	 * @param databaseName 接続データベース名
	 */
	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}
	/**
	 * 処理実行しているノードアドレスを返します。
	 * @return 処理実行しているノードアドレス
	 */
	public String getNodeAddress() {
		return nodeAddress;
	}
	/**
	 * 処理実行しているノードアドレスを設定します。
	 * @param nodeAddress 処理実行しているノードアドレス
	 */
	public void setNodeAddress(String nodeAddress) {
		this.nodeAddress = nodeAddress;
	}
	/**
	 * 処理実行しているノードポートを返します。
	 * @return 処理実行しているノードポート
	 */
	public int getNodePort() {
		return nodePort;
	}
	/**
	 * 処理実行しているノードポートを設定します。
	 * @param nodePort 処理実行しているノードポート
	 */
	public void setNodePort(int nodePort) {
		this.nodePort = nodePort;
	}
	/**
	 * 処理開始時刻を返します。
	 * @return 処理開始時刻
	 */
	public Date getStartTime() {
		return startTime;
	}
	/**
	 * 処理開始時刻を設定します。
	 * @param startTime 処理開始時刻
	 */
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	/**
	 * アプリケーション名を返します。
	 * @return アプリケーション名
	 */
	public String getApplicationName() {
		return applicationName;
	}
	/**
	 * アプリケーション名を設定します。
	 * @param applicationName アプリケーション名
	 */
	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}
	/**
	 * クエリ文字列を返します。
	 * @return クエリ文字列
	 */
	public String getSql() {
		return sql;
	}
	/**
	 * クエリ文字列を設定します。
	 * @param sql クエリ文字列
	 */
	public void setSql(String sql) {
		this.sql = sql;
	}
	/**
	 * クエリIDを返します。
	 * @return クエリID
	 */
	public String getQueryId() {
		return queryId;
	}
	/**
	 * クエリIDを設定します。
	 * @param queryId クエリID
	 */
	public void setQueryId(String queryId) {
		this.queryId = queryId;
	}
	/**
	 * ジョブIDを返します。
	 * @return ジョブID
	 */
	public String getJobId() {
		return jobId;
	}
	/**
	 * ジョブIDを設定します。
	 * @param jobId ジョブID
	 */
	public void setJobId(String jobId) {
		this.jobId = jobId;
	}





}
