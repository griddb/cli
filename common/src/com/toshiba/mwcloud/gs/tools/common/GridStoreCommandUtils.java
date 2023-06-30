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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toshiba.mwcloud.gs.tools.common.GridStoreRemoteCommandUtils.RemoteCommandResult;
import com.toshiba.mwcloud.gs.tools.common.GridStoreWebAPI.AddressType;
import com.toshiba.mwcloud.gs.tools.common.GridStoreWebAPI.PartitionInfo;

/**
 * Call wrapped WebApi implemented by GridStoreWebAPI class
 *
 */
public class GridStoreCommandUtils {
	/**
	 * Start the node (gsserver).<br>
	 * If the node is already running, do nothing.
	 *
	 * @param node node variable
	 * @param userId GirdDB user name
	 * @param password password of GridDB user
	 * @param osPassword password of OS user 'gsadm'
	 * @return Monitoring object for waiting until recovery is completed (STARTED)
	 * @throws GridStoreCommandException When an unexpected error occurs
	 */
	public static Watcher startNode(final GSNode node, final String userId, final String password, String osPassword)
			throws GridStoreCommandException {
		return startNode(node, userId, password, osPassword, -1);
	}
	/**
	 * Start the node (gsserver).<br>
	 * If the node is already running, do nothing.<br>
	 *
	 * (Because startnode has to wait on the machine that executed startnode when waiting for startup <br> 
	 * (process confirmation with PID is required), wait with the gs_startnode waiting option -w)
	 *
	 * @param node node variable
	 * @param userId GirdDB user name
	 * @param password password of GridDB user
	 * @param osPassword password of OS user 'gsadm'
	 * @param waitTime -1 if no waiting,  0 if permanently waiting,  1 or greater when waiting for a specified time
	 * @return Monitoring object for waiting until recovery is completed (STARTED)
	 * @throws GridStoreCommandException when SSH connection error or it failed to start node
	 * @throws IllegalStateException node status is invalid
	 */
	public static Watcher startNode(final GSNode node, final String userId, final String password, String osPassword, int waitTime)
			throws GridStoreCommandException {
		final GridStoreWebAPI webapi = new GridStoreWebAPI(node, userId, password);
		// 事前条件チェック
		CombinedStatus status = null;
		status = getCombinedStatus(webapi);
		switch (status) {
		case STOPPED:
			// startnodeを実行する
			break;
		case STARTING:
			// 起動中なので起動待ち (waitTimeは指定できない)
			return new StatusWatcher(webapi, CombinedStatus.STARTED);
		case STARTED:
		case WAIT:
		case SERVICING:
			// 既にstart済み
			return NullWatcher.INSTANCE;

		case STOPPING:
		case ABNORMAL:
		default:
			throw new IllegalStateException("D10101: Node status is invalid. (status=" + status + ")");
		}

		String command = "gs_startnode -u "+userId+"/"+password;
		if ( waitTime > -1 ){
			command += " -w "+waitTime;
		}
		// gs_shで定義されているポートと、起動対象のGridStoreのポートが一致するかを確認する
        command += " --checkPort " + node.getNodeKey().getPort();
		RemoteCommandResult commandResult = GridStoreRemoteCommandUtils.executeRemoteCommand(
				node, osPassword, command);
		if (commandResult.getExitStatus() != 0) {
			throw new GridStoreCommandException("D10102: Failed to start " + node +
					". (status=" + commandResult.getExitStatus() +
					",messages=" + commandResult.getStdout() + commandResult.getStderr() + ")");
		}

		return new StatusWatcher(webapi, CombinedStatus.STARTED);
	}

	/**
	 * Stop the node.<br>
	 *
	 * @param node node variable
	 * @param userId GirdDB user name
	 * @param password password of GridDB user
	 * @param force {@code true} if force to stop node, otherwise {@code false}
	 * @return Monitoring object for waiting until the node stops (STOP)
	 * @throws GridStoreCommandException when it failed to stop node
	 * @throws IllegalStateException if it meets 1 of the below conditions:
	 * <ul>
	 * <li>The node of status 'STARTING' cannot stop</li>
	 * <li>The node joined cluster cannot stop</li>
	 * <li>Node status is invalid when {@code force} is {@code false}</li>
	 * </ul>
	 */
	public static Watcher stopNode(final GSNode node, String userId, String password, boolean force)
			throws GridStoreCommandException {
		final GridStoreWebAPI webapi = new GridStoreWebAPI(node, userId, password);

		// 事前条件チェック
		CombinedStatus status = getCombinedStatus(webapi);
		switch (status) {
			case STOPPING:
				if (force) {
					// forceが指定されていたら再度REST(force)を実行する
					break;
				} else {
					// 停止処理中なので停止を待つ
					return new StatusWatcher(webapi, CombinedStatus.STOPPED);
				}
			case STOPPED:
				// 既にstop済み
				return NullWatcher.INSTANCE;

			case STARTING:
				// 起動処理中は停止できない
				throw new IllegalStateException("D10103: The node of status 'STARTING' cannot stop. (node="+node+")");

			case STARTED:
				// stopnodeを実行する
				break;

			case SERVICING:
			case WAIT:
				if (!force) {
					// クラスタ参加中は停止できない
					throw new IllegalStateException("D10104: The node joined cluster cannot stop. (node="+node+", status=" + status + ")");
				}
				break;
			case ABNORMAL:
			default:
				if (!force) {
					throw new IllegalStateException("D10105: Node status is invalid. (node="+node+", status=" + status + ")");
				}
		}

		// リクエスト発行
		try {
			webapi.postNodeShutdown(force);
		} catch (GridStoreWebAPIException e) {
			throw new GridStoreCommandException("D10106: Failed to stop node. msg=["+e.getMessage()+"]", e);
		}

		return new StatusWatcher(webapi, CombinedStatus.STOPPED);
	}

