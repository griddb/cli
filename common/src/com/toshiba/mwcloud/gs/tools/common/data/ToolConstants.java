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


public class ToolConstants {

	/** メタ情報ファイルフォーマットのバージョン */
	public static String META_FILE_VERSION = "5.3.0";


	/** ロウファイルのタイプ(CSV/バイナリ) */
	public static enum RowFileType { CSV, BINARY, AVRO, ARCHIVE_CSV }

	/** JSONファイルの文字コード */
	public static final String ENCODING_JSON			= "UTF-8";

	/** ファイルの拡張子(メタ情報ファイル) */
	public static final String FILE_EXT_METAINFO		= "_properties.json";

	/** デフォルトデータベース名 */
	public static final String PUBLIC_DB				= "public";

	/** DBとコンテナ名の区切り文字 */
	public static final String DB_DELIMITER				= ".";

	/** DATE TIME FORMAT */
	public static final String DATE_FORMAT_MILLISECOND = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
	public static final String DATE_FORMAT_MICROSECOND = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX";
	public static final String DATE_FORMAT_NANOSECOND = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSXXX";

	//******************************************************************
	// メタ情報ファイル (*_properties.json)の項目名
	//******************************************************************
	public static final String JSON_META_VERSION				= "version";
	public static final String JSON_META_DBNAME					= "database";
	public static final String JSON_META_CONTAINER				= "container";
	public static final String JSON_META_CONTAINER_TYPE			= "containerType";
	public static final String JSON_META_CONTAINER_ATTRIBUTE	= "attribute";
	public static final String JSON_META_CONTAINER_FILE			= "containerFile";
	public static final String JSON_META_CONTAINER_INTERNAL_FILE	= "containerInternalFile";
	public static final String JSON_META_CONTAINER_FILE_TYPE	= "containerFileType";
	public static final String JSON_META_DATA_AFFINITY			= "dataAffinity";
	public static final String JSON_META_ROW_KEY				= "rowKeyAssigned";
	public static final String JSON_META_PARTITION_NO			= "partitionNo";
	// V4.3 "rowKeySet"追加
	public static final String JSON_META_ROWKEY_SET			= "rowKeySet";

	// 期限解放情報
    public static final String JSON_META_EXPIRATION_TYPE            = "expirationType";
    public static final String JSON_META_EXPIRATION_TIME            = "expirationTime";
    public static final String JSON_META_EXPIRATION_TIME_UNIT       = "expirationTimeUnit";

	// 長期アーカイブ専用の項目名
	public static final String JSON_META_ARCHIVE_INFO			= "archiveInfo";
	public static final String JSON_META_NODE_ADDR			= "nodeAddr";
	public static final String JSON_META_NODE_PORT			= "nodePort";
	public static final String JSON_META_DATABASE_ID			= "databaseId";
	public static final String JSON_META_CONTAINER_ID			= "containerId";
	public static final String JSON_META_DATAPARTITION_ID		= "dataPartitionId";
	public static final String JSON_META_ROW_INDEX_OID			= "rowIndexOID";
	public static final String JSON_META_MVCC_INDEX_OID			= "mvccIndexOID";
	public static final String JSON_META_INIT_SCHEMA_STATUS		= "initSchemaStatus";
	public static final String JSON_META_SCHEMA_VERSION			= "schemaVersion";
	public static final String JSON_META_START_TIME			= "startTime";
	public static final String JSON_META_END_TIME			= "endTime";
	public static final String JSON_META_EXPIRED_TIME			= "expiredTime";
	public static final String JSON_META_ERASABLE_TIME		= "erasableTime";
	public static final String JSON_META_SCHEMA_INFORMATION	= "schemaInformation";

	public static final String[] JSON_META_GROUP_CONTAINER ={
		JSON_META_DBNAME, JSON_META_CONTAINER, JSON_META_CONTAINER_TYPE, JSON_META_CONTAINER_ATTRIBUTE,
		JSON_META_CONTAINER_FILE, JSON_META_CONTAINER_FILE_TYPE, JSON_META_DATA_AFFINITY,
		JSON_META_ROW_KEY, JSON_META_PARTITION_NO, JSON_META_ROWKEY_SET, JSON_META_CONTAINER_INTERNAL_FILE,
		JSON_META_EXPIRATION_TYPE, JSON_META_EXPIRATION_TIME, JSON_META_EXPIRATION_TIME_UNIT,
		JSON_META_ARCHIVE_INFO, JSON_META_NODE_ADDR, JSON_META_NODE_PORT,
		JSON_META_DATABASE_ID, JSON_META_CONTAINER_ID, JSON_META_DATAPARTITION_ID,
		JSON_META_ROW_INDEX_OID, JSON_META_MVCC_INDEX_OID, JSON_META_INIT_SCHEMA_STATUS,
		JSON_META_SCHEMA_VERSION, JSON_META_START_TIME, JSON_META_END_TIME,
		JSON_META_EXPIRED_TIME, JSON_META_ERASABLE_TIME, JSON_META_SCHEMA_INFORMATION
	};

