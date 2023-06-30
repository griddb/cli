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
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import com.toshiba.mwcloud.gs.ColumnInfo;
import com.toshiba.mwcloud.gs.CompressionMethod;
import com.toshiba.mwcloud.gs.ContainerInfo;
import com.toshiba.mwcloud.gs.ContainerType;
import com.toshiba.mwcloud.gs.GSType;
import com.toshiba.mwcloud.gs.IndexInfo;
import com.toshiba.mwcloud.gs.IndexType;
import com.toshiba.mwcloud.gs.TimeSeriesProperties;
import com.toshiba.mwcloud.gs.TimeUnit;
import com.toshiba.mwcloud.gs.TriggerInfo;
import com.toshiba.mwcloud.gs.experimental.ExtendedContainerInfo;
import com.toshiba.mwcloud.gs.tools.common.GridStoreCommandException;
import com.toshiba.mwcloud.gs.tools.common.data.ToolConstants.RowFileType;
/**
 * コンテナ情報クラス
 */
public class ToolContainerInfo {

	/**
	 * Java APIのコンテナ情報オブジェクト
	 */
	private ContainerInfo m_conInfo;

	/**
	 * カラム情報リスト
	 */
	private List<ColumnInfo> m_columnInfoList;

	/**
	 * 索引情報リスト(V3.5～)
	 */
	private List<IndexInfo> m_indexInfoList;

	/**
	 * トリガ情報リスト
	 */
	private List<TriggerInfo> m_triggerInfoList;

	/**
	 * 時系列情報
	 */
	private TimeSeriesProperties m_timeSeriesProperties;

	/**
	 * パーティション番号
	 */
	private int partitionNo = 0;

	/**
	 * データベース名
	 */
	private String m_dbName;

	/**
	 * 互換性オプション  (バージョン間の互換性を担保する）
	 */
	private boolean m_compatibleOption;

	/**
	 * メタ情報ファイルのバージョン
	 */
	private String m_version;

	/**
	 * サブコンテナのコンテナ情報リスト
	 */
	//private List<ToolContainerInfo> m_subContainerInfoList;

	/**
	 * パーティショニングテーブル情報リスト
	 * <p>
	 * [0]がパーティション、[1]がサブパーティションの情報をそれぞれ表す。
	 */
	private List<TablePartitionProperty> m_tablePartitionProperties;


	/**
	 * タイムインターバル情報リスト
	 */
	private List<TimeIntervalInfo> m_timeIntervalInfos;
	
	/**
	 * 期限解放情報
	 * <p>
	 * @see ExpirationInfo
	 */
	private ExpirationInfo m_expirationInfo = null;


	// [gs_sh]----------------------------------------
	/**
	 * コンテナ属性文字列
	 */
	private String m_attribute = "SINGLE";

	// [Exp/Imp]--------------------------------------
	/**
	 * ロウデータファイル形式 (Exp/Imp用)
	 */
	private RowFileType containerFileType;

	/**
	 * コンテナ情報／ロウデータファイル名  (Exp/Imp用)
	 */
	private List<String> containerFileList;
	/**
	 * バイナリマルチの場合のZipファイル内のファイル名(Exp/Imp用)
	 */
	private String containerInternalFileName;

	/**
	 * ファイル名のベース  (Exp/Imp用)
	 */
	private String fileBaseName;

	/**
	 * ロウデータファイルの境界値(日付)
	 */
	private String m_intervals = null;
	
	/**
	 * 	条件式文字列　--fileterFile で一致した条件式を
	 */
	private String filterCondition = null;
	// -----------------------------------------------

	/**
	 * コンテナ情報の整合性チェックでエラーになった箇所のメッセージ
	 */
	private StringBuilder m_msg;

	/**
	 * JSONにキー"rowKeyAssigned"があった場合の値
	 */

	private Boolean JSONRowKeyAssignedValue = null;

	/**
	 * JSONのキー"rowKeySet"の値
	 */
	private List<String> JSONrowKeySetStringValueList = null;

	/**
	 * コンストラクタ
	 */
	public ToolContainerInfo() {
		m_indexInfoList = new ArrayList<IndexInfo>();
		m_columnInfoList = new ArrayList<ColumnInfo>();
		m_triggerInfoList = new ArrayList<TriggerInfo>();
		m_conInfo = new ExtendedContainerInfo(null, null, m_columnInfoList, false, null);

		m_tablePartitionProperties = new ArrayList<TablePartitionProperty>();

		m_timeIntervalInfos = new ArrayList<TimeIntervalInfo>();
		// デフォルト値
		// V3.5～ BASEからSINGLEへ変更
		//m_conInfo.setAttribute(ContainerAttribute.SINGLE);

		// Exp/Imp
		//containerFileList = new ArrayList<String>();
	}

	/**
	 * コンストラクタ
	 * @param conInfo コンテナ情報オブジェクト
	 */
	public ToolContainerInfo(ExtendedContainerInfo conInfo){
		updateContainerInfo(conInfo);

		// Exp/Imp
		containerFileList = new ArrayList<String>();
	}

	/**
	 * 引数で与えられたオブジェクトの情報をコピーします。
	 *
	 * 注意： 完全なオブジェクトコピーではない。
	 *      メンバ変数の一部のオブジェクトは、コピー元のオブジェクトと共通になる。(そのため、コピーコンストラクタ・cloneの実装ではない）
	 *
	 *     ExtendedContainerInfo.Timeseries など
	 *
	 *     ExtendedContainerInfo自体は、newされるので別オブジェクトになる。
	 *
	 * @param tcInfo
	 */
	public void copyObject(ToolContainerInfo tcInfo){

		m_indexInfoList = tcInfo.getIndexInfoList();		// 参照コピー
		m_columnInfoList = tcInfo.getColumnInfoList();		// 参照コピー
		m_triggerInfoList = tcInfo.getTriggerInfoList();	// 参照コピー
		m_timeSeriesProperties = tcInfo.getTimeSeriesProperties();	// 参照コピー
		m_tablePartitionProperties = tcInfo.getTablePartitionProperties();	// 参照コピー
		m_timeIntervalInfos = tcInfo.getTimeIntervalInfos();	// 参照コピー
		m_expirationInfo = tcInfo.getExpirationInfo();

		partitionNo = tcInfo.getPartitionNo();
		m_dbName = tcInfo.getDbName();
		m_version = tcInfo.getVersion();
		containerFileType = tcInfo.getContainerFileType();
		containerFileList = tcInfo.getContainerFileList();			// 参照コピー
		containerInternalFileName = tcInfo.getContainerInternalFileName();			// 参照コピー
		fileBaseName = tcInfo.getFileBaseName();
		filterCondition = tcInfo.getFilterCondition();

		// ExtendedContainerInfo
		//ExtendedContainerInfo eInfo = tcInfo.getContainerInfo();
		//m_conInfo.setAttribute(eInfo.getAttribute());
		ContainerInfo cInfo = tcInfo.getContainerInfo();
		m_conInfo.setColumnInfoList(tcInfo.getColumnInfoList());
		m_conInfo.setDataAffinity(cInfo.getDataAffinity());
		m_conInfo.setName(cInfo.getName());
		// V4.3 RowKeyAssignedのコピーしない
		// m_conInfo.setRowKeyAssigned(cInfo.isRowKeyAssigned());
		// V4.3 rowKeyColumnListの値をコピー
		m_conInfo.setRowKeyColumnList(cInfo.getRowKeyColumnList());	// 参照コピー
		m_conInfo.setTimeSeriesProperties(cInfo.getTimeSeriesProperties());	// 参照コピー
		m_conInfo.setTriggerInfoList(cInfo.getTriggerInfoList());			// 参照コピー
		m_conInfo.setType(cInfo.getType());
		// V4.3 JSONRowKeyAssignedValueの値をコピー
		JSONRowKeyAssignedValue = tcInfo.getJSONRowKeyAssignedValue();
		// V4.3 JSONrowKeySetStringValueListの値をコピー
		JSONrowKeySetStringValueList = tcInfo.getJSONrowKeySetStringValueList(); // 参照コピー
	}


	/**
	 * コンテナ情報オブジェクトを返します。
	 * @return コンテナ情報オブジェクト
	 */
	public ContainerInfo getContainerInfo(){
		// ためておいた設定を反映してから返す
		m_conInfo.setColumnInfoList(m_columnInfoList);
		m_conInfo.setTriggerInfoList(m_triggerInfoList);
		m_conInfo.setTimeSeriesProperties(m_timeSeriesProperties);

		return m_conInfo;
	}

	public void setContainerInfo(ContainerInfo contInfo){
		// 内部情報を更新する
		updateContainerInfo(contInfo);
	}

	private void updateContainerInfo(ContainerInfo contInfo){

		m_conInfo = contInfo;
		m_indexInfoList = contInfo.getIndexInfoList();
		m_columnInfoList = new ArrayList<ColumnInfo>();

		if ( m_conInfo != null ){

			for ( int i = 0; i < m_conInfo.getColumnCount(); i++ ){
				ColumnInfo columnInfo = m_conInfo.getColumnInfo(i);
				m_columnInfoList.add(columnInfo);
			}

			m_triggerInfoList = m_conInfo.getTriggerInfoList(); //未設定の場合は空リストが返る
			m_timeSeriesProperties = m_conInfo.getTimeSeriesProperties(); // 未設定の場合はnullが返る
		}
	}

	/**
	 * メタ情報ファイルのバージョンを設定します。
	 *
	 * @param version バージョン
	 */
	public void setVersion(String version){
		m_version = version;
	}

	/**
	 * メタ情報ファイルのバージョンを返します。
	 *
	 * @param バージョン
	 */
	public String getVersion(){
		return m_version;
	}

	/**
	 * コンテナ名を設定します。
	 * @param name コンテナ名
	 */
	public void setName(String name) {
		m_conInfo.setName(name);
	}
	/**
	 * コンテナ名を返します。
	 * @retrun コンテナ名
	 */
	public String getName(){
		return m_conInfo.getName();
	}
	/**
	 * フルコンテナ名（DB名も含む）を返します。
	 * @return フルコンテナ名
	 */
	public String getFullName(){
		if ( (m_dbName == null) || (m_dbName.length() == 0) ){
			return m_conInfo.getName();
		} else {
			return m_dbName + ToolConstants.DB_DELIMITER + m_conInfo.getName();
		}
	}

	/**
	 * コンテナタイプを設定します。
	 * @param type コンテナタイプ
	 */
	public void setType(ContainerType type){
		m_conInfo.setType(type);
	}

	/**
	 * コンテナタイプを設定します。
	 * @param type コンテナタイプ(文字列)
	 * @throws GSEIException
	 */
	public void setType(String type) throws GridStoreCommandException{
		if ( type == null || type.length() == 0 ){
			// コンテナタイプは省略不可
			throw new GridStoreCommandException("Invalid value '"+ ToolConstants.JSON_META_CONTAINER_TYPE +"'. : value=["+type+"]");
		}
		if (type.equalsIgnoreCase(ContainerType.COLLECTION.toString())) {
			m_conInfo.setType(ContainerType.COLLECTION);

		} else if (type.equalsIgnoreCase(ContainerType.TIME_SERIES.toString())) {
			m_conInfo.setType(ContainerType.TIME_SERIES);

		} else {
			throw new GridStoreCommandException("Invalid value '"+ ToolConstants.JSON_META_CONTAINER_TYPE +"'. : value=["+type+"]");
		}
	}
	/**
	 * コンテナタイプを返します。
	 * @return コンテナタイプ
	 */
	public ContainerType getType(){
		return m_conInfo.getType();
	}