	/**
	 * Specify the cluster name and the number of constituent nodes, and join the node to the cluster.<br>
	 *
	 * @param clusterName cluster name
	 * @param designatedCount Number of configuration nodes
	 * @param node node variable
	 * @param userId GridDB user name
	 * @param password password of GridDB user
	 * @return Monitoring object for waiting until the node is in cluster operation (SERVICING, data operation can be performed)
	 * @throws GridStoreCommandException if it's unable to join cluster
	 * @throws IllegalStateException if the status of node is not 'STARTED'
	 */
	private static Watcher joinCluster(String clusterName, int designatedCount, GSNode node, String userId, String password, boolean waitServicing) throws GridStoreCommandException {
		final GridStoreWebAPI webapi = new GridStoreWebAPI(node, userId, password);

		// 事前条件チェック
		CombinedStatus status = getCombinedStatus(webapi);
		switch(status){
		case STARTED:
			// joinClusterを実行する
			break;

		case WAIT:
		case SERVICING:
			// 既にjoin済みの場合
			return NullWatcher.INSTANCE;

		case STOPPED:
		case STARTING:
		case STOPPING:
		case ABNORMAL:
		default:
			throw new IllegalStateException("D10107: The status of node must be \"STARTED\". : status=["+status+"]");
		}

		// リクエスト発行
		try {
			webapi.postNodeJoin(clusterName, designatedCount);
		} catch (GridStoreWebAPIException e) {
			throw new GridStoreCommandException("D10108: An error occurred while joining to cluster. msg=["+e.getMessage()+"]", e);
		}

		if ( waitServicing ){
			return new StatusWatcher(webapi, CombinedStatus.SERVICING);
		} else {
			return new StatusWatcher(webapi, CombinedStatus.SERVICING, CombinedStatus.WAIT);
		}
	}

	/**
	 * Join the node to the cluster by specifying the cluster definition.<br>
	 * 
	 * @param <T> Generic type
	 * @param cluster Cluster variable
	 * @param node node variable
	 * @param userId GridDB user name
	 * @param password password of GridDB user
	 * @return Monitoring object to wait until the node is in cluster operation (data operations can be performed)
	 * @throws GridStoreCommandException when it's unable to join cluster
	 * @throws IllegalStateException if it meets 1 of the below conditions:
	 * <ul>
	 * <li>Node is not in cluster definition</li>
	 * <li>Current cluster configuration is mismatched with cluster definition</li>
	 * <li>Node status is not 'STARTED'</li>
	 * </ul>
	 */
	public static <T> Watcher joinCluster(GSCluster<T> cluster, GSNode node, String userId, String password)
			throws GridStoreCommandException {
		if (cluster.getNode(node.getNodeKey()) == null) {
			throw new IllegalStateException("D10109: Node \""+node.getIdentifier()+"\" is not in cluster definition.");
		}
		// クラスタの状態を取得
		if (node.getSystemSSL()) {
			cluster.setSystemSSL(true);
		}
		if ( !getStatCluster(cluster, userId, password) ){
			throw new GridStoreCommandException("D10148: Current cluster configuration is mismatched with cluster definition.");
		}

		int designatedCount = cluster.getNodes().size();
		int serviceCount = cluster.getStat().getServiceNodeCount();
		int waitCount = cluster.getStat().getWaitNodeCount();

		boolean waitService = false;
		switch ( cluster.getStat().getClusterStatus() ){
		case SERVICE_UNSTABLE:
			waitService = true;
			break;
		case INIT_WAIT:
			if ( (designatedCount-1) == (waitCount+serviceCount) ){
				waitService = true;
			}
			break;
		case WAIT:
			if ( (designatedCount/2) <= (waitCount+serviceCount) ){
				waitService = true;
			}
			break;
		default:
			break;
		}

		// joinClusterを実行
		return joinCluster(cluster.getName(), designatedCount, node, userId, password, waitService);

	}

	/**
	 * Leave the node from the cluster.<br>
	 *
	 * @param node node variable
	 * @param userId GridDB user name
	 * @param password password of GridDB user
	 * @param force force to leave cluster or not
	 * @return Monitoring object to wait until the node has left the cluster (STARTED)
	 * @throws GridStoreCommandException when it failed to check node status (in case {@code force} is {@code false}) or unable to leave cluster
	 * @throws IllegalStateException if node status is invalid (neither 'SERVICING' nor 'WAIT')
	 */
	public static Watcher leaveCluster(GSNode node, String userId, String password, boolean force)
			throws GridStoreCommandException {
		final GridStoreWebAPI webapi = new GridStoreWebAPI(node, userId, password);

		// 事前条件チェック
		CombinedStatus status = getCombinedStatus(webapi);
		switch(status){
		case WAIT:
		case SERVICING:
			// leaveClusterを実行する
			break;

		case STARTED:
		case STOPPED:
		case STARTING:
		case STOPPING:
			// 既にleave済みの場合
			return NullWatcher.INSTANCE;

		case ABNORMAL:
		default:
			throw new IllegalStateException("D10140: Node status is invalid. The status must be 'SERVICING' or 'WAIT'. : status=[" + status + "]");
		}

		// forceチェック（クリティカルノードチェック）
		if ( !force ){
			try {
				// 自分のGridStore上でのアドレスを取得する
				JsonNode stats = webapi.getNodeStat();
				String address = stats.path("cluster").path("nodeList").get(0).path("address").textValue();
				int port = stats.path("cluster").path("nodeList").get(0).path("port").asInt();

				// パーティション情報を取得する
				PartitionInfo[] pInfoList = webapi.getNodePartition();
				for ( PartitionInfo pInfo : pInfoList ){
					if ( pInfo.owner != null ){
						if ( (pInfo.backup.length == 0) && address.equals(pInfo.owner.address) &&
								( port == pInfo.owner.port) ){

							// ownerが存在して、backupが存在しない場合はエラー
							throw new GridStoreCommandException("D10141: Some data in this node will be unavailable. Please use leave force command to forcibly stop.");
						}
					} else {
						// ownerが存在せずに、backupが存在する場合はエラー
						if ( pInfo.backup.length != 0 ){
							for ( GridStoreWebAPI.NodeKeyPartition key : pInfo.backup ){
								if ( address.equals(key.address) && (port == key.port) ){
									throw new GridStoreCommandException("D10142: Some data in this node will be unavailable. Please use leave force command to forcibly stop.");
								}
							}
						}
					}
				}

			} catch (GridStoreWebAPIException e) {
				throw new GridStoreCommandException("D10143: Failed to check node status. msg=["+e.getMessage()+"]", e);
			}
		}


		// リクエスト発行
		try {
			webapi.postNodeLeave();
		} catch (GridStoreWebAPIException e) {
			throw new GridStoreCommandException("D10112: An error occurred while leaving from cluster. msg=["+e.getMessage()+"]", e);
		}

		return new StatusWatcher(webapi, CombinedStatus.STARTED);
	}

