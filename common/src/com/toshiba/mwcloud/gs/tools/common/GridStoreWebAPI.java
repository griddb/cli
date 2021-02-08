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

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;


/**
 * A wrapper around WebAPI of gsserver
 *
 */
public class GridStoreWebAPI {
	private final NodeKey nodeKey;
	private final String userId;
	private final String password;
	/** System SSL information */
	private boolean systemSSL;

	/**
	 * Get system SSL.
	 * 
	 * @return system SSL
	 */
	public boolean getSystemSSL() {
		return systemSSL;
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
	 * Get node key.
	 * @return node key
	 */
	public NodeKey getNodeKey() {
		return nodeKey;
	}
	/**
	 * Get User ID.
	 * @return user ID
	 */
	public String getUserId() {
		return userId;
	}
	/**
	 * Get password.
	 * @return password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Constructor for GridStoreWebAPI.
	 * 
	 * @param nodeKey {@code NodeKey} object (include IP address and port)
	 * @param userId GridDB user name
	 * @param password GridDB password
	 */
	public GridStoreWebAPI(NodeKey nodeKey, String userId, String password) {
		this.nodeKey = nodeKey;
		this.userId = userId;
		this.password = password;
	}

	/**
	 * Constructor for GridStoreWebAPI.
	 * 
	 * @param node GridDB node
	 * @param userId GridDB user name
	 * @param password GridDB password
	 */
	public GridStoreWebAPI(GSNode node, String userId, String password) {
		this(node.getNodeKey(), userId, password);
		this.systemSSL = node.getSystemSSL();
	}

	/**
	 * HTTP GET method
	 */
	public static final String GET = "GET";
	
	/**
	 * HTTP POST method
	 */
	public static final String POST = "POST";

	private static final int CONNECT_TIMEOUT = 3000;

	/**
	 * Address type enum 
	 *
	 */
	public static enum AddressType { SYSTEM, CLUSTER, TRANSACTION, SYNC };

	/**
	 * Call WebAPI of GridDB node.
	 * 
	 * @param <Result> generic type
	 * @param method HTTP method (GET/POST)
	 * @param path WebAPI end-point
	 * @param params in the case of GET, it is added to the URL as a query parameter. In the case of POST, it is added to the request body as {@code application-www-urlencoded}.
	 * @param resultClass result class
	 * @return WebAPI results. An instance of the class specified in resultClass.
	 * @throws GridStoreWebAPIException if it meets 1 of the below conditions:
	 * <ul>
	 * <li>Unable to connect to node WebAPI</li>
	 * <li>Response status is not 200</li>
	 * <li>Failed to convert result data</li>
	 * </ul>
	 */
	public <Result> Result callWebApi(String method, String path,
				MultivaluedMap<String, String> params, Class<Result> resultClass)
				throws GridStoreWebAPIException {
		Client client = null;
		try {
			// BASIC認証に対応したHTTPクライアントを取得する
			client = ClientHelper.createClient();
			client.setConnectTimeout(CONNECT_TIMEOUT);
			client.addFilter(new HTTPBasicAuthFilter(userId, password));
			String url;
			if (getSystemSSL()) {
				// Use url with SSL connection
				url = "https://" + nodeKey.getAddress() + ":" + nodeKey.getPort() + path;
			} else {
				// Does not use url with SSL connection
				url = "http://" + nodeKey.getAddress() + ":" + nodeKey.getPort() + path;
			}
			WebResource webResource = client.resource(url);
			if (params != null && !method.equals(POST)) {
				webResource = webResource.queryParams(params);
			}
			Builder builder = webResource.accept(MediaType.APPLICATION_JSON_TYPE);
			if (params != null && method.equals(POST)) {
				builder = builder.entity(params, MediaType.APPLICATION_FORM_URLENCODED_TYPE);
			}
			ClientResponse response = builder.method(method, ClientResponse.class);
			if (response.getStatus() != 200) {
				throwException(response);
			}

			String result = response.getEntity(String.class);
			if (resultClass == String.class) {
				return resultClass.cast(result);
			} else {
				return new ObjectMapper().readValue(result, resultClass);
			}

		} catch (JsonParseException e) {
			throw new GridStoreWebAPIException("D10000:Failed to convert result data to "+resultClass.getSimpleName()+" (node=" + nodeKey+")", e);
		} catch (JsonMappingException e) {
			throw new GridStoreWebAPIException("D10001:Failed to convert result data to "+resultClass.getSimpleName()+" (node=" + nodeKey+")", e);
		} catch (IOException e) {
			throw new GridStoreWebAPIException("D10002:Failed to convert result data to "+resultClass.getSimpleName()+" (node=" + nodeKey+")", e);
		} catch (UniformInterfaceException e){
			throw new GridStoreWebAPIException("D10003:Failed to http request (node=" + nodeKey+ ", "+e.getMessage()+")", e);
		} catch (ClientHandlerException e) {
			Throwable t = e.getCause();
			String message = "D10004:Failed to connect (node=" + nodeKey;
			if ( t != null ){
				message += ", "+t.getMessage();
			}
			message += ")";

			if ( t instanceof ConnectException ){
				// 対象のGridStroreノードが起動していない場合
				throw new GridStoreWebAPIException(GridStoreWebAPIException.CODE_API_CONNECT_ERROR, message, e);
			} else if ( t instanceof SocketTimeoutException ){
				// 対象のマシンが起動していない場合
				throw new GridStoreWebAPIException(GridStoreWebAPIException.CODE_API_CONNECT_ERROR, message, e);

			} else {
				// それ以外の接続エラー
				throw new GridStoreWebAPIException(GridStoreWebAPIException.CODE_API_CONNECT_OTHER_ERROR, message, e);
			}

		} finally {
			if (client != null) {
				client.destroy();
			}
		}
	}

	/**
	 * Stop cluster using node WebAPI
	 * @throws GridStoreWebAPIException @see {@link GridStoreWebAPI#callWebApi}
	 */
	public void postClusterStop() throws GridStoreWebAPIException {
		callWebApi(POST, "/cluster/stop", null, String.class);
	}

	/**
	 * Get node host using node WebAPI
	 * @param addressType address type
	 * @return JSON node
	 * @throws GridStoreWebAPIException @see {@link GridStoreWebAPI#callWebApi}
	 */
	public JsonNode getNodeHost(AddressType addressType) throws GridStoreWebAPIException {
		MultivaluedMap<String, String> params = new MultivaluedMapImpl();
		params.add("addressType", addressType.toString().toLowerCase());
		return callWebApi(GET, "/node/host", null, JsonNode.class);
	}
	/**
	 * Get node host using node WebAPI
	 * @return JSON node
	 * @throws GridStoreWebAPIException @see {@link GridStoreWebAPI#callWebApi}
	 */
	public JsonNode getNodeHost() throws GridStoreWebAPIException {
		return getNodeHost(AddressType.SYSTEM);
	}

	/**
	 * Get node configuration using node WebAPI
	 * @return JSON node
	 * @throws GridStoreWebAPIException @see {@link GridStoreWebAPI#callWebApi}
	 */
	public JsonNode getNodeConfig() throws GridStoreWebAPIException {
		return callWebApi(GET, "/node/config", null, JsonNode.class);
	}

	/**
	 * Leave a node from cluster using node WebAPI
	 * @throws GridStoreWebAPIException @see {@link GridStoreWebAPI#callWebApi}
	 */
	public void postNodeLeave() throws GridStoreWebAPIException {
		callWebApi(POST, "/node/leave", null, String.class);
	}

	/**
	 * Increase cluster using node WebAPI
	 * @throws GridStoreWebAPIException @see {@link GridStoreWebAPI#callWebApi}
	 */
	public void postClusterIncrease() throws GridStoreWebAPIException {
		callWebApi(POST, "/cluster/increase", null, String.class);
	}

	/**
	 * Join a node to cluster using node WebAPI
	 * @param clusterName cluster name
	 * @param minNodeNum minimum number of nodes
	 * @throws GridStoreWebAPIException @see {@link GridStoreWebAPI#callWebApi}
	 */
	public void postNodeJoin(String clusterName, int minNodeNum) throws GridStoreWebAPIException {
		MultivaluedMap<String, String> params = new MultivaluedMapImpl();
		params.add("clusterName", clusterName);
		params.add("minNodeNum", Integer.toString(minNodeNum));
		callWebApi(POST, "/node/join", params, String.class);
	}

	/**
	 * Shutdown a node using node WebAPI
	 * @param force {@code true} if force to stop node, otherwise {@code false}
	 * @throws GridStoreWebAPIException @see {@link GridStoreWebAPI#callWebApi}
	 */
	public void postNodeShutdown(boolean force) throws GridStoreWebAPIException {
		MultivaluedMap<String, String> params = new MultivaluedMapImpl();
		params.add("force", Boolean.toString(force));
		callWebApi(POST, "/node/shutdown", params, String.class);
	}

	/**
	 * Get node log using node WebAPI
	 * @return node log
	 * @throws GridStoreWebAPIException @see {@link GridStoreWebAPI#callWebApi}
	 */
	public String[] getNodeLog() throws GridStoreWebAPIException {
		return callWebApi(GET, "/node/log", null, String[].class);
	}

	private static class LogConfig {
		public Map<String, String> levels;
	}
	/**
	 * Get node trace using node WebAPI
	 * @return a Map between String and String that represents node trace
	 * @throws GridStoreWebAPIException @see {@link GridStoreWebAPI#callWebApi}
	 */
	public Map<String, String> getNodeTrace() throws GridStoreWebAPIException {
		LogConfig logConfig = callWebApi(GET, "/node/trace", null, LogConfig.class);
		return logConfig.levels;
	}

	/**
	 * Get node trace using node WebAPI
	 * @param category category
	 * @return a Map between String and String that represents node trace
	 * @throws GridStoreWebAPIException @see {@link GridStoreWebAPI#callWebApi}
	 */
	public Map<String, String> getNodeTrace(String category) throws GridStoreWebAPIException {
		MultivaluedMap<String, String> params = new MultivaluedMapImpl();
		params.add("category", category);
		LogConfig logConfig = callWebApi(GET, "/node/trace", params, LogConfig.class);
		return logConfig.levels;
	}

	/**
	 * Set node trace using node WebAPI
	 * @param category category
	 * @param level log level
	 * @throws GridStoreWebAPIException @see {@link GridStoreWebAPI#callWebApi}
	 */
	public void postNodeTrace(String category, String level) throws GridStoreWebAPIException {
		MultivaluedMap<String, String> params = new MultivaluedMapImpl();
		params.add("category", category);
		params.add("level", level);
		callWebApi(POST, "/node/trace", params, String.class);
	}

	/**
	 * Get node status using node WebAPI
	 * @param addressType address type
	 * @return node status
	 * @throws GridStoreWebAPIException @see {@link GridStoreWebAPI#callWebApi}
	 */
	public JsonNode getNodeStat(AddressType addressType) throws GridStoreWebAPIException {
		MultivaluedMap<String, String> params = new MultivaluedMapImpl();
		params.add("addressType", addressType.toString().toLowerCase());
		return callWebApi(GET, "/node/stat", params, JsonNode.class);
	}
	/**
	 * Get node status using node WebAPI
	 * @return node status
	 * @throws GridStoreWebAPIException @see {@link GridStoreWebAPI#getNodeStat(AddressType)}
	 */
	public JsonNode getNodeStat() throws GridStoreWebAPIException {
		return getNodeStat(AddressType.CLUSTER);
	}

	/**
	 * Partition information class. 
	 *
	 */
	public static class PartitionInfo {
		/**
		 * The owner node.
		 */
		public NodeKeyPartition owner;
		/**
		 * Partition ID.
		 */
		public int pId;
		/**
		 * Status of partition.
		 */
		public String status;
		/**
		 * List of backup nodes.
		 */
		public NodeKeyPartition[] backup;
		/**
		 * List of catch-up nodes.
		 */
		public NodeKeyPartition[] catchup;
		/**
		 * List of all nodes.
		 */
		public NodeKeyPartition[] all;
		/**
		 * SQL owner.
		 */
		public SqlOwner sqlOwner;
		/**
		 * Maximum Log Sequence Number.
		 */
		public int maxLsn;
	}
	
	/**
	 * Node key partition class.
	 *
	 */
	public static class NodeKeyPartition {
		/**
		 * Node address.
		 */
		public String address;
		/**
		 * Log Sequence Number.
		 */
		public int lsn;
		/**
		 * Node port.
		 */
		public int port;
	}
	/**
	 * SQL Owner class. 
	 *
	 */
	public static class SqlOwner {
		/**
		 * SQL address.
		 */
		public String address;
		/**
		 * SQL port.
		 */
		public int port;
	}
	/**
	 * Get node partition using node WebAPI
	 * @param addressType address type
	 * @return array of partition information
	 * @throws GridStoreWebAPIException @see {@link GridStoreWebAPI#callWebApi}
	 */
	public PartitionInfo[] getNodePartition(AddressType addressType) throws GridStoreWebAPIException {
		MultivaluedMap<String, String> params = new MultivaluedMapImpl();
		params.add("addressType", addressType.toString().toLowerCase());
		return callWebApi(GET, "/node/partition", params, PartitionInfo[].class);
	}
	/**
	 * Get node partition using node WebAPI
	 * @return array of partition information
	 * @throws GridStoreWebAPIException @see {@link GridStoreWebAPI#getNodePartition(AddressType)}
	 */
	public PartitionInfo[] getNodePartition() throws GridStoreWebAPIException {
		return getNodePartition(AddressType.CLUSTER);
	}

	/**
	 * Kill SQL using node WebAPI
	 * @param queryId Query ID
	 * @return Response value
	 * @throws GridStoreWebAPIException @see {@link GridStoreWebAPI#callWebApi}
	 */
	public String postKillSql(String queryId) throws GridStoreWebAPIException {
		MultivaluedMap<String, String> params = new MultivaluedMapImpl();
		params.add("clientId", queryId);
		return callWebApi(POST, "/node/sql/cancel", params, String.class);
	}

	/**
	 * Kill job using node WebAPI
	 * @param jobId Job ID
	 * @return  Response value
	 * @throws GridStoreWebAPIException @see {@link GridStoreWebAPI#callWebApi}
	 */
	public String postKillJob(String jobId) throws GridStoreWebAPIException {
		MultivaluedMap<String, String> params = new MultivaluedMapImpl();
		params.add("jobId", jobId);
		return callWebApi(POST, "/node/sql/job/cancel", params, String.class);
	}

	private void throwException(ClientResponse response) throws GridStoreWebAPIException {
		int httpStatus = response.getStatus();
		String result = response.getEntity(String.class);

		if ( httpStatus == 400 ){
			// パラメータ不正

			// 詳細メッセージがある場合
			if ( result != null && !result.isEmpty()){
				JsonNode details = null;
				try {
					details = new ObjectMapper().readValue(result, JsonNode.class);

					int errorStatus = details.path("errorStatus").asInt(0);
					String errorMessage = details.path("errorMessage").asText();

					if (errorStatus == 104) {
						String address = details.path("master").path("address").asText();
						int port = details.path("master").path("port").asInt();
						NodeKey master = new NodeKey(address, port);
						throw new GridStoreWebAPINotMasterException(GridStoreWebAPIException.CODE_API_PARAM_ERROR,
								"D10004:Parameter invalid. "+errorMessage+" (node="+nodeKey+")", httpStatus, errorStatus, details, master);
					} else {
						throw new GridStoreWebAPIException(GridStoreWebAPIException.CODE_API_PARAM_ERROR,
								"D10005:Parameter invalid. "+errorMessage+" (node="+nodeKey+",errorStatus="+errorStatus+")", httpStatus, errorStatus, details);
					}
				} catch ( IOException e ){
					throw new GridStoreWebAPIException(GridStoreWebAPIException.CODE_API_PARAM_ERROR,
							"D10006: Parameter invalid. (node="+nodeKey+","+result+")", httpStatus);
				}
			} else {
				throw new GridStoreWebAPIException(GridStoreWebAPIException.CODE_API_PARAM_ERROR,
						"D10007: Parameter invalid. (node="+nodeKey+")", httpStatus );
			}

		} else if ( httpStatus == 401 ){
			// 認証エラー
			throw new GridStoreWebAPIException(GridStoreWebAPIException.CODE_API_AUTH_ERROR,
					"D10008: Authentication Error (node="+nodeKey+")", httpStatus );

		} else {
			// その他エラー
			throw new GridStoreWebAPIException(GridStoreWebAPIException.CODE_API_OTHER_ERROR,
					"D10009: Http Response Error (node="+nodeKey+",httpStatus="+httpStatus+")", httpStatus);
		}
	}

}
