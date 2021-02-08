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

package com.toshiba.mwcloud.gs.tools.common.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

public class ToolProperties {

	private static int fetchSize = 1000;

	public static int getFetchSize(){
		return fetchSize;
	}


	/**
	 * 初期プロパティファイルの項目名
	 */
	private static final String INIT_PROP_ADMIN_HOME_DIR		= "adminHome";
	private static final String INIT_PROP_PROPERTY_FILE_PATH	= "propertyFilePath";

	/**
	 * 設定プロパティのファイルパスのデフォルト値   (gs_adminのファイル名)
	 */
	private static final String PROPERTY_FILE_PATH				= "conf/gs_admin.properties";

	/**
	 * 設定プロパティファイルの項目名
	 */
	private static final String PROP_REPOSITORY_TYPE			= "repositoryType";


	/**
	 * ホームディレクトリ
	 */
	private static String m_homeDir;

	/**
	 * 設定プロパティのファイルパス
	 */
	private static String m_propertyFilePath;

	/**
	 * 設定プロパティの値
	 */
	private static volatile Properties m_properties;


	/**
	 * パス設定プロパティファイルを読み込み、ホームディレクトリ・設定プロパティファイルのパスを取得します。
	 *
	 * @param path パス設定ファイルのパス (WEB-INF/classes 以下のパスを指定します）
	 */
	public static void readInitPropertyFile(String path){
		readInitPropertyFile(path, null);
	}
	public static void readInitPropertyFile(String path, String contextPath){
		// ホームディレクトリ設定ファイルを読み込み
		readPathPropertyFile(path, contextPath);

		// 設定ファイルを読み込み
		readPropertyFile();
	}

	private static void readPathPropertyFile(String path, String contextPath){
		ResourceBundle rb = ResourceBundle.getBundle(path);

		// ホームディレクトリの取得
		String homeDir = rb.getString(INIT_PROP_ADMIN_HOME_DIR);
		if ( contextPath != null ){
			if ( new File(homeDir).isAbsolute() ){
				m_homeDir = homeDir;
			} else {
				m_homeDir = (new File(contextPath, homeDir)).getAbsolutePath();
			}
		} else {
			m_homeDir = homeDir;
		}
		//System.out.println("contextPath="+contextPath);
		//System.out.println("m_homeDir="+m_homeDir);

		// プロパティファイルのパスの取得
		try {
			m_propertyFilePath = rb.getString(INIT_PROP_PROPERTY_FILE_PATH);
		} catch (MissingResourceException e) {
			m_propertyFilePath = null;
		}
		if ( m_propertyFilePath == null ){
			m_propertyFilePath = PROPERTY_FILE_PATH;	// gs_adminのパスをデフォルト
		}
	}

	/**
	 * 設定用プロパティファイルを読み込みます。
	 */
	private static void readPropertyFile(){
		readPropertyFile(m_homeDir, m_propertyFilePath);
	}

	private static void readPropertyFile(String dir, String filePath){
		InputStream is = null;
		File file = new File(dir, filePath);
		try {
			is = new FileInputStream(file);
			Properties confNew = new Properties();
			confNew.load(is);
			m_properties = confNew;

		} catch (IOException e) {
			System.err.println("Cannot open " + file.getAbsolutePath() + ".");
			// スタックトレースが出力されないため記述
			e.printStackTrace();
			throw new Error(e);

		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// スタックトレースが出力されないため記述
					e.printStackTrace();
				}
			}
		}
	}

	public static String getMessage(String key) {
		//if (m_properties == null) { // 複数スレッドから同時に呼び出されても2回ロードしてしまうだけなので、排他制御しなくてもよい。
		//	readPropertyFile();
		//}
		return m_properties.getProperty(key);
	}


	/**
	 * Webアプリケーションのホームディレクトリを返します。
	 *
	 * @return
	 */
	public static String getHomeDir(){
		return m_homeDir;
	}
}