	/**
	 * Specify the master node of the running cluster and add nodes to the cluster.<br>
	 * If queuing is enabled, wait until the node is in cluster operation (data operations can be performed).
	 * 
	 * @param masterNode GridDB master node
	 * @param node GridDB node to append
	 * @param userId GriDB user name
	 * @param password GridDB password
	 * @return Monitoring object for waiting until the cluster starts
	 * @throws GridStoreCommandException if it's unable to append node to the cluster
	 * @throws IllegalStateException if it meets 1 of the below conditions:
	 * <ul>
	 * <li>Master node status is not SERVICING</li>
	 * <li>Cluster is unstable (designated node is different from active node)</li>
	 * <li>Single node cluster</li>
	 * <li>Node status is not 'STARTED'</li>
	 * </ul>
	 */
	public static Watcher appendCluster(GSNode masterNode, GSNode node, String userId, String password)
			throws GridStoreCommandException {
		final GridStoreWebAPI masterApi = new GridStoreWebAPI(masterNode, userId, password);
		final GridStoreWebAPI nodeApi = new GridStoreWebAPI(node, userId, password);

		// クラスタ稼働中かどうかを確認
		CombinedStatus masterStatus = getCombinedStatus(masterApi);
		if (masterStatus != CombinedStatus.SERVICING) {
			throw new IllegalStateException("D10113: Master node status must be SERVICING.");
		}

		// 構成ノード数と参加ノード数の確認
		JsonNode stats = getStat(masterNode, userId, password);
		int activeCount = getActiveCount(stats);
		int desinatedCount = getDesinatedCount(stats);
		if (activeCount != desinatedCount) {
			throw new IllegalStateException("D10114: Cluster is unstable. (designatedCount=" +
											desinatedCount + ", activeCount=" + activeCount + ")");
		}
		if (desinatedCount == 1) {
			throw new IllegalStateException("D10115: Single node cluster is not expandable.");
		}

		// 増設ノードがクラスタ未参加かどうかを確認
		CombinedStatus status = getCombinedStatus(nodeApi);
		if (status != CombinedStatus.STARTED) {
			throw new IllegalStateException("D10116: Status of the node must be 'STARTED'. : status=["+status+"]");
		}

		try {
			// クラスタにノードをjoinする
			String clusterName = getClusterName(stats);
			nodeApi.postNodeJoin(clusterName, 0);
			// クラスタの構成ノード数を増やす
			masterApi.postClusterIncrease();
		} catch (GridStoreWebAPIException e) {
			throw new GridStoreCommandException("D10117: An error occurred while appending cluster. : msg=["+e.getMessage()+"]", e);
		}

		return new StatusWatcher(nodeApi, CombinedStatus.SERVICING);
	}

	/**
	 * Specify a running cluster and add nodes to the cluster.<br>
	 * If queuing is enabled, wait until the node is in cluster operation (data operations can be performed).
	 * 
	 * @param <T> generic type
	 * @param cluster GridDB cluster variable  
	 * @param node GridDB node variable
	 * @param userId GridDB user name
	 * @param password GridDB password
	 * @return Monitoring object for waiting until the cluster starts
	 * @throws GridStoreCommandException if it meets 1 of the below conditions:
	 * <ul>
	 * <li>Current cluster configuration is mismatched with cluster definition</li>
	 * <li>All nodes in the cluster are not 'SERVICING'</li>
	 * </ul>
	 * @throws IllegalStateException if it meets 1 of the below conditions:
	 * <ul>
	 * <li>Node is already in cluster definition</li>
	 * <li>All nodes in the cluster are stopped</li>
	 * <li>Master node status is not SERVICING</li>
	 * <li>Cluster is unstable (designated node is different from active node)</li>
	 * <li>Single node cluster</li>
	 * <li>Node status is not 'STARTED'</li>
	 * </ul>
	 */
	public static <T> Watcher appendCluster(GSCluster<T> cluster, GSNode node, String userId, String password)
			throws GridStoreCommandException {
		if (cluster.getNode(node.getNodeKey()) != null) {
			throw new IllegalStateException("D10118: Node is a part of cluster definition.");
		}

		if (node.getSystemSSL()) {
			cluster.setSystemSSL(true);
		}

		if ( !getStatCluster(cluster, userId, password) ){
			throw new GridStoreCommandException("D10150: Current cluster configuration is mismatched with cluster definition.");
		}

		if (cluster.getStat().getClusterStatus() != ClusterStatus.SERVICE_STABLE ){
			throw new GridStoreCommandException("D10151: All nodes in the cluster must be 'SERVICING'.");
		}
		
		GSNode masterNode = cluster.getStat().getMasterNode();
		if (node.getSystemSSL()) {
			masterNode.setSystemSSL(true);
		}
		if (masterNode == null) {
			throw new IllegalStateException("D10119: All nodes in the cluster are stopped.");
		}

		return appendCluster(masterNode, node, userId, password);
	}