	// ColumnSet
	public static final String JSON_META_COLUMN_SET				= "columnSet";
	public static final String JSON_META_COLUMN_NAME			= "columnName";
	public static final String JSON_META_COLUMN_TYPE			= "type";
	public static final String JSON_META_COLUMN_TIME_PRECISION	= "precision";
	//public static final String JSON_META_COLUMN_CSTR			= "constraints";
	public static final String JSON_META_COLUMN_CSTR_NOTNULL	= "notNull";

	// TimeSeriesProperties
	public static final String JSON_META_TIME_PROP				= "timeSeriesProperties";
	public static final String JSON_META_TIME_COMP				= "compressionMethod";
	public static final String JSON_META_TIME_WINDOW			= "compressionWindowSize";
	public static final String JSON_META_TIME_WINDOW_UNIT		= "compressionWindowSizeUnit";
	public static final String JSON_META_TIME_EXPIRATION_DIV_COUNT	= "expirationDivisionCount";
	public static final String JSON_META_TIME_EXPIRATION		= "rowExpirationElapsedTime";
	public static final String JSON_META_TIME_EXPIRATION_UNIT	= "rowExpirationTimeUnit";
	public static final String JSON_META_TIME_UNIT_NULL			= "null";

	// timeSeriesPropertiesの中に出現する項目名
	public static final String[] JSON_META_GROUP_TIME	= {JSON_META_TIME_COMP, JSON_META_TIME_WINDOW,
		JSON_META_TIME_WINDOW_UNIT, JSON_META_TIME_EXPIRATION_DIV_COUNT ,
		JSON_META_TIME_EXPIRATION, JSON_META_TIME_EXPIRATION_UNIT};

	// IndexSet
	public static final String JSON_META_INDEX_SET				= "indexSet";
	public static final String JSON_META_INDEX_NAME				= "columnName";
	// V4.3 "columnNames" 追加
	public static final String JSON_META_INDEX_COLUMN_NAMES		= "columnNames";
	public static final String JSON_META_INDEX_TYPE1			= "type";
	public static final String JSON_META_INDEX_TYPE2			= "indexType";
	public static final String JSON_META_INDEX_INDEXNAME		= "indexName";

	// TriggerInfoSet
	public static final String JSON_META_TRIGGER_SET			= "triggerInfoSet";
	public static final String JSON_META_TRIGGER_EVENTNAME		= "eventName";
	public static final String JSON_META_TRIGGER_TYPE			= "notificationType";
	public static final String JSON_META_TRIGGER_TARGET			= "targetEvents";
	public static final String JSON_META_TRIGGER_COLUMN			= "targetColumnNames";
	public static final String JSON_META_TRIGGER_URI			= "notificationURI";
	public static final String JSON_META_TRIGGER_JMS_TYPE		= "JmsDestinationType";
	public static final String JSON_META_TRIGGER_JMS_NAME		= "JmsDestinationName";
	public static final String JSON_META_TRIGGER_JMS_USER		= "JmsUser";
	public static final String JSON_META_TRIGGER_JMS_PASS		= "JmsPassword";

	public static final String[] JSON_META_GROUP_TRIGGER =
		{JSON_META_TRIGGER_EVENTNAME, JSON_META_TRIGGER_TYPE, JSON_META_TRIGGER_TARGET,
		JSON_META_TRIGGER_COLUMN, JSON_META_TRIGGER_URI, JSON_META_TRIGGER_JMS_TYPE,
		JSON_META_TRIGGER_JMS_NAME, JSON_META_TRIGGER_JMS_USER,
		JSON_META_TRIGGER_JMS_PASS};

	// CompressionInfoSet
	public static final String JSON_META_CMP_SET				= "compressionInfoSet";
	public static final String JSON_META_CMP_NAME				= "columnName";
	public static final String JSON_META_CMP_TYPE				= "compressionType";
	public static final String JSON_META_CMP_RATE				= "rate";
	public static final String JSON_META_CMP_SPAN				= "span";
	public static final String JSON_META_CMP_WIDTH				= "width";

	public static final String[] JSON_META_GROUP_CMP = {
		JSON_META_CMP_NAME, JSON_META_CMP_TYPE, JSON_META_CMP_RATE, JSON_META_CMP_SPAN,
		JSON_META_CMP_WIDTH
	};

