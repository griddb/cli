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

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import com.toshiba.mwcloud.gs.ColumnInfo;
import com.toshiba.mwcloud.gs.ContainerType;
import com.toshiba.mwcloud.gs.GSException;
import com.toshiba.mwcloud.gs.GSType;
import com.toshiba.mwcloud.gs.IndexInfo;
import com.toshiba.mwcloud.gs.IndexType;
import com.toshiba.mwcloud.gs.Row;
import com.toshiba.mwcloud.gs.TimeSeriesProperties;
import com.toshiba.mwcloud.gs.TimeUnit;
import com.toshiba.mwcloud.gs.TriggerInfo;
import com.toshiba.mwcloud.gs.tools.common.GridStoreCommandException;


/**
 * コンテナのメタ情報ファイルのI/Oクラスです。
 *
 */
public class MetaContainerFileIO {

	/** 読み込み中のメタ情報ファイルのフルパス */
	private String	m_metaFilePath;

	// 処理中のメタ情報ファイル
	private File m_metaFile;

	/** メタ情報ファイルのParser */
	private JsonParser jp;

	private PrintWriter m_outMetaFile;

	//**********************************************************************
	// メタ情報ファイル書き込み
	//**********************************************************************
	public void writeStart(boolean multi){
	}

	public void writeEnd(){
		if ( m_metaFile != null ){
			m_outMetaFile.print("]");
			m_outMetaFile.close();
			m_outMetaFile = null;
			m_metaFile = null;
		}
	}

	public void writeMetaFile(ToolContainerInfo contInfo, String dir, boolean multi) throws GridStoreCommandException{
		List<ToolContainerInfo> contInfoList = new ArrayList<ToolContainerInfo>();
		contInfoList.add(contInfo);
		writeMetaFile(contInfoList, dir, multi);
	}


	/**
	 * メタ情報ファイルを書き込みます。
	 *
	 * @param contInfoList
	 * @param dir
	 * @param multi
	 * @return 書き込みが成功したコンテナ情報ファイルのリスト
	 */
	public List<ToolContainerInfo> writeMetaFile(List<ToolContainerInfo> contInfoList, String dir, boolean multi) throws GridStoreCommandException{

		// コンテナのループ
		List<ToolContainerInfo> resultList = new ArrayList<ToolContainerInfo>();
		File metaFile = null;
		for ( ToolContainerInfo cInfo: contInfoList ){
			try {
				// 1. ファイルパス取得
				metaFile = new File(dir, cInfo.getFileBaseName() + ToolConstants.FILE_EXT_METAINFO);

				// 3. スキーマ情報のJSON化
				StringWriter sw = buildJsonObjects(cInfo);

				// 4. ファイル作成
				if ( (m_metaFile == null) || !m_metaFile.equals(metaFile) ){
					if ( m_metaFile != null ){
						m_outMetaFile.print("]");
						m_outMetaFile.close();
						m_outMetaFile = null;
					}
					m_metaFile = metaFile;

					// ファイル作成
					m_outMetaFile = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(m_metaFile),ToolConstants.ENCODING_JSON)));
					if ( multi ){
						m_outMetaFile.print("[");
					}
				} else {
					m_outMetaFile.println(",");
				}

				// 5. ファイル書き込み
				m_outMetaFile.write(sw.toString());
				m_outMetaFile.flush();

				// 6. ファイルクローズ
				if (multi) {
					/*
					// マルチコンテナ形式
					if ( (i+1) == contInfoList.size() ){
						m_outMetaFile.print("]");
						m_outMetaFile.close();
						m_outMetaFile = null;
					}
					 */
				} else {
					// シングルコンテナ形式
					m_outMetaFile.close();
					m_outMetaFile = null;
					m_metaFile = null;
				}