	/**
	 * コンテナ属性を設定します。
	 * @param arg コンテナ属性
	 */
//	public void setAttribute(String arg){
//		m_conInfo.setAttribute(ContainerAttribute.valueOf(arg));
//	}
	/**
	 * コンテナ属性を返します。
	 * @return コンテナ属性
	 */
//	public ContainerAttribute getAttribute(){
//		return m_conInfo.getAttribute();
//	}

	/**
	 * データアフィニティ文字列を設定します。
	 * @param dataAffinity データアフィニティ
	 */
	public void setDataAffinity(String dataAffinity) throws GridStoreCommandException{
		if ( m_compatibleOption ){
			return;
		}
		if ( dataAffinity == null || dataAffinity.length() == 0 ){
			// データアフィニティは省略可
			return;
		}
		dataAffinity = dataAffinity.trim();
		if ( dataAffinity.length() > 8 ){ // 8文字以上はエラー(サーバ仕様)
			String msg = "DataAffinity name is over 8 characters"+"'. : dataAffinityName=["+dataAffinity+"]";
			throw new GridStoreCommandException(msg);
		}
		m_conInfo.setDataAffinity(dataAffinity);
	}
	/**
	 * データアフィニティを返します。
	 * @return データアフィニティ
	 */
	public String getDataAffinity(){
		if ( m_compatibleOption ){
			return null;
		}
		return m_conInfo.getDataAffinity();
	}

	/**
	 * ロウキーを設定します。(Collectionの時のみ有効)
	 * @param assigned true/false
	 */
	public void setRowKeyAssigned(boolean assigned){
		m_conInfo.setRowKeyAssigned(assigned);
	}

	/**
	 * ロウキー設定値を返します。
	 * @return true/false
	 */
	public boolean getRowKeyAssigned(){
		return m_conInfo.isRowKeyAssigned();
	}

	/**
	 * ロウキーのカラム番号のリストを設定します。
	 *
	 * @param rowKeyColumnList List<Integer> ロウキーのカラム番号のリスト
	 */
	public void setRowKeyColumnList(List<Integer> rowKeyColumnList) {
		m_conInfo.setRowKeyColumnList(rowKeyColumnList);
	}
	/**
	 * ロウキーのカラム番号のリストを返します。
	 *
	 * @return List<Integer> ロウキーのカラム番号のリスト
	 */
	public List<Integer> getRowKeyColumnList() {
		return m_conInfo.getRowKeyColumnList();
	}

	/**
	 * パーティション番号を設定します。
	 *
	 * @param arg int パーティション番号
	 */
	public void setPartitionNo(int no) {
		partitionNo = no;
	}
	/**
	 * パーティション番号情報を返します。
	 *
	 * @return int パーティション番号
	 */
	public int getPartitionNo() {
		return partitionNo;
	}

	/**
	 * データベース名を設定します。
	 *
	 * @param dbName データベース名
	 */
	public void setDbName(String dbName){
		m_dbName = dbName;
	}

	/**
	 * データベース名を返します。
	 *
	 * @return データベース名
	 */
	public String getDbName(){
		return m_dbName;
	}

	//=====================================================================
	// カラム情報
	//=====================================================================
	/**
	 * カラム情報を追加します。
	 * @param columnInfo カラム情報オブジェクト
	 */
	public void addColumnInfo(ColumnInfo columnInfo){
		m_columnInfoList.add(columnInfo);
	}
	/**
	 * カラム情報を追加します。
	 * @param columnName カラム名
	 * @param columnType カラムタイプ
	 */
	public void addColumnInfo(String columnName, GSType columnType){
		m_columnInfoList.add(new ColumnInfo(columnName, columnType));
	}

	public void setColumnInfoList(List<ColumnInfo> columnInfoList){
		m_columnInfoList = columnInfoList;
	}
	/**
	 * カラム情報を返します。
	 * @param index カラム番号 (0から)
	 * @return カラム情報
	 */
	public ColumnInfo getColumnInfo(int index){
		return m_columnInfoList.get(index);
	}
	/**
	 * 全カラム情報を返します。
	 * @return カラム情報リスト
	 */
	public List<ColumnInfo> getColumnInfoList(){
		return m_columnInfoList;
	}
	/**
	 * カラムの数を返します。
	 * @return カラム数
	 */
	public int getColumnCount(){
		return m_columnInfoList.size();
	}

	//=====================================================================
	// トリガ
	//=====================================================================
	/**
	 * トリガ情報を追加します。
	 * @param triggerInfo トリガ情報オブジェクト
	 */
	public void addTriggerInfo(TriggerInfo triggerInfo){
		if ( m_triggerInfoList == null ){
			m_triggerInfoList = new ArrayList<TriggerInfo>();
		}
		m_triggerInfoList.add(triggerInfo);
	}
	public void setTriggerInfoList(List<TriggerInfo> triggerInfoList){
		m_triggerInfoList = triggerInfoList;
	}
	/**
	 * トリガ情報一覧を返します。
	 * @return トリガ情報一覧
	 */
	public List<TriggerInfo> getTriggerInfoList(){
		return m_triggerInfoList;
	}

	/**
	 * 索引情報を追加します。
	 *
	 * @param columnName カラム名
	 * @param indexType 索引タイプ
	 * @param indexName 索引名
	 * @since V3.5
	 */
	public void addIndexInfo(String columnName, IndexType indexType, String indexName) {
		if ( m_indexInfoList == null ) {
			m_indexInfoList = new ArrayList<IndexInfo>();
		}
		IndexInfo index = IndexInfo.createByColumn(columnName, indexType);
		if ( indexName != null && !indexName.isEmpty() ) {
			index.setName(indexName);
		}
		m_indexInfoList.add(index);
	}

	/**
	 * 索引情報を追加します。
	 *
	 * @param columnNameList カラム名のリスト
	 * @param indexType 索引タイプ
	 * @param indexName 索引名
	 * @since V4.3
	 */
	public void addIndexInfo(List<String> columnNameList, IndexType indexType, String indexName) {
		// V4.3 複合索引対応 カラム名のリストからIndexInfoを作成
		if ( m_indexInfoList == null ) {
			m_indexInfoList = new ArrayList<IndexInfo>();
		}
		IndexInfo index = new IndexInfo();
		index.setColumnNameList(columnNameList);
		index.setType(indexType);
		if ( indexName != null && !indexName.isEmpty() ) {
			index.setName(indexName);
		}
		m_indexInfoList.add(index);
	}

	/**
	 * 索引情報を返します。
	 * @return 索引情報リスト
	 * @since V3.5
	 */
	public List<IndexInfo> getIndexInfoList() {
		return m_indexInfoList;
	}

	//=====================================================================
	// 時系列情報
	//=====================================================================
	/**
	 * 時系列情報を設定します。
	 * （setCompression***メソッドなどで、個別に値を設定することもできます）
	 *
	 * @param timeProp 時系列情報オブジェクト
	 */
	public void setTimeSeriesProperties(TimeSeriesProperties timeProp){
		m_timeSeriesProperties = timeProp;
	}

	public TimeSeriesProperties getTimeSeriesProperties(){
		return m_timeSeriesProperties;
	}

	/**
	 * 圧縮方式(NO,SS,HI)を設定します。
	 *
	 * @param compressionMethod 圧縮方式
	 */
	public void setCompressionMethod(CompressionMethod compressionMethod){
		if ( m_timeSeriesProperties == null ){
			m_timeSeriesProperties = new TimeSeriesProperties();
		}
		m_timeSeriesProperties.setCompressionMethod(compressionMethod);
	}

	/**
	 * 圧縮方式を返します。
	 * @return
	 */
	public CompressionMethod getCompressionMethod(){
		return m_timeSeriesProperties.getCompressionMethod();
	}

	/**
	 * 圧縮方式(NO,SS,HI)を設定します。
	 *
	 * @param compressionMethodString 圧縮方式(文字列)
	 */
	public void setCompressionMethod(String compressionMethodString) throws GridStoreCommandException{
		if ( m_timeSeriesProperties == null ){
			m_timeSeriesProperties = new TimeSeriesProperties();
		}
		try {
			m_timeSeriesProperties.setCompressionMethod(CompressionMethod.valueOf(compressionMethodString.toUpperCase()));
		} catch ( IllegalArgumentException e ){
			throw new GridStoreCommandException("'"+ToolConstants.JSON_META_TIME_COMP+"' is invalid. value=["
						+compressionMethodString+"]", e);
		}
	}

	/**
	 * 有効期間分割数を設定します。
	 *
	 * @since NoSQL 1.5
	 * @param count
	 */
	public void setExpirationDivisionCount(int count){
		if ( !m_compatibleOption ){
			if ( m_timeSeriesProperties == null ){
				m_timeSeriesProperties = new TimeSeriesProperties();
			}
			if ( count != -1 ){
				m_timeSeriesProperties.setExpirationDivisionCount(count);
			}
		}
	}

	/**
	 * ロウの有効期限を設定します。
	 *
	 * @param elapsedTime 有効期限
	 * @param timeUnit 時間単位
	 */
	public void setRowExpiration(int elapsedTime, TimeUnit timeUnit) throws GridStoreCommandException{
		if ( m_timeSeriesProperties == null ){
			m_timeSeriesProperties = new TimeSeriesProperties();
		}
		if ( timeUnit == null ){
			throw new GridStoreCommandException("RowExpirationTimeUnit must not be null.");
		}
		m_timeSeriesProperties.setRowExpiration(elapsedTime, timeUnit);
	}

	/**
	 * 間引き圧縮(SS, HI)の時、連続して間引きされるロウの最大期間を設定します。
	 *
	 * @param elapsedTime 期間
	 * @param timeUnit 時間単位
	 */
	public void setCompressionWindowSize(int compressionWindowSize, TimeUnit compressionWindowSizeUnit) throws GridStoreCommandException {
		if ( m_timeSeriesProperties == null ){
			m_timeSeriesProperties = new TimeSeriesProperties();
		}
		if ( compressionWindowSizeUnit == null ){
			throw new GridStoreCommandException("compressionWindowSizeUnit must not be null.");
		}
		m_timeSeriesProperties.setCompressionWindowSize(compressionWindowSize, compressionWindowSizeUnit);
	}

	public void setRelativeHiCompression(String column, double rate, double span) throws GridStoreCommandException{
		if ( m_timeSeriesProperties == null ){
			m_timeSeriesProperties = new TimeSeriesProperties();
		}
		if ( getCompressionMethod() != CompressionMethod.HI ){
			throw new GridStoreCommandException("CompressionMethod must be 'HI' when ReletiveHiCompression is specified. method=["+getCompressionMethod()+"]");
		}
		if (!(0 <= rate && rate <= 1)) {
			throw new GridStoreCommandException("The value of Rate for compression must be '0 <= and <=1'. name=["+getName()+"] column=["+column+"]");
		}
		m_timeSeriesProperties.setRelativeHiCompression(column, rate, span);
	}

	public void setAbsoluteHiCompression(String column, double width) throws GridStoreCommandException{
		if ( m_timeSeriesProperties == null ){
			m_timeSeriesProperties = new TimeSeriesProperties();
		}
		if ( getCompressionMethod() != CompressionMethod.HI ){
			throw new GridStoreCommandException("CompressionMethod must be 'HI' when AbsoluteHiCompression is specified. method=["+getCompressionMethod()+"]");
		}
		m_timeSeriesProperties.setAbsoluteHiCompression(column, width);
	}

	/*
	public void addSubContainerInfo(ToolContainerInfo subInfo){
		if ( m_subContainerInfoList == null ){
			m_subContainerInfoList = new ArrayList<ToolContainerInfo>();
		}
		m_subContainerInfoList.add(subInfo);
	}
	public List<ToolContainerInfo> getSubContainerList(){
		return m_subContainerInfoList;
	}
	*/


