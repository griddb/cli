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
 * イベント一覧情報
 * <p>
 * イベント一覧の各種情報を保持します。
 * <p>
 *
 * @since 4.0
 */
public class EventInfo {
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
	 * サービス
	 */
	private String serviceType;
	/**
	 * 実行中イベント
	 */
	private String eventType;
	/**
	 * ワーカID
	 */
	private String workerId;
	/**
	 * クラスタパーティションID
	 */
	private int clusterPartitionId;

	/**
	 * コンストラクタ
	 *
	 */
	public EventInfo(String nodeAddress, int nodePort,
			Date startTime, String applicationName, String serviceType, String eventType,
			String workerId, int clusterPartitionId) {
		this.nodeAddress = nodeAddress;
		this.nodePort = nodePort;
		this.startTime = startTime;
		this.applicationName = applicationName;
		this.serviceType = serviceType;
		this.eventType = eventType;
		this.workerId = workerId;
		this.clusterPartitionId = clusterPartitionId;
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
	 * サービスを返します。
	 * @return サービス
	 */
	public String getServiceType() {
		return serviceType;
	}

	/**
	 * サービスを設定します。
	 * @param serviceType サービス
	 */
	public void setServiceType(String serviceType) {
		this.serviceType = serviceType;
	}

	/**
	 * 実行中イベントを返します。
	 * @return 実行中イベント
	 */
	public String getEventType() {
		return eventType;
	}

	/**
	 * 実行中イベントを設定します。
	 * @param eventType 実行中イベント
	 */
	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	/**
	 * ワーカIDを返します。
	 * @return ワーカID
	 */
	public String getWorkerId() {
		return workerId;
	}

	/**
	 * ワーカIDを設定します。
	 * @param workerId ワーカID
	 */
	public void setWorkerId(String workerId) {
		this.workerId = workerId;
	}

	/**
	 * クラスタパーティションIDを返します。
	 * @return クラスタパーティションID
	 */
	public int getClusterPartitionId() {
		return clusterPartitionId;
	}

	/**
	 * クラスタパーティションIDを設定します。
	 * @param clusterPartitionId クラスタパーティションID
	 */
	public void setClusterPartitionId(int clusterPartitionId) {
		this.clusterPartitionId = clusterPartitionId;
	}



}