	// TablePartitionProperties
	public static final String JSON_META_TP_PROPS				= "tablePartitionInfo";
	public static final String JSON_META_TP_TYPE				= "type";
	public static final String JSON_META_TP_COLUMN				= "column";
	public static final String JSON_META_TP_DIV_COUNT			= "divisionCount";
	public static final String JSON_META_TP_ITV_VALUE			= "intervalValue";
	public static final String JSON_META_TP_ITV_UNIT			= "intervalUnit";

	// TimeIntervalInfo
	public static final String JSON_META_TIME_INTERVAL_INFO	= "timeIntervalInfo";
	public static final String JSON_META_BOUNDARY_VALUE = "boundaryValue";	
	
	//******************************************************************
	//  メタ情報ファイルの値
	//******************************************************************
	// 圧縮指定関連
	public static final double COMPRESSION_WIDTH_INIT = -1;
	public static final double COMPRESSION_RATE_INIT  = -1;
	public static final double COMPRESSION_SPAN_INIT  = -1;

	public static final String COMPRESSION_TYPE_INIT  = "";
	public static final String COMPRESSION_TYPE_RELATIVE = "RELATIVE";
	public static final String COMPRESSION_TYPE_ABSOLUTE = "ABSOLUTE";

	// カラムタイプ
	public static final String COLUMN_TYPE_STRING_ARRAY		= "string[]";
	public static final String COLUMN_TYPE_BOOL_ARRAY		= "boolean[]";
	public static final String COLUMN_TYPE_BYTE_ARRAY		= "byte[]";
	public static final String COLUMN_TYPE_SHORT_ARRAY		= "short[]";
	public static final String COLUMN_TYPE_INTEGER_ARRAY	= "integer[]";
	public static final String COLUMN_TYPE_LONG_ARRAY		= "long[]";
	public static final String COLUMN_TYPE_FLOAT_ARRAY		= "float[]";
	public static final String COLUMN_TYPE_DOUBLE_ARRAY		= "double[]";
	public static final String COLUMN_TYPE_TIMESTAMP_ARRAY	= "timestamp[]";
	public static final String COLUMN_TYPE_BOOL				= "boolean";
	public static final String COLUMN_TYPE_TIMESTAMP_MILI	= "timestamp(3)";
	public static final String COLUMN_TYPE_TIMESTAMP_MICRO	= "timestamp(6)";
	public static final String COLUMN_TYPE_TIMESTAMP_NANO	= "timestamp(9)";

	// (互換性用)コンテナ属性
	public static final String EXP_TOOL_ATTRIBUTE_BASE = "BASE";
	public static final String EXP_TOOL_ATTRIBUTE_SINGLE = "SINGLE";
	public static final String EXP_TOOL_ATTRIBUTE_LARGE = "LARGE";
	public static final String EXP_TOOL_ATTRIBUTE_SUB = "SUB";
	public static final String EXP_TOOL_ATTRIBUTE_SINGLE_SYSTEM = "SINGLE_SYSTEM";

	// テーブルパーティション
	public static final String TABLE_PARTITION_TYPE_HASH		= "HASH";
	public static final String TABLE_PARTITION_TYPE_INTERVAL	= "INTERVAL";
	public static final String TABLE_PARTITION_TYPE_RANGE		= "RANGE";
	public static final String TABLE_PARTITION_ITV_UNIT_DAY		= "DAY";

	// 期限解放種別
	public static final String EXPIRATION_TYPE_PARTITION = "PARTITION";
	public static final String EXPIRATION_TYPE_ROW = "ROW";
//	public static final String EXPIRATION_TYPE_TABLE = "table";

	//******************************************************************
	// ビュー定義ファイル (gs_export_view.json)の項目名
	//******************************************************************
	public static final String JSON_VIEW_VERSION = "version";
	public static final String JSON_VIEW_VIEW = "view";
	public static final String JSON_VIEW_VIEW_DATABASE = "database";
	public static final String JSON_VIEW_VIEW_NAME = "name";
	public static final String JSON_VIEW_VIEW_DEFINITION = "definition";

	//******************************************************************
	//  JDBC関連
	//******************************************************************

	/*
	 * メタテーブル名
	 */
	public static final String META_TABLES 				= "#tables";
	public static final String META_TABLE_PARTITIONS	= "#table_partitions";
	public static final String META_COLUMNS				= "#columns";
	public static final String META_INDEX_INFO			= "#index_info";
	public static final String META_PRIMARY_KEYS		= "#primary_keys";
	public static final String META_EVENT_TRIGGERS		= "#event_triggers";
	public static final String META_VIEWS				= "#views";
	public static final String META_SQL_INFO			= "#sqls";
	public static final String META_EVENT_INFO			= "#events";
	public static final String META_CONNECTION_INFO	= "#sockets";