	//--------------------------------------------------
	// Exp/Imp
	//--------------------------------------------------
	/**
	 * ロウデータファイルの種類を設置します。
	 *
	 * @param arg ロウデータファイル形式(csv/binary)
	 */
	public void setContainerFileType(String fileType) throws GridStoreCommandException{
		if ( fileType.toUpperCase().equalsIgnoreCase(RowFileType.CSV.toString()) ){
			containerFileType = RowFileType.CSV;

		} else if ( fileType.toUpperCase().equalsIgnoreCase(RowFileType.BINARY.toString()) ){
			containerFileType = RowFileType.BINARY;

		} else if ( fileType.toUpperCase().equalsIgnoreCase(RowFileType.AVRO.toString()) ){
			containerFileType = RowFileType.AVRO;

		} else if ( fileType.toUpperCase().equalsIgnoreCase(RowFileType.ARCHIVE_CSV.toString()) ){
			containerFileType = RowFileType.ARCHIVE_CSV;

		} else {
			String msg = "Invalid value was specified as '"+ToolConstants.JSON_META_CONTAINER_FILE_TYPE
					+"'. : value=["+fileType+"]";
			throw new GridStoreCommandException(msg);
		}
	}
	public void setContainerFileType(RowFileType fileType){
		containerFileType = fileType;
	}

	/**
	 * コンテナ情報／ロウデータファイル名設定メソッド
	 *
	 * @param arg コンテナ情報／ロウデータファイル名
	 */
	public void setContainerFile(List<String> list){
		containerFileList = list;
	}
	public void setContainerInternalFileName(String fileName){
		containerInternalFileName = fileName;
	}
	/*
	public void setContainerFile(String arg) {
		containerFileList = new ArrayList<String>(1);
		containerFileList.add(arg);

	}
	*/
	/*
	public void setContainerFile(String arg, boolean append) {
		if ( append ){
			containerFileList.add(arg);
		} else {
			setContainerFile(arg);
		}
	}
	*/



	/**
	 * ロウデータファイルの名前を設定します。
	 * @param fileName
	 * @throws GSEIException
	 */
	public void addContainerFile(String fileName) throws GridStoreCommandException {
		addContainerFile(fileName, null);
	}

	/**
	 * ロウデータファイルの名前を設定します。ファイルの存在確認も行います。
	 * @param fileName
	 * @param dirPath
	 * @throws GSEIException
	 */
	public void addContainerFile(String fileName, String dirPath) throws GridStoreCommandException {
		if ( fileName == null || fileName.length() == 0 ) {
			throw new GridStoreCommandException("'"+ToolConstants.JSON_META_CONTAINER_FILE+"' is required.");
		}
		if ( dirPath != null ){
			// データファイルの存在確認
			File dataFile = new File(dirPath, fileName);
			if ( !dataFile.exists() ){
				throw new GridStoreCommandException("Data File not found.: dataFile=["+dataFile.getAbsolutePath()+"]");
			}
		}

		if ( containerFileList == null ){
			containerFileList = new ArrayList<String>();
		}
		containerFileList.add(fileName);

	}





	/**
	 * 出力ファイル名の基幹部分を設定します。
	 *
	 * @param arg
	 */
	public void setFileBaseName(String arg){
		fileBaseName = arg;
	}

	/**
	 * 出力ファイル名の基幹部分を返します。
	 * @return
	 */
	public String getFileBaseName(){
		return fileBaseName;
	}


	/**
	 * ロウデータファイルの境界値(日付)を設定します。
	 * @param intervals
	 */
	public void setIntervals(String intervals) {
		m_intervals = intervals;
	}

	/**
	 * ロウデータファイルの境界値(日付)を返します。
	 * @return
	 */
	public String getIntervals() {
		return m_intervals;
	}

	public void setFilterCondition(String str) {
		filterCondition = str;
	}

	public String getFilterCondition() {
		return filterCondition;
	}

	/**
	 * ロウデータファイル形式取得メソッド
	 *
	 * @return ロウデータファイル形式
	 */
	public RowFileType getContainerFileType() {
		return containerFileType;
	}


	/**
	 * コンテナ情報／ロウデータファイル名取得メソッド
	 *
	 * @return コンテナ情報／ロウデータファイル名
	 */
	public String getContainerFile() {
		if ( (containerFileList!=null) && (containerFileList.size() > 0) ){
			return containerFileList.get(0);
		} else {
			return null;
		}
	}
	public List<String> getContainerFileList() {
		return containerFileList;
	}

	public String getContainerInternalFileName() {
		return containerInternalFileName;
	}


	/**
	 * (互換性用)コンテナ属性文字列の取得
	 * @return m_attribute コンテナ属性文字列
	 */
	public String getAttribute() {
		return m_attribute;
	}

	/**
	 * (互換性用)コンテナ属性文字列の設定
	 * @param attribute コンテナ属性文字列
	 */
	public void setAttribute(String attribute) {
		m_attribute = attribute;
	}

	/**
	 * テーブルパーティショニング情報リストの取得
	 * @return m_tablePartitionProperties テーブルパーティショニング情報リスト
	 */
	public List<TablePartitionProperty> getTablePartitionProperties() {
		return m_tablePartitionProperties;
	}

	/**
	 * テーブルパーティショニング情報リストの設定
	 * @param tablePartitionProperties テーブルパーティショニング情報リスト
	 */
	public void setTablePartitionProperties(List<TablePartitionProperty> properties) {
		m_tablePartitionProperties = properties;
	}

	/**
	 * タイムインターバル情報リストの取得
	 * @return m_timeIntervalInfos タイムインターバル情報リスト
	 */
	public List<TimeIntervalInfo> getTimeIntervalInfos() {
		return m_timeIntervalInfos;
	}
	
	/**
	 * タイムインターバル情報リストの設定
	 * @param m_timeIntervalInfos タイムインターバル情報リスト
	 */
	public void setTimeIntervalInfos(List<TimeIntervalInfo> timeIntervalInfos) {
		m_timeIntervalInfos = timeIntervalInfos;
	}	
	
	/**
	 * 期限解放情報の取得
	 * @return m_expirationInfo 期限解放情報
	 */
	public ExpirationInfo getExpirationInfo() {
		return m_expirationInfo;
	}

	/**
	 * 期限解放情報の設定
	 * @param info 期限解放情報
	 * @throws GridStoreCommandException
	 */
	public void setExpirationInfo(ExpirationInfo info) throws GridStoreCommandException {
		// NULLチェック
		if (info != null) {
			if ( info.getType() == null ) {
				throw new GridStoreCommandException("expirationType must not be null.");
			}
			if ( info.getTimeUnit() == null ){
				throw new GridStoreCommandException("expirationTimeUnit must not be null.");
			}
		}
		this.m_expirationInfo = info;
	}

	/**
	 * JSONのキー"rowKeyAssigned"の値の設定
	 * @param rowKeyAssignedValue JSONのキー"rowKeyAssigned"の値
	 */
	public void setJSONRowKeyAssignedValue(Boolean rowKeyAssignedValue) {
		JSONRowKeyAssignedValue = rowKeyAssignedValue;
	}

	/**
	 * JSONのキー"rowKeyAssigned"の値の取得
	 * @return JSONRowKeyAssignedValue JSONにキー"rowKeyAssigned"があった場合の値
	 */
	public Boolean getJSONRowKeyAssignedValue() {
		return JSONRowKeyAssignedValue;
	}

	/**
	 * JSONのキー"rowKeySet"の値の設定
	 * @param rowKeyAssignedValue JSONのキー"rowKeyAssigned"の値
	 */
	public void setJSONrowKeySetStringValueList(List<String> rowKeySetValueList) {
		JSONrowKeySetStringValueList = rowKeySetValueList;
	}

	/**
	 * JSONのキー"rowKeySet"の値の取得
	 * @return JSONrowKeySetValueList JSONにキー"rowKeySet"があった場合の値
	 */
	public List<String> getJSONrowKeySetStringValueList() {
		return JSONrowKeySetStringValueList;
	}

	/**
	 * テーブルパーティショニング設定が存在するかどうか
	 * @return 存在すればtrue
	 */
	public boolean isPartitioned() {
		return !m_tablePartitionProperties.isEmpty();
	}

	/**
	 * CREATE文のWITH句に書く必要のあるプロパティを含むか
	 * <p>
	 * (V4.0)時系列オプションを保持していればtrue
	 * <p>
	 * (V4.1)期限解放情報を保持していればtrue
	 */
	public boolean hasAdditionalProperty() {
		// 時系列オプション
		if (m_timeSeriesProperties != null) {
			return true;
		}

		// 期限解放情報
		if (m_expirationInfo != null) {
			return true;
		}

		return false;
	}