				//commandProgressStatus.setContainerStatus(cInfo.getContainerInfo().getName(), true, null);
				resultList.add(cInfo);

			} catch ( Exception e ){
				String errMsg = "Failed to read Meta Information file."
						+ ": containerName=["+cInfo.getContainerInfo().getName()+"] msg=["+ e.getMessage()
						+"] path=["+metaFile.getAbsolutePath()+"]";
				throw new GridStoreCommandException(errMsg, e);
			}
		}

		return resultList;
	}



	//**********************************************************************
	// メタ情報ファイル読み込み
	//**********************************************************************
	/**
	 * gs_sh 指定されたメタ情報ファイルからコンテナ情報を読み込みます。
	 *
	 *   ・メタ情報ファイルに複数のコンテナ情報が含まれていた場合はエラーになります。
	 *
	 * @param filePath メタ情報ファイルのパス
	 * @return コンテナ情報オブジェクト
	 * @throws GridStoreCommandException 複数のコンテナ情報が含まれていた場合、ファイル読み込みに失敗した場合など
	 */
	public ToolContainerInfo readMetaInfo(String filePath) throws GridStoreCommandException {

		try {
			if ( jp != null ){
				jp.close();
				jp = null;
			}
			File file = new File(filePath);
			m_metaFilePath = file.getCanonicalPath();
			jp = Json.createParser(new InputStreamReader(skipBOM(new FileInputStream(file)), ToolConstants.ENCODING_JSON));

			if ( !jp.hasNext() ) throw new GridStoreCommandException("The json format of metaFile is invalid.");
			Event e = jp.next();
			if ( e == Event.START_ARRAY ){
				// マルチコンテナ形式
				if ( !jp.hasNext() ) throw new GridStoreCommandException("The json format of metaFile is invalid.");
				if ( jp.next() != Event.START_OBJECT) throw new GridStoreCommandException("The json format of metaFile is invalid.");
			} else if ( e == Event.START_OBJECT ){
				// シングルコンテナ形式
			} else {
				throw new GridStoreCommandException("The json format of metaFile is invalid. : event=["+e+"]");
			}

			// ファイルからコンテナ情報を読み込む。
			ToolContainerInfo ci = readMetaFile(null, null);

			// 次の情報がもしあればエラー。
			ToolContainerInfo nextContInfo = readMetaFile(null, null);
			if ( nextContInfo != null ){
				throw new GridStoreCommandException("The number of ContainerInfo in metaFile must be 1.");
			}

			// クローズ
			m_metaFilePath = null;
			jp.close();
			jp = null;

			return ci;

		} catch ( Exception e ){
			throw new GridStoreCommandException(e.getMessage(), e);
		}
	}


	/**
	 * Export/Import メタ情報ファイルを読み込み、指定されたコンテナ名のコンテナ情報オブジェクトを返します。
	 *
	 *   ・ひとつのメタ情報ファイルを継続して読み込みます。
	 *
	 * @param file メタ情報ファイル
	 * @param containerName コンテナ名文字列
	 * @param dbName データベース名 (デフォルトDBの場合はnull)
	 * @return コンテナ情報クラス
	 */
	public ToolContainerInfo readMetaInfo(File file, String containerName, String dbName) throws GridStoreCommandException{

		try {
			//File file = new File(comLineInfo.getDirectoryPath(), filePath);
			boolean readNewFile = true;

			if ( file.getCanonicalPath().equals(m_metaFilePath) /*&& comLineInfo.getAllFlag()*/ && jp.hasNext() ){
				if ( jp.next() != Event.START_OBJECT) {
					// 続きのJsonオブジェクトが無かったときはもう一度ファイルの先頭から読む
					// （管理ファイルとメタ情報ファイルのコンテナ順が不一致の場合の対策)
					readNewFile = true;
				} else {
					// --allで、かつ前回のファイルと同じ場合は続きを読む
					readNewFile = false;
				}
			}

			ToolContainerInfo ci = null;

			// 最大2回読む(管理ファイルとメタ情報ファイルのコンテナ順が不一致の場合の対策)
			for ( int i = 0; i < 2; i++ ){
				if (readNewFile) {
					// 新しくファイルを読み込む
					if ( jp != null ) jp.close();
					m_metaFilePath = file.getCanonicalPath();
					jp = Json.createParser(new InputStreamReader(skipBOM(new FileInputStream(file)), ToolConstants.ENCODING_JSON));

					if ( !jp.hasNext() ) throw new GridStoreCommandException("json invalid");
					Event e = jp.next();
					if ( e == Event.START_ARRAY ){
						// マルチコンテナ形式
						if ( !jp.hasNext() ) throw new GridStoreCommandException("json invalid");
						if ( jp.next() != Event.START_OBJECT) throw new GridStoreCommandException("Object format is required.");
					} else if ( e == Event.START_OBJECT ){
						// シングルコンテナ形式
					} else {
						throw new GridStoreCommandException("json invalid.: event=["+e+"]");
					}
				}

				// コンテナ情報を読み込む
				ci = readMetaFile(containerName, dbName);

				if ( ci != null ){
					// コンテナが見つかった場合
					break;
				} else {
					// 既にファイルの先頭から読んでいる場合、または2 回目のときはエラー
					if ( readNewFile || ( i == 1 ) ){
						// "JSONファイルに指定のコンテナ情報がありませんでした"
						//throw new GridStoreCommandException(messageResource.getString("MESS_COMM_ERR_METAINFO_27"));
						throw new GridStoreCommandException("Container info does not exist in metaFile.");
					}
					// 次の読み込みはファイルの先頭から
					readNewFile = true;
				}
			}

			/*
			ci.setContainerFileList(fileList);

			// rowKeyAssignedの指定のチェック
			if ( !typeFlag ){
				throw new GSEIException("'"+GSConstants.JSON_META_ROW_KEY+"' is required.");
			}

			checkContainerInfo(ci);
			 */

			return ci;

		} catch ( GridStoreCommandException e ){
			throw new GridStoreCommandException("D00B01: Meta Information File Read failed."
					+": containerName=["+containerName+"] msg=["+e.getMessage()+"] path=["+m_metaFilePath+"]", e);

		} catch (Exception e) {
			// "メタ情報ファイルから指定コンテナ読込処理でエラーが発生しました"
			//throw new GridStoreCommandException(messageResource.getString("MESS_COMM_ERR_METAINFO_15")
			throw new GridStoreCommandException("Error occured in read metaFile."
					+": containerName=["+containerName+"] msg=["+e.getMessage()+"] path=["+m_metaFilePath+"]", e);

		} finally {
		}
	}


	private ToolContainerInfo readMetaFile(String containerName, String dbName) throws GridStoreCommandException {
		try {
			int depth = 1;
			boolean isContainer = false;
			boolean endFlag = false;
			int containerDepth = 0;
			String key = null;
			ToolContainerInfo ci = null;
			boolean typeFlag = false;
			String jsonDbName = ToolConstants.PUBLIC_DB;
			String version = null;
			String expirationType = null;
			int expirationTime = -1;
			TimeUnit expirationTimeUnit = null;

			while(jp.hasNext()){
				Event event = jp.next();
				switch(event){
				case KEY_NAME:
					key = jp.getString();

					if ( isContainer ){
						// カラム情報
						if ( key.equalsIgnoreCase(ToolConstants.JSON_META_COLUMN_SET)){
							readColumnSet(jp, ci);

						// ロウキーカラム名
						} else if ( key.equalsIgnoreCase(ToolConstants.JSON_META_ROWKEY_SET) ){
							readRowKeySetProperties(jp, ci);

						// 時系列プロパティ
						} else if ( key.equalsIgnoreCase(ToolConstants.JSON_META_TIME_PROP) ){
							readTimeSeriesProperties(jp, ci);

						// 索引情報
						} else if ( key.equalsIgnoreCase(ToolConstants.JSON_META_INDEX_SET) ){
							readIndexSet(jp, ci);

						// トリガ情報
						} else if ( key.equalsIgnoreCase(ToolConstants.JSON_META_TRIGGER_SET) ){
							readTriggerInfoSet(jp, ci);

						// 圧縮情報
						} else if ( key.equalsIgnoreCase(ToolConstants.JSON_META_CMP_SET) ){
							readCompressionInfoSet(jp, ci);

						// パーティショニング情報
						} else if ( key.equalsIgnoreCase(ToolConstants.JSON_META_TP_PROPS) ){
							readTablePartitionProperties(jp, ci);

						} else if ( key.equalsIgnoreCase(ToolConstants.JSON_META_SCHEMA_INFORMATION) ) {
							// schemaInformation
							// schemaInformation に定義された値はimport時に復元しない定義情報であるため
							// schemaInformation の END_OBJECT になるまで読み飛ばす
							int schemaInformationDepth = depth;
							int tmpDepth = depth;

							// schemaInformation
							boolean schemaInformationEndFlag = false;
							boolean firstEvent = true;
							while(jp.hasNext()){
								Event readEvent = jp.next();
								if ( firstEvent && readEvent != Event.START_OBJECT ) {
									throw new GridStoreCommandException("The value of '"+ToolConstants.JSON_META_SCHEMA_INFORMATION+"' must be object."
											+": line(about)=["+jp.getLocation().getLineNumber()+"]");
								}

								switch(readEvent){
								case START_ARRAY:
									tmpDepth++;
									break;
								case END_ARRAY:
									tmpDepth--;
									break;
								case START_OBJECT:
									tmpDepth++;
									break;
								case END_OBJECT:
									tmpDepth--;
									if ( tmpDepth == schemaInformationDepth ){
										schemaInformationEndFlag = true;
									}
									break;
								default:
									break;
								}
								firstEvent = false;

								if ( schemaInformationEndFlag ) {
									break;
								}
							}
						} else if ( key.equalsIgnoreCase(ToolConstants.JSON_META_ARCHIVE_INFO) ) {
							// archiveInfo
							// archiveInfo に定義された値はimport時に復元しない定義情報であり
							// また、archiveInfo/expirationType の値が読み込まれると意図しない結果となるため
							// archiveInfo の END_OBJECT になるまで読み飛ばす
							int archiveInfoDepth = depth;
							int tmpDepth = depth;

							// archiveInfo
							boolean archiveInfoEndFlag = false;
							boolean firstEvent = true;
							while(jp.hasNext()){
								Event readEvent = jp.next();
								if ( firstEvent && readEvent != Event.START_OBJECT ) {
									throw new GridStoreCommandException("The value of '"+ToolConstants.JSON_META_ARCHIVE_INFO+"' must be object."
											+": line(about)=["+jp.getLocation().getLineNumber()+"]");
								}

								switch(readEvent){
								case START_ARRAY:
									tmpDepth++;
									break;
								case END_ARRAY:
									tmpDepth--;
									break;
								case START_OBJECT:
									tmpDepth++;
									break;
								case END_OBJECT:
									tmpDepth--;
									if ( tmpDepth == archiveInfoDepth ){
										archiveInfoEndFlag = true;
									}
									break;
								default:
									break;
								}
								firstEvent = false;

								if ( archiveInfoEndFlag ) {
									break;
								}
							}
						// タイムインターバル情報
						} else if ( key.equalsIgnoreCase(ToolConstants.JSON_META_TIME_INTERVAL_INFO) ) {
							// _properties.json/timeIntervalInfoを読込み
							readTimeIntervalInfos(jp, ci);
						} else {
							boolean match = false;
							for ( int j = 0; j < ToolConstants.JSON_META_GROUP_CONTAINER.length; j++ ){
								if ( key.equalsIgnoreCase(ToolConstants.JSON_META_GROUP_CONTAINER[j]) ){
									match = true;
									break;
								}
							}
							if ( !match )
								throw new GridStoreCommandException("key '"+key+"' is invalid.: line(about)=["+jp.getLocation().getLineNumber()+"]");
						}
					}
					break;

				case VALUE_STRING:
					String value = jp.getString();
					value = value.trim();

					// JSONの記述は、データベース名・コンテナ名の順番になっている必要がある。
					if ( key.equalsIgnoreCase(ToolConstants.JSON_META_CONTAINER) && (depth == 1)){
						if ( (containerName == null) || value.equalsIgnoreCase(containerName) ){
							if ( ( ((dbName == null)||dbName.equalsIgnoreCase(ToolConstants.PUBLIC_DB))
									  && ((jsonDbName == null)||jsonDbName.equalsIgnoreCase(ToolConstants.PUBLIC_DB)) )
									|| dbName.equalsIgnoreCase(jsonDbName) ){
								// 該当コンテナ情報
								isContainer = true;
								typeFlag = false;
								containerDepth = depth;
								ci = new ToolContainerInfo();
								ci.setName(value);
								ci.setDbName(jsonDbName);
								ci.setVersion(version);
							}
						}
						jsonDbName = ToolConstants.PUBLIC_DB;

					} else if ( key.equalsIgnoreCase(ToolConstants.JSON_META_DBNAME) && (depth==1)){
						jsonDbName = value;
						// publicデータベース名の正規化
						if ( jsonDbName.equalsIgnoreCase(ToolConstants.PUBLIC_DB) ) jsonDbName = ToolConstants.PUBLIC_DB;

					} else if (key.equalsIgnoreCase(ToolConstants.JSON_META_VERSION)){
						version = value;

					} else if ( isContainer ){
						try {
							if (key.equalsIgnoreCase(ToolConstants.JSON_META_CONTAINER_TYPE)) {
								ci.setType(value);

							} else if (key.equalsIgnoreCase(ToolConstants.JSON_META_CONTAINER_ATTRIBUTE)) {
								if ( value != null ) {
									ci.setAttribute(value);
								}

							} else if (key.equalsIgnoreCase(ToolConstants.JSON_META_CONTAINER_FILE_TYPE)) {
								ci.setContainerFileType(value);

							} else if (key.equalsIgnoreCase(ToolConstants.JSON_META_CONTAINER_FILE)) {
								if ( containerName != null ){
									ci.addContainerFile(value, new File(m_metaFilePath).getParent());
								} else {
									ci.addContainerFile(value);	// gs_shの場合はチェック不要
								}

							} else if ( key.equalsIgnoreCase(ToolConstants.JSON_META_CONTAINER_INTERNAL_FILE)){
								ci.setContainerInternalFileName(value);

							} else if (key.equalsIgnoreCase(ToolConstants.JSON_META_DATA_AFFINITY)) {
								ci.setDataAffinity(value);
							} else if (key.equalsIgnoreCase(ToolConstants.JSON_META_EXPIRATION_TYPE)) {
								expirationType = value;
							} else if (key.equalsIgnoreCase(ToolConstants.JSON_META_EXPIRATION_TIME_UNIT)) {
								if ( !value.equalsIgnoreCase(ToolConstants.JSON_META_TIME_UNIT_NULL) ){
									try {
										expirationTimeUnit = TimeUnit.valueOf(value.toUpperCase());
									} catch ( Exception e ){
										throw new GridStoreCommandException("'"+ToolConstants.JSON_META_EXPIRATION_TIME_UNIT+"' is invalid. value=["
												+value+"] line(about)=["+jp.getLocation().getLineNumber()+"]", e);
							}
								}
							}
						} catch ( GridStoreCommandException e ){
							throw new GridStoreCommandException(e.getMessage() + " line(about)=["+jp.getLocation().getLineNumber()+"]", e);
						}
					}
					break;
				case VALUE_NUMBER:
					if ( isContainer ){
						if (key.equalsIgnoreCase(ToolConstants.JSON_META_PARTITION_NO)) {
							ci.setPartitionNo(jp.getInt());
						} else if (key.equalsIgnoreCase(ToolConstants.JSON_META_EXPIRATION_TIME)) {
							expirationTime = jp.getInt();
						}
					}
					break;

				case VALUE_TRUE:
					if ( isContainer ){
						if ( key.equalsIgnoreCase(ToolConstants.JSON_META_ROW_KEY) ){
							// V4.3 ToolContainerInfo.setRowKeyAssigned()でロウキー設定しない
							//ci.setRowKeyAssigned(true);
							// V4.3 "rowKeyAssigned"の値はToolContainerInfo.setJSONRowKeyAssignedValue()に渡す
							ci.setJSONRowKeyAssignedValue(Boolean.TRUE);
							typeFlag = true;
						}
					}
					break;
				case VALUE_FALSE:
					if ( isContainer ){
						if ( key.equalsIgnoreCase(ToolConstants.JSON_META_ROW_KEY) ){
							// V4.3 ToolContainerInfo.setRowKeyAssigned()でロウキー設定しない
							//ci.setRowKeyAssigned(false);
							// V4.3 "rowKeyAssigned"の値はToolContainerInfo.setJSONRowKeyAssignedValue()に渡す
							ci.setJSONRowKeyAssignedValue(Boolean.FALSE);
							typeFlag = true;
						}
					}
					break;
				case VALUE_NULL:
					break;
				case START_ARRAY:
					depth++;
					break;
				case END_ARRAY:
					depth--;
					break;
				case START_OBJECT:
					depth++;
					break;
				case END_OBJECT:
					if ( (depth == containerDepth) && isContainer ){
						endFlag = true;
					}
					depth--;
					break;
				}
				if ( endFlag ){
					break;
				}
			}

			// 期限解放情報のセット
			if (expirationType != null) {
				if (expirationTime <= 0) {
					throw new GridStoreCommandException("'"+ToolConstants.JSON_META_EXPIRATION_TIME+"' must be more than 0. value=["
							+expirationTime+"]");
				}
				if (expirationTimeUnit == null) {
					throw new GridStoreCommandException("'"+ToolConstants.JSON_META_EXPIRATION_TIME_UNIT+"' is required.");
				}
				ci.setExpirationInfo(new ExpirationInfo(expirationType, expirationTime, expirationTimeUnit));
			}

			// V4.3 rowKeySet[]の設定値よりロウキー情報を設定
			if (ci != null && ci.getJSONrowKeySetStringValueList() != null) {
				List<Integer> rowKeyColumnList = new ArrayList<Integer>();
				for (String rowKeySetStringValue : ci.getJSONrowKeySetStringValueList()) {
					boolean match = false;
					for (int i = 0; i < ci.getColumnCount(); i++) {
						ColumnInfo columnInfo = ci.getColumnInfo(i);

						// V4.3 rowKeySet[]の設定値と一致するカラム名のカラム番号をsetRowKeyColumnList()の設定値とする
						String columnName = columnInfo.getName();
						if (rowKeySetStringValue.equalsIgnoreCase(columnName)) {
							rowKeyColumnList.add(Integer.valueOf(i));
							match = true;
						}
					}
					// ロウキー設定されているカラムがカラム情報に存在するか
					if (!match) {
						throw new GridStoreCommandException("The columnName of rowKeySet does not exist in ColumnInfoList. : name=["+ci.getName()
						+"] rowKeySet=[" + ci.getJSONrowKeySetStringValueList() + "]");
					}
				}
				// V4.3 複合キー対応 ContainerInfo.setRowKeyColumnList()でロウキー設定する
				ci.setRowKeyColumnList(rowKeyColumnList);
			}


			// V4.3 rowKeyAssignedの設定値がある場合の処理
			if (ci != null && ci.getJSONRowKeyAssignedValue() != null) {
				List<Integer> rowKeyColumnList = ci.getRowKeyColumnList();
				if ((rowKeyColumnList != null && rowKeyColumnList.size() > 0) || ci.getJSONrowKeySetStringValueList() != null) {
					// V4.3 rowKeyAssignedの設定値、rowKeySet[]の設定値の双方が存在する場合はエラー
					throw new GridStoreCommandException("'"+ToolConstants.JSON_META_ROW_KEY+"' and '"
							+ToolConstants.JSON_META_ROWKEY_SET+"' can not be set at same time");
				}

				// V4.3 rowKeyAssignedがtrueでrowKeySet[]の設定値がない場合、先頭カラムをロウキーにする
				if (Boolean.TRUE.equals(ci.getJSONRowKeyAssignedValue())) {
					List<Integer> setRowKeyColumnList = new ArrayList<Integer>();
					setRowKeyColumnList.add(Integer.valueOf(0));
					ci.setRowKeyColumnList(setRowKeyColumnList);
				}
			}

			// コンテナ情報の整合性チェック
			if ( ci != null ){
				ci.checkContainerInfo();

			}

			return ci;

		} catch ( GridStoreCommandException e ){
			throw e;
		} catch ( Exception e ){
			throw new GridStoreCommandException(e.getMessage()+": line(about)=["+jp.getLocation().getLineNumber()+"]", e);
		}
	}

	/**
	 * カラムセットの情報を読み込みます。
	 *
	 * @param jp JsonParser
	 * @param ci コンテナ情報オブジェクト
	 * @throws GridStoreCommandException
	 */
	private void readColumnSet(JsonParser jp, ToolContainerInfo ci) throws GridStoreCommandException {
		// 配列
		if (!jp.hasNext()) {
			throw new GridStoreCommandException("'"+ToolConstants.JSON_META_COLUMN_SET+"' is invalid."
					+": line(about)=["+jp.getLocation().getLineNumber()+"]");
		}
		if ( jp.next() != Event.START_ARRAY ){
			throw new GridStoreCommandException("'"+ToolConstants.JSON_META_COLUMN_SET+"' must be array."
					+": line(about)=["+jp.getLocation().getLineNumber()+"]");
		}

		// オブジェクト複数
		Event event = null;
		String columnKey = null;
		while (jp.hasNext()){
			event = jp.next();
			if ( event == Event.END_ARRAY ) break;
			if ( event != Event.START_OBJECT ){
				throw new GridStoreCommandException("The value of '"+ToolConstants.JSON_META_COLUMN_SET+"' must be object."
						+": line(about)=["+jp.getLocation().getLineNumber()+"]");
			}

			boolean typeFlag = false;

			// カラム名、カラム型、カラム制約
			String columnName = null;
			GSType columnType = null;
			TimeUnit precision = null;
			Boolean nullable = null;
			while(event != Event.END_OBJECT){
				event = jp.next();
				if ( event == Event.KEY_NAME ){
					if ( jp.getString().equalsIgnoreCase(ToolConstants.JSON_META_COLUMN_NAME)
							|| jp.getString().equalsIgnoreCase(ToolConstants.JSON_META_COLUMN_TYPE)
							|| jp.getString().equalsIgnoreCase(ToolConstants.JSON_META_COLUMN_TIME_PRECISION)
							|| jp.getString().equalsIgnoreCase(ToolConstants.JSON_META_COLUMN_CSTR_NOTNULL)){
						columnKey = jp.getString();
					} else {
						throw new GridStoreCommandException("'"+jp.getString()+"' is not the key of '"+ToolConstants.JSON_META_COLUMN_SET+"'."
								+": line(about)=["+jp.getLocation().getLineNumber()+"]");
					}
				} else if ( event == Event.VALUE_STRING ){
					String value = jp.getString();
					if ( columnKey.equalsIgnoreCase(ToolConstants.JSON_META_COLUMN_NAME)) {
						if ( value.length() == 0 ) {
							throw new GridStoreCommandException("'"+ToolConstants.JSON_META_COLUMN_NAME+"' is required."
									+": line(about)=["+jp.getLocation().getLineNumber()+"]");
						}
						columnName = jp.getString();
					} else if ( columnKey.equalsIgnoreCase(ToolConstants.JSON_META_COLUMN_TYPE) ){
						columnType = convertStringToColumnType(jp.getString());
						typeFlag = true;
					} else if ( columnKey.equalsIgnoreCase(ToolConstants.JSON_META_COLUMN_TIME_PRECISION) ){
						precision = convertStringToTimeUnit(jp.getString());
					} else if ( columnKey.equalsIgnoreCase(ToolConstants.JSON_META_COLUMN_CSTR_NOTNULL) ){
						// notNullはtrue、false、またはnullのみ受け付ける
						throw new GridStoreCommandException("The value of '"+ ToolConstants.JSON_META_COLUMN_CSTR_NOTNULL + "' must be boolean or null"
								+"'.: line(about)=["+jp.getLocation().getLineNumber()+"]");
					} else {
						throw new GridStoreCommandException("'"+columnKey+"' is not the key of '"+ToolConstants.JSON_META_COLUMN_SET
								+"'.: line(about)=["+jp.getLocation().getLineNumber()+"]");
					}
				} else if ( event == Event.VALUE_TRUE || event == Event.VALUE_FALSE || event == Event.VALUE_NULL ) {
					if ( columnKey.equalsIgnoreCase(ToolConstants.JSON_META_COLUMN_CSTR_NOTNULL) ) {
						if ( event == Event.VALUE_TRUE ) {
							nullable = false;
						} else if ( event == Event.VALUE_FALSE ) {
							nullable = true;
						}
					} else {
						throw new GridStoreCommandException("The value of '"+ columnKey + "' must be string"
								+"'.: line(about)=["+jp.getLocation().getLineNumber()+"]");
					}
				} else if ( event == Event.END_OBJECT ) {
					break;
				} else {
					throw new GridStoreCommandException("'"+ToolConstants.JSON_META_COLUMN_SET+"' is invalid."
							+": event=["+event+"] line(about)=["+jp.getLocation().getLineNumber()+"]");
				}
			}

			// カラムに関するオブジェクトを読み終わっていない場合は例外
			if ( event != Event.END_OBJECT && jp.next() != Event.END_OBJECT ) {
				throw new GridStoreCommandException("'"+ToolConstants.JSON_META_COLUMN_SET+"' is invalid."
						+": line(about)=["+jp.getLocation().getLineNumber()+"]");
			}

			if ( columnName == null ){
				throw new GridStoreCommandException("'"+ToolConstants.JSON_META_COLUMN_NAME+"' is required."
						+": line(about)=["+jp.getLocation().getLineNumber()+"]");
			} else if ( !typeFlag ){
				throw new GridStoreCommandException("'"+ToolConstants.JSON_META_COLUMN_TYPE+"' is required."
						+": line(about)=["+jp.getLocation().getLineNumber()+"]");
			}

			ColumnInfo columnInfo = new ColumnInfo(columnName, columnType, nullable, null);
			if (precision != null && columnType == GSType.TIMESTAMP) {
				ColumnInfo.Builder builder = new ColumnInfo.Builder(columnInfo);
				builder.setTimePrecision(precision);
				ColumnInfo swap = builder.toInfo();
				columnInfo = swap;
			}
			ci.addColumnInfo(columnInfo);
		}
	}


	/**
	 * インデックスセットの情報を読み込みます。
	 *
	 * @param jp JsonParser
	 * @param ci コンテナ情報オブジェクト
	 * @throws GridStoreCommandException
	 */
	public void readIndexSet(JsonParser jp, ToolContainerInfo ci) throws GridStoreCommandException {
		// 配列
		if (!jp.hasNext()) {
			throw new GridStoreCommandException("'"+ToolConstants.JSON_META_INDEX_SET+"' is invalid."
					+": line(about)=["+jp.getLocation().getLineNumber()+"]");
		}
		if ( jp.next() != Event.START_ARRAY ){
			throw new GridStoreCommandException("'"+ToolConstants.JSON_META_INDEX_SET+"' must be array."
					+": line(about)=["+jp.getLocation().getLineNumber()+"]");
		}
		// オブジェクト複数
		Event event = null;
		String key = null;
		while (jp.hasNext()){
			event = jp.next();
			if ( event == Event.END_ARRAY ) break;
			if ( event != Event.START_OBJECT ){
				throw new GridStoreCommandException("The value of '"+ToolConstants.JSON_META_INDEX_SET+"' must be object."
						+": line(about)=["+jp.getLocation().getLineNumber()+"]");
			}
			String columnName = null;
			IndexType indexType = null;
			boolean typeFlag = false;
			String indexName = null;
			// V4.3 複合索引対応 索引設定のカラムを複数指定可能にする
			List<String> indexColumnNames = new ArrayList<String>();
			// V4.3 columnName columnNames 両方とも定義がある場合はエラー
			boolean readColumnName = false;
			boolean readColumnNames = false;

			// カラム名、索引タイプ、索引名
			// V4.3 "columnName" "columnNames" 両方とも定義がある場合はエラーとする
			//      "columnName" "columnNames" 両方とも定義がある場合でも読み込み処理までは実施するためにループ回数を増やす
			//      キー名"columnName"の読み込み、"columnName"の値の読み込み…キー名"indexName"の読み込み、"indexName"の値の読み込みで
			//      計8回読み込みするためループの回数を8とする
			//for ( int i = 0; i < 6; i++ ){
			for ( int i = 0; i < 8; i++ ){
				event = jp.next();
				// V4.3 indexSet の中にcolumnNames追加
				if ( event == Event.KEY_NAME ){
					if ( jp.getString().equalsIgnoreCase(ToolConstants.JSON_META_INDEX_NAME)){
						key = ToolConstants.JSON_META_INDEX_NAME;

					} else if (jp.getString().equalsIgnoreCase(ToolConstants.JSON_META_INDEX_TYPE1)
							|| jp.getString().equalsIgnoreCase(ToolConstants.JSON_META_INDEX_TYPE2)){
						key = ToolConstants.JSON_META_INDEX_TYPE1;
					} else if ( jp.getString().equalsIgnoreCase(ToolConstants.JSON_META_INDEX_INDEXNAME )) {
						key = ToolConstants.JSON_META_INDEX_INDEXNAME;
					} else if ( jp.getString().equalsIgnoreCase(ToolConstants.JSON_META_INDEX_COLUMN_NAMES )) {
						key = ToolConstants.JSON_META_INDEX_COLUMN_NAMES;
					} else {
						throw new GridStoreCommandException("'"+jp.getString()+"' is not the key of '"+ToolConstants.JSON_META_INDEX_SET+"'."
								+": line(about)=["+jp.getLocation().getLineNumber()+"]");
					}
				} else if ( event == Event.VALUE_STRING ){
					String value = jp.getString();
					if ( key.equalsIgnoreCase(ToolConstants.JSON_META_INDEX_NAME) ) {
						if ( value.length() == 0 ) {
							throw new GridStoreCommandException("'"+ToolConstants.JSON_META_INDEX_NAME+"' is required."
									+": line(about)=["+jp.getLocation().getLineNumber()+"]");
						}
						columnName = value;
						// V4.3 columnName columnNames 両方とも定義がある場合はエラー
						readColumnName = true;
					} else if ( key.equalsIgnoreCase(ToolConstants.JSON_META_INDEX_TYPE1) ){
						try {
							indexType = IndexType.valueOf(value.toUpperCase());	// 大文字でなければならない
						} catch ( Exception e ){
							throw new GridStoreCommandException("'"+ToolConstants.JSON_META_INDEX_TYPE1+"' is invalid."
									+": value=["+value+"] line(about)=["+jp.getLocation().getLineNumber()+"]", e);
						}
						typeFlag = true;
					} else if ( key.equalsIgnoreCase(ToolConstants.JSON_META_INDEX_INDEXNAME) ) {
						indexName = value;
					} else {
						throw new GridStoreCommandException("'"+ToolConstants.JSON_META_INDEX_SET+"' is invalid."+
								": key=["+key+"] line(about)=["+jp.getLocation().getLineNumber()+"]");
					}
				} else if ( event == Event.VALUE_NULL ) {
					if ( key.equalsIgnoreCase(ToolConstants.JSON_META_INDEX_INDEXNAME) ) {
						indexName = null;
					} else {
						throw new GridStoreCommandException("'"+ToolConstants.JSON_META_INDEX_SET+"' is invalid."
								+": event=["+event+"] line(about)=["+jp.getLocation().getLineNumber()+"]");
					}
				} else if ( event == Event.START_ARRAY ) {
					// V4.3 複合索引対応 columnNamesの値を読み込む
					if (key.equalsIgnoreCase(ToolConstants.JSON_META_INDEX_COLUMN_NAMES)) {
						indexColumnNames = readIndexcolumnNames(jp);
						// V4.3 複合索引対応 columnNamesの値が0個の場合はエラー
						if (indexColumnNames.size() == 0) {
							throw new GridStoreCommandException("'"+ToolConstants.JSON_META_INDEX_COLUMN_NAMES+"' is invalid."
									+": event=["+event+"] line(about)=["+jp.getLocation().getLineNumber()+"]");
						}
						// V4.3 columnName columnNames 両方とも定義がある場合はエラー
						readColumnNames = true;
					} else {
						throw new GridStoreCommandException("'"+ToolConstants.JSON_META_INDEX_SET+"' is invalid."
								+": event=["+event+"] line(about)=["+jp.getLocation().getLineNumber()+"]");
					}
				} else if ( event == Event.END_OBJECT ) {
					break;
				} else {
					throw new GridStoreCommandException("'"+ToolConstants.JSON_META_INDEX_SET+"' is invalid."
							+": event=["+event+"] line(about)=["+jp.getLocation().getLineNumber()+"]");
				}
			}

			// 索引に関するオブジェクトを読み終わっていない場合は例外
			if ( event != Event.END_OBJECT && jp.next() != Event.END_OBJECT ) {
				throw new GridStoreCommandException("'"+ToolConstants.JSON_META_INDEX_SET+"' is invalid."
						+": line(about)=["+jp.getLocation().getLineNumber()+"]");
			}

			// V4.3 columnName columnNames 両方とも定義がある場合はエラー
			if (readColumnName && readColumnNames) {
				throw new GridStoreCommandException("'"+ToolConstants.JSON_META_INDEX_NAME+"' is invalid."
						+": line(about)=["+jp.getLocation().getLineNumber()+"]");
			}

			// V4.3 columnNames による索引カラム名の指定がなく columnName による索引カラム名指定がある場合、columnNameの指定値を用いる
			//      従来のバージョンのメタデータファイルを読み込み可能とするための処理
			if (columnName != null && indexColumnNames.size() == 0) {
				indexColumnNames.add(columnName);
			}

			// V4.3 複合索引対応 索引カラム名指定がない場合エラー
			//if ( columnName == null ){
			//	throw new GridStoreCommandException("'"+ToolConstants.JSON_META_INDEX_NAME+"' is required."
			//			+": line(about)=["+jp.getLocation().getLineNumber()+"]");
			if ( indexColumnNames.size() == 0 ){
				throw new GridStoreCommandException("'"+ToolConstants.JSON_META_INDEX_COLUMN_NAMES+"' is required."
						+": line(about)=["+jp.getLocation().getLineNumber()+"]");
			} else if ( !typeFlag ){
				throw new GridStoreCommandException("'"+ToolConstants.JSON_META_INDEX_TYPE1+"' is required."
						+": line(about)=["+jp.getLocation().getLineNumber()+"]");
			}

			// indexSetで指定可能な索引とデータ型の組み合わせをチェックする
			//checkIndexType(index,ci); // データ型誤りはGSEIExceptionがthrowされる

			// V4.3 複合索引対応 索引カラム名のリストを指定して索引作成
			//ci.addIndexInfo(columnName, indexType, indexName);
			ci.addIndexInfo(indexColumnNames, indexType, indexName);
		}
	}

	/**
	 * インデックスのカラム名の情報を読み込みます。
	 *
	 * @param jp JsonParser
	 * @return  columnNames に定義されたカラム名のリスト
	 * @throws GridStoreCommandException
	 */
	public List<String> readIndexcolumnNames(JsonParser jp) throws GridStoreCommandException {
		List<String> retIndexcolumnNames = new ArrayList<String>();

		Event event = null;
		while (jp.hasNext()){
			event = jp.next();
			if ( event == Event.END_ARRAY ) break;

			if ( event == Event.VALUE_STRING ){
				String value = jp.getString();
				retIndexcolumnNames.add(value);
			} else {
				throw new GridStoreCommandException("'"+ToolConstants.JSON_META_INDEX_SET+"' is invalid."
						+": event=["+event+"] line(about)=["+jp.getLocation().getLineNumber()+"]");
			}
		}
		return retIndexcolumnNames;
	}

	/**
	 * トリガー情報を読み込みます。
	 *
	 * @param jp JsonParser
	 * @param ci コンテナ情報オブジェクト
	 * @throws GridStoreCommandException
	 */
	public void readTriggerInfoSet(JsonParser jp, ToolContainerInfo ci) throws GridStoreCommandException {
		// 配列
		if (!jp.hasNext()) {
			throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TRIGGER_SET+"' is invalid."
					+": line(about)=["+jp.getLocation().getLineNumber()+"]");
		}
		if ( jp.next() != Event.START_ARRAY ){
			throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TRIGGER_SET+"' must be array."
					+": line(about)=["+jp.getLocation().getLineNumber()+"]");
		}

		// オブジェクト複数
		Event event = null;
		Set<String> nameSet = new HashSet<String>();
		List<TriggerInfo> triggerList = new ArrayList<TriggerInfo>();
		while (jp.hasNext()){
			event = jp.next();
			if ( event == Event.END_ARRAY ) break;
			if ( event != Event.START_OBJECT ){
				throw new GridStoreCommandException("The value of '"+ToolConstants.JSON_META_TRIGGER_SET+"' must be object."
						+": line(about)=["+jp.getLocation().getLineNumber()+"]");
			}

			TriggerInfo trigger = new TriggerInfo();
			String key = null;
			while ( jp.hasNext() ){
				event = jp.next();
				if ( event == Event.KEY_NAME ){
					key = null;
					for ( int i = 0; i < ToolConstants.JSON_META_GROUP_TRIGGER.length; i++ ){
						if ( jp.getString().equalsIgnoreCase(ToolConstants.JSON_META_GROUP_TRIGGER[i])){
							key = ToolConstants.JSON_META_GROUP_TRIGGER[i];
							break;
						}
					}
					if ( key == null ){
						throw new GridStoreCommandException("'"+jp.getString()+"' is not the key of "+ToolConstants.JSON_META_TRIGGER_SET+"'."
								+": line(about)=["+jp.getLocation().getLineNumber()+"]");
					}
				} else if ( event == Event.VALUE_STRING ){
					String value = jp.getString();

					if ( key == ToolConstants.JSON_META_TRIGGER_EVENTNAME) {
						if ( nameSet.contains(value) ){
							// 既に同じ名前の記述がある
							throw new GridStoreCommandException("key:'"+key+"' value:'"+value+"'  same value already exists."
									+": line(about)=["+jp.getLocation().getLineNumber()+"]");
						}
						trigger.setName(value);
						nameSet.add(value);

					} else if ( key == ToolConstants.JSON_META_TRIGGER_COLUMN ){
						Set<String> targetColumns = new HashSet<String>(Arrays.asList(value.split(",", 0)));
						trigger.setTargetColumns(targetColumns);

					} else if ( key == ToolConstants.JSON_META_TRIGGER_TARGET ){
						String[] list = value.split(",", 0);
						Set<TriggerInfo.EventType> triggerTypeList = new HashSet<TriggerInfo.EventType>(list.length);
						for ( int j = 0; j < list.length; j++ ){
							try {
								triggerTypeList.add(TriggerInfo.EventType.valueOf(list[j]));
							} catch ( IllegalArgumentException e ){
								throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TRIGGER_TARGET+"' is invalid."
										+": value=["+list[j]+"] line(about)=["+jp.getLocation().getLineNumber()+"]", e);
							}
						}
						trigger.setTargetEvents(triggerTypeList);

					} else if ( key == ToolConstants.JSON_META_TRIGGER_TYPE ) {
						try {
							trigger.setType(TriggerInfo.Type.valueOf(value));
						} catch ( IllegalArgumentException e ){
							throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TRIGGER_TYPE+"' is invalid."
									+": value=["+value+"] line(about)=["+jp.getLocation().getLineNumber()+"]", e);
						}
					} else if ( key == ToolConstants.JSON_META_TRIGGER_URI ) {
						try {
							trigger.setURI(new URI(value));
						} catch (URISyntaxException e) {
							throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TRIGGER_URI+"' is invalid."
									+": key=["+key+"] line(about)=["+jp.getLocation().getLineNumber()+"]", e);
						}
					} else if ( key == ToolConstants.JSON_META_TRIGGER_JMS_NAME ) {
						trigger.setJMSDestinationName(value);

					} else if ( key == ToolConstants.JSON_META_TRIGGER_JMS_TYPE ) {
						trigger.setJMSDestinationType(value);

					} else if ( key == ToolConstants.JSON_META_TRIGGER_JMS_USER ) {
						trigger.setUser(value);

					} else if ( key == ToolConstants.JSON_META_TRIGGER_JMS_PASS ) {
						trigger.setPassword(value);

					} else {
						throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TRIGGER_SET+"' is invalid."
								+": key=["+key+"] line(about)=["+jp.getLocation().getLineNumber()+"]");
					}

				} else if ( event == Event.END_OBJECT ) {
					break;
				} else {
					throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TRIGGER_SET+"' is invalid."
							+": event=["+event+"] line(about)=["+jp.getLocation().getLineNumber()+"]");
				}
			}

			// 名前、タイプの設定は必須
			if ( (trigger.getName() == null) || (trigger.getName().length()== 0)){
				throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TRIGGER_EVENTNAME+"' is required."
						+": line(about)=["+jp.getLocation().getLineNumber()+"]");

			} else if ( trigger.getType() == null ){
				throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TRIGGER_TYPE+"' is required."
						+": line(about)=["+jp.getLocation().getLineNumber()+"]");
			}

			triggerList.add(trigger);
		}

		ci.setTriggerInfoList(triggerList);
	}

	/**
	 * カラム単位の圧縮情報を読み込みます。
	 *
	 * @param jp JsonParser
	 * @param ci コンテナ情報オブジェクト
	 * @throws GridStoreCommandException
	 */
	public void readCompressionInfoSet(JsonParser jp, ToolContainerInfo ci) throws GridStoreCommandException {
		// 配列
		if (!jp.hasNext()) {
			throw new GridStoreCommandException("'"+ToolConstants.JSON_META_CMP_SET+"' is invalid."
					+": line(about)=["+jp.getLocation().getLineNumber()+"]");
		}
		if ( jp.next() != Event.START_ARRAY ){
			throw new GridStoreCommandException("'"+ToolConstants.JSON_META_CMP_SET+"' must be array."
					+": line(about)=["+jp.getLocation().getLineNumber()+"]");
		}

		// オブジェクト複数
		Event event = null;
		while (jp.hasNext()){
			event = jp.next();
			if ( event == Event.END_ARRAY ) break;
			if ( event != Event.START_OBJECT ) {
				throw new GridStoreCommandException("The value of '"+ToolConstants.JSON_META_CMP_SET+"' must be object."
						+": line(about)=["+jp.getLocation().getLineNumber()+"]");
			}

			// 1カラムごとの設定情報
			String key = null;
			String compressionType = null;
			String columnName = null;
			double rate = -1;
			double span = -1;
			double width = -1;
			while ( jp.hasNext() ){
				event = jp.next();
				if ( event == Event.KEY_NAME ){
					key = null;
					for ( int j = 0; j < ToolConstants.JSON_META_GROUP_CMP.length; j++ ){
						if ( jp.getString().equalsIgnoreCase(ToolConstants.JSON_META_GROUP_CMP[j]) ){
							key = jp.getString();
							break;
						}
					}
					if ( key == null ){
						throw new GridStoreCommandException("key '"+jp.getString()+"' is invalid."
								+": line(about)=["+jp.getLocation().getLineNumber()+"]");
					}

				} else if ( event == Event.VALUE_STRING ){
					String value = jp.getString();
					if ( key.equalsIgnoreCase(ToolConstants.JSON_META_CMP_NAME)) {
						if ( value.length() == 0 ) {
							throw new GridStoreCommandException("'"+ToolConstants.JSON_META_CMP_NAME+"' is required."
									+": line(about)=["+jp.getLocation().getLineNumber()+"]");
						}
						//cmp.setColumnName(value);
						columnName = value;

					} else if ( key.equalsIgnoreCase(ToolConstants.JSON_META_CMP_TYPE) ){
						compressionType = value;
						if (value.equalsIgnoreCase(ToolConstants.COMPRESSION_TYPE_RELATIVE)){
							//cmp.setCompressionType(GSConstants.COMPRESSION_TYPE_RELATIVE);
						} else if (value.equalsIgnoreCase(ToolConstants.COMPRESSION_TYPE_ABSOLUTE)){
							//cmp.setCompressionType(GSConstants.COMPRESSION_TYPE_ABSOLUTE);
						} else{
							throw new GridStoreCommandException("'"+ToolConstants.JSON_META_CMP_TYPE+"' is invalid."
									+": key=["+key+"] value=["+value+"] line(about)=["+jp.getLocation().getLineNumber()+"]");
						}
					} else {
						throw new GridStoreCommandException("'"+ToolConstants.JSON_META_CMP_SET+"' is invalid."
								+": key=["+key+"] line(about)=["+jp.getLocation().getLineNumber()+"]");
					}

				} else if ( event == Event.VALUE_NUMBER ){
					if ( key.equalsIgnoreCase(ToolConstants.JSON_META_CMP_RATE) ){
						//cmp.setRate(jp.getBigDecimal().doubleValue());
						rate = jp.getBigDecimal().doubleValue();
					} else if ( key.equalsIgnoreCase(ToolConstants.JSON_META_CMP_SPAN) ){
						//cmp.setSpan(jp.getBigDecimal().doubleValue());
						span = jp.getBigDecimal().doubleValue();
					} else if ( key.equalsIgnoreCase(ToolConstants.JSON_META_CMP_WIDTH) ){
						//cmp.setWidth(jp.getBigDecimal().doubleValue());
						width = jp.getBigDecimal().doubleValue();
					}

				} else if ( event == Event.END_OBJECT ) {
					break;
				} else {
					throw new GridStoreCommandException("'"+ToolConstants.JSON_META_CMP_SET+"' is invalid."
							+": event=["+event+"] line(about)=["+jp.getLocation().getLineNumber()+"]");
				}
			}

			if ( compressionType.equalsIgnoreCase(ToolConstants.COMPRESSION_TYPE_RELATIVE) ){
				if ( width != -1){
					throw new GridStoreCommandException("'"+ToolConstants.JSON_META_CMP_SET+"' is invalid."
							+": event=["+event+"] line(about)=["+jp.getLocation().getLineNumber()+"]");
				}
				ci.setRelativeHiCompression(columnName, rate, span);
			} else{
				if ( rate != -1 || span != -1){
					throw new GridStoreCommandException("'"+ToolConstants.JSON_META_CMP_SET+"' is invalid."
							+": event=["+event+"] line(about)=["+jp.getLocation().getLineNumber()+"]");
				}
				ci.setAbsoluteHiCompression(columnName, width);
			}
		}
	}

	/**
	 * ロウキーカラム名を読み込みます。
	 *
	 * @param jp JsonParser
	 * @param ci コンテナ情報オブジェクト
	 * @throws GridStoreCommandException
	 */
	public void readRowKeySetProperties(JsonParser jp, ToolContainerInfo ci) throws GridStoreCommandException {
		// 配列
		if (!jp.hasNext()) {
			throw new GridStoreCommandException("'"+ToolConstants.JSON_META_ROWKEY_SET+"' is invalid."
					+": line(about)=["+jp.getLocation().getLineNumber()+"]");
		}
		if ( jp.next() != Event.START_ARRAY ){
			throw new GridStoreCommandException("'"+ToolConstants.JSON_META_ROWKEY_SET+"' is invalid."
					+": line(about)=["+jp.getLocation().getLineNumber()+"]");
		}

		Event event = null;

		List<String> rowKeySetValList = new ArrayList<String>();

		// 文字列配列
		while (jp.hasNext()){
			event = jp.next();
			if ( event == Event.VALUE_STRING ){
				String value = jp.getString();
				rowKeySetValList.add(value);
			} else if ( event == Event.END_ARRAY ){
				break;
			} else {
				throw new GridStoreCommandException("'"+ToolConstants.JSON_META_ROWKEY_SET+"' is invalid."
						+": line(about)=["+jp.getLocation().getLineNumber()+"]");
			}
		}
		ci.setJSONrowKeySetStringValueList(rowKeySetValList);
	}

	/**
	 * 時系列プロパティを読み込みます。
	 *
	 * @param jp JsonParser
	 * @param ci コンテナ情報オブジェクト
	 * @throws GridStoreCommandException
	 */
	public void readTimeSeriesProperties(JsonParser jp, ToolContainerInfo ci) throws GridStoreCommandException {
		// 配列
		if (!jp.hasNext()) {
			throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TIME_PROP+"' is invalid."
					+": line(about)=["+jp.getLocation().getLineNumber()+"]");
		}
		if ( jp.next() != Event.START_OBJECT ){
			throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TIME_PROP+"' is invalid."
					+": line(about)=["+jp.getLocation().getLineNumber()+"]");
		}

		Event event = null;
		String key = null;
		int windowSize = -1;
		int rowExpirationTime = -1;
		TimeUnit windowUnit = null;
		TimeUnit rowExpirationUnit = null;

		// オブジェクト複数
		while (jp.hasNext()){
			event = jp.next();
			if ( event == Event.KEY_NAME ){
				key = null;
				for ( int i = 0; i < ToolConstants.JSON_META_GROUP_TIME.length; i++ ){
					if ( jp.getString().equalsIgnoreCase(ToolConstants.JSON_META_GROUP_TIME[i])){
						key = ToolConstants.JSON_META_GROUP_TIME[i];
					}
				}
				if ( key == null ){
					throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TIME_PROP+"' is invalid."
							+": key=["+jp.getString()+"] line(about)=["+jp.getLocation().getLineNumber()+"]");
				}
			} else if ( event == Event.VALUE_STRING ){
				String value = jp.getString();
				if ( key.equalsIgnoreCase(ToolConstants.JSON_META_TIME_COMP) ) {
					ci.setCompressionMethod(value);

				} else if ( key.equalsIgnoreCase(ToolConstants.JSON_META_TIME_WINDOW_UNIT) ){
					if ( !value.equalsIgnoreCase(ToolConstants.JSON_META_TIME_UNIT_NULL) ){
						try {
							windowUnit = TimeUnit.valueOf(value.toUpperCase());
						} catch ( Exception e ){
							throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TIME_WINDOW_UNIT+"' is invalid. value=["
									+value+"] line(about)=["+jp.getLocation().getLineNumber()+"]", e);
						}
					}
				} else if ( key.equalsIgnoreCase(ToolConstants.JSON_META_TIME_EXPIRATION_UNIT) ){
					if ( !value.equalsIgnoreCase(ToolConstants.JSON_META_TIME_UNIT_NULL) ){
						try {
							rowExpirationUnit = TimeUnit.valueOf(value.toUpperCase());
						} catch ( Exception e ){
							throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TIME_EXPIRATION_UNIT+"' is invalid. value=["
									+value+"] line(about)=["+jp.getLocation().getLineNumber()+"]", e);
						}
					}
				} else {
					throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TIME_PROP+"' is invalid."
							+": line(about)=["+jp.getLocation().getLineNumber()+"]");
				}
				key = null;
			} else if ( event == Event.VALUE_NUMBER ){
				if ( key.equalsIgnoreCase(ToolConstants.JSON_META_TIME_WINDOW) ){
					windowSize = jp.getInt();

				} else if ( key.equalsIgnoreCase(ToolConstants.JSON_META_TIME_EXPIRATION) ){
					rowExpirationTime = jp.getInt();

				} else if ( key.equalsIgnoreCase(ToolConstants.JSON_META_TIME_EXPIRATION_DIV_COUNT) ){// 分割数
					ci.setExpirationDivisionCount(jp.getInt());

				} else {
					throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TIME_PROP+"' is invalid."
							+": line(about)=["+jp.getLocation().getLineNumber()+"]");
				}
			} else if ( event == Event.END_OBJECT ){
				break;
			} else {
				throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TIME_PROP+"' is invalid."
						+": line(about)=["+jp.getLocation().getLineNumber()+"]");
			}
		}
		if ( windowSize != -1 ){
			ci.setCompressionWindowSize(windowSize, windowUnit);
		}
		if ( rowExpirationTime != -1 ){
			ci.setRowExpiration(rowExpirationTime, rowExpirationUnit);
		}
	}

	/**
	 * テーブルパーティション情報を読み込みます。
	 * <p>
	 * 読込み対象のJSONの例は以下のとおり。
	 * <pre>
	 * tablePartitionInfo: [
	 *     {
	 *         type: "INTERVAL",
	 *         column: "mydate",
	 *         intervalValue: "5",
	 *         intervalUnit: "DAY"
	 *     },
	 *     {
	 *         type: "HASH",
	 *         column: "myvalue",
	 *         divisionCount: 10
	 *     }
	 * ]
	 * </pre>
	 *
	 * @param jp JsonParser
	 * @param ci コンテナ情報オブジェクト
	 * @throws GridStoreCommandException 配列ではないか、配列の長さが1～2ではない場合
	 * @throws GridStoreCommandException 配列の要素がオブジェクトではない場合
	 * @throws GridStoreCommandException 配列要素のオブジェクト内に指定が必須のキーが存在しない場合
	 * @throws GridStoreCommandException 配列要素のオブジェクト内に指定が必須のキーの値が無効である場合
	 * @throws GridStoreCommandException コンポジットパーティションの組合せが未サポートの場合
	 */
	public void readTablePartitionProperties(JsonParser jp, ToolContainerInfo ci) throws GridStoreCommandException {

		// 要素が無い場合はException
		if (!jp.hasNext()) {
			throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TP_PROPS+"' is invalid."
					+": line(about)=["+jp.getLocation().getLineNumber()+"]");
		}

		// 配列、または、オブジェクトでない場合はException
		Event event = jp.next();
		if ( (event != Event.START_ARRAY) && (event != Event.START_OBJECT) ){
			throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TP_PROPS+"' is invalid."
					+": line(about)=["+jp.getLocation().getLineNumber()+"]"+event);
		}

		List<TablePartitionProperty> properties = new ArrayList<TablePartitionProperty>();

		// オブジェクト形式の場合
		if (event == Event.START_OBJECT){
			TablePartitionProperty tpp = readTablePartitionPropertiesObject(jp, ci);
			properties.add(tpp);

		// 配列形式の場合
		} else if (event == Event.START_ARRAY){
			while (jp.hasNext()){
				event = jp.next();
				if ( event == Event.START_OBJECT ){
					if (properties.size() == 2) {
						// 配列長が2より大きい場合はエラー。パーティションとサブパーティションしかないため。
						// FIXME ユーザに分かるエラー内容にする。パーティショニングは2つしか設定できない、など。
						throw new GridStoreCommandException("The array length of '"+ToolConstants.JSON_META_TP_PROPS+"' must be 2 or less."
								+": line(about)=["+jp.getLocation().getLineNumber()+"]");
					}

					TablePartitionProperty tpp = readTablePartitionPropertiesObject(jp, ci);
					properties.add(tpp);

				} else if ( event == Event.END_ARRAY ){
					break;

				} else {
					throw new GridStoreCommandException("Elements of the array of '"+ToolConstants.JSON_META_TP_PROPS+"' must be object."
							+": line(about)=["+jp.getLocation().getLineNumber()+"]");
				}
			}
		}

		if (properties.size() == 0) {
			throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TP_PROPS+"' is invalid."
					+": line(about)=["+jp.getLocation().getLineNumber()+"]");
		}

		// 対応コンポジットチェック
		if (properties.size() == 2) {
			String type0 = properties.get(0).getType();
			String type1 = properties.get(1).getType();

			if ( !(type0.equalsIgnoreCase(ToolConstants.TABLE_PARTITION_TYPE_INTERVAL)
					&& type1.equalsIgnoreCase(ToolConstants.TABLE_PARTITION_TYPE_HASH)) ) {
				throw new GridStoreCommandException(type0 + "-" + type1 + " is not supported for composite partitioning.");
			}
		}

		// ツールコンテナ情報にテーブルパーティショニング情報リストを詰める
		ci.setTablePartitionProperties(properties);
	}



	private TablePartitionProperty readTablePartitionPropertiesObject(JsonParser jp, ToolContainerInfo ci ) throws GridStoreCommandException {

		// パーティション種別、パーティション対象カラム、インターバル区間値、インターバル区間値単位、ハッシュ分割数
		TablePartitionProperty partProp = null;
		String type = null;
		String column = null;
		String intervalValue = null;
		String intervalUnit = null;
		Integer divisionCount = null;

		Event event = null;
		String key = null;
		while (jp.hasNext()){
			event = jp.next();

			switch (event) {
				case END_OBJECT:
					break;

				case KEY_NAME:
					// プロパティ名のバリデーション
					if ( jp.getString().equalsIgnoreCase(ToolConstants.JSON_META_TP_TYPE)
							|| jp.getString().equalsIgnoreCase(ToolConstants.JSON_META_TP_COLUMN)
							|| jp.getString().equalsIgnoreCase(ToolConstants.JSON_META_TP_ITV_VALUE)
							|| jp.getString().equalsIgnoreCase(ToolConstants.JSON_META_TP_ITV_UNIT)
							|| jp.getString().equalsIgnoreCase(ToolConstants.JSON_META_TP_DIV_COUNT)){
						key = jp.getString();
					} else {
						throw new GridStoreCommandException("'"+jp.getString()+"' is not the key of '"+ToolConstants.JSON_META_TP_PROPS+"'."
								+": line(about)=["+jp.getLocation().getLineNumber()+"]");
					}
					break;

				case VALUE_STRING:
					// 値が文字列値の場合(パーティション種別、パーティション対象カラム名、インターバル区間値、インターバル区間値単位)
					String value = jp.getString();

					if (key.equalsIgnoreCase(ToolConstants.JSON_META_TP_TYPE)) {
						// パーティション種別
						if (value.equalsIgnoreCase(ToolConstants.TABLE_PARTITION_TYPE_HASH)) {
							type = ToolConstants.TABLE_PARTITION_TYPE_HASH;
						} else if (value.equalsIgnoreCase(ToolConstants.TABLE_PARTITION_TYPE_INTERVAL)) {
							type = ToolConstants.TABLE_PARTITION_TYPE_INTERVAL;
						} else {
							// The value of 'tablePartitionProperties' must be "HASH" or "INTERVAL".
							// TODO V4.x "HASH", "INTERVAL", or "RANGE".
							throw new GridStoreCommandException("The value of '"+ ToolConstants.JSON_META_TP_TYPE + "' must be \""
									+ToolConstants.TABLE_PARTITION_TYPE_HASH + "\" or \"" + ToolConstants.TABLE_PARTITION_TYPE_INTERVAL
									+"\".: line(about)=["+jp.getLocation().getLineNumber()+"]");
						}

					} else if (key.equalsIgnoreCase(ToolConstants.JSON_META_TP_COLUMN)) {
						// パーティション対象カラム名
						if (!value.isEmpty()) {
							column = value;
						}
					} else if (key.equalsIgnoreCase(ToolConstants.JSON_META_TP_ITV_VALUE)) {
						// インターバル区間値
						if (!value.isEmpty()) {
							intervalValue = value;
						}
					} else if (key.equalsIgnoreCase(ToolConstants.JSON_META_TP_ITV_UNIT)) {
						// インターバル区間値単位
						if (!value.isEmpty()) {
							intervalUnit = value;
						}
						//if (value.equalsIgnoreCase(ToolConstants.TABLE_PARTITION_ITV_UNIT_DAY)) {
						//	intervalUnit = ToolConstants.TABLE_PARTITION_ITV_UNIT_DAY;
						//}
					} else {
						if ( (type==null) || type.equalsIgnoreCase(ToolConstants.TABLE_PARTITION_TYPE_HASH)) {
							// 無効な値(例:キーがハッシュ分割数で、値が文字列)
							throw new GridStoreCommandException("The value of '"+ key + "' is invalid."
									+": line(about)=["+jp.getLocation().getLineNumber()+"]");
						}
					}
					break;

				case VALUE_NUMBER:
					// 値が数値の場合(インターバル区間値、ハッシュ分割数)
					if (key.equalsIgnoreCase(ToolConstants.JSON_META_TP_ITV_VALUE)) {
						// インターバル区間値:文字列にする
						intervalValue = Long.toString(jp.getLong());
					} else if (key.equalsIgnoreCase(ToolConstants.JSON_META_TP_DIV_COUNT)) {
						// ハッシュ分割数
						divisionCount = jp.getInt();
					} else {
						// 無効な値(例:キーがパーティション種別で、値が数値)
						throw new GridStoreCommandException("The type of '"+ key + "' must be string."
								+": line(about)=["+jp.getLocation().getLineNumber()+"]");
					}
					break;

				case VALUE_TRUE:
				case VALUE_FALSE:
					// 値がtrue/false/nullの場合(対応プロパティなし)
					if (key.equalsIgnoreCase(ToolConstants.JSON_META_TP_TYPE)
							|| key.equalsIgnoreCase(ToolConstants.JSON_META_TP_COLUMN)) {
						// キーがパーティション種別またはパーティション対象カラムの場合はエラー
						throw new GridStoreCommandException("The type of '"+ key + "' must be string."
								+": line(about)=["+jp.getLocation().getLineNumber()+"]");
					}
					break;

				case VALUE_NULL:
					// NULLの指定は未設定とする
					break;
				default:
					throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TP_PROPS+"' is invalid."
							+": event=["+event+"] line(about)=["+jp.getLocation().getLineNumber()+"]");
			}

			if ( event == Event.END_OBJECT ) break;
		} // while

		// ---------------------------------------------------
		// パーティショニング情報単体でのバリデーションチェック
		// ---------------------------------------------------
		// 種別とカラム名が無い場合はエラー
		if ( type == null ) {
			throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TP_TYPE+"' is required in '"+ToolConstants.JSON_META_TP_PROPS+"'."
					+": line(about)=["+jp.getLocation().getLineNumber()+"]");
		}
		if ( column == null ) {
			throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TP_COLUMN+"' is required in '"+ToolConstants.JSON_META_TP_PROPS+"'."
					+": line(about)=["+jp.getLocation().getLineNumber()+"]");
		}

		// パーティショニング種別に応じて、情報を詰める
		if ( type.equalsIgnoreCase(ToolConstants.TABLE_PARTITION_TYPE_HASH) ) {
			// ハッシュでは分割数のみ必須
			if ( divisionCount == null ) {
				throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TP_DIV_COUNT+"' is required when 'type' is 'HASH'."
						+": line(about)=["+jp.getLocation().getLineNumber()+"]");
			}
			partProp = new TablePartitionProperty(type, column, divisionCount);

		} else if ( type.equalsIgnoreCase(ToolConstants.TABLE_PARTITION_TYPE_INTERVAL) ) {
			// インターバルでは区間値のみ必須
			if ( intervalValue == null ) {
				throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TP_ITV_VALUE+"' is required when 'type' is 'INTERVAL'."
						+": line(about)=["+jp.getLocation().getLineNumber()+"]");
			}
			partProp = new TablePartitionProperty(type, column, intervalValue, intervalUnit);
		}

		return partProp;
	}

	/**
	 * タイムインターバル情報を読み込みます。
	 * <p>
	 * 読込み対象のJSONの例は以下のとおり。
	 * <pre>
	 * "timeIntervalInfo":[
   *     {
   *         "containerFile":"public.ParallelMultiPut_1_2020-12-21_2020-12-22.csv",
   *         "boundaryValue":"2020-12-21T00:00:00.000+0000"
   *     }
   * ]
	 * </pre>
	 *
	 * @param jp JsonParser
	 * @param ci コンテナ情報オブジェクト
	 * @throws GridStoreCommandException 要素がない場合
	 * @throws GridStoreCommandException 配列またはオブジェクトでない場合
	 * @throws GridStoreCommandException 配列の要素がオブジェクトではない場合
	 * @throws GridStoreCommandException 配列要素のオブジェクト内に指定が必須のキーが存在しない場合
	 * @throws GridStoreCommandException 配列要素のオブジェクト内に指定が必須のキーの値が無効である場合
	 */
	public void readTimeIntervalInfos(JsonParser jp, ToolContainerInfo ci) throws GridStoreCommandException {
		// 要素が無い場合はException
		if (!jp.hasNext()) {
			throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TIME_INTERVAL_INFO+"' is invalid."
					+": line(about)=["+jp.getLocation().getLineNumber()+"]");
		}

		// 配列、または、オブジェクトでない場合はException
		Event event = jp.next();
		if ( (event != Event.START_ARRAY) && (event != Event.START_OBJECT) ){
			throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TIME_INTERVAL_INFO+"' is invalid."
					+": line(about)=["+jp.getLocation().getLineNumber()+"]"+event);
		}
		
		List<TimeIntervalInfo> timeIntervalInfos = new ArrayList<TimeIntervalInfo>();

		if (event == Event.START_ARRAY){
			while (jp.hasNext()){
				event = jp.next();
				if ( event == Event.START_OBJECT ){
					TimeIntervalInfo timeIntervalInfo = readTimeIntervalInfo(jp, ci);
					timeIntervalInfos.add(timeIntervalInfo);

				} else if ( event == Event.END_ARRAY ){
					break;

				} else {
					throw new GridStoreCommandException("Elements of the array of '"+ToolConstants.JSON_META_TIME_INTERVAL_INFO+"' must be object."
							+": line(about)=["+jp.getLocation().getLineNumber()+"]");
				}
			}
		}
		
		// ツールコンテナ情報にインターバル分割情報リストを詰める
		ci.setTimeIntervalInfos(timeIntervalInfos);
	}

	private TimeIntervalInfo readTimeIntervalInfo(JsonParser jp, ToolContainerInfo ci ) throws GridStoreCommandException {
		
		TimeIntervalInfo ti = null;
		
		Event event = null;
		String key = null;
		String containerFileName = null;//import対象のファイル名
		String boundaryValue = null;//対象ファイルのインターバル

		while (jp.hasNext()){
			event = jp.next();
			switch (event) {
				case END_OBJECT:
					break;

				case KEY_NAME:
					// プロパティ名のバリデーション
					if ( jp.getString().equalsIgnoreCase(ToolConstants.JSON_META_CONTAINER_FILE)
							|| jp.getString().equalsIgnoreCase(ToolConstants.JSON_META_BOUNDARY_VALUE)){
						key = jp.getString();
					} else {
						throw new GridStoreCommandException("'"+jp.getString()+"' is not the key of '"+ToolConstants.JSON_META_TIME_INTERVAL_INFO+"'."
								+": line(about)=["+jp.getLocation().getLineNumber()+"]");
					}
					break;

				case VALUE_STRING:
					// 値が文字列値の場合(import対象のファイル名、対象ファイルのインターバル)
					String value = jp.getString();

					if (key.equalsIgnoreCase(ToolConstants.JSON_META_CONTAINER_FILE)) {
						containerFileName = value;
					} else if (key.equalsIgnoreCase(ToolConstants.JSON_META_BOUNDARY_VALUE)) {
						boundaryValue = value;
					} else {
							throw new GridStoreCommandException("The value of '"+ key + "' is invalid."
									+": line(about)=["+jp.getLocation().getLineNumber()+"]");
					}
					break;

				case VALUE_NUMBER:
						throw new GridStoreCommandException("The type of '"+ key + "' must be string."
								+": line(about)=["+jp.getLocation().getLineNumber()+"]");

				case VALUE_TRUE:
				case VALUE_FALSE:
						throw new GridStoreCommandException("The type of '"+ key + "' must be string."
								+": line(about)=["+jp.getLocation().getLineNumber()+"]");

				case VALUE_NULL:
					// NULLの指定は未設定とする
					break;
				default:
					throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TIME_INTERVAL_INFO+"' is invalid."
							+": event=["+event+"] line(about)=["+jp.getLocation().getLineNumber()+"]");
			}
			if ( event == Event.END_OBJECT ) break;
		}
		
		ti = new TimeIntervalInfo(containerFileName, boundaryValue);
		
		return ti;
	}