	/*
	 * SQL文
	 */
	private static final String STMT_SELECT_ANY_FROM = "SELECT * FROM ";

	private static final String STMT_SELECT_TABLE_NAME_FROM = "SELECT TABLE_NAME FROM ";
	
	/** テーブルメタテーブル取得  */		// SELECT * FROM "#tables"
	public static final String STMT_SELECT_META_TABLES = STMT_SELECT_ANY_FROM + "\"" + META_TABLES + "\"";

	/** テーブルメタテーブル名取得  */ // SELECT TABLE_NAME FROM "#tables"
	public static final String STMT_SELECT_META_TABLE_NAMES = STMT_SELECT_TABLE_NAME_FROM + "\"" + META_TABLES + "\"";

	/** TimeSeriesのテーブル名取得  */ // SELECT TABLE_NAME FROM "#tables" WHERE TABLE_OPTIONAL_TYPE = 'TIMESERIES'
	public static final String STMT_SELECT_META_TIMESERIES_NAMES = STMT_SELECT_META_TABLE_NAMES + " WHERE TABLE_OPTIONAL_TYPE = 'TIMESERIES'";
	
	/** パーティションメタテーブル取得 */		// SELECT * FROM "#table_partitions"
	public static final String STMT_SELECT_META_TABLE_PARTITIONS = STMT_SELECT_ANY_FROM + "\"" + META_TABLE_PARTITIONS + "\"";

	/** カラムメタテーブル取得 */			// SELECT * FROM "#columns"
	public static final String STMT_SELECT_META_COLUMNS = STMT_SELECT_ANY_FROM + "\"" + META_COLUMNS + "\"";

	/** 索引メタテーブル取得 */			// SELECT * FROM "#index_info"
	public static final String STMT_SELECT_META_INDEX_INFO = STMT_SELECT_ANY_FROM + "\"" + META_INDEX_INFO + "\"";

	/** プライマリキーメタテーブル取得 */		// SELECT * FROM "#primary_keys"
	public static final String STMT_SELECT_META_PRIMARY_KEYS = STMT_SELECT_ANY_FROM + "\"" + META_PRIMARY_KEYS + "\"";

	/** イベントトリガメタテーブル取得 */	// SELECT * FROM "#event_triggers"
	public static final String STMT_SELECT_META_EVENT_TRIGGERS = STMT_SELECT_ANY_FROM + "\"" + META_EVENT_TRIGGERS + "\"";

	/** パーティションテーブル名一覧取得 */	// SELECT * FROM "#tables" WHERE PARTITION_TYPE IS NOT NULL
	public static final String STMT_SELECT_META_TABLES_PATITIONNAMES = STMT_SELECT_META_TABLES + " WHERE PARTITION_TYPE IS NOT NULL";

	/** パーティショニングテーブルかつパーティションキーがTimeStamp型のインターバルパーティションテーブル名一覧取得 */ // SELECT TABLE_NAME FROM "#tables" WHERE PARTITION_TYPE = 'INTERVAL' and PARTITION_INTERVAL_UNIT IS NOT NULL
	public static final String STMT_SELECT_META_TABLES_INTERVAL_PARTITIONNAMES = STMT_SELECT_META_TABLE_NAMES + " WHERE PARTITION_TYPE = 'INTERVAL' and PARTITION_INTERVAL_UNIT IS NOT NULL";
	
	/** パーティショニングテーブルかつパーティションキーがTimeStamp型以外のインターバルパーティションテーブル名一覧取得 */ // SELECT TABLE_NAME FROM "#tables" WHERE PARTITION_TYPE = 'INTERVAL' and PARTITION_INTERVAL_UNIT IS NULL
	public static final String STMT_SELECT_META_TABLES_INTERVAL_NOT_TIMESTAMP_PARTITIONNAMES = STMT_SELECT_META_TABLE_NAMES + " WHERE PARTITION_TYPE = 'INTERVAL' and PARTITION_INTERVAL_UNIT IS NULL";

	/** パーティショニングテーブルかつパーティションキーがTimeStamp型以外のハッシュパーティションテーブル名一覧取得 */ // SELECT TABLE_NAME FROM "#tables" WHERE PARTITION_TYPE = 'HASH' and PARTITION_INTERVAL_UNIT IS NULL
	public static final String STMT_SELECT_META_TABLES_HASH_NOT_TIMESTAMP_PARTITIONNAMES = STMT_SELECT_META_TABLE_NAMES + " WHERE PARTITION_TYPE = 'HASH' and PARTITION_INTERVAL_UNIT IS NULL";