	/**
	 * メタ情報からテーブルのCREATE文を組み立てます。
	 * @param metaInfo
	 * @return CREATE文
	 */
	public String buildCreateTableStatement() {
		StringBuilder builder = new StringBuilder();
		// V4.3 PRIMARY KEY(カラム名)でロウキー指定する
		StringBuilder primaryKeyBuilder = new StringBuilder();
		// V4.3 ContainerInfo.getRowKeyColumnList()より取得した値よりロウキーであるかを判断
		List<Integer> rowKeyColumnList = this.getRowKeyColumnList();
		builder.append("CREATE TABLE \"");
		builder.append(this.getName());
		builder.append("\" (\"");

		// カラム情報
		for (int i = 0; i < this.getColumnCount(); i++) {
			ColumnInfo col = this.getColumnInfo(i);
			if (i != 0) {
				builder.append(",\"");
			}
			builder.append(col.getName());
			builder.append("\" ");
			builder.append(col.getType());
			// V4.3 getRowKeyAssigned()で判定しない
			// V4.3 PRIMARY KEY(カラム名)でロウキー指定する
			// if (i == 0 && this.getRowKeyAssigned()) {
			// 	builder.append(" PRIMARY KEY");
			// } else if (col.getNullable() != null && !col.getNullable()) {
			// 	builder.append(" NOT NULL");
			// }
			// V4.3 ContainerInfo.getRowKeyColumnList()より取得した値よりロウキーであるかを判断
			if (isRowKeyColumn(i, rowKeyColumnList)) {
				if (primaryKeyBuilder.length() != 0) {
					primaryKeyBuilder.append("\"");
					primaryKeyBuilder.append(", ");
					primaryKeyBuilder.append("\"");
				}
				primaryKeyBuilder.append(col.getName());
			}
			if (col.getNullable() != null && !col.getNullable()) {
				builder.append(" NOT NULL");
			}
		}
		// V4.3 PRIMARY KEY(カラム名)でロウキー指定する
		if (primaryKeyBuilder.length() != 0) {
			builder.append(" ,PRIMARY KEY(\"");
			builder.append(primaryKeyBuilder.toString());
			builder.append("\")");
		}
		builder.append(") ");

		// 時系列テーブル
		if (this.getType().equals(ContainerType.TIME_SERIES)) {
			builder.append("USING TIMESERIES ");
		}

		//WITH (プロパティキー=プロパティ値, ...)"の形式で指定することができます。
		// (V4.0) ロウ期限解放
		// (V4.1) パーティション期限解放
		if (this.hasAdditionalProperty()) {
			StringBuilder timePropertyStr = new StringBuilder();
			boolean additional = false;

			// 時系列ロウ期限解放
			if (this.getTimeSeriesProperties() != null) {
				TimeSeriesProperties tsProp = this.getTimeSeriesProperties();
				if ( tsProp.getRowExpirationTime() != -1 ){
					timePropertyStr.append("expiration_time=");
					timePropertyStr.append(tsProp.getRowExpirationTime());
					additional = true;
				}
				if ( tsProp.getRowExpirationTimeUnit() != null ){
					if ( additional ) timePropertyStr.append(",");
					timePropertyStr.append("expiration_time_unit='");
					timePropertyStr.append(tsProp.getRowExpirationTimeUnit());
					timePropertyStr.append("'");
					additional = true;
				}
				if ( tsProp.getExpirationDivisionCount() != -1 ){
					if ( additional ) timePropertyStr.append(",");
					timePropertyStr.append("expiration_division_count=");
					timePropertyStr.append(tsProp.getExpirationDivisionCount());
					additional = true;
				}
				if ( additional == true ){
					// (V4.3.1) CREATE TABLEにてデータアフィニティを設定
					builder.append("WITH(");
					if (this.getDataAffinity() != null) {
						builder.append("DATA_AFFINITY='" + this.getDataAffinity() + "',");
					}
					builder.append("expiration_type='ROW',");
					builder.append(timePropertyStr);
					builder.append(")");
				}
			}

			// ロウ期限解放以外の期限解放
			// ロウ期限解放と同時指定不可
			if (additional != true && this.getExpirationInfo() != null) {
				ExpirationInfo expInfo = this.getExpirationInfo();
				// (V4.3.1) CREATE TABLEにてデータアフィニティを設定
				builder.append("WITH(");
				if (this.getDataAffinity() != null) {
					builder.append("DATA_AFFINITY='" + this.getDataAffinity() + "',");
				}
				builder.append(String.format("expiration_type='%s', expiration_time=%d, expiration_time_unit='%s') ",
						expInfo.getType(), expInfo.getTime(), expInfo.getTimeUnit().toString()));
			} else if (additional != true && this.getExpirationInfo() == null) {
				// 時系列オプションを保持しているが期限解放情報を保持していない場合（CREATE TABLEにてexpiration_typeを指定しない場合）
				// (V4.3.1) データアフィニティの情報がある場合、CREATE TABLEにてデータアフィニティを設定
				if (this.getDataAffinity() != null) {
					builder.append("WITH(");
					builder.append("DATA_AFFINITY='" + this.getDataAffinity()+ "'");
					builder.append(")");
				}
			}
		} else {
			// 時系列オプションを保持していない AND 期限解放情報を保持していない場合（CREATE TABLEにてexpiration_typeを指定しない場合）
			// (V4.3.1) データアフィニティの情報がある場合、CREATE TABLEにてデータアフィニティを設定
			if (this.getDataAffinity() != null) {
				builder.append("WITH(");
				builder.append("DATA_AFFINITY='" + this.getDataAffinity()+ "'");
				builder.append(")");
			}
		}

		// テーブルパーティショニング
		if (this.isPartitioned()) {
			for (int i = 0;  i < this.getTablePartitionProperties().size(); i++) {
				boolean isSub = (i == 1);
				TablePartitionProperty partProp = this.getTablePartitionProperties().get(i);
				if (isSub) {
					builder.append("SUB");
				}
				builder.append("PARTITION BY ");
				if (partProp.getType().equals(ToolConstants.TABLE_PARTITION_TYPE_HASH)) {
					builder.append(partProp.getType());
					builder.append("(\"");
					builder.append(partProp.getColumn());
					builder.append("\") ");
					if (isSub) {
						builder.append("SUB");
					}
					builder.append("PARTITIONS ");
					builder.append(partProp.getDivisionCount());
				} else if (partProp.getType().equals(ToolConstants.TABLE_PARTITION_TYPE_INTERVAL)) {
					builder.append(ToolConstants.TABLE_PARTITION_TYPE_RANGE);
					builder.append("(\"");
					builder.append(partProp.getColumn());
					builder.append("\") ");
					builder.append("EVERY(");
					builder.append(partProp.getIntervalValue());
					if (partProp.getIntervalUnit() != null) {
						builder.append(",");
						builder.append(partProp.getIntervalUnit());
						builder.append("");
					}
					builder.append(")");
				}
				builder.append(" ");
			}
		}
		builder.append(";");
		return builder.toString();
	}

	/**
	 * ロウキーのカラムであるかを返します。
	 *
	 * @param colNo カラム番号
	 * @param rowKeyColumnList ロウキーのカラム番号のリスト
	 * @return colNoがロウキーのカラム番号である場合true
	 */
	private boolean isRowKeyColumn(int colNo, List<Integer> rowKeyColumnList) {
		// V4.3 ContainerInfo.getRowKeyColumnList()より取得した値よりロウキーであるかを判断
		boolean ret = false;
		if (rowKeyColumnList != null && rowKeyColumnList.contains(Integer.valueOf(colNo))) {
			ret = true;
		}
		return ret;
	}

	/**
	 * メタ情報から索引作成のSQL文を組み立てます。
	 *
	 * @return 索引作成SQL文
	 */
	public Map<IndexInfo, String> buildCreateIndexStatements() {

		Map<IndexInfo, String> sqlMap = null;
		if ( this.getIndexInfoList() != null ){
			sqlMap = new HashMap<IndexInfo, String>();

			for ( IndexInfo indexInfo : this.getIndexInfoList() ){

				// ロウキーによって作成されるデフォルトの索引が指定されている場合は、SQLによる索引作成は不要
				// V4.3 比較処理用に m_columnInfoList よりロウキーとなっているカラム名のリストを作成
				List<String> rowKeyColumnNameList = new ArrayList<String>();
				// V4.3 ContainerInfo.getRowKeyColumnList()より取得した値よりロウキーであるかを判断
				List<Integer> rowKeyColumnList = this.getRowKeyColumnList();
				for ( int i = 0; i <  m_columnInfoList.size(); i++ ) {
					if (isRowKeyColumn(i, rowKeyColumnList)) {
						ColumnInfo rowKeyColumnInfo = m_columnInfoList.get(i);
						rowKeyColumnNameList.add(rowKeyColumnInfo.getName());
					}
				}
				// V4.3 getColumnName()で情報取得しない getColumnNameList()で情報取得する
				//if ( indexInfo.getColumnName().equalsIgnoreCase(m_columnInfoList.get(0).getName())
				if ( isEqualStrListIgnoreCase(indexInfo.getColumnNameList(), rowKeyColumnNameList)
						&& (indexInfo.getName()==null) && (indexInfo.getType() == IndexType.TREE) ){
					continue;
				}

				StringBuilder builder = new StringBuilder();
				builder.append("CREATE INDEX \"");
				builder.append(indexInfo.getName());
				builder.append("\" ON \"");
				builder.append(this.getName());
				builder.append("\" ( \"");
				// V4.3 getColumnName()で情報取得しない getColumnNameList()で情報取得する
				//builder.append(indexInfo.getColumnName());
				// V4.3 CREATE INDEX 複数カラム指定形式に対応
				StringJoiner sjColumnName = new StringJoiner("\",\"");
				for (String indexColumnName : indexInfo.getColumnNameList()) {
					sjColumnName.add(indexColumnName);
				}
				builder.append(sjColumnName.toString());
				builder.append("\")");
				sqlMap.put(indexInfo, builder.toString());
			}
		}
		return sqlMap;
	}

	/**
	 * 引数のStringのリストが大文字と小文字の区別なしで等しいかを返します。
	 * @param list1 比較するStringのリスト
	 * @param list2 比較するStringのリスト
	 * @return 大文字と小文字の区別なしで等しいか
	 */
	private boolean isEqualStrListIgnoreCase (List<String> list1, List<String> list2) {
		boolean ret = false;
		if (list1 == null && list2 == null) {
			ret = true;
		} else if (list1 == null || list2 == null) {
			ret = false;
		} else if (list1.size() == list2.size()) {
			ret = true;
			int size = list1.size();
			for (int i = 0; i < size; i++) {
				if (!isEqualIgnoreCase(list1.get(i), list2.get(i))) {
					ret = false;
					break;
				}
			}
		}
		return ret;
	}

	/**
	 * 引数のStringが大文字と小文字の区別なしで等しいかを返します。
	 * @param s1 比較するString
	 * @param s2 比較するString
	 * @return 大文字と小文字の区別なしで等しいか
	 */
	private boolean isEqualIgnoreCase(String s1, String s2) {
		boolean ret = false;
		if (s1 == null && s2 == null) {
			ret = true;
		} else if (s1 == null || s2 == null) {
			ret = false;
		} else {
			ret = s1.equalsIgnoreCase(s2);
		}
		return ret;
	}


	//============================================================
	// コンテナ情報比較
	//============================================================
	/**
	 * GridStoreのコンテナ情報オブジェクトと同じ設定であることを確認します。
	 *
	 * @param gsContInfo
	 */
	public boolean compareContainerInfo(ContainerInfo anotherInfo){

		boolean checkErrorFlag = false;
		m_msg = new StringBuilder();

		// コンテナタイプ   (未設定の場合、null)
		if ( m_conInfo.getType() != anotherInfo.getType()) {
			addMessage("ContainerType", m_conInfo.getType(), anotherInfo.getType(), null);
			checkErrorFlag = true;
		}
//		if ( m_conInfo.getAttribute() != anotherInfo.getAttribute() ){
//			addMessage("Attribute", m_conInfo.getType(), anotherInfo.getType(), null);
//			checkErrorFlag = true;
//		}

		// ロウキー      (未設定の場合、false)
		// V4.3 isRowKeyAssigned()で判定しない
		// if ( m_conInfo.isRowKeyAssigned() != anotherInfo.isRowKeyAssigned() ) {
		// 	addMessage("rowKeyAssigned", m_conInfo.isRowKeyAssigned(), anotherInfo.isRowKeyAssigned(), null);
		// 	checkErrorFlag = true;
		// }
		// V4.3 ロウキー設定の比較はContainerInfo.getRowKeyColumnList()より取得した値を比較
		List<Integer> rowKeyColumnList1 = m_conInfo.getRowKeyColumnList();
		List<Integer> rowKeyColumnList2 = anotherInfo.getRowKeyColumnList();
		boolean rowKeyColumnListResult = false;
		if (rowKeyColumnList1 == null && rowKeyColumnList2 == null) {
			rowKeyColumnListResult = true;
		} else if (rowKeyColumnList1 == null) {
			rowKeyColumnListResult = false;
		} else if (rowKeyColumnList2 == null) {
			rowKeyColumnListResult = false;
		} else {
			rowKeyColumnListResult = rowKeyColumnList1.equals(rowKeyColumnList2);
		}
		if (!rowKeyColumnListResult) {
			addMessage("rowKeySet", rowKeyColumnList1, rowKeyColumnList2, null);
			checkErrorFlag = true;
		}
		// データアフィニティ     (未設定の場合、null)
		if ( !m_compatibleOption ){
			String d1 = m_conInfo.getDataAffinity();
			String d2 = anotherInfo.getDataAffinity();
			if ( ((d1 == null)&&(d1!=d2)) || ((d1!=null)&&(!d1.equals(d2))) ){
				addMessage("dataAffinity", d1, d2, null);
				checkErrorFlag = true;
			}
		}

		// カラム情報チェック
		if (!compareColumnInfo(anotherInfo)) {
			checkErrorFlag = true;
		}
		// 索引情報チェック
		if (!compareIndexInfo(anotherInfo)) {
			checkErrorFlag = true;
		}
		// トリガー情報チェック
		if (!compareTriggerInfo(anotherInfo)) {
			checkErrorFlag = true;
		}
		// 時系列情報チェック
		if (!compareTimeSeriesProperties(anotherInfo)) {
			checkErrorFlag = true;
		}

		//　上記チェックでエラーの場合
		if ( checkErrorFlag ){
			return false;	// 不一致の内容はメッセージ変数に格納
		} else {
			return true;
		}
	}

	/**
	 * テーブルパーティション情報も含めて、コンテナ定義の比較を行います。
	 */
	public boolean compareContainerInfo(ToolContainerInfo anotherInfo){
		if ( !compareContainerInfo(anotherInfo.getContainerInfo()) ){
			return false;
		}
		if ( !compareTablePartitionProperties(anotherInfo.getTablePartitionProperties())){
			return false;
		}
		if ( !compareExpirationInfo(anotherInfo.getExpirationInfo())) {
			return false;
		}
		return true;
	}