/**
 * コンテナ情報をJSON文字列に変換します。
 *
 * @param cInfo コンテナ情報クラスリスト
 * @return 出力文字列データ
 */
private StringWriter buildJsonObjects(ToolContainerInfo cInfo) throws Exception {

	StringWriter sw = new StringWriter();
	Map<String, Object> properties = new HashMap<String, Object>(1);
	properties.put(JsonGenerator.PRETTY_PRINTING, true);	// 整形して出力する設定
	JsonGeneratorFactory factory = Json.createGeneratorFactory(properties);
	JsonGenerator gen = factory.createGenerator(sw);

	buildJson(cInfo, gen);

	gen.close();
	return sw;

}

private JsonGenerator buildJson(ToolContainerInfo cInfo, JsonGenerator gen) throws Exception{

	gen.writeStartObject();
	gen.write("version", ToolConstants.META_FILE_VERSION); // 将来用の互換性用(メタ情報ファイルのバージョン)
	if ( cInfo.getDbName() != null ){
		gen.write(ToolConstants.JSON_META_DBNAME, cInfo.getDbName());
	}
	gen.write(ToolConstants.JSON_META_CONTAINER, cInfo.getName());
	gen.write(ToolConstants.JSON_META_CONTAINER_TYPE, cInfo.getType().toString());
	//		if ( cInfo.getAttribute() != ContainerAttribute.BASE ){
	//			gen.write(ToolConstants.JSON_META_CONTAINER_ATTRIBUTE, cInfo.getAttribute().toString());
	//		}
	gen.write(ToolConstants.JSON_META_CONTAINER_FILE_TYPE, cInfo.getContainerFileType().toString().toLowerCase());
	if ( cInfo.getContainerFileList() == null ){
		// 出力しない。
	} else if ( cInfo.getContainerFileList().size() == 1 ){
		gen.write(ToolConstants.JSON_META_CONTAINER_FILE, cInfo.getContainerFile());
	} else {
		gen.writeStartArray(ToolConstants.JSON_META_CONTAINER_FILE);
		for (String fileName : cInfo.getContainerFileList()){
			gen.write(fileName);
		}
		gen.writeEnd();
	}
	if ( cInfo.getContainerInternalFileName() != null ){
		gen.write(ToolConstants.JSON_META_CONTAINER_INTERNAL_FILE, cInfo.getContainerInternalFileName());
	}
	if(cInfo.getDataAffinity()!=null){
		gen.write(ToolConstants.JSON_META_DATA_AFFINITY, cInfo.getDataAffinity());// Data Affinity
	}
	// V4.3 rowKeyAssigned 出力しない
	// gen.write(ToolConstants.JSON_META_ROW_KEY, cInfo.getRowKeyAssigned());
	gen.write(ToolConstants.JSON_META_PARTITION_NO, cInfo.getPartitionNo());

	//------------------------------------
	// カラム情報書き込み
	//------------------------------------
	if (cInfo.getColumnCount() > 0) {
		gen.writeStartArray(ToolConstants.JSON_META_COLUMN_SET);
		for (int j = 0; j < cInfo.getColumnCount(); j++) {
			gen.writeStartObject();
			gen.write(ToolConstants.JSON_META_COLUMN_NAME, cInfo.getColumnInfo(j).getName());
			gen.write(ToolConstants.JSON_META_COLUMN_TYPE,convertColumnType(cInfo.getColumnInfo(j).getType()));
			ColumnInfo columnInfo = cInfo.getColumnInfo(j);
			if (columnInfo.getType() == GSType.TIMESTAMP && isPreciseColumn(columnInfo)) {
				TimeUnit precision = columnInfo.getTimePrecision();
				gen.write(ToolConstants.JSON_META_COLUMN_TIME_PRECISION, precision.name());
			}
			Boolean nullable = cInfo.getColumnInfo(j).getNullable();
			if ( nullable == null ) {
				gen.writeNull(ToolConstants.JSON_META_COLUMN_CSTR_NOTNULL);
			} else {
				gen.write(ToolConstants.JSON_META_COLUMN_CSTR_NOTNULL, !nullable);
			}
			gen.writeEnd();
		}
		gen.writeEnd();
	}

	// V4.3 複合キー対応 rowKeySet追加
	gen.writeStartArray(ToolConstants.JSON_META_ROWKEY_SET);
	List<Integer> rowKeyColumnList = cInfo.getRowKeyColumnList();
	if (rowKeyColumnList == null) {
		rowKeyColumnList = new ArrayList<Integer>();
	}
	for (Integer rowKey : rowKeyColumnList) {
		int rowKeyColNo = rowKey.intValue();
		ColumnInfo rowKeyColumnInfo = cInfo.getColumnInfo(rowKeyColNo);
		gen.write(rowKeyColumnInfo.getName());
	}
    gen.writeEnd();

	//------------------------------------
	// 索引情報書き込み
	//------------------------------------
	int indexCount = 0;
	//		for (int j = 0; j < cInfo.getColumnCount(); j++) {
	//			Set<IndexType> indexTypes = cInfo.getColumnInfo(j).getIndexTypes();
	//			for ( IndexType type : indexTypes ){
	//				if ( indexCount == 0 ) {
	//					gen.writeStartArray(ToolConstants.JSON_META_INDEX_SET);
	//				}
	//				gen.writeStartObject();
	//				gen.write(ToolConstants.JSON_META_INDEX_NAME, cInfo.getColumnInfo(j).getName());
	//				gen.write(ToolConstants.JSON_META_INDEX_TYPE1, type.toString());
	//				gen.writeEnd();
	//				indexCount++;
	//			}
	//		}
	for (IndexInfo index : cInfo.getIndexInfoList()) {
		if ( indexCount == 0 ) {
			gen.writeStartArray(ToolConstants.JSON_META_INDEX_SET);
		}
		gen.writeStartObject();
		// V4.3 indexSet[]/columnName 出力しない
		//gen.write(ToolConstants.JSON_META_INDEX_NAME, index.getColumnName());
		// V4.3 indexSet[]/columnNames[] 出力
		gen.writeStartArray(ToolConstants.JSON_META_INDEX_COLUMN_NAMES);
		for (String indexColumnName : index.getColumnNameList()) {
			gen.write(indexColumnName);
		}
		gen.writeEnd();
		gen.write(ToolConstants.JSON_META_INDEX_TYPE1, index.getType().toString());
		String indexName = index.getName();
		if ( indexName == null ) {
			gen.writeNull(ToolConstants.JSON_META_INDEX_INDEXNAME);
		} else {
			gen.write(ToolConstants.JSON_META_INDEX_INDEXNAME, indexName);
		}
		gen.writeEnd();
		indexCount++;
	}
	if ( indexCount > 0 ) gen.writeEnd();


	//------------------------------------
	// トリガ情報書き込み
	//------------------------------------
	if (cInfo.getTriggerInfoList().size() > 0) {	// getTriggerInfoList:トリガ未設定の場合は空リストが返る
		gen.writeStartArray(ToolConstants.JSON_META_TRIGGER_SET);
		for (TriggerInfo event : cInfo.getTriggerInfoList()) {
			gen.writeStartObject();
			gen.write(ToolConstants.JSON_META_TRIGGER_EVENTNAME, event.getName());
			gen.write(ToolConstants.JSON_META_TRIGGER_TYPE, event.getType().toString());
			// targetEvent CREATE/UPDATE/DELETE
			StringBuilder sb_targetEvent = new StringBuilder();
			for (TriggerInfo.EventType ev : event.getTargetEvents()) {
				sb_targetEvent.append(ev.toString() + ",");
			}
			if (sb_targetEvent.length() > 2)
				sb_targetEvent.deleteCharAt(sb_targetEvent.length() - 1);
			gen.write(ToolConstants.JSON_META_TRIGGER_TARGET, sb_targetEvent.toString());
			StringBuilder sb_TargetColumnNames = new StringBuilder();
			for (String s : event.getTargetColumns()) {
				sb_TargetColumnNames.append(s + ",");
			}
			if (sb_TargetColumnNames.length() > 2)
				sb_TargetColumnNames.deleteCharAt(sb_TargetColumnNames.length() - 1);
			gen.write(ToolConstants.JSON_META_TRIGGER_COLUMN, sb_TargetColumnNames.toString());
			gen.write(ToolConstants.JSON_META_TRIGGER_URI, event.getURI().toString());
			if (event.getType().equals(TriggerInfo.Type.JMS)) {
				gen.write(ToolConstants.JSON_META_TRIGGER_JMS_TYPE,event.getJMSDestinationType());
				gen.write(ToolConstants.JSON_META_TRIGGER_JMS_NAME,event.getJMSDestinationName());
				gen.write(ToolConstants.JSON_META_TRIGGER_JMS_USER, event.getUser());
				gen.write(ToolConstants.JSON_META_TRIGGER_JMS_PASS, event.getPassword());
			}
			gen.writeEnd();
		}
		gen.writeEnd();
	}


	//------------------------------------
	// 時系列情報書き込み
	//------------------------------------
	if (cInfo.getType().equals(ContainerType.TIME_SERIES)) {
		TimeSeriesProperties timeProp = cInfo.getTimeSeriesProperties();
		gen.writeStartObject(ToolConstants.JSON_META_TIME_PROP);
		gen.write(ToolConstants.JSON_META_TIME_COMP, timeProp.getCompressionMethod().toString());
		gen.write(ToolConstants.JSON_META_TIME_WINDOW, timeProp.getCompressionWindowSize());
		if (timeProp.getCompressionWindowSizeUnit() == null) {
			gen.write(ToolConstants.JSON_META_TIME_WINDOW_UNIT, "null");
		} else {
			gen.write(ToolConstants.JSON_META_TIME_WINDOW_UNIT, timeProp.getCompressionWindowSizeUnit().toString());
		}
		gen.write(ToolConstants.JSON_META_TIME_EXPIRATION_DIV_COUNT, timeProp.getExpirationDivisionCount());
		gen.write(ToolConstants.JSON_META_TIME_EXPIRATION, timeProp.getRowExpirationTime());
		if (timeProp.getRowExpirationTimeUnit() == null) {
			gen.write(ToolConstants.JSON_META_TIME_EXPIRATION_UNIT, "null");
		} else {
			gen.write(ToolConstants.JSON_META_TIME_EXPIRATION_UNIT, timeProp.getRowExpirationTimeUnit().toString());
		}
		gen.writeEnd();

		gen.writeStartArray(ToolConstants.JSON_META_CMP_SET);
		for (String compColmnName : timeProp.getSpecifiedColumns()) {
			gen.writeStartObject();
			gen.write(ToolConstants.JSON_META_CMP_NAME, compColmnName);
			if( timeProp.isCompressionRelative(compColmnName) ){
				gen.write(ToolConstants.JSON_META_CMP_TYPE, ToolConstants.COMPRESSION_TYPE_RELATIVE);
				gen.write(ToolConstants.JSON_META_CMP_RATE, timeProp.getCompressionRate(compColmnName));
				gen.write(ToolConstants.JSON_META_CMP_SPAN, timeProp.getCompressionSpan(compColmnName));
			}else{
				gen.write(ToolConstants.JSON_META_CMP_TYPE, ToolConstants.COMPRESSION_TYPE_ABSOLUTE);
				gen.write(ToolConstants.JSON_META_CMP_WIDTH, timeProp.getCompressionWidth(compColmnName));
			}
			gen.writeEnd();
		}
		gen.writeEnd();
	}

	/*
		// サブコンテナ情報
		if ( cInfo.getSubContainerList() != null ){
			gen.writeStartArray("SubContainer");

			for ( ToolContainerInfo subInfo : cInfo.getSubContainerList() ){
				gen = buildJson(subInfo, gen);
			}

			gen.writeEnd();
		}
	 */

	//------------------------------------
	// テーブルパーティショング情報書き込み
	//------------------------------------
	if (cInfo.getTablePartitionProperties().size() > 0) {	// getTablePartitionProperties:未設定の場合は空リストが返る
		if ( cInfo.getTablePartitionProperties().size() != 1 ){
			gen.writeStartArray(ToolConstants.JSON_META_TP_PROPS);
		}
		for (TablePartitionProperty prop : cInfo.getTablePartitionProperties()) {
			if ( cInfo.getTablePartitionProperties().size()==1 ) {
				gen.writeStartObject(ToolConstants.JSON_META_TP_PROPS);
			} else {
				gen.writeStartObject();
			}
			gen.write(ToolConstants.JSON_META_TP_TYPE, prop.getType());
			gen.write(ToolConstants.JSON_META_TP_COLUMN, prop.getColumn());
			if (prop.getType().equalsIgnoreCase(ToolConstants.TABLE_PARTITION_TYPE_HASH)) {
				// 種別がハッシュなら分割値のみ出力
				gen.write(ToolConstants.JSON_META_TP_DIV_COUNT, prop.getDivisionCount());
			} else if (prop.getType().equalsIgnoreCase(ToolConstants.TABLE_PARTITION_TYPE_INTERVAL)) {
				// 種別がインターバルなら区間値を出力
				gen.write(ToolConstants.JSON_META_TP_ITV_VALUE, prop.getIntervalValue());
				if (prop.getIntervalUnit() != null && !prop.getIntervalUnit().isEmpty()) {
					// 区間値単位は値があれば出力(TIMESTAMP型のときのみ値がある)
					gen.write(ToolConstants.JSON_META_TP_ITV_UNIT, prop.getIntervalUnit());
				}
			}
			gen.writeEnd();
		}
		if ( cInfo.getTablePartitionProperties().size() != 1 ){
			gen.writeEnd();
		}
	}

	//------------------------------------
	// 期限解放情報書き込み
	//------------------------------------
	ExpirationInfo expInfo = cInfo.getExpirationInfo();
	if (expInfo != null) {	// 未設定の場合はnullが返る
		gen.write(ToolConstants.JSON_META_EXPIRATION_TYPE, expInfo.getType());
		gen.write(ToolConstants.JSON_META_EXPIRATION_TIME, expInfo.getTime());
		gen.write(ToolConstants.JSON_META_EXPIRATION_TIME_UNIT, expInfo.getTimeUnit().toString());
	}

	//------------------------------------
	// タイムインターバル情報書き込み
	//------------------------------------	
	if ( cInfo.getTimeIntervalInfos() == null || cInfo.getTimeIntervalInfos().size() == 0){
		// 出力しない。
	} else {
		gen.writeStartArray(ToolConstants.JSON_META_TIME_INTERVAL_INFO);
		for (TimeIntervalInfo splitFileInfo : cInfo.getTimeIntervalInfos()){
			gen.writeStartObject();
			gen.write(ToolConstants.JSON_META_CONTAINER_FILE, splitFileInfo.getContainerFile());
			gen.write(ToolConstants.JSON_META_BOUNDARY_VALUE, splitFileInfo.getBoundaryValue());
			gen.writeEnd();
		}
		gen.writeEnd();
	}
	
	gen.writeEnd();

	return gen;
}


