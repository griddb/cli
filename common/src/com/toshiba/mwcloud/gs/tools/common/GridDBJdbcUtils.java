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

package com.toshiba.mwcloud.gs.tools.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.toshiba.mwcloud.gs.TimeUnit;
import com.toshiba.mwcloud.gs.tools.common.data.ExpirationInfo;
import com.toshiba.mwcloud.gs.tools.common.data.SqlInfo;
import com.toshiba.mwcloud.gs.tools.common.data.TablePartitionProperty;
import com.toshiba.mwcloud.gs.tools.common.data.ToolConstants;

public class GridDBJdbcUtils {


	/**
	 * 指定したテーブルのテーブルパーティショニング情報リストを取得します。
	 * <p>
	 * テーブルパーティショニング情報が存在しない場合はnullを返します。
	 *
	 * @return テーブルパーティショニング情報リスト
	 * @throws GridDBJdbcException
	 */
	public static List<TablePartitionProperty> getTablePartitionProperties(Connection conn, String name) throws GridDBJdbcException {

		TablePartitionProperty partProp = null;
		TablePartitionProperty subProp = null;

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
//			pstmt = conn.prepareStatement(ToolConstants.PSTMT_SELECT_META_TABLES_WITH_TABLE);
//			pstmt.setString(1, name);
			pstmt = conn.prepareStatement(String.format(ToolConstants.PSTMT_SELECT_META_TABLES_WITH_TABLE, name));
			rs = pstmt.executeQuery();
			if ( !rs.next() ){
				throw new GridDBJdbcException("Container not found. name=["+name+"]");
			}

			String partType = rs.getString(ToolConstants.META_TABLES_PARTITION_TYPE);
			String partColumn = rs.getString(ToolConstants.META_TABLES_PARTITION_COLUMN);

			if (partType != null) {
				if (partType.equals(ToolConstants.TABLE_PARTITION_TYPE_HASH)) {
					partProp = new TablePartitionProperty(partType, partColumn,
							rs.getInt(ToolConstants.META_TABLES_PARTITION_DIVISION_COUNT));
				} else if (partType.equals(ToolConstants.TABLE_PARTITION_TYPE_INTERVAL)) {
					partProp = new TablePartitionProperty(partType, partColumn,
							rs.getString(ToolConstants.META_TABLES_PARTITION_INTERVAL_VALUE),
							rs.getString(ToolConstants.META_TABLES_PARTITION_INTERVAL_UNIT));
				}
			}

			String subPartType = rs.getString(ToolConstants.META_TABLES_SUBPARTITION_TYPE);
			String subPartColumn = rs.getString(ToolConstants.META_TABLES_SUBPARTITION_COLUMN);

			if (subPartType != null) {
				if (subPartType.equals(ToolConstants.TABLE_PARTITION_TYPE_HASH)) {
					subProp = new TablePartitionProperty(subPartType, subPartColumn,
							rs.getInt(ToolConstants.META_TABLES_SUBPARTITION_DIVISION_COUNT));
				} else if (subPartType.equals(ToolConstants.TABLE_PARTITION_TYPE_INTERVAL)) {
					subProp = new TablePartitionProperty(subPartType, subPartColumn,
							rs.getString(ToolConstants.META_TABLES_SUBPARTITION_INTERVAL_VALUE),
							rs.getString(ToolConstants.META_TABLES_SUBPARTITION_INTERVAL_UNIT));
				}
			}

		} catch (Exception e) {
			throw new GridDBJdbcException("Failed to get table partitioning information. name=[" + name + "]", e);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (pstmt != null) {
					pstmt.close();
				}
			} catch (Exception e) {}
		}

		// テーブルパーティショニング情報が無かったらnullを返す
		if (partProp == null) {
			return null;
		}

		List<TablePartitionProperty> props = new ArrayList<TablePartitionProperty>();
		props.add(partProp);
		if (subProp != null) {
			props.add(subProp);
		}
		return props;
	}

	/**
	 * 指定したテーブルの期限解放情報をメタテーブルから取得します。
	 * <p>
	 * 期限解放情報が存在しない場合はnullを返します。
	 * <p>
	 * (V4.1)expiration_type='row'の場合はnullを返します。
	 *
	 * @return 期限解放情報
	 * @throws GridDBJdbcException
	 */
	public static ExpirationInfo getExpirationInfo(Connection conn, String name) throws GridDBJdbcException {

		ExpirationInfo expInfo = null;

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement(String.format(ToolConstants.PSTMT_SELECT_META_TABLES_WITH_TABLE, name));
			rs = pstmt.executeQuery();
			if ( !rs.next() ){
				throw new GridDBJdbcException("Container not found. name=["+name+"]");
			}

			String expType = rs.getString(ToolConstants.META_TABLES_EXPIRATION_TYPE);
			int expTime = rs.getInt(ToolConstants.META_TABLES_EXPIRATION_TIME);
			String expTimeUnitStr = rs.getString(ToolConstants.META_TABLES_EXPIRATION_TIME_UNIT);

			// type=nullまたはtype=rowのときは未設定とみなす
			if (expType != null && !expType.equalsIgnoreCase(ToolConstants.EXPIRATION_TYPE_ROW)) {
				expInfo = new ExpirationInfo(expType, expTime, TimeUnit.valueOf(expTimeUnitStr));
			}

		} catch (Exception e) {
			throw new GridDBJdbcException("Failed to get expiration information. name=[" + name + "]", e);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (pstmt != null) {
					pstmt.close();
				}
			} catch (Exception e) {}
		}

		// 期限解放情報が無かったらnullを返す
		if (expInfo == null) {
			return null;
		}

		return expInfo;
	}

	/**
	 * 実行中のSQL処理情報のリストを取得します。
	 * @param conn
	 * @param searchQueryId
	 * @return 実行中のSQL処理情報のリスト
	 * @throws GridDBJdbcException
	 */
	public static List<SqlInfo> getSqlInfo(Connection conn, String searchQueryId, boolean isSearchQueryOnly) throws GridDBJdbcException {
		return getSqlInfo(conn, searchQueryId, null, false, isSearchQueryOnly);
	}

	/**
	 * 実行中のSQL処理情報のリストを取得します。
	 * @param conn
	 * @param searchJobId
	 * @return 実行中のSQL処理情報のリスト
	 * @throws GridDBJdbcException
	 */
	public static List<SqlInfo> getSqlInfoJob(Connection conn, String searchJobId) throws GridDBJdbcException {
		return getSqlInfo(conn, null, searchJobId, true, false);
	}

	/**
	 * 実行中のSQL処理情報のリストを取得します。
	 * @param conn
	 * @param searchQueryId
	 * @param searchJobId
	 * @param isSearchJob
	 * @param isSearchQueryOnly（isSearchJob = flase、searchQueryIdに指定がある場合に有効）
	 * @return 実行中のSQL処理情報のリスト
	 * @throws GridDBJdbcException
	 */
	private static List<SqlInfo> getSqlInfo(Connection conn, String searchQueryId, String searchJobId, boolean isSearchJob, boolean isSearchQueryOnly) throws GridDBJdbcException {

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		List<SqlInfo> props = new ArrayList<SqlInfo>();
		try {
			if (isSearchJob == false) {
				if (searchQueryId == null) {
					// クエリIDの指定がない場合
					pstmt = conn.prepareStatement(ToolConstants.PSTMT_SELECT_META_SQL_INFO_NO_QUERY_ID);
				} else {
					// クエリIDの指定がある場合
					if (isSearchQueryOnly) {
						// クエリのみ（JOBをヒットさせない）とする場合、SQL IS NOT NULL の条件付きのStatementを作成
						pstmt = conn.prepareStatement(ToolConstants.PSTMT_SELECT_META_SQL_INFO_WITH_QUERY_ID_SQLONLY);
					} else {
						pstmt = conn.prepareStatement(ToolConstants.PSTMT_SELECT_META_SQL_INFO_WITH_QUERY_ID);
					}
					pstmt.setString(1, searchQueryId);
				}
			} else {
				// ジョブIDの指定がある場合
				pstmt = conn.prepareStatement(ToolConstants.PSTMT_SELECT_META_SQL_INFO_WITH_JOB_ID);
				pstmt.setString(1, searchJobId);
			}
			rs = pstmt.executeQuery();
			while (rs.next()) {
				String databaseName = rs.getString(ToolConstants.META_TABLE_SQL_INFO_DATABASE_NAME);
				String nodeAddress = rs.getString(ToolConstants.META_TABLE_SQL_INFO_NODE_ADDRESS);
				int nodePort = rs.getInt(ToolConstants.META_TABLE_SQL_INFO_NODE_PORT);
				Date startTime = rs.getTimestamp(ToolConstants.META_TABLE_SQL_INFO_START_TIME);
				String applicationName = rs.getString(ToolConstants.META_TABLE_SQL_INFO_APPLICATION_NAME);
				String sql = rs.getString(ToolConstants.META_TABLE_SQL_INFO_SQL);
				String queryId = rs.getString(ToolConstants.META_TABLE_SQL_INFO_QUERY_ID);
				String jobId = rs.getString(ToolConstants.META_TABLE_SQL_INFO_JOB_ID);
				SqlInfo sqlInfoProperty = new SqlInfo(databaseName, nodeAddress, nodePort, startTime, applicationName, sql, queryId, jobId);
				props.add(sqlInfoProperty);
			}
		} catch (Exception e) {
			if (searchQueryId == null) {
				throw new GridDBJdbcException("Failed to get sql information.", e);
			} else {
				throw new GridDBJdbcException("Failed to get sql information. query id=["+searchQueryId+"]", e);
			}
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (pstmt != null) {
					pstmt.close();
				}
			} catch (Exception e) {}
		}

		return props;
	}

	/**
	 * 指定されたキー/バリューを設定するpragma文を実行します。
	 * <p>
	 * 例: pragma internal.compiler.meta_table_visible=1
	 *
	 * @param conn
	 * @param key
	 * @param value
	 * @throws GridDBJdbcException
	 */
	public static void executePragma(Connection conn, String key, String value) throws GridDBJdbcException {
		Statement stmt = null;
		String sql = createPragmaStatement(key, value);
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		} catch (Exception e) {
			throw new GridDBJdbcException("Failed to execute internal statement. key=[" + key + "], value=[" + value + "]", e);
		} finally {
			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (Exception e) {}
		}
	}

	private static String createPragmaStatement(String key, String value) {
		StringBuilder builder = new StringBuilder();
		builder.append("pragma ");
		builder.append(key);
		builder.append("=");
		builder.append(value);
		return builder.toString();
	}
}