	/**
	 * Join all the nodes included in the cluster definition to the cluster and start the cluster.<br>
	 * 
	 * @param <T> generic type
	 * @param cluster cluster variable
	 * @param userId GridDB user name
	 * @param password GridDB password
	 * @return Monitoring object for waiting until the cluster starts
	 * @throws GridStoreCommandException if it meets 1 of the below conditions:
	 * <ul>
	 * <li>Current cluster configuration is mismatched with cluster definition</li>
	 * <li>There is not enough number of STARTED/WAIT nodes to start cluster</li>
	 * <li>It's unable to join cluster</li>
	 * </ul>
	 * @throws IllegalArgumentException if there is no node in cluster definition
	 */
	public static <T> List<Watcher> startCluster(GSCluster<T> cluster, String userId, String password)
			throws GridStoreCommandException {

		// クラスタ変数で定義されている構成ノード数を取得
		int designatedCount = cluster.getNodes().size();
		if (designatedCount == 0) {
			throw new IllegalArgumentException("D10124: There is no node in cluster definition '$"+cluster.getName()+"'.");
		}

		// 各ノードの状態を取得
		if ( !getStatCluster(cluster, userId, password) ){
			throw new GridStoreCommandException("D10146: Current cluster configuration is mismatched with cluster definition.");
		}

		List<Watcher> watchers = new ArrayList<Watcher>();
		if ( cluster.getStat().getClusterStatus() == ClusterStatus.SERVICE_STABLE ){
			watchers.add(NullWatcher.INSTANCE);
			return watchers;
		}

		// 各ノードのステータスをチェック
		List<GSNode> targetList = new ArrayList<GSNode>();
		int countJoin = 0;
		for (Object obj : cluster.getNodes()) {
			GSNode node = (GSNode)obj;
			CombinedStatus status = node.getStat().getCombinedStatus();
			switch(status){
			case WAIT:
			case SERVICING:		// 既にクラスタ参加済み
				countJoin++;
				break;
			case STARTED:		// joinclusterを実行する
				targetList.add(node);
				break;
			case STOPPED:
			case STARTING:
			case STOPPING:
			case ABNORMAL:
			default:			// ステータスが適さないので対象から除く
				break;
			}
		}

		// startclusterが実行可能かチェック
		int requiredCount = 0;
		if ( (cluster.getStat().getClusterStatus() == ClusterStatus.STOP) || (cluster.getStat().getClusterStatus() == ClusterStatus.INIT_WAIT)){
			requiredCount = designatedCount;
		} else {
			requiredCount = designatedCount/2 + 1;
		}
		if ( (countJoin + targetList.size()) < requiredCount ){
			// SERVICINGになるのに必要な台数を満たしていない
			throw new GridStoreCommandException("D10147: There is not enough number of STARTED/WAIT nodes to start cluster. : requiredNodeNum="+requiredCount);
		}

		String clusterName = cluster.getName();

		// リクエスト発行
		Watcher watcher = NullWatcher.INSTANCE;
		for (GSNode node : targetList) {
			try {
				if(cluster.getSystemSSL()) {
					node.setSystemSSL(true);
				}
				watcher = GridStoreCommandUtils.joinCluster(clusterName, designatedCount, node, userId, password, true);
				watchers.add(watcher);
			} catch (GridStoreCommandException e) {
				throw new GridStoreCommandException("D10125: An error occurred while starting cluster. : msg=["+e.getMessage()+"]", e);
			}
		}

		return watchers;
	}

	/**
	 * Specify the master node of the running cluster and stop the cluster (leave all nodes)
	 * 
	 * @param masterNode GridDB master node
	 * @param userId GridDB user name
	 * @param password GriDDB password
	 * @return Monitoring object for waiting until the cluster stops
	 * @throws IllegalStateException if master node status is not 'SERVICING'
	 * @throws GridStoreCommandException if it's unable to stop cluster
	 */
	public static Watcher stopCluster(final GSNode masterNode, String userId, String password)
			throws GridStoreCommandException {
		final GridStoreWebAPI masterApi = new GridStoreWebAPI(masterNode, userId, password);
		// クラスタ稼働中かどうかを確認
		CombinedStatus status = getCombinedStatus(masterApi);
		if (status != CombinedStatus.SERVICING) {
			throw new IllegalStateException("D10126: Master node status must be 'SERVICING'. (master="+masterNode+", status="+status+")");
		}

		// リクエスト発行
		try {
			masterApi.postClusterStop();
		} catch (GridStoreWebAPIException e) {
			throw new GridStoreCommandException("D10127:An error occurred while stopping cluster. : msg=["+e.getMessage()+"]", e);
		}

		return new StatusWatcher(masterApi, CombinedStatus.STARTED);
	}