	/**
	 * カラム情報を比較します。
	 * @param gsContInfo
	 * @param log
	 * @return
	 */
	private boolean compareColumnInfo(ContainerInfo anotherInfo) {
		// カラム数
		if ( m_columnInfoList.size()!= anotherInfo.getColumnCount() ) {
			addMessage("ColumnCount", m_conInfo.getColumnCount(), anotherInfo.getColumnCount(), null);
			return false;
		}

		int error_count = 0;
		for ( int i = 0; i <  m_columnInfoList.size(); i++ ) {
			ColumnInfo localColumn = m_columnInfoList.get(i);
			ColumnInfo gsColumn = anotherInfo.getColumnInfo(i);

			// カラム名（大文字小文字区別なし)
			if ( !localColumn.getName().equalsIgnoreCase(gsColumn.getName()) ) {
				addMessage("ColumnName", localColumn.getName(), gsColumn.getName(), null);
				error_count++;
			}
			// カラムタイプ   (未設定の場合、null)
			if ( localColumn.getType() != gsColumn.getType() ) {
				addMessage("ColumnType", localColumn.getType(), gsColumn.getType()," columnName=["+localColumn.getName()+"]");
				error_count++;
			}
			// NOT NULL制約
			Boolean localNullable = localColumn.getNullable();
			if ( localNullable == null ) {
				// V4.3 isRowKeyAssigned()でロウキーであるか判定しない
				// if ( i == 0 && m_conInfo.isRowKeyAssigned() ) {
				// V4.3 ContainerInfo.getRowKeyColumnList()より取得した値よりロウキーであるかを判断
				if (isRowKeyColumn(i, m_conInfo.getRowKeyColumnList())) {
					localNullable = false;
				} else {
					localNullable = true;
				}
			}

			if ( localNullable != gsColumn.getNullable() ) {
				addMessage("notNull", !localNullable, !gsColumn.getNullable(), " columnName=["+localColumn.getName()+"]");
				error_count++;
			}
			// 索引  (未設定の場合、null)
//			Set<IndexType> localIndex = m_indexInfoMap.get(localColumn.getName());
//			Set<IndexType> gsIndex = gsColumn.getIndexTypes();
//			if ( ((localIndex == null)||(localIndex.size()==0))&&((gsIndex == null)||(gsIndex.size()==0))){
//				// OK
//				// new ContainerInfo() -> 索引未設定の場合：null
//				// getContainerInfo()  -> 索引未設定の場合：空リスト
//			} else if ( localIndex!=null ){
//				// Collection rowkey=trueの場合は、1番目のカラムに自動的にTREEが付いている。
//				if ( (i == 0) && m_conInfo.isRowKeyAssigned() && (m_conInfo.getType() == ContainerType.COLLECTION) ){
//					localIndex.add(IndexType.TREE);
//				}
//				if ( !localIndex.equals(gsIndex) ){
//					addMessage("IndexType", localIndex, gsIndex, " columnName=["+localColumn.getName()+"]");
//					error_count++;
//				}
//			} else {
//				if ( localIndex != gsIndex ){
//					addMessage("IndexType", localIndex, gsIndex, " columnName=["+localColumn.getName()+"]");
//					error_count++;
//				}
//			}


		}
		if ( error_count == 0 ){
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 索引情報比較メソッド
	 *
	 * <p>
	 * 下記の場合falseを返す。
	 * <li>索引数が合わない</li>
	 * <li>メタ情報に存在する索引情報が実際の索引情報リストに存在しない</li>
	 * <li>実際の索引情報リストに存在する索引情報がメタ情報に存在しない</li>
	 *
	 * @param gsContInfo GridDBから取得したコンテナ情報
	 * @since V3.5
	 */
	private boolean compareIndexInfo(ContainerInfo gsContInfo) {
		// V4.3 複合キー対応 ロウキーが複数カラムになるためStringではなくList<String>を用いる
		// メタ情報のロウキーカラム名を取得
		//String rowKeyColumnName = null;
		List<String> rowKeyColumnNameList = null;
		// V4.3 isRowKeyAssigned()でロウキーであるか判定しない
		//if ( gsContInfo.isRowKeyAssigned() && gsContInfo.getType() == ContainerType.COLLECTION ) {
		//	rowKeyColumnName = m_columnInfoList.get(0).getName();
		//}
		if ( gsContInfo.getType() == ContainerType.COLLECTION ) {
			int columnCnt = gsContInfo.getColumnCount();
			for (int i = 0; i < columnCnt; i++) {
				// V4.3 ContainerInfo.getRowKeyColumnList()より取得した値よりロウキーであるかを判断
				if (isRowKeyColumn(i, gsContInfo.getRowKeyColumnList())) {
					if (rowKeyColumnNameList == null) {
						rowKeyColumnNameList = new ArrayList<String>();
					}
					rowKeyColumnNameList.add(gsContInfo.getColumnInfo(i).getName());
				}
			}
		}

		// メタ情報の索引リストでGridDBに作成したときの索引情報リストを作成する。
		List<IndexInfo> virtualIndices = new ArrayList<IndexInfo>();
		boolean rowKeyIndexFound = false;
		for ( IndexInfo metaIndex : m_indexInfoList ) {
			virtualIndices.add(metaIndex);
			// コレクションのロウキーの索引(TREE)がメタ情報に存在する
			// V4.3 getColumnName()で情報取得しない getColumnNameList()で情報取得する
			// V4.3 複合キー対応 ロウキーのカラム名の比較はStringの比較ではなくリストを比較する
			//      ロウキーの各カラム名の比較はString.equalsIgnoreCase()で行う
			// if ( rowKeyColumnName != null && rowKeyColumnName.equals(metaIndex.getColumnName()) && metaIndex.getType() == IndexType.TREE) {
			if ( rowKeyColumnNameList != null && isEqualStrListIgnoreCase(rowKeyColumnNameList, metaIndex.getColumnNameList()) && metaIndex.getType() == IndexType.TREE) {
				rowKeyIndexFound = true;
			}
		}
		// コレクションのロウキーの索引(TREE)がメタ情報に存在しないなら、仮想的に追加する
		// V4.3 複合キー対応 IndexInfo.setColumnNameList()でロウキーのカラム名を指定して仮想的に追加する索引情報を作成する
		//if ( rowKeyColumnName != null && !rowKeyIndexFound ) {
		//	virtualIndices.add(IndexInfo.createByColumn(rowKeyColumnName, IndexType.TREE));
		//}
		if ( rowKeyColumnNameList != null && !rowKeyIndexFound ) {
			IndexInfo indexInfo = new IndexInfo();
			indexInfo.setColumnNameList(rowKeyColumnNameList);
			indexInfo.setType(IndexType.TREE);
			virtualIndices.add(indexInfo);
		}

		// 索引数
		if ( virtualIndices.size() != gsContInfo.getIndexInfoList().size() ) {
			addMessage("IndexCount", virtualIndices.size(), gsContInfo.getIndexInfoList().size(), null);
			return false;
		}

		int foundCount = 0;
		boolean match = false;
		// メタ情報の索引が実際の索引に存在するかどうか
		for ( IndexInfo metaIndex : virtualIndices ) {
			for ( IndexInfo gsIndex : gsContInfo.getIndexInfoList() ) {
				// カラム名と索引タイプが一致
				if ( isEqualStrListIgnoreCase(metaIndex.getColumnNameList(), gsIndex.getColumnNameList()) &&
						metaIndex.getType().equals(gsIndex.getType()) ) {
					// 索引名のNULLチェック
					if ( metaIndex.getName() == null || gsIndex.getName() == null ) {
						if ( metaIndex.getName() == gsIndex.getName() ) {
							// 索引名が両方NULLなら同一
							foundCount++;
							match = true;
						}
					} else if (metaIndex.getName().equals(gsIndex.getName())){
						// 索引名が等しいなら同一
						foundCount++;
						match = true;
					}
				}
			}
			if ( !match ){
				// V4.3 getColumnName()で情報取得しない getColumnNameList()で情報取得する
				//addMessage("IndexInfo", "column="+metaIndex.getColumnName()+",type="+metaIndex.getType()+",name="+metaIndex.getName()
				//		, "unmatch", null);
				addMessage("IndexInfo", "column="+metaIndex.getColumnNameList()+",type="+metaIndex.getType()+",name="+metaIndex.getName()
				, "unmatch", null);
			}
			match = false;
		}

		// すべて見つかっていれば同一
		if ( foundCount == virtualIndices.size() ){
			return true;
		} else {
			return false;
		}
	}

	/**
	 * トリガー情報比較メソッド
	 *
	 * @param gsContInfo GridStore_コンテナ情報
	 * @param log ログオブジェクト
	 * @return true-トリガー情報が一致,false-トリガー情報が不一致
	 */
	private boolean compareTriggerInfo(ContainerInfo gsContInfo) {

		try {
			// トリガ情報のリスト (未設定の場合、空リスト)
			List<TriggerInfo> triggerList = m_triggerInfoList;
			List<TriggerInfo> gsTriggerList = gsContInfo.getTriggerInfoList();

			if ( triggerList.size() != gsTriggerList.size() ) {
				addMessage("Trigger Count", triggerList.size(), gsTriggerList.size(), null);
				return false;
			}

			int error_count = 0;
			for ( TriggerInfo localTrigger : triggerList ) {
				boolean match = false;

				for ( TriggerInfo gsTrigger : gsTriggerList ) {

					// イベント名の一致を確認
					if (!gsTrigger.getName().equalsIgnoreCase(localTrigger.getName())) {
						continue;
					}
					match = true;

					// トリガ発火時に通知対象とするカラム名 (未設定の場合、空リスト)
					if (!localTrigger.getTargetColumns().equals(gsTrigger.getTargetColumns())){
						addMessage("Trigger Columns", localTrigger.getTargetColumns(),
								gsTrigger.getTargetColumns(), " triggerName=["+localTrigger.getName()+"]");
						error_count++;
					}
					// トリガ発火対象とする更新操作種別  (未設定の場合、空リスト)
					if (!gsTrigger.getTargetEvents().equals(localTrigger.getTargetEvents())) {
						addMessage("Trigger Events", localTrigger.getTargetEvents(),
								gsTrigger.getTargetEvents(), " triggerName=["+localTrigger.getName()+"]");
						error_count++;
					}
					// トリガ種別  (未設定の場合、null)
					if (gsTrigger.getType() != localTrigger.getType()) {
						addMessage("Trigger Type", localTrigger.getType(), gsTrigger.getType(), " triggerName=["+localTrigger.getName()+"]");
						error_count++;
					}
					// JMS通知で使用するデスティネーション名
					String local = localTrigger.getJMSDestinationName();
					String gs = gsTrigger.getJMSDestinationName();
					if ( ((local==null)||(local.length()==0)) && ((gs==null)||(gs.length()==0))){
						// OK
						// new TriggerInfo() ->  未設定の場合null
						// getContainerInfo  ->  未設定の場合""
					} else if ( ((gs==null)&&(gs!=local)) || ((gs!=null)&& !gs.equalsIgnoreCase(local))){
						addMessage("Trigger JmsDestinationName", local, gs, " triggerName=["+localTrigger.getName()+"]");
						error_count++;
					}
					// JMS通知で使用するデスティネーション種別   (未設定の場合、null)
					local = localTrigger.getJMSDestinationType();
					gs = gsTrigger.getJMSDestinationType();
					if ( ((local==null)||(local.length()==0)) && ((gs==null)||(gs.length()==0))){
						// OK
						// new TriggerInfo() ->  未設定の場合null
						// getContainerInfo  ->  未設定の場合""
					} else if ( ((gs==null)&&(gs!=local)) || ((gs!=null)&& !gs.equalsIgnoreCase(local))){
						addMessage("Trigger JmsDestinationType", localTrigger.getJMSDestinationType(),
								gsTrigger.getJMSDestinationType(), " triggerName=["+localTrigger.getName()+"]");
						error_count++;
					}
					// 通知先サーバに接続する際のユーザ名
					local = localTrigger.getUser();
					gs = gsTrigger.getUser();
					if ( ((local==null)||(local.length()==0)) && ((gs==null)||(gs.length()==0))){
						// OK
						// new TriggerInfo() ->  未設定の場合null
						// getContainerInfo  ->  未設定の場合""
					} else if ( ((gs==null)&&(gs!=local)) || ((gs!=null)&& !gs.equalsIgnoreCase(local))){
						addMessage("Trigger User", localTrigger.getUser(), gsTrigger.getUser(), " triggerName=["+localTrigger.getName()+"]");
						error_count++;
					}
					// 通知先サーバに接続する際のパスワード
					local = localTrigger.getPassword();
					gs = gsTrigger.getPassword();
					if ( ((local==null)||(local.length()==0)) && ((gs==null)||(gs.length()==0))){
						// OK
						// new TriggerInfo() ->  未設定の場合null
						// getContainerInfo  ->  未設定の場合""
					} else if ( ((gs==null)&&(gs!=local)) || ((gs!=null)&& !gs.equalsIgnoreCase(local))){
						addMessage("Trigger Password", "***", "***", " triggerName=["+localTrigger.getName()+"]");
						error_count++;
					}
					// トリガ発火時の通知先URI   (未設定の場合、null)
					URI localURI = localTrigger.getURI();
					URI gsURI = gsTrigger.getURI();
					if ( ((gsURI==null)&&(gsURI!=localURI)) || ((gsURI!=null)&& !gsURI.equals(localURI))){
						addMessage("Trigger URI", localTrigger.getURI(), gsTrigger.getURI(), " triggerName=["+localTrigger.getName()+"]\n");
						error_count++;
					}
					break;
				}
				if ( !match ) {
					m_msg.append("Trigger Name \""+localTrigger.getName() +"\" does not exist on another.\n");
					error_count++;
				}
			}

			if ( error_count == 0 ){
				return true;
			} else {
				return false;
			}

		} catch (Exception e) {
//			m_msg.append("compareColumnInfo():Exception Error:" + e.toString(), e );
			m_msg.append("compareColumnInfo():Columninfo Compare [false]");
			return false;
		}
	}


	/**
	 * 時系列プロパティ比較
	 *
	 * @param gsContInfo GridStore側情報
	 * @param local_container ローカル定義情報
	 * @return  true：同一情報、false：差異有情報
	 */
	private boolean compareTimeSeriesProperties(ContainerInfo gsContInfo) {

		try {
			int error_count = 0;

			TimeSeriesProperties localTimesereis = m_timeSeriesProperties;
			TimeSeriesProperties gsTimesereis = gsContInfo.getTimeSeriesProperties();

			if ( (localTimesereis == null) && (gsTimesereis == null) ) {
				return true;
			} else if ( localTimesereis == null ){
				// 初期値で比較
				localTimesereis = new TimeSeriesProperties();
			} else if ( gsTimesereis == null ){
				gsTimesereis = new TimeSeriesProperties();
			}

			// 時系列圧縮方式     (未設定の場合、CompressionMethod.NO)
			if (!gsTimesereis.getCompressionMethod().equals(localTimesereis.getCompressionMethod())) {
				addMessage("CompressionMethod", localTimesereis.getCompressionMethod(), gsTimesereis.getCompressionMethod(), null);
				error_count++;
			}

			// 最大期間      (未設定の場合、-1)
			if (gsTimesereis.getCompressionWindowSize() != localTimesereis.getCompressionWindowSize()) {
				addMessage("CompressionWindowSize", localTimesereis.getCompressionWindowSize(), gsTimesereis.getCompressionWindowSize(), null);
				error_count++;
			}

			// 最大期間の単位   (未設定の場合、null)
			if ( gsTimesereis.getCompressionWindowSizeUnit() != localTimesereis.getCompressionWindowSizeUnit()) {
				addMessage("CompressionWindowSizeUnit", localTimesereis.getCompressionWindowSizeUnit(), gsTimesereis.getCompressionWindowSizeUnit(), null);
				error_count++;
			}

			// 分割数  (未設定の場合、-1)
			if ( !m_compatibleOption ){
				if (gsTimesereis.getExpirationDivisionCount() != localTimesereis.getExpirationDivisionCount()) {
					if ( (gsTimesereis.getExpirationDivisionCount()==8) && (localTimesereis.getExpirationDivisionCount()==-1) ){
						// サーバが勝手に初期値8を返す
					} else {
						addMessage("ExpirationDivisionCount", localTimesereis.getExpirationDivisionCount(), gsTimesereis.getExpirationDivisionCount(), null);
						error_count++;
					}
				}
			}

			// 経過期間 (未設定の場合、-1)
			if (gsTimesereis.getRowExpirationTime() != localTimesereis.getRowExpirationTime()) {
				addMessage("RowExpirationTime", localTimesereis.getRowExpirationTime(), gsTimesereis.getRowExpirationTime(), null);
				error_count++;
			}

			// 経過期間の単位   (未設定の場合、null)
			if (gsTimesereis.getRowExpirationTimeUnit() != localTimesereis.getRowExpirationTimeUnit()) {
				addMessage("RowExpirationTimeUnit", localTimesereis.getRowExpirationTimeUnit(), gsTimesereis.getRowExpirationTimeUnit(), null);
				error_count++;
			}

			// 追加設定(圧縮)のあるカラム (未設定の場合、空リスト）
			Set<String> localColums = localTimesereis.getSpecifiedColumns();
			Set<String> gsColums = gsTimesereis.getSpecifiedColumns();

			if (gsColums.size() != localColums.size()) {
				addMessage("SpecifiedColumns(CompressionColumns) Count", localColums.size(), gsColums.size(), null);
				error_count++;
			}
			if (!gsColums.equals(localColums)){
				addMessage("SpecifiedColumns(CompressionColumns)", localColums, gsColums, null);
				error_count++;
			}

			for (String column : gsColums) {
				// Rate   (未設定の場合null)
				if (!gsTimesereis.getCompressionRate(column).equals(localTimesereis.getCompressionRate(column))) {
					addMessage("Compression Rate", localTimesereis.getCompressionRate(column),
							gsTimesereis.getCompressionRate(column), " column=["+column+"]");
					error_count++;
				}
				// Span   (未設定の場合null)
				if (!gsTimesereis.getCompressionSpan(column).equals(localTimesereis.getCompressionSpan(column))) {
					addMessage("Compression Span", localTimesereis.getCompressionSpan(column),
							gsTimesereis.getCompressionSpan(column), " column=["+column+"]");
					error_count++;
				}
				// Width   (未設定の場合null)
				if (!gsTimesereis.getCompressionWidth(column).equals(localTimesereis.getCompressionWidth(column))) {
					addMessage("Compression Width", localTimesereis.getCompressionWidth(column),
							gsTimesereis.getCompressionWidth(column), " column=["+column+"]");
					error_count++;
				}
			}

			if ( error_count == 0 ){
				return true;
			} else {
				return false;
			}

		} catch (Exception e) {
			//log.error("compareTimeSeriesProperties():Exception Error:", e );
			m_msg.append("compareTimeSeriesProperties():TimeSeriesProperties Compare [false]");
			return false;
		}
	}

	/**
	 * テーブルパーティショニング情報比較
	 *
	 * @param gsProperties GridDB側情報
	 * @return  true：同一情報、false：差異有情報
	 */
	private boolean compareTablePartitionProperties(List<TablePartitionProperty> gsProperties) {
		if ( (m_tablePartitionProperties==null)&&(gsProperties == null) ){
			return true;
		}
		if ( (m_tablePartitionProperties==null)&&(gsProperties!=null) || (m_tablePartitionProperties!=null)&&(gsProperties==null)){
			addMessage("TablePartitionInfo", (m_tablePartitionProperties==null)?"Not Partitioned":"Partitioned", (gsProperties==null)?"Not Partitioned":"Partitioned", null);
			return false;
		}
		if (m_tablePartitionProperties.size() != gsProperties.size()) {
			String t1 = m_tablePartitionProperties.get(0).getType()+(m_tablePartitionProperties.size()==2?"-"+m_tablePartitionProperties.get(1).getType():"");
			String t2 = gsProperties.get(0).getType()+(gsProperties.size()==2?"-"+gsProperties.get(1).getType():"");
			addMessage("TablePartitionInfo", t1, t2, null);
			return false;
		}

		int i = 0;
		for ( TablePartitionProperty prop : m_tablePartitionProperties ){
			if (!compareTablePartitionProperty(prop, gsProperties.get(i++))) {
					return false;
			}
		}
		return true;
	}

	private boolean compareTablePartitionProperty(TablePartitionProperty localProp, TablePartitionProperty gsProp) {
		// パーティショニング種別一致確認
		if (!localProp.getType().equals(gsProp.getType())) {
			addMessage("TablePartitionInfo type", localProp.getType(), gsProp.getType(), null);
			return false;
		}
		// パーティショニング対象カラム一致確認
		if (!localProp.getColumn().equals(gsProp.getColumn())) {
			addMessage("TablePartitionInfo column", localProp.getColumn(), gsProp.getColumn(), null);
			return false;
		}
		// HASHの場合
		if (localProp.getType().equals(ToolConstants.TABLE_PARTITION_TYPE_HASH)) {
			// ハッシュ分割数一致確認
			if (localProp.getDivisionCount() != gsProp.getDivisionCount()) {
				addMessage("TablePartitionInfo divisionCount", localProp.getDivisionCount(), gsProp.getDivisionCount(), null);
				return false;
			} else {
				return true;
			}

		// INTERVALの場合
		} else if (localProp.getType().equals(ToolConstants.TABLE_PARTITION_TYPE_INTERVAL)) {
			// 区間値一致確認
			if (!localProp.getIntervalValue().equals(gsProp.getIntervalValue())) {
				addMessage("TablePartitionInfo intervalValue", localProp.getIntervalValue(), gsProp.getIntervalValue(), null);
				return false;
			// 区間値単位一致確認
			} else if ( ((localProp.getIntervalUnit()==null)&&(gsProp.getIntervalUnit()!=null))
					|| ((localProp.getIntervalUnit()!=null)&&!localProp.getIntervalUnit().equalsIgnoreCase(gsProp.getIntervalUnit())) ) {
				addMessage("TablePartitionInfo intervalUnit", localProp.getIntervalUnit(), gsProp.getIntervalUnit(), null);
				return false;
			} else {
				return true;
			}
		}
		return false;
	}

	/**
	 * 期限解放情報比較
	 *
	 * @param gsInfo GridDB側情報
	 * @return  true：同一情報、false：差異有情報
	 */
	private boolean compareExpirationInfo(ExpirationInfo gsInfo) {
		ExpirationInfo localInfo = m_expirationInfo;

		// どちらもnullなら一致、どちらかしかnullでない場合は不一致
		// 以降どちらもnullではない
		if (localInfo == null && gsInfo == null) {
			return true;
		} else if (localInfo == null && gsInfo != null) {
			addMessage("Expiration", "disable", "enable", null);
			return false;
		} else if (localInfo != null && gsInfo == null){
			addMessage("Expiration", "enable", "disable", null);
		}

		// オブジェクトがnullでない場合、要素にnullは設定されない前提
		// いずれかの要素が異なったらエラー
		if (!localInfo.getType().equalsIgnoreCase(gsInfo.getType())) {
			addMessage("expirationType", localInfo.getType(), gsInfo.getType(), null);
			return false;
		} else if (localInfo.getTime() != gsInfo.getTime()) {
			addMessage("expirationTime", localInfo.getTime(), gsInfo.getTime(), null);
			return false;
		} else if (!localInfo.getTimeUnit().equals(gsInfo.getTimeUnit())) {
			addMessage("expirationTimeUnit", localInfo.getTimeUnit(), gsInfo.getTimeUnit(), null);
			return false;
		} else {
			return true;
		}
	}

	/**
	 * コンテナ情報の比較で相違点があった場合、その相違内容のメッセージを返します。
	 * （compareContainerInfoの結果がfalseだった場合に実行してください。）
	 *
	 * @return メッセージ
	 */
	public String getMessage(){
		return m_msg.toString();
	}

	private void addMessage(String itemName, Object self, Object another, String additional){
		m_msg.append("[Unmatch]");
		m_msg.append(itemName);
		m_msg.append(" : self=[");
		if ( self == null ){
			m_msg.append("null");
		} else {
			m_msg.append(self.toString());
		}
		m_msg.append("] another=[");
		if ( another == null ){
			m_msg.append("null");
		} else {
			m_msg.append(another.toString());
		}
		m_msg.append("]");
		if ( additional != null){
			m_msg.append(additional);
		}
		m_msg.append("\n");
	}

	//============================================================
	// コンテナ情報 整合性チェック
	//============================================================
	/**
	 * 設定されたコンテナ情報の整合性をチェックします。
	 * @return
	 * @throws GSEIException
	 */
	public void checkContainerInfo() throws GridStoreCommandException {
		try {
			String conName = getName();
			ContainerType conType = getType();

			// コンテナ情報の必須項目チェック
			if ( (conName == null) || (conName.length() == 0) ) {
				throw new GridStoreCommandException("ContainerName is required.");
			}
			if ( conType == null ){
				throw new GridStoreCommandException("ContainerType is required. name=["+conName+"]");
			}

			//--------------------------------------------
			// カラム情報のチェック
			//--------------------------------------------
			if (getColumnCount()==0){
				// カラム情報は必須
				throw new GridStoreCommandException("ColumnInfo is required. name=["+conName+"]");
			}
			List<String> columnNameList = new ArrayList<String>();
			// V4.3 ContainerInfo.getRowKeyColumnList()より取得した値よりロウキーであるかを判断
			List<Integer> rowKeyColumnList = this.getRowKeyColumnList();
			for (int i = 0; i < getColumnCount(); i++) {
				ColumnInfo columnInfo = getColumnInfo(i);

				// カラム名
				String columnName = columnInfo.getName();
				if ((columnName == null) || (columnName.length() == 0)) {
					throw new GridStoreCommandException("ColumnName is required. name=["+conName+"]");
				}
				// カラムタイプ
				GSType columnType = columnInfo.getType();
				if (columnType == null) {
					throw new GridStoreCommandException("ColumnType is required. name=["+conName+"] columnName=["+columnName+"]");
				}

				// ロウキーに指定可能なカラムタイプのチェック
				// V4.3 getRowKeyAssigned() で判定しない ContainerInfo.getRowKeyColumnList()より取得した値よりロウキーであるかを判断
				// if ( (i==0) && getRowKeyAssigned() ){
				if (isRowKeyColumn(i, rowKeyColumnList)) {
					switch(conType){
					case COLLECTION:
						switch(columnType){
						case INTEGER:
						case STRING:
						case LONG:
						case TIMESTAMP:
							// OK
							break;
						default:
							throw new GridStoreCommandException("ColumnType of the rowkey is invalid in type \'"+ContainerType.COLLECTION+"'. name=["+conName
									+"] columnName=["+columnName+"] columnType=["+columnType+"]");
						}
						break;
					case TIME_SERIES:
						if ( columnType == GSType.TIMESTAMP ){
							// OK
						} else {
							throw new GridStoreCommandException("ColumnType of the rowkey is invalid in type \'"+ContainerType.TIME_SERIES+"\'. name=["+conName
									+"] columnName=["+columnName+"] columnType=["+columnType+"]");
						}
						break;
					}

					// NotNull制約も必要
					if ( (columnInfo.getNullable()!=null) && (columnInfo.getNullable() == true) ){
						throw new GridStoreCommandException("Row key cannot be null. name=["+conName
								+"] columnName=["+columnName+"] columnType=["+columnType+"]");
					}
				}

				columnNameList.add(columnName);
			}

			//--------------------------------------------
			// コンテナタイプごとの整合性チェック
			//--------------------------------------------
			switch(conType){
			case COLLECTION:
				if(m_timeSeriesProperties != null){
					throw new GridStoreCommandException("TimeSeriesProperties is not required in type \'"+ContainerType.COLLECTION+"'. name=["+getName()+"]");
				}
				break;

			case TIME_SERIES:
				// V4.3 getRowKeyAssigned() で判定しない
				boolean hasRowKey = false;
				for (int i = 0; i < getColumnCount(); i++) {
					// V4.3 ContainerInfo.getRowKeyColumnList()より取得した値よりロウキーであるかを判断
					if (isRowKeyColumn(i, rowKeyColumnList)) {
						hasRowKey = true;
						break;
					}
				}
				// if ( !getRowKeyAssigned() ){
				if ( !hasRowKey ){
					// V4.3 メッセージ変更
					// throw new GridStoreCommandException("RowKeyAssined is required in type \'"+ContainerType.TIME_SERIES+"\'. name=["+getName()+"]");
					throw new GridStoreCommandException("RowKeySet is required in type \'"+ContainerType.TIME_SERIES+"\'. name=["+getName()+"]");
				}

				// 時系列コンテナのチェック
				checkTimeSeriesProperties();

				break;
			}

			//--------------------------------------------
			// 索引チェック
			//--------------------------------------------
			checkIndexInfo();

			//--------------------------------------------
			// トリガー情報チェック
			//--------------------------------------------
			for (TriggerInfo trigger : m_triggerInfoList) {// 未設定の場合は空リスト
				if ( (trigger.getName()==null) || (trigger.getName().length()==0) ){
					throw new GridStoreCommandException("TriggerName is required. name=["+getName()+"]");
				}
				if ( trigger.getType() == null ){
					throw new GridStoreCommandException("TriggerType is required. name=["+getName()+"] triggerName=["+trigger.getName()+"]");
				}
				if ( trigger.getURI() == null ){
					throw new GridStoreCommandException("URI is required. name=["+getName()+"] triggerName=["+trigger.getName()+"]");
				}
				if ( (trigger.getTargetEvents() == null) || (trigger.getTargetEvents().size()==0) ){
					throw new GridStoreCommandException("TargetEvents is required. name=["+getName()+"] triggerName=["+trigger.getName()+"]");
				}
				for (String name : trigger.getTargetColumns()) {
					if ( !columnNameList.contains(name) ) {
						// "トリガー情報のカラム名はカラム情報に登録されていません"
						throw new GridStoreCommandException("The columnName of triggerInfo does not exist in ColumnInfoList. name=["+getName()
								+ "] triggerName=["+trigger.getName()+"]");
					}
				}
			}

			//--------------------------------------------
			// パーティショニング情報チェック
			//--------------------------------------------
			if ( isPartitioned() ){
				// SEではできない
				try {
					Class.forName("com.toshiba.mwcloud.gs.sql.Driver");
				} catch (Exception e) {
					throw new GridStoreCommandException("The table partitioning function is not supported in Standard Edition. name=["+getName()+"]");
				}

				// 圧縮設定はできない -> checkTimeSeriesPropertiesでチェック済み

				// データアフィニティは設定できない
				// (V4.3.1) パーティショニングでもデータアフィニティ設定可能となったためチェックを外す
				// if ( this.getDataAffinity() != null ){
				// 	throw new GridStoreCommandException("DataAffinity cannot be specified in a partitioned table. name=["+getName()+"]");
				// }

				// トリガ設定はできない
				if ( !m_triggerInfoList.isEmpty() ){
					throw new GridStoreCommandException("Trigger cannot be specified in a partitioned table. name=["+getName()+"]");
				}

				// カラムにGeometryは指定できない
				for (int i = 0; i < getColumnCount(); i++) {
					ColumnInfo columnInfo = getColumnInfo(i);
					if ( columnInfo.getType() == GSType.GEOMETRY ){
						throw new GridStoreCommandException("Geometry type column cannot be specified in a partitioned table. name=["+getName()+"] columnName="+columnInfo.getName()+"]");
					}
				}

				// 索引
				for(IndexInfo index : m_indexInfoList){
					// 索引は名前付きでなければならない (SQLで索引作成するため)
					if ( (index.getName() == null) || index.getName().isEmpty() ){
						// V4.3 getRowKeyAssigned()で判定しない
						// if ( !getRowKeyAssigned() || !index.getColumnName().equals(getColumnInfo(0).getName()) ){
						// V4.3 ロウキーのカラム名と索引のカラム名を比較して一致していたらロウキーの索引であると判定
						List<String> rowKeyColumnName = new ArrayList<String>();
						for (int i = 0; i < getColumnCount(); i++) {
							ColumnInfo columnInfo = getColumnInfo(i);
							// V4.3 ContainerInfo.getRowKeyColumnList()より取得した値よりロウキーであるかを判断
							if (isRowKeyColumn(i, rowKeyColumnList)) {
								rowKeyColumnName.add(columnInfo.getName());
							}
						}
						if ( !rowKeyColumnName.equals(index.getColumnNameList())) {
							// V4.3 getColumnName()で情報取得しない getColumnNameList()で情報取得する
							//throw new GridStoreCommandException("The index name is required in a partitioned table. name=["+getName()+"] columnName=["+index.getColumnName()+"] indexType=["+index.getType()+"]");
							throw new GridStoreCommandException("The index name is required in a partitioned table. name=["+getName()+"] columnName=["+index.getColumnNameList()+"] indexType=["+index.getType()+"]");
						}
					}

					// 索引の種別は決まっている (SQLのCREATE INDEXでは、索引種別は指定しない)
					if ( index.getType() != IndexType.TREE ){
						// V4.3 getColumnName()で情報取得しない getColumnNameList()で情報取得する
						//throw new GridStoreCommandException("The type of indexes must be TREE in a partitioned table.: columnName=["+index.getColumnName()+"] indexType=["+index.getType()+"]");
						throw new GridStoreCommandException("The type of indexes must be TREE in a partitioned table.: columnName=["+index.getColumnNameList()+"] indexType=["+index.getType()+"]");
					}
				}
			}

			//--------------------------------------------
			// 期限解放情報チェック
			//--------------------------------------------
			ExpirationInfo expInfo = getExpirationInfo();
			if (expInfo != null) {
				// SEではできない
				try {
					Class.forName("com.toshiba.mwcloud.gs.sql.Driver");
				} catch (Exception e) {
					throw new GridStoreCommandException("The partition expiration function is not supported in Standard Edition. name=["+getName()+"]");
				}

				String expType = expInfo.getType();

				// パーティション期限解放
				if (expType.equalsIgnoreCase(ToolConstants.EXPIRATION_TYPE_PARTITION)) {
					// ロウ期限解放と同時指定はできない
					if (m_timeSeriesProperties != null && m_timeSeriesProperties.getRowExpirationTime() != -1) {
						throw new GridStoreCommandException("The partition expiration function can not be used with row expiration. name=["+getName()+"]");
					}
				} else {
					// 期限解放種別が無効
					throw new GridStoreCommandException("The expiration type is invalid. name=["+getName()+"] expirationType=["+expType+"]");
				}
			}
		} catch ( GridStoreCommandException e ){
			throw e;

		} catch (Exception e) {
			// "コンテナ情報の検査処理でエラーが発生しました"
			throw new GridStoreCommandException("Error occurs in Check ContainerInfo."+": name=["+getName()+"] msg=["+e.getMessage()+"]", e);
		}
	}


	/**
	 * 時系列プロパティの整合性をチェックします。
	 */
	private void checkTimeSeriesProperties() throws GridStoreCommandException {
		if ( m_timeSeriesProperties == null ){
			return;
		}

		CompressionMethod cmType = m_timeSeriesProperties.getCompressionMethod(); //未設定の時はNO
		Set<String> specifiedColumnList = m_timeSeriesProperties.getSpecifiedColumns(); // 未設定の時は空リスト
		List<String> columnNameList = new ArrayList<String>();

		// パーティションテーブルの場合
		if ( this.isPartitioned() && (cmType != CompressionMethod.NO) ){
			throw  new GridStoreCommandException("CompressionMethod must be 'NO' in a partitioned table. name=["+getName()+"]");
		}

		// カラム毎の圧縮指定のタイプと値のチェック
		for ( int i = 0; i < getColumnCount(); i++ ){
			ColumnInfo columnInfo = getColumnInfo(i);
			String columnName = columnInfo.getName();
			columnNameList.add(columnName);

			if ( !specifiedColumnList.contains(columnName) ){
				// 圧縮設定されていないカラムは次へ。
				continue;
			}

			switch (cmType){
			case HI:
				GSType type = columnInfo.getType();

				// 組み合わせ可能なデータ型
				switch(type){
				case BYTE:
				case SHORT:
				case INTEGER:
				case LONG:
				case FLOAT:
				case DOUBLE:
					// OK
					break;
				default:
					throw  new GridStoreCommandException("The columnType cannot be specified for a compressed column. name=["+getName()
							+"] columnName=["+ columnName+"] columnType=["+type+"]");
				}

				if ( m_timeSeriesProperties.isCompressionRelative(columnName) ){
					if ( m_timeSeriesProperties.getCompressionWidth(columnName) != null){
						throw  new GridStoreCommandException("\"with\" cannot be specified as \"compressionType\":\"RELATIVE\". name=["+getName()
							+"] columnName=["+ columnName+"]");
					}
				} else {
					if( m_timeSeriesProperties.getCompressionRate(columnName)!= null
							||  m_timeSeriesProperties.getCompressionSpan(columnName)!= null ){
						throw  new GridStoreCommandException("\"rate\" and \"span\" cannot be specified as \"compressionType\":\"ABSOLUTE\". name=["+getName()
							+"] columnName=["+ columnName+"]");
					}
				}
				// widthとRate/Spanと同時指定はエラー
				if( m_timeSeriesProperties.getCompressionWidth(columnName)!= null
						&& (m_timeSeriesProperties.getCompressionRate(columnName)!= null
						||  m_timeSeriesProperties.getCompressionSpan(columnName)!= null ) ) {
					throw  new GridStoreCommandException("\"with\" and \"rate/span\" cannot be specified at the same time. name=["+getName()
							+"] columnName=["+ columnName+"]");
				}
				break;

			case NO:
			case SS:
				// 圧縮方式がNO、SSの時は、カラムごとの圧縮指定はできない。
				throw  new GridStoreCommandException("\"rate\" ,\"span\" and \"width\" cannot be specified "
						+ "as \"compressionMethod\":\"NO|SS\". name=["+getName()+"] columnName=["+ columnName+"]");
			}
		}

		// 圧縮設定されたカラムは、カラム情報に含まれるかどうか
		for ( String column2 : m_timeSeriesProperties.getSpecifiedColumns() ){
			boolean match = false;
			for ( String column : columnNameList ){
				if ( column2.equalsIgnoreCase(column) ) {
					match = true;
					break;
				}
			}
			if ( !match ){
				throw new GridStoreCommandException("The name of compressed column does not exist in ColumnInfoList. name=["+getName()
						+"] compressedColumn="+m_timeSeriesProperties.getSpecifiedColumns()
						+" columnInfoList="+columnNameList.toString());
			}
		}

	}

	/**
	 * 索引情報の整合性をチェックします。
	 */
	private void checkIndexInfo() throws GridStoreCommandException {

		for(IndexInfo index : m_indexInfoList){
			// V4.3 getColumnName()で情報取得しない getColumnNameList()で情報取得する
			// V4.3 複合索引対応 索引が複数カラムになるためStringではなくList<String>を用いる
			//String currentColumnName = index.getColumnName();
			List<String> currentColumnNameList = index.getColumnNameList();
			//Set<IndexType> indexTypeSet = entry.getValue();
			IndexType indexType = index.getType();

			// カラム名
			// V4.3 複合索引対応 索引が複数カラムになるためList<String>の各値をチェックする
			//if ((currentColumnName == null) || (currentColumnName.length() == 0)) {
			//	throw new GridStoreCommandException("ColumnName of index is required. name=["+getName()+"]");
			//}
			if ((currentColumnNameList == null) || (currentColumnNameList.size() == 0)) {
				throw new GridStoreCommandException("ColumnName of index is required. name=["+getName()+"]");
			} else {
				for (String currentColumnName : currentColumnNameList) {
					if ((currentColumnName == null) || (currentColumnName.length() == 0)) {
						throw new GridStoreCommandException("ColumnName of index is required. name=["+getName()+"]");
					}
				}
			}
			// 索引タイプ
			if (indexType == null) {
				// V4.3 複合索引対応 索引が複数カラムになるためStringではなくList<String>を用いる
				//throw new GridStoreCommandException("IndexType is required. : name=["+getName()+"] columnName=["+currentColumnName+"]");
				throw new GridStoreCommandException("IndexType is required. : name=["+getName()+"] columnName=["+currentColumnNameList+"]");
			}

			// 時系列の場合の索引タイプ
			if ( getType() == ContainerType.TIME_SERIES ) {
				// 時系列の場合はTREEのみ
				if ( (indexType!=null) && indexType != IndexType.TREE ) {
					// V4.3 複合索引対応 索引が複数カラムになるためStringではなくList<String>を用いる
					//throw new GridStoreCommandException("IndexType is invalid in type 'TimeSeries'. : name=["+getName()
					//	+"] columnName=["+currentColumnName+"] indexType=["+indexType+"]");
					throw new GridStoreCommandException("IndexType is invalid in type 'TimeSeries'. : name=["+getName()
					+"] columnName=["+currentColumnNameList+"] indexType=["+indexType+"]");
				}
			}

			// 索引タイプとカラムデータ型の組合せ
			// V4.3 複合索引対応 索引が複数カラムになるため索引のカラム名とコンテナのカラム名が一致する回数が複数回になる
			//      一致した回数をカウントする
			int matchCnt = 0;
			boolean match = false;
			for ( int i = 0; i < getColumnCount(); i++ ){
				ColumnInfo columnInfo = getColumnInfo(i);

				// カラム名一致
				// V4.3 複合キー対応  columnInfo が索引を構成するカラムであるかを判定する
				//if( currentColumnName.equalsIgnoreCase(columnInfo.getName())){
				boolean isIndexColumn = false;
				for (String currentColumnName : currentColumnNameList) {
					if (currentColumnName.equalsIgnoreCase(columnInfo.getName())) {
						isIndexColumn = true;
						break;
					}
				}
				if (isIndexColumn) {
					GSType dataType = columnInfo.getType();

					// TimeSeriesの第1カラムには索引は設定できない
					if ( (i==0) && (getType() == ContainerType.TIME_SERIES) ){
						// V4.3 複合索引対応 索引が複数カラムになるためStringではなくList<String>を用いる
						//throw new GridStoreCommandException("The first column of TimeSeries cannot index. : name=["
						//		+getName()+"] columnName=["+currentColumnName+"] indexType=["+indexType+"]");
						throw new GridStoreCommandException("The first column of TimeSeries cannot index. : name=["
								+getName()+"] columnName=["+currentColumnNameList+"] indexType=["+indexType+"]");
					}

					switch(indexType){
					case HASH:
						switch(dataType){
						case BOOL:
						case STRING:
						case BYTE:
						case SHORT:
						case INTEGER:
						case LONG:
						case FLOAT:
						case DOUBLE:
						case TIMESTAMP:
							// OK
							break;
						default:
							// V4.3 複合索引対応 索引が複数カラムになるためStringではなくList<String>を用いる
							//throw  new GridStoreCommandException("The combination of ColumnType and IndexType is invalid. name=["+getName()
							//		+"] columnName=["+ currentColumnName+"] columnType=["+dataType+"] indexType=["+indexType+"]");
							throw  new GridStoreCommandException("The combination of ColumnType and IndexType is invalid. name=["+getName()
									+"] columnName=["+ currentColumnNameList+"] columnType=["+dataType+"] indexType=["+indexType+"]");
						}
						break;

					case SPATIAL:
						if( dataType!=GSType.GEOMETRY ){
							// V4.3 複合索引対応 索引が複数カラムになるためStringではなくList<String>を用いる
							//throw  new GridStoreCommandException("The combination of ColumnType and IndexType is invalid. name=["+getName()
							//		+"] columnName=["+ currentColumnName+"] columnType=["+dataType+"] indexType=["+indexType+"]");
							throw  new GridStoreCommandException("The combination of ColumnType and IndexType is invalid. name=["+getName()
									+"] columnName=["+ currentColumnNameList+"] columnType=["+dataType+"] indexType=["+indexType+"]");
						}
						break;

					case TREE:
						switch(dataType){
						case BOOL:
						case STRING:
						case BYTE:
						case SHORT:
						case INTEGER:
						case LONG:
						case FLOAT:
						case DOUBLE:
						case TIMESTAMP:
							// OK
							break;
						default:
							// V4.3 複合索引対応 索引が複数カラムになるためStringではなくList<String>を用いる
							//throw  new GridStoreCommandException("The combination of ColumnType and IndexType is invalid. name=["+getName()
							//		+"] columnName=["+ currentColumnName+"] columnType=["+dataType+"] indexType=["+indexType+"]");
							throw  new GridStoreCommandException("The combination of ColumnType and IndexType is invalid. name=["+getName()
									+"] columnName=["+ currentColumnNameList+"] columnType=["+dataType+"] indexType=["+indexType+"]");
						}
						break;
					}
					// V4.3 複合索引対応 索引が複数カラムになるため索引のカラム名とコンテナのカラム名が一致する回数が複数回になる
					//      一致した回数をカウントする
					//match = true;
					matchCnt++;
					// V4.3 複合索引対応 索引が複数カラムになるため索引のカラム名とコンテナのカラム名が一致する回数が複数回になる
					//      一度カラム名が一致してもbreakしない
					//      索引のカラム名とコンテナのカラム名が一致した回数が索引のカラム数に達したらbreak
					//break;
					if (currentColumnNameList.size() <= matchCnt) {
						match = true;
						break;
					}
				}
			}

			// 索引設定されているカラムがカラム情報に存在するか
			if ( !match ){
				// V4.3 複合索引対応 索引が複数カラムになるためStringではなくList<String>を用いる
				//throw new GridStoreCommandException("The columnName of IndexList does not exist in ColumnInfoList. : name=["+getName()
				//		+"] columnName=["+currentColumnName+"]");
				throw new GridStoreCommandException("The columnName of IndexList does not exist in ColumnInfoList. : name=["+getName()
						+"] columnName=["+currentColumnNameList+"]");
			}

		}
	}

	/**
	 * Export/Import用の設定の整合性をチェックします。
	 *
	 * @return
	 */
	public boolean checkExpImpSetting(){

		for ( String fileName : containerFileList ){
			new File(fileName);
		}

		return true;
	}
}
