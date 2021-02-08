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

public class GSNodeStat {
	/** ノードのステータス */
	private CombinedStatus status = CombinedStatus.STOPPED;

	/** ノードの役割  */
	private NodeRole role = NodeRole.NONE;

	/** 自分が参加しているクラスタに参加しているノード数  (MASTERの時だけ持つ値 ) */
	private int activeCount;

	/** 自分が参加しているクラスタの構成ノード数 (クラスタに参加している時だけ持つ値) */
	private int designatedCount;

	/** 自分が参加しているクラスタのクラスタ名 (クラスタに参加している時だけ持つ値) */
	private String clusterName;

	/** クラスタ未稼動のフラグ (このノードはまだ一度もMASTER/FOLLOWERになったことがない） */
	private boolean initClusterFlag = true;


	/**
	 * ノードのステータスを格納します。
	 * @param status ステータス
	 */
	public void setCombinedStatus(CombinedStatus status){
		this.status = status;
	}
	/**
	 * ノードのステータスを返します。
	 * @return ステータス
	 */
	public CombinedStatus getCombinedStatus(){
		return status;
	}

	/**
	 * ノードの役割を格納します。
	 * @param role 役割
	 */
	public void setNodeRole(NodeRole role){
		this.role = role;
	}
	/**
	 * ノードの役割を返します。
	 * @return 役割
	 */
	public NodeRole getNodeRole(){
		return role;
	}

	/**
	 * ノードが参加しているクラスタに参加しているノード数を格納します。
	 * （ノードがMASTERの時のみ）
	 * @param activeCount 参加済みノード数
	 */
	public void setActiveCount(int activeCount){
		this.activeCount = activeCount;
	}
	/**
	 * ノードが参加しているクラスタに参加しているノード数を返します。
	 * （ノードがMASTERの時のみ）
	 * @return 参加済みノード数
	 */
	public int getActiveCount(){
		return activeCount;
	}

	/**
	 * ノードが参加しているクラスタの構成数を格納します。
	 * @param designatedCount 構成ノード数
	 */
	public void setDesignatedCount(int designatedCount){
		this.designatedCount = designatedCount;
	}
	/**
	 * ノードが参加しているクラスタの構成数を返します。
	 * @return 構成ノード数
	 */
	public int getDesignatedCount(){
		return designatedCount;
	}

	/**
	 * ノードが参加しているクラスタの名前を格納します。
	 * @param clusterName クラスタ名
	 */
	public void setClusterName(String clusterName){
		this.clusterName = clusterName;
	}
	/**
	 * ノードが参加しているクラスの名前を返します。
	 * @return クラスタ名
	 */
	public String getClusterName(){
		return clusterName;
	}

	public void setInitClusterFlag(boolean initClusterFlag){
		this.initClusterFlag = initClusterFlag;
	}

	public boolean getInitClusterFlag(){
		return initClusterFlag;
	}
}