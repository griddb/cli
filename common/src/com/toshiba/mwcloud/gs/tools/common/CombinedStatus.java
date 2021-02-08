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

/**
 * ノードのステータスを表す。
 */
public enum CombinedStatus {
	/** ノード停止 */ STOPPED,
	/** 起動処理中 */ STARTING,
	/** ノード起動済み、クラスタ未参加 */ STARTED,
	/** クラスタ稼働中 */ SERVICING,
	/** クラスタ参加済み、クラスタ稼動待ち */ WAIT,
	/** 異常停止 */ ABNORMAL,
	/** 終了処理中 */ STOPPING,
	/** 不明(サーバが想定外のステータスを返した場合) */ UNKNOWN,
}
