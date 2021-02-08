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
 * コネクション一覧情報
 * <p>
 * コネクション一覧の各種情報を保持します。
 * <p>
 *
 * @since 4.0
 */
public class ConnectionInfo {

	/**
	 * サービス種別
	 */
	private String serviceType;
	/**
	 * ソケット種別
	 */
	private String socketType;
	/**
	 * 処理実行しているノードアドレス
	 */
	private String nodeAddress;
	/**
	 * 処理実行しているノードポート
	 */
	private int nodePort;
	/**
	 * 接続先アドレス
	 */
	private String remoteAddress;
	/**
	 * 接続先ポート
	 */
	private int remotePort;
	/**
	 * アプリケーション名
	 */
	private String applicationName;
	/**
	 * ソケット生成時刻
	 */
	private Date creationTime;
	/**
	 * イベントハンドリングの要求を開始した総回数
	 */
	private long dispatchingEventCount;
	/**
	 * イベント送信を開始した総回数
	 */
	private long sendingEventCount;


	public ConnectionInfo(String serviceType, String socketType, String nodeAddress, int nodePort,
			String remoteAddress, int remotePort, String applicationName, Date creationTime,
			long dispatchingEventCount, long sendingEventCount) {
		this.serviceType = serviceType;
		this.socketType = socketType;
		this.nodeAddress = nodeAddress;
		this.nodePort = nodePort;
		this.remoteAddress = remoteAddress;
		this.remotePort = remotePort;
		this.applicationName = applicationName;
		this.creationTime = creationTime;
		this.dispatchingEventCount = dispatchingEventCount;
		this.sendingEventCount = sendingEventCount;
	}

	/**
	 * サービス種別を返します。
	 * @return サービス種別
	 */
	public String getServiceType() {
		return serviceType;
	}
	/**
	 * サービス種別を設定します。
	 * @param serviceType サービス種別
	 */
	public void setServiceType(String serviceType) {
		this.serviceType = serviceType;
	}
	/**
	 * ソケット種別を返します。
	 * @return ソケット種別
	 */
	public String getSocketType() {
		return socketType;
	}
	/**
	 * ソケット種別を設定します。
	 * @param socketType ソケット種別
	 */
	public void setSocketType(String socketType) {
		this.socketType = socketType;
	}
	/**
	 * 接続元アドレスを返します。
	 * @return 接続元アドレス
	 */
	public String getNodeAddress() {
		return nodeAddress;
	}
	/**
	 * 接続元アドレスを設定します。
	 * @param nodeAddress 接続元アドレス
	 */
	public void setNodeAddress(String nodeAddress) {
		this.nodeAddress = nodeAddress;
	}
	/**
	 * 接続元ポートを返します。
	 * @return 接続元ポート
	 */
	public int getNodePort() {
		return nodePort;
	}
	/**
	 * 接続元ポートを設定します。
	 * @param nodePort 接続元ポート
	 */
	public void setNodePort(int nodePort) {
		this.nodePort = nodePort;
	}
	/**
	 * 接続先アドレスを返します。
	 * @return 接続先アドレス
	 */
	public String getRemoteAddress() {
		return remoteAddress;
	}
	/**
	 * 接続先アドレスを設定します。
	 * @param remoteAddress 接続先アドレス
	 */
	public void setRemoteAddress(String remoteAddress) {
		this.remoteAddress = remoteAddress;
	}
	/**
	 * 接続先ポートを返します。
	 * @return 接続先ポート
	 */
	public int getRemotePort() {
		return remotePort;
	}
	/**
	 * 接続先ポートを設定します。
	 * @param remotePort 接続先ポート
	 */
	public void setRemotePort(int remotePort) {
		this.remotePort = remotePort;
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
	 * ソケット生成時刻を返します。
	 * @return ソケット生成時刻
	 */
	public Date getCreationTime() {
		return creationTime;
	}
	/**
	 * ソケット生成時刻を設定します。
	 * @param creationTime ソケット生成時刻
	 */
	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}
	/**
	 * イベントハンドリングの要求を開始した総回数を返します。
	 * @return イベントハンドリングの要求を開始した総回数
	 */
	public long getDispatchingEventCount() {
		return dispatchingEventCount;
	}
	/**
	 * イベントハンドリングの要求を開始した総回数を設定します。
	 * @param dispatchingEventCount イベントハンドリングの要求を開始した総回数
	 */
	public void setDispatchingEventCount(long dispatchingEventCount) {
		this.dispatchingEventCount = dispatchingEventCount;
	}
	/**
	 * イベント送信を開始した総回数を返します。
	 * @return イベント送信を開始した総回数
	 */
	public long getSendingEventCount() {
		return sendingEventCount;
	}
	/**
	 * イベント送信を開始した総回数を設定します。
	 * @param sendingEventCount イベント送信を開始した総回数
	 */
	public void setSendingEventCount(long sendingEventCount) {
		this.sendingEventCount = sendingEventCount;
	}

}
