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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * GridStoreクラスタを表すクラス。
 * @param <T> generic type
 */
public class GSCluster<T>{
	/**
	 * クラスタ名
	 * <p>
	 * /cluster/clusterName
	 */
	private String name;

	/**
	 * クラスタ構成方式
	 * <p>
	 * /cluster/notificationMode
	 * @see NotificationMode
	 */
	private NotificationMode mode = NotificationMode.MULTICAST;

	/**
	 * マルチキャストアドレス
	 * <p>
	 * /transaction/notificationAddress
	 */
	private String address;

	/**
	 * マルチキャストポート
	 * <p>
	 * /transaction/notificationPort
	 */
	private int port;

	/**
	 * JDBCマルチキャストアドレス
	 * <p>
	 * /sql/notificationAddress
	 */
	private String jdbcAddress;

	/**
	 * JDBCマルチキャストポート
	 * <p>
	 * /sql/notificationPort
	 */
	private int jdbcPort;

	/**
	 * 固定アドレスリスト(transaction)
	 * <p>
	 * /transaction/serviceAddress, /transaction/servicePort
	 * <p>
	 * ex. 172.17.0.13:10001,172.17.0.14:10001,172.17.0.15:10001
	 */
	private String transactionMember;

	/**
	 * 固定アドレスリスト(sql)
	 * <p>
	 * /sql/serviceAddress, /sql/servicePort
	 * <p>
	 * ex. 172.17.0.13:20001,172.17.0.14:20001,172.17.0.15:20001
	 */
	private String sqlMember;

	/**
	 * アドレスプロバイダURL
	 * <p>
	 * /cluster/notificationProvider/url
	 */
	private String providerUrl;

	/** クラスタ変数に定義されているノード群 */
	private List<T> nodes = new ArrayList<T>();

	/** クラスタのステータスなどの稼動情報 */
	private GSClusterStat stat;

	/** System SSL information */
	private boolean systemSSL;
	
	/**
	 * JSONシリアライズ用のダミーコンストラクタ
	 * <p>
	 * http://stackoverflow.com/questions/7625783/jsonmappingexception-no-suitable-constructor-found-for-type-simple-type-class
	 */
	public GSCluster() {

	}

	public GSCluster(String clusterName, String multicastAddr, int port, List<T> nodes) {
		this.name = clusterName;
		this.address = multicastAddr;
		this.port = port;
		this.nodes = new ArrayList<T>(nodes);
	}

	public GSCluster(String clusterName, String multicastAddr, int port, T... nodes) {
		this(clusterName, multicastAddr, port, Arrays.asList(nodes));
	}

	public GSCluster(String clusterName, String jdbcAddr, int jdbcPort ){
		this.name = clusterName;
		this.jdbcAddress = jdbcAddr;
		this.jdbcPort = jdbcPort;
	}

	/** gs_admin リポジトリ用 */
	public GSCluster(String clusterName, String multicastAddr, int port,
						String jdbcAddr, int jdbcPort, List<T> nodes){
		this.name = clusterName;
		this.address = multicastAddr;
		this.port = port;
		this.jdbcAddress = jdbcAddr;
		this.jdbcPort = jdbcPort;
		this.nodes = new ArrayList<T>(nodes);
	}

	/** gs_admin リポジトリ用 */
	public GSCluster(String clusterName, String multicastAddr, int port,
						String jdbcAddr, int jdbcPort, T... nodes){
		this(clusterName, multicastAddr, port, jdbcAddr, jdbcPort, Arrays.asList(nodes));
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public NotificationMode getMode() {
		return mode;
	}
	public void setMode(NotificationMode mode) {
		this.mode = mode;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public String getJdbcAddress() {
		return jdbcAddress;
	}
	public void setJdbcAddress(String jdbcAddress) {
		this.jdbcAddress = jdbcAddress;
	}
	public int getJdbcPort() {
		return jdbcPort;
	}
	public void setJdbcPort(int jdbcPort) {
		this.jdbcPort = jdbcPort;
	}
	public String getTransactionMember() {
		return transactionMember;
	}
	public void setTransactionMember(String transactionMember) {
		this.transactionMember = transactionMember;
	}
	public String getSqlMember() {
		return sqlMember;
	}
	public void setSqlMember(String sqlMember) {
		this.sqlMember = sqlMember;
	}
	public String getProviderUrl() {
		return providerUrl;
	}
	public void setProviderUrl(String providerUrl) {
		this.providerUrl = providerUrl;
	}

	@JsonIgnore
	public List<T> getNodes() {
		return nodes;
	}
	public void setNodes(List<T> nodes){
		this.nodes = nodes;
	}

	@JsonIgnore
	public GSClusterStat getStat(){
		return stat;
	}
	public void setStat(GSClusterStat stat){
		this.stat = stat;
	}

	/**
	 * Set system SSL.
	 * 
	 * @param systemSSL system SSL
	 */
	public void setSystemSSL(boolean systemSSL) {
		this.systemSSL = systemSSL;
	}

	/**
	 * Get system SSL.
	 * 
	 * @return system SSL
	 */
	public boolean getSystemSSL() {
		return systemSSL;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Cluster[name=").append(name);
		if ( address != null ){
			builder.append(",").append(address).append(":").append(port);
		}
		if ( jdbcAddress != null ){
			builder.append(",sql=").append(jdbcAddress).append(":").append(jdbcPort);
		}
		builder.append(",nodes=(");
		for (int i = 0; i < nodes.size(); i++) {
			if (i != 0) {
				builder.append(",");
			}
			//builder.append("$");
			//builder.append(nodes.get(i).getName());
			builder.append(((GSNode) nodes.get(i)).getIdentifier());
		}
		builder.append(")]");
		return builder.toString();
	}


	/**
	 * このクラスタに登録されているノードの中から、指定したノードキーに一致するノードを検索する。
	 * 見つからない場合はnullを返す。
	 *
	 * @param nodeKey
	 * @return
	 */
	public GSNode getNode(NodeKey nodeKey) {
		for (Object obj : nodes) {
			GSNode node = (GSNode)obj;
			if (node.getNodeKey().equals(nodeKey)) {
				return node;
			}
		}
		return null;
	}

	/**
	 * ノード群で同じ定義内容のものがないかチェックします。
	 */
	public void checkNodes() throws GridStoreCommandException {
		List<GSNode> nodeList = new ArrayList<GSNode>(nodes.size());
		//Set<String> nodeNameSet = new HashSet<String>(nodes.size());

		for ( Object obj : nodes ){
			GSNode node = (GSNode)obj;
			//if ( nodeNameSet.contains(node.getName()) ){
			//	throw new GridStoreCommandException("D10301: Duplicate name exists in the node list. name=["+node.getName()+"]");
			//}
			if ( nodeList.contains(node) ){
				throw new GridStoreCommandException("D10302: Duplicate value exists in the node list. node=["+node.getIdentifier()
						+","+nodeList.get(nodeList.indexOf(node)).getIdentifier()+"]");
			}
			//nodeNameSet.add(node.getName());
			nodeList.add(node);
		}
	}

}