	/** ビューメタテーブル取得 */	// SELECT * FROM "#views"
	public static final String STMT_SELECT_META_VIEWS = STMT_SELECT_ANY_FROM + "\"" + META_VIEWS + "\"";

	/** SQL処理一覧テーブルメタテーブル取得  */		// SELECT * FROM "#sqls"
	public static final String STMT_SELECT_SQL_INFO = STMT_SELECT_ANY_FROM + "\"" + META_SQL_INFO + "\"";

	/** SQL処理一覧テーブルメタテーブル取得  */		// ORDER BY START_TIME, NODE_ADDRESS, NODE_PORT, QUERY_ID, JOB_ID
	public static final String STMT_SELECT_SQL_INFO_ORDER_BY = " ORDER BY START_TIME, NODE_ADDRESS, NODE_PORT, QUERY_ID, JOB_ID";

	/** SQL処理一覧テーブルメタテーブル取得  */		// WHERE START_TIME < now()
	public static final String STMT_SELECT_SQL_INFO_PAST = " WHERE START_TIME < now()";

	/*
	 * プリペアードステートメント
	 */
	private static final String PSTMT_WHERE_TABLE_NAME = " WHERE TABLE_NAME='%s';";

	private static final String PSTMT_WHERE_QUERY_ID = " WHERE QUERY_ID=?";

	private static final String PSTMT_WHERE_QUERY_ID_AND_SQLONLY = " WHERE QUERY_ID=? AND SQL IS NOT NULL";

	private static final String PSTMT_WHERE_JOB_ID = " WHERE JOB_ID=?";

	/** テーブル削除(1:テーブル名) */
	public static final String PSTMT_DROP_TABLE = "DROP TABLE \"%s\";";

	/** テーブルメタテーブル取得(1:テーブル名) */		// SELECT * FROM "#tables" WHERE TABLE_NAME=?
	public static final String PSTMT_SELECT_META_TABLES_WITH_TABLE = STMT_SELECT_META_TABLES + PSTMT_WHERE_TABLE_NAME;

	/** パーティションメタテーブル取得(1:テーブル名) */		// SELECT * FROM "#table_partitions" WHERE TABLE_NAME=?
	public static final String PSTMT_SELECT_META_TABLE_PARTITIONS_WITH_TABLE = STMT_SELECT_META_TABLE_PARTITIONS + PSTMT_WHERE_TABLE_NAME;

	/** カラムメタテーブル取得(1:テーブル名) */			// SELECT * FROM "#columns" WHERE TABLE_NAME=?
	public static final String PSTMT_SELECT_META_COLUMNS_WITH_TABLE = STMT_SELECT_META_COLUMNS + PSTMT_WHERE_TABLE_NAME;

	/** 索引メタテーブル取得(1:テーブル名) */			// SELECT * FROM "#index_info" WHERE TABLE_NAME=?
	public static final String PSTMT_SELECT_META_INDEX_INFO_WITH_TABLE = STMT_SELECT_META_INDEX_INFO + PSTMT_WHERE_TABLE_NAME;

	/** プライマリキーメタテーブル取得(1:テーブル名) */	// SELECT * FROM "#primary_keys" WHERE TABLE_NAME=?
	public static final String PSTMT_SELECT_META_PRIMARY_KEYS_WITH_TABLE = STMT_SELECT_META_PRIMARY_KEYS + PSTMT_WHERE_TABLE_NAME;

	/** イベントトリガメタテーブル取得(1:テーブル名) */	// SELECT * FROM "#event_triggers" WHERE TABLE_NAME=?
	public static final String PSTMT_SELECT_META_EVENT_TRIGGERS_WITH_TABLE = STMT_SELECT_META_EVENT_TRIGGERS + PSTMT_WHERE_TABLE_NAME;

	/** WHERE VIEW_NAME= */
	private static final String PSTMT_WHERE_VIEW_NAME = " WHERE VIEW_NAME='%s';";

	/** ビュー取得(1:ビュー名) */ // SELECT * FROM "#views" WHERE TABLE_NAME=?
	public static final String PSTMT_SELECT_META_VIEWS_WITH_TABLE = STMT_SELECT_META_VIEWS + PSTMT_WHERE_VIEW_NAME;

	/** ビュー削除(1:ビュー名) */ // DROP VIEW ?;
	public static final String PSTMT_DROP_VIEW = "DROP VIEW \"%s\";";

	/** ビュー作成(1:ビュー名, 2:ビュー定義) */ // CREATE VIEW ? AS ?;
	public static final String PSTMT_CREATE_VIEW = "CREATE FORCE VIEW \"%s\" AS %s";

