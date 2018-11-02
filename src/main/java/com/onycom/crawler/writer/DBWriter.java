package com.onycom.crawler.writer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.onycom.crawler.data.Config;
import com.onycom.crawler.data.Contents;
import com.onycom.crawler.data.KeyValue;
import com.onycom.SettingBasedCrawler.Crawler;
import com.onycom.common.CrawlerLog;
import com.onycom.crawler.data.CollectRecode;

/**
 * DB 저장 구현제. 기본 JDBC 를 활용. 안정성을 위한 구현 고도화 필요
 */
public class DBWriter implements Writer {
	static Logger mLogger = CrawlerLog.GetInstance(DBWriter.class);
	Config mConfig;

	static String DRIVER = "org.mariadb.jdbc.Driver";
	static String PATH = "jdbc:mariadb://localhost:3306/DEV_CRAWLER_LOG"; // 172.17.0.10
	static String USER = "root";
	static String PW = "sairport2018";

	static final String Q_INSERT_HISTORY = "INSERT INTO T_HISTORY (URL) VALUES (\"%s\")";
	static final String Q_INSERT_ERR = "INSERT INTO T_ERR_URL (URL,REASON) VALUES (\"%s\", \"%s\")";
	static final String Q_INSERT_FILTER = "INSERT INTO T_FILTER_URL (URL,LINK_URL, REASON) VALUES (\"%s\", \"%s\", \"filter\")";

	static final String Q_INSERT_CONTENTS = "INSERT INTO %s (%s) VALUES (%s)";

	static final String Q_CREATE_CONTENTS_TABLE = "CREATE TABLE IF NOT EXISTS %s ("
			+ "ROW_ID INT(11) NOT NULL AUTO_INCREMENT %s, PRIMARY KEY (ROW_ID %s))";

	Connection mConn;

	public synchronized boolean open() throws Exception {
		Class.forName(DRIVER);
		Connection conn = null;
		try {
			Properties properties = new Properties();
			properties.put("connectTimeout", "300000");
			String dbConnectionString = PATH + "?user=" + USER + "&password=" + PW;
			conn = DriverManager.getConnection(dbConnectionString, properties);
			// conn = DriverManager.getConnection(PATH, USER, PW, properties);
			mConn = conn;
			// 콘텐츠 저장을 위한 DB TABLE 을 준비해라!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			String keys= null;
			String query = null;
			try {
				List<CollectRecode> collects = mConfig.getCollects();
				String tableName, colName, colType;
				for (CollectRecode c : collects) {
					// TABLE NAME
					tableName = c.getName().toUpperCase();
					query = "";
					keys = "";
					for (CollectRecode.Column col : c.getColumns()) {
						colName = col.getDataName().toUpperCase(); // 컬럼 명
						colType = col.getDataType(); // 컬럼 타입
						query += ", " + colName + " " + colType;
						if(col.isKey()){
							keys += ", " + colName;
						}
					}
					if (query.length() > 0) {
						query = String.format(Q_CREATE_CONTENTS_TABLE, tableName, query, keys);
						insert(query);
					}
				}
			} catch (Exception e) {
				mLogger.error("[SQL] " + query, e.fillInStackTrace());
				close();
				return false;
			}
		} catch (Exception e) {
			mLogger.error(e.getMessage());
			return false;
		}
		return true;
	}

//	public synchronized int writeHistory(String url) {
//		try {
//			return insert(String.format(Q_INSERT_HISTORY, url));
//		} catch (SQLNonTransientConnectionException e) {
//			mLogger.error(e.getMessage(), e.fillInStackTrace());
//			return 0;
//		}
//	}
//
//	public synchronized int writeErr(String url, String reason) {
//		try {
//			return insert(String.format(Q_INSERT_ERR, url, reason));
//		} catch (SQLNonTransientConnectionException e) {
//			mLogger.error(e.getMessage(), e.fillInStackTrace());
//			return 0;
//		}
//	}

	private synchronized int insert(String query) throws Exception {
		mConn.prepareStatement(query).execute();
		return 1;
	}

	public synchronized void close() {
		if (mConn != null) {
			try {
				mConn.close();
			} catch (SQLException e) {
				mLogger.error(e.getMessage(), e.fillInStackTrace());
			}
		}
	}

	public void setConfig(Config config) {
		PATH = config.OUTPUT_DB_PATH;
		USER = config.OUTPUT_DB_ID;
		PW = config.OUTPUT_DB_PW;
		mConfig = config;
		String keys= null;
		String query = null;
	}

	public synchronized int write(String... values) throws Exception {
		return 0;
	}

	public synchronized int write(Contents contents) throws Exception {
		String tableName = contents.getName();
		List<KeyValue> data = contents.getData();

		String cols = "";
		String values = "";
		boolean isFisrt = true;
		for (KeyValue kv : data) {
			if (kv != null) {
				if (isFisrt) {
					cols += kv.key();
					values += "\"" + kv.value().replace("\"", "\\\"") + "\"";
					isFisrt = false;
				} else {
					cols += "," + kv.key();
					values += ", \"" + kv.value().replace("\"", "\\\"") + "\"";
				}
				
				if(kv.type().equalsIgnoreCase("file")){
					
				}
			}
		}
		int ret = 0;
		try {
			ret = insert(String.format(Q_INSERT_CONTENTS, tableName, cols, values));
		} catch (Exception e) {
			open();
			ret = insert(String.format(Q_INSERT_CONTENTS, tableName, cols, values));
		}
//		if(ret == -1){
//			mLogger.error(e2.getMessage(), e2.fillInStackTrace());
//		}
		return ret;
	}

	public synchronized int write(List<Contents> aryContents) {
		// TODO Auto-generated method stub
		return 0;
	}
}