//=====================================================================
// Utility
//=====================================================================
/**
 * カラム種別JSONオブジェクト解析メソッド
 *
 * @param type カラム種別文字列
 * @return カラム種別
 */
public static GSType convertStringToColumnType(String type) throws GridStoreCommandException {
	try {
		type = type.trim();

		if (type.equalsIgnoreCase(ToolConstants.COLUMN_TYPE_STRING_ARRAY)) {
			return GSType.STRING_ARRAY;
		} else if (type.equalsIgnoreCase(ToolConstants.COLUMN_TYPE_BOOL_ARRAY)) {
			return GSType.BOOL_ARRAY;
		} else if (type.equalsIgnoreCase(ToolConstants.COLUMN_TYPE_BYTE_ARRAY)) {
			return GSType.BYTE_ARRAY;
		} else if (type.equalsIgnoreCase(ToolConstants.COLUMN_TYPE_SHORT_ARRAY)) {
			return GSType.SHORT_ARRAY;
		} else if (type.equalsIgnoreCase(ToolConstants.COLUMN_TYPE_INTEGER_ARRAY)) {
			return GSType.INTEGER_ARRAY;
		} else if (type.equalsIgnoreCase(ToolConstants.COLUMN_TYPE_LONG_ARRAY)) {
			return GSType.LONG_ARRAY;
		} else if (type.equalsIgnoreCase(ToolConstants.COLUMN_TYPE_FLOAT_ARRAY)) {
			return GSType.FLOAT_ARRAY;
		} else if (type.equalsIgnoreCase(ToolConstants.COLUMN_TYPE_DOUBLE_ARRAY)) {
			return GSType.DOUBLE_ARRAY;
		} else if (type.equalsIgnoreCase(ToolConstants.COLUMN_TYPE_TIMESTAMP_ARRAY)) {
			return GSType.TIMESTAMP_ARRAY;
		} else if (type.equalsIgnoreCase(ToolConstants.COLUMN_TYPE_BOOL)) {
			return GSType.BOOL;
		} else if (type.equalsIgnoreCase(ToolConstants.COLUMN_TYPE_TIMESTAMP_MILI)
				|| type.equalsIgnoreCase(ToolConstants.COLUMN_TYPE_TIMESTAMP_MICRO)
				|| type.equalsIgnoreCase(ToolConstants.COLUMN_TYPE_TIMESTAMP_NANO)) {
			return GSType.TIMESTAMP;
		}
		return GSType.valueOf(type.toUpperCase().trim());

	} catch (Exception e) {
		// "カラム種別の解析処理でエラーが発生しました"
		//throw new GridStoreCommandException(messageResource.getString("MESS_COMM_ERR_METAINFO_13")+ ": type=["
		throw new GridStoreCommandException("Error occurred when converting to type"+ ": type=["
				+type+"] msg=[" + e.getMessage()+"]", e);
	}
}

