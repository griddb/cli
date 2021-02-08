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

import com.toshiba.mwcloud.gs.TimeUnit;

/**
 * 期限解放情報
 * <p>
 * コンテナ(テーブル)の期限解放情報を保持します。
 * <p>
 * (V4.1)パーティション期限解放に対応
 * 
 * @since 4.1
 */
public class ExpirationInfo {
	/**
	 * 期限解放種別
	 * <ul>
	 * <li>partition: パーティション期限解放</li>
	 * </ul>
	 * <p>
	 * (設計メモ)ロウ期限解放については、従来通りtimeSeriesPropertiesで管理する
	 */
	private String type;

	/**
	 * 期限解放対象の有効期限の基準とする経過期間
	 */
	private int time;

	/**
	 * 期限解放対象の有効期限の基準とする経過期間の単位
	 */
	private TimeUnit timeUnit;

	/**
	 * コンストラクタ
	 */
	public ExpirationInfo(String type, int time, TimeUnit timeUnit) {
		this.setType(type);
		this.setTime(time);
		this.setTimeUnit(timeUnit);
	}

	/**
	 * 期限解放種別を取得します。
	 * @return 期限解放種別文字列
	 */
	public String getType() {
		return type;
	}

	/**
	 * 期限解放種別を設定します。
	 * @param type 期限解放種別文字列
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * 期限解放対象の有効期限の基準とする経過期間を取得します。
	 * @return 期限解放対象の有効期限の基準とする経過期間
	 */
	public int getTime() {
		return time;
	}

	/**
	 * 期限解放対象の有効期限の基準とする経過期間を設定します。
	 * @param time 期限解放対象の有効期限の基準とする経過期間
	 */
	public void setTime(int time) {
		this.time = time;
	}

	/**
	 * 期限解放対象の有効期限の基準とする経過期間の単位を取得します。
	 * @return 期限解放対象の有効期限の基準とする経過期間の単位
	 */
	public TimeUnit getTimeUnit() {
		return timeUnit;
	}

	/**
	 * 期限解放対象の有効期限の基準とする経過期間の単位を設定します。
	 * @param timeUnit
	 */
	public void setTimeUnit(TimeUnit timeUnit) {
		this.timeUnit = timeUnit;
	}
}
