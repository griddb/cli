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

/**
 * テーブルパーティショニング情報
 * <p>
 * パーティショニングテーブルの各種情報を保持します。
 * <p>
 * メインパーティション、サブパーティションの区別はありません。コンポジットパーティションを表す場合は、
 * このクラスのListを作成して、[0]をメインパーティションの情報、[1]をサブパーティションの情報として表現します。
 * 
 * @since 4.0
 */
public class TablePartitionProperty {
	/**
	 * テーブルパーティション種別
	 * <ul>
	 * <li>HASH: ハッシュパーティショニング</li>
	 * <li>INTERVAL: インターバルパーティショニング</li>
	 * </ul>
	 */
	private String type;
	
	/**
	 * パーティション対象カラム名
	 * <p>
	 * 指定できるカラム型はパーティションの種別によって異なります。
	 * <ul>
	 * <li>HASH: STRING/INTEGERのいずれか</li>
	 * <li>INTERVAL: BYTE/SHORT/INTEGER/LONG/TIMESTAMPのいずれか</li>
	 * </ul>
	 */
	private String column;
	
	/**
	 * ハッシュ分割数
	 * <p>
	 * ハッシュパーティショニングを使用する際の分割数です。
	 */
	private int divisionCount;
	
	/**
	 * インターバル間隔
	 * <p>
	 * インターバルパーティショニングを使用する際の間隔を表す値です。
	 */
	private String intervalValue;
	
	/**
	 * インターバル時間単位
	 * <p>
	 * インターバルパーティショニングを使用する際のインターバル間隔の時間単位です。
	 * パーティション対象カラムの型がTIMESTAMPであるときのみ有効です。
	 * <p>
	 * 指定できる時間単位はDAYのみです。(V4.0)
	 */
	private String intervalUnit;

	/**
	 * コンストラクタ(ハッシュパーティショニング用)
	 * 
	 * @param type
	 * @param column
	 * @param divisionCount
	 */
	public TablePartitionProperty(String type, String column, int divisionCount) {
		this.type = type;
		this.column = column;
		this.divisionCount = divisionCount;
	}

	/**
	 * コンストラクタ(インターバルパーティショニング用)
	 * 
	 * @param type
	 * @param column
	 * @param intervalValue
	 * @param intervalUnit
	 */
	public TablePartitionProperty(String type, String column, String intervalValue, String intervalUnit) {
		this.type = type;
		this.column = column;
		this.intervalValue = intervalValue;
		this.intervalUnit = intervalUnit;
	}

	/**
	 * テーブルパーティション種別を取得します。
	 * @return テーブルパーティション種別文字列
	 */
	public String getType() {
		return type;
	}

	/**
	 * テーブルパーティション種別を設定します。
	 * @param type テーブルパーティション種別文字列
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * パーティション対象カラム名を取得します。
	 * @return パーティション対象カラム名文字列
	 */
	public String getColumn() {
		return column;
	}

	/**
	 * パーティション対象カラム名を設定します。
	 * @param column パーティション対象カラム名文字列
	 */
	public void setColumn(String column) {
		this.column = column;
	}

	/**
	 * ハッシュ分割数を取得します。
	 * @return ハッシュ分割数
	 */
	public int getDivisionCount() {
		return divisionCount;
	}

	/**
	 * ハッシュ分割数を設定します。
	 * @param divisionCount ハッシュ分割数
	 */
	public void setDivisionCount(int divisionCount) {
		this.divisionCount = divisionCount;
	}

	/**
	 * インターバル間隔を取得します。
	 * @return インターバル間隔文字列
	 */
	public String getIntervalValue() {
		return intervalValue;
	}

	/**
	 * インターバル間隔を設定します。
	 * @param intervalValue インターバル間隔文字列
	 */
	public void setIntervalValue(String intervalValue) {
		this.intervalValue = intervalValue;
	}

	/**
	 * インターバル時間単位を取得します。
	 * @return インターバル時間単位文字列
	 */
	public String getIntervalUnit() {
		return intervalUnit;
	}

	/**
	 * インターバル時間単位を設定します。
	 * @param intervalUnit インターバル時間単位文字列
	 */
	public void setIntervalUnit(String intervalUnit) {
		this.intervalUnit = intervalUnit;
	}
}