/**
 * Convert string to value of TimeUnit type
 *
 * @param unit the unit of precision
 * @return TimeUnit value
 * @throws GridStoreCommandException
 */
public static TimeUnit convertStringToTimeUnit(String unit) throws GridStoreCommandException {
	try {
		return TimeUnit.valueOf(unit.toUpperCase().trim());
	} catch (Exception e) {
		throw new GridStoreCommandException("Error occurred when converting to time unit"
				+ ": unit=[" + unit + "] msg=[" + e.getMessage() + "]", e);
	}
}

/**
 * Convert precise timestamp type to TimeUnit type
 *     TIMESTAMP(3) -> MILLISECOND
 *     TIMESTAMP(6) -> MICROSECOND
 *     TIMESTAMP(9) -> NANOSECOND
 *
 * @param preciseTimestampType the precise timestamp type string
 * @return TimeUnit
 * @throws GridStoreCommandException
 */
public static TimeUnit convertTimestampStringToTimeUnit(String preciseTimestampType) throws GridStoreCommandException {
	String type = preciseTimestampType.trim();
	if (type.equalsIgnoreCase(ToolConstants.COLUMN_TYPE_TIMESTAMP_MILI)) {
		return TimeUnit.MILLISECOND;
	} else if (type.equalsIgnoreCase(ToolConstants.COLUMN_TYPE_TIMESTAMP_MICRO)) {
		return TimeUnit.MICROSECOND;
	} else if (type.equalsIgnoreCase(ToolConstants.COLUMN_TYPE_TIMESTAMP_NANO)) {
		return TimeUnit.NANOSECOND;
	} else {
		throw new GridStoreCommandException("Error occurred when converting to type"
				+ ": type=[" + type + "] msg=[Not a precise timestamp type]");
	}
}

