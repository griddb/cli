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
 * 処理完了を待機する処理を簡潔に書くためのユーティリティ。
 */
public interface Watcher {
	/**
	 * タイムアウトなしを表す定数。
	 */
	public static final int WAIT_FOREVER = Integer.MAX_VALUE; // Integer.MAX_VALUE秒≒68年なので溢れる心配はない

	/**
	 * 処理が完了したかどうかを返す。
	 * @return 処理が完了していたらtrue。そうでなければfalse。
	 */
	public boolean isCompleted();

	/**
	 * 処理が完了するかタイムアウトするまで待つ。
	 * 
	 * @param waitSeconds タイムアウト時間(秒)。WAIT_FOREVERを指定した場合はタイムアウトなし。
	 * @return 処理が完了した場合はtrue。タイムアウトした場合はfalse。
	 */
	public boolean waitCompletion(int waitSeconds);
}