	/** ビュー参照可否チェック(1:ビュー名) */ // SELECT * FROM CREATE VIEW ? AS ?;
	public static final String PSTMT_CHECK_VIEW = STMT_SELECT_ANY_FROM + "\"%s\" LIMIT 0";

	/** SQL処理一覧メタテーブル取得 */		// SELECT * FROM "#sqls" WHERE START_TIME < now() ORDER BY START_TIME, NODE_ADDRESS, NODE_PORT, QUERY_ID, JOB_ID
	public static final String PSTMT_SELECT_META_SQL_INFO_NO_QUERY_ID = STMT_SELECT_SQL_INFO + STMT_SELECT_SQL_INFO_PAST + STMT_SELECT_SQL_INFO_ORDER_BY;

	/** SQL処理一覧メタテーブル取得(1:クエリID) */		// SELECT * FROM "#sqls" WHERE QUERY_ID=? ORDER BY START_TIME, NODE_ADDRESS, NODE_PORT, QUERY_ID, JOB_ID
	public static final String PSTMT_SELECT_META_SQL_INFO_WITH_QUERY_ID = STMT_SELECT_SQL_INFO + PSTMT_WHERE_QUERY_ID + STMT_SELECT_SQL_INFO_ORDER_BY;

	/** SQLキャンセルメタテーブル取得(1:クエリID) */		// SELECT * FROM "#sqls" WHERE QUERY_ID=? AND SQL IS NOT NULL ORDER BY START_TIME, NODE_ADDRESS, NODE_PORT, QUERY_ID, JOB_ID
	public static final String PSTMT_SELECT_META_SQL_INFO_WITH_QUERY_ID_SQLONLY = STMT_SELECT_SQL_INFO + PSTMT_WHERE_QUERY_ID_AND_SQLONLY + STMT_SELECT_SQL_INFO_ORDER_BY;

	/** SQL処理一覧メタテーブル取得(1:ジョブID) */		// SELECT * FROM "#sqls" WHERE JOB_ID=? ORDER BY START_TIME, NODE_ADDRESS, NODE_PORT, QUERY_ID, JOB_ID
	public static final String PSTMT_SELECT_META_SQL_INFO_WITH_JOB_ID = STMT_SELECT_SQL_INFO + PSTMT_WHERE_JOB_ID + STMT_SELECT_SQL_INFO_ORDER_BY;

	/*
	 * TQL文
	 */
	private static final String TQL_SELECT_ANY = "SELECT * ";
	private static final String TQL_WHERE_EVENT_INFO = " WHERE START_TIME < now() ";
	private static final String TQL_WHERE_SOCKET_TYPE_CLIENT = " WHERE SOCKET_TYPE='CLIENT' ";

	/** イベント一覧メタテーブル取得 */		// SELECT * WHERE START_TIME < now()
	public static final String TQL_SELECT_META_EVENT_INFO = TQL_SELECT_ANY + TQL_WHERE_EVENT_INFO;
	/** コネクション一覧メタテーブル取得 */		// SELECT * WHERE SOCKET_TYPE='CLIENT'
	public static final String TQL_SELECT_META_CONNECTION_INFO = TQL_SELECT_ANY + TQL_WHERE_SOCKET_TYPE_CLIENT;

	/*
	 * pragmaのパラメータ名
	 */
	/** 非公開メタテーブル可視化(0: 不可視(デフォルト), 1:可視) */
	public static final String PRAMGMA_INTERNAL_META_TABLE_VISIBLE = "internal.compiler.meta_table_visible";