/**
 * Convert TimeUnit to String of type Timestamp in uppercase
 *     TimeUnit.MILLISECOND -> TIMESTAMP(3)
 *     TimeUnit.MICROSECOND -> TIMESTAMP(6)
 *     TimeUnit.NANOSECOND  -> TIMESTAMP(9)
 *
 * @param timeUnit The precision time unit of timestamp
 * @return String of TIMESTAMP type with numbered suffix
 * @throws GridStoreCommandException
 */
public static String convertTimeunitToTimestampType(TimeUnit timeUnit) throws GridStoreCommandException {
	switch (timeUnit) {
		case MILLISECOND :
			return ToolConstants.COLUMN_TYPE_TIMESTAMP_MILI.toUpperCase();
		case MICROSECOND :
			return ToolConstants.COLUMN_TYPE_TIMESTAMP_MICRO.toUpperCase();
		case NANOSECOND :
			return ToolConstants.COLUMN_TYPE_TIMESTAMP_NANO.toUpperCase();
		default :
			throw new GridStoreCommandException("Error occurred when converting time unit"
					+ ": type=[" + timeUnit.name() + "] msg=[Not a time unit]");
	}
}

/**
 * Check if a string is TIMESTAMP type  has suffix
 * The valid string is "TIMESTAMP(3)", "TIMESTAMP(6)", and "TIMESTAMP(9)"
 *
 * @param timestampTypeHasSuffix the TIMESTAMP type  has suffix
 * @return true if the given string is timestamp type with number
 */