	/**
	 * Specify a running cluster and stop the cluster (leave all nodes).<br>
	 * If queuing is enabled, wait until the node is in cluster operation (data operations can be performed).
	 * 
	 * @param <T> generic type
	 * @param cluster cluster variable
	 * @param userId GridDB user name
	 * @param password GridDB password
	 * @return a list of monitoring object for waiting until the cluster stops
	 * @throws GridStoreCommandException if current cluster configuration is mismatched with cluster definition or it's unable to stop cluster
	 */
	public static <T> List<Watcher> stopCluster(GSCluster<T> cluster, String userId, String password)
			throws GridStoreCommandException {

		List<Watcher> watcherList = new ArrayList<Watcher>();

		//GSNode masterNode = findMasterNode(cluster, userId, password);
		if ( !getStatCluster(cluster, userId, password) ){
			throw new GridStoreCommandException("D10149: Current cluster configuration is mismatched with cluster definition.");
		}
		GSNode masterNode = cluster.getStat().getMasterNode();
		if (masterNode == null) {
			// マスタが存在しなかったらleaveClusterする
			for ( T obj : cluster.getNodes() ){		 
				GSNode node = (GSNode)obj;
				if (cluster.getSystemSSL()) {
					node.setSystemSSL(true);
				}
				GridStoreWebAPI nodeApi = new GridStoreWebAPI(node, userId, password);
				CombinedStatus status = getCombinedStatus(nodeApi);
				if ( (status == CombinedStatus.SERVICING) || (status == CombinedStatus.WAIT) ){
					try {
						nodeApi.postNodeLeave();
					} catch (GridStoreWebAPIException e) {
						throw new GridStoreCommandException("D10144:An error occurred while stopping cluster. : msg=["+e.getMessage()+"]", e);
					}
					watcherList.add(new StatusWatcher(nodeApi, CombinedStatus.STARTED));
				}
			}

		} else {
			// マスタが存在すれば、stopClusterを実行
			if (cluster.getSystemSSL()) {
				masterNode.setSystemSSL(true);
			}
			final GridStoreWebAPI masterApi = new GridStoreWebAPI(masterNode, userId, password);
			try {
				masterApi.postClusterStop();
			} catch (GridStoreWebAPIException e) {
				throw new GridStoreCommandException("D10145:An error occurred while stopping cluster. : msg=["+e.getMessage()+"]", e);
			}
			watcherList.add(new StatusWatcher(masterApi, CombinedStatus.STARTED));
		}

		return watcherList;
	}

	/**
	 * Get status information.
	 *
	 * @param node node variable
	 * @param userId GridDB user name
	 * @param password password of GridDB user
	 * @return Status information
	 * @throws GridStoreCommandException if it's unable to get status
	 */
	public static JsonNode getStat(GSNode node, String userId, String password) throws GridStoreCommandException {
		try {
			return new GridStoreWebAPI(node, userId, password).getNodeStat();
		} catch (GridStoreWebAPIException e) {
			throw new GridStoreCommandException("D10129: An error occurred while getting status info. : msg=["+e.getMessage()+"]", e);
		}
	}



	/**
	 * Get the cluster status
	 * @param <T> generic type
	 *
	 * @param cluster Cluster object
	 * @param userId GridDB user name
	 * @param password password of GridDB user
	 * @return Whether the actual operating configuration and the cluster variable definitions match
	 * @throws GridStoreCommandException if it's unable to get cluster status
	 */
	public static <T> boolean getStatCluster(GSCluster<T> cluster, final String userId, final String password) throws GridStoreCommandException{
		GSClusterStat clStat = new GSClusterStat();
		cluster.setStat(clStat);

		// 各ノードのステータスを取得する  (並列)
		List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
		if ( cluster.getNodes().size() == 0 ){
			// gs_adminから1台でappendされたときの回避用
			return true;
		}
		if ( cluster.getNodes().size() == 1 ){
			GSNode node = (GSNode) cluster.getNodes().get(0);
			if (cluster.getSystemSSL()) {
				node.setSystemSSL(true);
			}
			final GSNode nodeFinal = node;	
			GridStoreCommandUtils.getNodeStat(nodeFinal, userId, password);
		} else {
			int threadCount = THREAD_COUNT;
			if ( cluster.getNodes().size() < THREAD_COUNT ){
				threadCount = cluster.getNodes().size();
			}
			ExecutorService pool = Executors.newFixedThreadPool(threadCount);
			try {
				for (final T obj : cluster.getNodes()) {
					GSNode node = (GSNode) obj;
					if (cluster.getSystemSSL()) {
						node.setSystemSSL(true);
					}
					final GSNode nodeFinal = node;	
					futures.add(pool.submit(new Callable<Boolean>(){
						@Override
						public Boolean call() throws Exception {
							try {
								GridStoreCommandUtils.getNodeStat(nodeFinal, userId, password);
								return true;
							} catch (Exception e) {
								throw e;
							}
						}
					}));
				}
				pool.shutdown();
				pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				pool.shutdownNow();
			}
		}

		String errMessage = "";
		for (Future<Boolean> future : futures) {
			try {
				future.get();
			} catch (InterruptedException e) {
				assert false; // 既に全タスク完了しているので待ちは発生せず、割り込みもないはず。
			} catch (ExecutionException e) { // 処理中に例外が発生した場合
				if ( e.getCause() != null ){
					errMessage += e.getCause().getMessage() +"\n";
				} else {
					errMessage += e.getMessage() +"\n";
				}
			}
		}
		if ( !errMessage.isEmpty() ){
			throw new GridStoreCommandException(errMessage);
		}

		int countWait = 0;
		int countService = 0;
		boolean result = true;
		boolean initFlag = true;
		// Get list of node in the cluster as IP Address (get from configuration file)
		List<NodeKey> nodesIPAddress = getNodesWithIPAddress(cluster.getNodes());

		// ノードごとのステータス数などの計算
		for ( T obj : cluster.getNodes() ){
			GSNode node = (GSNode)obj;
			GSNodeStat stat = node.getStat();

			// クラスタ参加済みの場合、実体とクラス変数の定義が一致していることを確認する
			if ((stat.getCombinedStatus()==CombinedStatus.WAIT) || (stat.getCombinedStatus()==CombinedStatus.SERVICING)){
				if ( cluster.getNodes().size() != stat.getDesignatedCount() ){
					result = false;
					continue;
				}
				if ( !cluster.getName().equals(stat.getClusterName()) ){
					result = false;
					continue;
				}
				if (stat.getCombinedStatus()==CombinedStatus.SERVICING){
					countService++;
				} else if (stat.getCombinedStatus()==CombinedStatus.WAIT){
					countWait++;
				}
				if ( !stat.getInitClusterFlag() ){
					initFlag = false;
				}
			}

			// MASTERが存在する場合、実際の稼動ノードとクラスタ変数の定義が一致するか確認する
			if ( (stat.getNodeRole() == NodeRole.MASTER) || (stat.getNodeRole() == NodeRole.SUB_MASTER)){
				JsonNode nodeHostJson = getConfig(node, userId, password);
				JsonNode followers = nodeHostJson.path("follower");
				NodeKey[] followerKeys = new ObjectMapper()
					.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
					.convertValue(followers, NodeKey[].class);

				for ( NodeKey nkey : followerKeys ){
					// Check node information get from API "/node/host" with node information get from configuration file
					if (!nodesIPAddress.contains(nkey)) {
						// 定義には含まれていない
						List<GSNode> undefNodes = clStat.getUndefinedNodes();
						if ( undefNodes == null ){
							undefNodes = new ArrayList<GSNode>();
							clStat.setUndefinedNodes(undefNodes);
						}
						GSNode undefNode = new GSNode(nkey, GSNode.DEFAULT_SSH_PORT);
						if (cluster.getSystemSSL()) {
							undefNode.setSystemSSL(true);
						}
						GridStoreCommandUtils.getNodeStat(undefNode, userId, password);
						undefNodes.add(undefNode);
						if ( undefNode.getStat().getCombinedStatus() == CombinedStatus.SERVICING ){
							countService++;
						} else if (undefNode.getStat().getCombinedStatus() == CombinedStatus.WAIT){
							countWait++;
						}
						result = false;
					}
				}
			}

			if ( stat.getNodeRole() == NodeRole.MASTER ){
				clStat.setMasterNode(node);
			}
		}
		clStat.setServiceNodeCount(countService);
		clStat.setWaitCount(countWait);

		// クラスタステータス
		if ( cluster.getNodes().size() == countService ){
			clStat.setClusterStatus(ClusterStatus.SERVICE_STABLE);
		} else if ( !initFlag && (countService > (cluster.getNodes().size()/2)) ){
			clStat.setClusterStatus(ClusterStatus.SERVICE_UNSTABLE);
		} else if ( (countService > 0) || (countWait > 0) ){
			if ( initFlag ){
				clStat.setClusterStatus(ClusterStatus.INIT_WAIT);
			} else {
				clStat.setClusterStatus(ClusterStatus.WAIT);
			}
		} else {
			clStat.setClusterStatus(ClusterStatus.STOP);
		}

		return result;
	}