	/*
	 * メタテーブル #tables のカラム名
	 */
	public static final String META_TABLES_DATABASE_NAME 				= "DATABASE_NAME";
	public static final String META_TABLES_TABLE_NAME 					= "TABLE_NAME";
//	public static final String META_TABLES_TABLE_OPTIONAL_TYPE 			= "TABLE_OPTIONAL_TYPE";
//	public static final String META_TABLES_DATA_AFFINITY 				= "DATA_AFFINITY";
	public static final String META_TABLES_EXPIRATION_TYPE				= "EXPIRATION_TYPE";
	public static final String META_TABLES_EXPIRATION_TIME 				= "EXPIRATION_TIME";
	public static final String META_TABLES_EXPIRATION_TIME_UNIT 		= "EXPIRATION_TIME_UNIT";
//	public static final String META_TABLES_EXPIRATION_DIVISION_COUNT 	= "EXPIRATION_DIVISION_COUNT";
//	public static final String META_TABLES_COMPRESSION_METHOD 			= "COMPRESSION_METHOD";
//	public static final String META_TABLES_COMPRESSION_WINDOW_SIZE 		= "COMPRESSION_WINDOW_SIZE";
//	public static final String META_TABLES_COMPRESSION_WINDOW_SIZE_UNIT = "COMPRESSION_WINDOW_SIZE_UNIT";
	public static final String META_TABLES_PARTITION_TYPE 				= "PARTITION_TYPE";
	public static final String META_TABLES_PARTITION_COLUMN 			= "PARTITION_COLUMN";
	public static final String META_TABLES_PARTITION_INTERVAL_VALUE 	= "PARTITION_INTERVAL_VALUE";
	public static final String META_TABLES_PARTITION_INTERVAL_UNIT 		= "PARTITION_INTERVAL_UNIT";
	public static final String META_TABLES_PARTITION_DIVISION_COUNT 	= "PARTITION_DIVISION_COUNT";
	public static final String META_TABLES_SUBPARTITION_TYPE 			= "SUBPARTITION_TYPE";
	public static final String META_TABLES_SUBPARTITION_COLUMN 			= "SUBPARTITION_COLUMN";
	public static final String META_TABLES_SUBPARTITION_INTERVAL_VALUE 	= "SUBPARTITION_INTERVAL_VALUE";
	public static final String META_TABLES_SUBPARTITION_INTERVAL_UNIT 	= "SUBPARTITION_INTERVAL_UNIT";
	public static final String META_TABLES_SUBPARTITION_DIVISION_COUNT 	= "SUBPARTITION_DIVISION_COUNT";
//	public static final String META_TABLES_CLUSTER_PARTITION_INDEX 		= "CLUSTER_PARTITION_INDEX";

	/*
	 * メタテーブル #columns のカラム名
	 */
//	public static final String META_COLUMNS_DATABASE_NAME 			= "DATABASE_NAME";
//	public static final String META_COLUMNS_TABLE_NAME 				= "TABLE_NAME";
//	public static final String META_COLUMNS_ORDINAL_POSITION 		= "ORDINAL_POSITION";
//	public static final String META_COLUMNS_COLUMN_NAME 			= "COLUMN_NAME";
//	public static final String META_COLUMNS_TYPE_NAME 				= "TYPE_NAME";
//	public static final String META_COLUMNS_NULLABLE 				= "NULLABLE";
//	public static final String META_COLUMNS_COMPRESSION_RELATIVE 	= "COMPRESSION_RELATIVE";
//	public static final String META_COLUMNS_COMPRESSION_RATE 		= "COMPRESSION_RATE";
//	public static final String META_COLUMNS_COMPRESSION_SPAN 		= "COMPRESSION_SPAN";
//	public static final String META_COLUMNS_COMPRESSION_WIDTH 		= "COMPRESSION_WIDTH";

//	public static final String META_PRIMARY_KEYS_DATABASE_NAME 		= "DATABASE_NAME";
//	public static final String META_PRIMARY_KEYS_TABLE_NAME 		= "TABLE_NAME";
//	public static final String META_PRIMARY_KEYS_COLUMN_NAME 		= "COLUMN_NAME";
//	public static final String META_PRIMARY_KEYS_KEY_SEQ 			= "KEY_SEQ";

	/*
	 * メタテーブル #table_partitions のカラム名
	 */
	public static final String META_TABLE_PARTITIONS_DATABASE_NAME 					= "DATABASE_NAME";
	public static final String META_TABLE_PARTITIONS_TABLE_NAME 					= "TABLE_NAME";
	public static final String META_TABLE_PARTITIONS_PARTITION_SEQ 					= "PARTITION_SEQ";
	public static final String META_TABLE_PARTITIONS_PARTITION_NAME 				= "PARTITION_NAME";
	public static final String META_TABLE_PARTITIONS_PARTITION_BOUNDARY_VALUE 		= "PARTITION_BOUNDARY_VALUE";
	public static final String META_TABLE_PARTITIONS_SUBPARTITION_BOUNDARY_VALUE 	= "SUBPARTITION_BOUNDARY_VALUE";
	public static final String META_TABLE_PARTITIONS_PARTITION_NODE_AFFINITY 		= "PARTITION_NODE_AFFINITY";
	public static final String META_TABLE_PARTITIONS_CLUSTER_PARTITION_INDEX 		= "CLUSTER_PARTITION_INDEX";

	/*
	 * メタテーブル #index_info のカラム名
	 */
//	public static final String META_INDEX_INFO_DATABASE_NAME 		= "DATABASE_NAME";
//	public static final String META_INDEX_INFO_TABLE_NAME 			= "TABLE_NAME";
//	public static final String META_INDEX_INFO_INDEX_NAME 			= "INDEX_NAME";
//	public static final String META_INDEX_INFO_INDEX_TYPE 			= "INDEX_TYPE";
//	public static final String META_INDEX_INFO_ORDINAL_POSITION 	= "ORDINAL_POSITION";
//	public static final String META_INDEX_INFO_COLUMN_NAME 			= "COLUMN_NAME";