public static boolean isTimestampStringInSeconds(String timestampTypeHasSuffix) {
	String type = timestampTypeHasSuffix.trim();
	return ToolConstants.COLUMN_TYPE_TIMESTAMP_MICRO.equalsIgnoreCase(type)
			|| ToolConstants.COLUMN_TYPE_TIMESTAMP_NANO.equalsIgnoreCase(type)
			|| ToolConstants.COLUMN_TYPE_TIMESTAMP_MILI.equalsIgnoreCase(type);
}

/**
 * Check if time unit is MILLISECOND, MICROSECOND or NANOSECOND
 *
 * @param timeUnit the precision time unit of timestamp
 * @return true if time unit is MILLISECOND, MICROSECOND or NANOSECOND
 */
public static boolean isTimestampUnit(TimeUnit timeUnit) {
	return TimeUnit.MILLISECOND == timeUnit
			|| TimeUnit.MICROSECOND == timeUnit
			|| TimeUnit.NANOSECOND == timeUnit;
}

/**
 * Check if a column is precise timestamp
 *
 * @param columnInfo GridStore column information
 * @return true if the given column is precise timestamp
 */
public static boolean isPreciseColumn(ColumnInfo columnInfo) {
	TimeUnit unit = columnInfo.getTimePrecision();
	return  unit == TimeUnit.MICROSECOND || unit == TimeUnit.NANOSECOND;
}