	/**
	 * Convert the address of NodeKey from host name to IPAddress
	 *
	 * @param nodeKey node with host name
	 * @return nodeKey node with IP Address
	 * @throws GridStoreCommandException if host name and IP Address not found in the /etc/hosts
	 */
	private static NodeKey convertHostnameToIPAddress(NodeKey nodeKey) throws GridStoreCommandException {
		InetAddress inet = null;
		NodeKey nodeKeyWithIPAddress = null;
		try {
			inet = InetAddress.getByName(nodeKey.getAddress());
			nodeKeyWithIPAddress = new NodeKey(inet.getHostAddress(), nodeKey.getPort());
		} catch (UnknownHostException e) {
			throw new GridStoreCommandException("D10137: Failed to get node status. ("+e.getMessage()+")", e);
		}
		return nodeKeyWithIPAddress;
	}

	/**
	 * Get list of node in the cluster as IP Address
	 *
	 * @param <T> generic type
	 * @param nodes list of nodes in the config file
	 * @return list of node in the cluster with IP Address
	 * @throws GridStoreCommandException
	 */
	private static <T> List<NodeKey> getNodesWithIPAddress(List<T> nodes) throws GridStoreCommandException {
		List<NodeKey> nodesIPAddress = new ArrayList<NodeKey>();
		for (T obj : nodes) {
			GSNode node = (GSNode) obj;
			NodeKey nodeKey = new NodeKey(node.getAddress(), node.getPort());
			nodesIPAddress.add(convertHostnameToIPAddress(nodeKey));
		}
		return nodesIPAddress;
	}


	/**
	 * Get cluster configuration information.
	 *
	 * @param node node variable
	 * @param userId GridDB user name
	 * @param password password of GridDB user
	 * @return Cluster configuration information
	 * @throws GridStoreCommandException if it's unable to get cluster configuration
	 */
	public static JsonNode getConfig(GSNode node, String userId, String password) throws GridStoreCommandException {
		try {
			return new GridStoreWebAPI(node, userId, password).getNodeHost();
		} catch (GridStoreWebAPIException e) {
			throw new GridStoreCommandException("D10130: An error occurred while getting cluster configuration info. : msg=["+e.getMessage()+"]", e);
		}
	}

	/**
	 * Get node parameter information.
	 *
	 * @param node node variable
	 * @param userId GridDB user name
	 * @param password password of GridDB user
	 * @return Node parameter information
	 * @throws GridStoreCommandException if it's unable to get node parameter information
	 */
	public static JsonNode getParamConf(GSNode node, String userId, String password) throws GridStoreCommandException {
		try {
			return new GridStoreWebAPI(node, userId, password).getNodeConfig();
		} catch (GridStoreWebAPIException e) {
			throw new GridStoreCommandException("D10138: An error occurred while getting node parameter info. : msg=["+e.getMessage()+"]", e);
		}
	}

	/**
	 * Get the log.
	 *
	 * @param node node variable
	 * @param userId GridDB user name
	 * @param password password of GridDB user
	 * @return log
	 * @throws GridStoreCommandException if it's unable to get log
	 */
	public static String[] getLogs(GSNode node, String userId, String password) throws GridStoreCommandException {
		try {
			return new GridStoreWebAPI(node, userId, password).getNodeLog();
		} catch (GridStoreWebAPIException e) {
			throw new GridStoreCommandException("D10131: An error occurred while getting log. : msg=["+e.getMessage()+"]", e);
		}
	}