	/*
	 * メタテーブル #event_triggers のカラム名
	 */
//	public static final String META_EVENT_TRIGGERS_DATABASE_NAME 			= "DATABASE_NAME";
//	public static final String META_EVENT_TRIGGERS_TABLE_NAME 				= "TABLE_NAME";
//	public static final String META_EVENT_TRIGGERS_TRIGGER_NAME 			= "TRIGGER_NAME";
//	public static final String META_EVENT_TRIGGERS_EVENT_TYPE 				= "EVENT_TYPE";
//	public static final String META_EVENT_TRIGGERS_COLUMN_NAME 				= "COLUMN_NAME";
//	public static final String META_EVENT_TRIGGERS_TRIGGER_TYPE 			= "TRIGGER_TYPE";
//	public static final String META_EVENT_TRIGGERS_URI 						= "URI";
//	public static final String META_EVENT_TRIGGERS_JMS_DESTINATION_TYPE 	= "JMS_DESTINATION_TYPE";
//	public static final String META_EVENT_TRIGGERS_JMS_DESTINATION_NAME 	= "JMS_DESTINATION_NAME";
//	public static final String META_EVENT_TRIGGERS_USER 					= "USER";
//	public static final String META_EVENT_TRIGGERS_PASSWORD 				= "PASSWORD";

	/*
	 * メタテーブル#sqls のカラム名
	 */
	public static final String META_TABLE_SQL_INFO_DATABASE_NAME = "DATABASE_NAME";
	public static final String META_TABLE_SQL_INFO_NODE_ADDRESS = "NODE_ADDRESS";
	public static final String META_TABLE_SQL_INFO_NODE_PORT = "NODE_PORT";
	public static final String META_TABLE_SQL_INFO_START_TIME = "START_TIME";
	public static final String META_TABLE_SQL_INFO_APPLICATION_NAME = "APPLICATION_NAME";
	public static final String META_TABLE_SQL_INFO_SQL = "SQL";
	public static final String META_TABLE_SQL_INFO_QUERY_ID = "QUERY_ID";
	public static final String META_TABLE_SQL_INFO_JOB_ID = "JOB_ID";

	/*
	 * メタテーブル#events のカラムインデックス
	 */
	public static final int META_TABLE_EVENT_INFO_NODE_ADDRESS_IDX = 0;
	public static final int META_TABLE_EVENT_INFO_NODE_PORT_IDX = 1;
	public static final int META_TABLE_EVENT_INFO_START_TIME_IDX = 2;
	public static final int META_TABLE_EVENT_INFO_APPLICATION_NAME_IDX = 3;
	public static final int META_TABLE_EVENT_SERVICE_TYPE_IDX = 4;
	public static final int META_TABLE_EVENT_EVENT_TYPE_IDX = 5;
	public static final int META_TABLE_EVENT_WORKER_ID_IDX = 6;
	public static final int META_TABLE_EVENT_CLUSTER_PARTITION_ID_IDX = 7;

	/*
	 * メタテーブル#sockets のカラムインデックス
	 */
	public static final int META_TABLE_CONNECTION_INFO_SERVICE_TYPE_IDX = 0;
	public static final int META_TABLE_CONNECTION_INFO_SOCKET_TYPE_IDX = 1;
	public static final int META_TABLE_CONNECTION_INFO_NODE_ADDRESS_IDX = 2;
	public static final int META_TABLE_CONNECTION_INFO_NODE_PORT_IDX = 3;
	public static final int META_TABLE_CONNECTION_INFO_REMOTE_ADDRESS_IDX = 4;
	public static final int META_TABLE_CONNECTION_INFO_REMOTE_PORT_IDX = 5;
	public static final int META_TABLE_CONNECTION_INFO_APPLICATION_NAME_IDX = 6;
	public static final int META_TABLE_CONNECTION_INFO_CREATION_TIME_IDX = 7;
	public static final int META_TABLE_CONNECTION_INFO_DISPATCHING_EVENT_COUNT_IDX = 8;
	public static final int META_TABLE_CONNECTION_INFO_SENDING_EVENT_COUNT_IDX = 9;

	/*
	 * メタテーブル#views のカラムインデックス
	 */
	public static final String META_VIEWS_DATABASE_NAME		= "DATABASE_NAME";
	public static final String META_VIEWS_VIEW_NAME			= "VIEW_NAME";
	public static final String META_VIEWS_VIEW_DEFINITION	= "VIEW_DEFINITION";
}
