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

import java.util.ArrayList;
import java.util.List;

/**
 * SQL処理一覧情報のうちクエリの情報とジョブの情報の紐付けを管理します。
 *
 * @since 4.0
 */
public class SqlInfoBinder {

	/**
	 * クエリの情報
	 */
	private SqlInfo querySqlInfo;
	/**
	 * ジョブの情報のリスト
	 */
	private List<SqlInfo> jobSqlInfoList = new ArrayList<SqlInfo>();

	/**
	 * クエリの情報を返します。
	 * @return クエリの情報
	 */
	public SqlInfo getQuerySqlInfo() {
		return querySqlInfo;
	}
	/**
	 * クエリの情報を設定します。
	 * @param querySqlInfo クエリの情報
	 */
	public void setQuerySqlInfo(SqlInfo querySqlInfo) {
		this.querySqlInfo = querySqlInfo;
	}
	/**
	 * ジョブの情報のリストを返します。
	 * @return ジョブの情報のリスト
	 */
	public List<SqlInfo> getJobSqlInfoList() {
		return jobSqlInfoList;
	}
	/**
	 * ジョブの情報のリストを設定します。
	 * @param jobSqlInfoList ジョブの情報のリスト
	 */
	public void setJobSqlInfoList(List<SqlInfo> jobSqlInfoList) {
		this.jobSqlInfoList = jobSqlInfoList;
	}





}