	/**
	 * Get all trace category names and their log levels.
	 *
	 * @param node node variable
	 * @param userId GridDB user name
	 * @param password password of GridDB user
	 * @return Map where the key is the category name and the value is the log level
	 * @throws GridStoreCommandException if it's unable to get log configuration
	 */
	public static Map<String, String> getLogConf(GSNode node, String userId, String password) throws GridStoreCommandException {
		try {
			return new GridStoreWebAPI(node, userId, password).getNodeTrace();
		} catch (GridStoreWebAPIException e) {
			throw new GridStoreCommandException("D10132: An error occurred while getting log conf. : msg=["+e.getMessage()+"]", e);
		}
	}

	/**
	 * Get the log level of the specified category.
	 *
	 * @param node node variable
	 * @param userId GridDB user name
	 * @param password password of GridDB user
	 * @param category Category name
	 * @return current log level of category
	 * @throws GridStoreCommandException if it's unable to get log configuration
	 */
	public static Map<String, String> getLogConf(GSNode node, String userId, String password, String category)
			throws GridStoreCommandException {
		try {
			return new GridStoreWebAPI(node, userId, password).getNodeTrace(category);
		} catch (GridStoreWebAPIException e) {
			throw new GridStoreCommandException("D10133: An error occurred while getting log conf. : msg=["+e.getMessage()+"]", e);
		}
	}

	/**
	 * Set the log configuration
	 * 
	 * @param node node variable
	 * @param userId user name
	 * @param password password
	 * @param category category of log
	 * @param level level of log
	 * @throws GridStoreCommandException if it's unable to set log configuration
	 */
	public static void setLogConf(GSNode node, String userId, String password, String category, String level)
			throws GridStoreCommandException {
		try {
			new GridStoreWebAPI(node, userId, password).postNodeTrace(category, level);
		} catch (GridStoreWebAPIException e) {
			throw new GridStoreCommandException("D10134: An error occurred while setting log conf.: msg=["+e.getMessage()+"]", e);
		}
	}

	/**
	 * Get node status.
	 * @param stats JSON node
	 * @return status of node
	 */
	public static String getNodeStatus(JsonNode stats) {
		return stats.path("cluster").path("nodeStatus").textValue();
	}

	/**
	 * Get cluster status.
	 * @param stats JSON node
	 * @return cluster status
	 */
	public static String getClusterStatus(JsonNode stats) {
		return stats.path("cluster").path("clusterStatus").textValue();
	}

	/**
	 * Get cluster name.
	 * @param stats JSON node
	 * @return cluster name
	 */
	public static String getClusterName(JsonNode stats) {
		return stats.path("cluster").path("clusterName").textValue();
	}

	/**
	 * Get designated count.
	 * @param stats JSON node
	 * @return designated count
	 */
	public static int getDesinatedCount(JsonNode stats) {
		return stats.path("cluster").path("designatedCount").intValue();
	}

	/**
	 * Get active count.
	 * @param stats JSON node
	 * @return active count
	 */
	public static int getActiveCount(JsonNode stats) {
		return stats.path("cluster").path("activeCount").intValue();
	}


	/**
	 * Get combined status.
	 * @param stats JSON node
	 * @return combined status
	 */
	public static CombinedStatus getCombinedStatus(JsonNode stats) {
		String nodeStatus = stats.path("cluster").path("nodeStatus").textValue();

		if (nodeStatus.equals("INACTIVE") || nodeStatus.equals("DEACTIVCATING")) {
			double recoveryProgress = stats.path("recovery").path("progressRate").asDouble(1); // 項目自体がないときは1(リカバリ完了済)とみなす
			return (recoveryProgress != 1) ? CombinedStatus.STARTING : CombinedStatus.STARTED;

		} else if (nodeStatus.equals("ACTIVE") || nodeStatus.equals("ACTIVATING")) {
			String clusterStatus = stats.path("cluster").path("clusterStatus").textValue();
			return (clusterStatus.equals("MASTER") || clusterStatus.equals("FOLLOWER"))
					? CombinedStatus.SERVICING : CombinedStatus.WAIT;

		} else if (nodeStatus.equals("ABNORMAL")) {
			return CombinedStatus.ABNORMAL;

		} else if (nodeStatus.equals("NORMAL_SHUTDOWN")) {
			return CombinedStatus.STOPPING;

		} else {
			return CombinedStatus.UNKNOWN;
		}
	}

	/**
	 * Get combined status.
	 * 
	 * @param webapi GridStoreWebApi
	 * @return Combined status
	 * @throws GridStoreCommandException if it's unable to get combined status
	 */
	public static CombinedStatus getCombinedStatus(GridStoreWebAPI webapi) throws GridStoreCommandException{
		try {
			JsonNode stats = webapi.getNodeStat();
			return getCombinedStatus(stats);

		} catch (GridStoreWebAPIException e1) {
			if ( e1.getErrorCode() == GridStoreWebAPIException.CODE_API_CONNECT_ERROR ){
				// ConnectionExceptionで接続できない場合はサーバが止まっているとみなす。
				return CombinedStatus.STOPPED;
			}
			throw new GridStoreCommandException(e1.getMessage(), e1);
		}
	}