/**
 * Get the DateTimeFormatter base on time unit
 *
 * @param timeUnit The precision time unit of timestamp
 * @return date time format of time unit
 */
public static DateTimeFormatter getDateTimeFormatter(TimeUnit timeUnit) {
  String format = "";
  switch(timeUnit)
  {
    case MILLISECOND:
      format = ToolConstants.DATE_FORMAT_MILLISECOND;
      break;
    case MICROSECOND:
      format = ToolConstants.DATE_FORMAT_MICROSECOND;
      break;
    case NANOSECOND:
      format = ToolConstants.DATE_FORMAT_NANOSECOND;
      break;
    default:
      throw new IllegalArgumentException("Invalid time unit. Valid unit is MILLISECOND, MICROSECOND and NANOSECOND.");
  }
  return DateTimeFormatter.ofPattern(format);
}

/**
 * GSType をJSONタグに変換するメソッド
 *
 * @param type GSType
 * @return タイプ文字列
 */
private String convertColumnType(GSType type) throws GridStoreCommandException {
	try {
		if (GSType.STRING_ARRAY.equals(type)) {
			return ToolConstants.COLUMN_TYPE_STRING_ARRAY;
		} else if (GSType.BOOL_ARRAY.equals(type)) {
			return ToolConstants.COLUMN_TYPE_BOOL_ARRAY;
		} else if (GSType.BYTE_ARRAY.equals(type)) {
			return ToolConstants.COLUMN_TYPE_BYTE_ARRAY;
		} else if (GSType.SHORT_ARRAY.equals(type)) {
			return ToolConstants.COLUMN_TYPE_SHORT_ARRAY;
		} else if (GSType.INTEGER_ARRAY.equals(type)) {
			return ToolConstants.COLUMN_TYPE_INTEGER_ARRAY;
		} else if (GSType.LONG_ARRAY.equals(type)) {
			return ToolConstants.COLUMN_TYPE_LONG_ARRAY;
		} else if (GSType.FLOAT_ARRAY.equals(type)) {
			return ToolConstants.COLUMN_TYPE_FLOAT_ARRAY;
		} else if (GSType.DOUBLE_ARRAY.equals(type)) {
			return ToolConstants.COLUMN_TYPE_DOUBLE_ARRAY;
		} else if (GSType.TIMESTAMP_ARRAY.equals(type)) {
			return ToolConstants.COLUMN_TYPE_TIMESTAMP_ARRAY;
		} else if (GSType.BOOL.equals(type)) {
			return ToolConstants.COLUMN_TYPE_BOOL;
		} else {
			return type.toString().toLowerCase();
		}
	} catch (Exception e) {
		// "カラム種別の変換処理でエラーが発生しました"
		//throw new GridStoreCommandException(messageResource.getString("MESS_COMM_ERR_METAINFO_30")+ ": type=["
		throw new GridStoreCommandException("Error occurred when converting to type"+ ": type=["
				+type+"] msg=[" + e.getMessage()+"]", e);
	}
}


/**
 * UTF-8のBOMスキップ処理メソッド
 *
 * @param in ファイル入力ストリーム
 * @return ファイル入力ストリーム
 */
public static InputStream skipBOM(InputStream in) throws Exception{
	if (!in.markSupported()) {
		in = new BufferedInputStream(in);
	}
	in.mark(3);
	if (in.available() >= 3) {
		byte b[] = { 0, 0, 0 };
		in.read(b, 0, 3);
		if (b[0] != (byte) 0xEF || b[1] != (byte) 0xBB || b[2] != (byte) 0xBF) {
			in.reset();
		}
	}
	return in;
}

}