	/**
	 * ノードの稼動情報を取得します。
	 * @param node
	 * @param userId
	 * @param password
	 * @return
	 */
	private static GSNodeStat getNodeStat(GSNode node, String userId, String password) throws GridStoreCommandException{

		GSNodeStat nodeStat = new GSNodeStat();
		node.setStat(nodeStat);

		try {
			JsonNode stats = null;
			try {
				stats = new GridStoreWebAPI(node, userId, password).getNodeStat();
			} catch (GridStoreWebAPIException e1) {
				if ( e1.getErrorCode() == GridStoreWebAPIException.CODE_API_CONNECT_ERROR ){
					// ConnectionExceptionで接続できない場合はサーバが止まっているとみなす。
					nodeStat.setCombinedStatus(CombinedStatus.STOPPED);
					return nodeStat;
				}
				throw e1;
			}

			// ノードステータス
			nodeStat.setCombinedStatus(getCombinedStatus(stats));

			// 初期クラスタフラグ
			JsonNode init = stats.path("cluster").path("initialCluster");
			if ( init.isMissingNode() ){
				nodeStat.setInitClusterFlag(false);
			} else {
				nodeStat.setInitClusterFlag(true);
			}

			// 役割
			String clusterStatus = getClusterStatus(stats);
			if ( clusterStatus != null && !clusterStatus.isEmpty() ){
				if ( clusterStatus.equalsIgnoreCase("MASTER") ){
					nodeStat.setNodeRole(NodeRole.MASTER);
				} else if ( clusterStatus.equalsIgnoreCase("FOLLOWER") ){
					nodeStat.setNodeRole(NodeRole.FOLLOWER);
				} else if ( clusterStatus.equalsIgnoreCase("SUB_CLUSTER") && (getCombinedStatus(stats) == CombinedStatus.WAIT)
						&& !nodeStat.getInitClusterFlag() ){
					nodeStat.setNodeRole(NodeRole.SUB_MASTER);
				} else if ( clusterStatus.equalsIgnoreCase("SUB_FOLLOWER")){
					nodeStat.setNodeRole(NodeRole.SUB_FOLLOWER);
				}
			}

			// クラスタ名
			String clusterName = getClusterName(stats);
			if ( clusterName != null && !clusterName.isEmpty() ){
				nodeStat.setClusterName(clusterName);
			}

			// 構成ノード数
			nodeStat.setDesignatedCount(getDesinatedCount(stats));
			// Activeノード数
			nodeStat.setActiveCount(getActiveCount(stats));

		} catch ( Exception e ){
			throw new GridStoreCommandException("D10137: Failed to get node status. ("+e.getMessage()+")", e);
		}

		return nodeStat;
	}


	static int THREAD_COUNT = 8;

	/**
	 * Get the cluster master node. <br>
	 * Returns a GSNode object corresponding to the master even if the master node is not included in the cluster definition.
	 * 
	 * @param <T> generic type
	 * @param cluster GridDB cluster 
	 * @param userId GridDB user name
	 * @param password GridDB password
	 * @return Master node of the cluster represented by cluster
	 * @throws GridStoreCommandException it's unable to check node status or the node has already joined the other cluster
	 */
	public static <T> GSNode findMasterNode(GSCluster<T> cluster, String userId, String password) throws GridStoreCommandException{

		for (T obj : cluster.getNodes()) {
			GSNode node = (GSNode)obj;
			if (cluster.getSystemSSL()) {
				node.setSystemSSL(true);
			}
			GridStoreWebAPI webapi = new GridStoreWebAPI(node, userId, password);

			// クラスタ名が正しいかを確認する
			JsonNode stats;
			try {
				stats = webapi.getNodeStat(AddressType.SYSTEM);
			} catch (GridStoreWebAPIException e) {
				if ( e.getErrorCode() != GridStoreWebAPIException.CODE_API_CONNECT_ERROR ){
					throw new GridStoreCommandException("D10135: Failed to check node status. ("+e.getMessage()+")", e);
				}
				continue;
			}
			// クラスタ名を取得
			// メモ：クラスタを組んでいない場合は空文字列
			JsonNode clusterStats = stats.path("cluster");
			String clusterName = clusterStats.path("clusterName").textValue();
			if (clusterName == null || clusterName.isEmpty() ){
				continue;	// どのクラスタにも参加していない
			} else if (!cluster.getName().equals(clusterName)) { // 別クラスタに参加している 場合
				throw new GridStoreCommandException("D10136: The node has already joined the other cluster \""+clusterName+"\". (node="+node+")");
			}

			// 自分がMASTERノードか確認する
			if ("MASTER".equals(getClusterStatus(clusterStats))){
				return node;
			}

			// クラスタを組んでいれば、MASTERの情報を持っているはず
			JsonNode master = clusterStats.path("master");
			if ( master.isMissingNode() ){
				return null;
			}
			String address = master.path("address").textValue();
			int port = master.path("port").intValue();

			GSNode masterNode = cluster.getNode(new NodeKey(address, port));
			if (masterNode == null) {
				masterNode = new GSNode(address, port);
			}
			if (cluster.getSystemSSL()) {
				masterNode.setSystemSSL(true);
			}

			// 本当にMASTERノードか確認する。
			JsonNode json = null;
			try {
				json = new GridStoreWebAPI(masterNode, userId, password).getNodeStat();
			} catch (GridStoreWebAPIException e) {
				continue;
			}
			if ("MASTER".equals(getClusterStatus(json))){
				return masterNode;
			}
		}
		return null;
	}

	/**
	 * Cancel SQL processing.
	 *
	 * @param node node variable
	 * @param userId GridDB user name
	 * @param password password of GridDB user
	 * @param queryId query ID
	 * @return Response value
	 * @throws GridStoreCommandException if it's unable to kill SQL
	 */
	public static String killSql(GSNode node, String userId, String password, String queryId) throws GridStoreCommandException {
		try {
			return new GridStoreWebAPI(node, userId, password).postKillSql(queryId);
		} catch (GridStoreWebAPIException e) {
			throw new GridStoreCommandException("D10139: An error occurred while kill sql. : msg=["+e.getMessage()+"]", e);
		}
	}

	/**
	 * Cancel the job.
	 *
	 * @param node node variable
	 * @param userId GridDB user name
	 * @param password password of GridDB user
	 * @param jobId Job ID
	 * @return Response value
	 * @throws GridStoreCommandException if it's unable to kill job
	 */
	public static String killJob(GSNode node, String userId, String password, String jobId) throws GridStoreCommandException {
		try {
			return new GridStoreWebAPI(node, userId, password).postKillJob(jobId);
		} catch (GridStoreWebAPIException e) {
			throw new GridStoreCommandException("D10140: An error occurred while kill job. : msg=["+e.getMessage()+"]", e);
		}
	}

}
