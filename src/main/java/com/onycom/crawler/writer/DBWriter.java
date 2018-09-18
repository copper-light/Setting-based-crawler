package com.onycom.crawler.writer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;

import com.onycom.crawler.data.Config;
import com.onycom.crawler.data.Contents;
import com.onycom.crawler.data.KeyValue;
import com.onycom.crawler.data.CollectRecode;

/**
 * DB 저장 구현제. 기본 JDBC 를 활용. 안정성을 위한 구현 고도화 필요 
 * */
public class DBWriter implements Writer{
	static Logger mLogger = Logger.getLogger(DBWriter.class);
	
	static String DRIVER = "org.mariadb.jdbc.Driver";
	static String PATH  = "jdbc:mariadb://localhost:3306/DEV_CRAWLER_LOG"; // 172.17.0.10
	static String USER = "root";
	static String PW = "sairport2018";
	
	static final String Q_INSERT_HISTORY = "INSERT INTO T_HISTORY (URL) VALUES (\"%s\")";
	static final String Q_INSERT_ERR = "INSERT INTO T_ERR_URL (URL,REASON) VALUES (\"%s\", \"%s\")";
	static final String Q_INSERT_FILTER = "INSERT INTO T_FILTER_URL (URL,LINK_URL, REASON) VALUES (\"%s\", \"%s\", \"filter\")";
	
	static final String Q_INSERT_CONTENTS = "INSERT INTO %s (%s) VALUES (%s)";
	
	static final String Q_CREATE_CONTENTS_TABLE = 
					"CREATE TABLE IF NOT EXISTS %s ("
					+ "ROW_ID INT(11) NOT NULL AUTO_INCREMENT,"
					+ "PRIMARY KEY (ROW_ID)"
					+ "%s)";
	
	Connection mConn;
	
	public synchronized void open() throws Exception {
		Class.forName(DRIVER);
		Connection conn = null;
		try{
			conn = DriverManager.getConnection(PATH, USER, PW);
		}catch(Exception e){
			mLogger.error(e.getMessage(), e.fillInStackTrace());
		}
		mConn = conn;
	}
	
	public synchronized int writeHistory(String url){
		return insert(String.format(Q_INSERT_HISTORY, url));
	}
	
	public synchronized int writeErr(String url, String reason){
		return insert(String.format(Q_INSERT_ERR, url, reason));
	}
	
	private synchronized int insert(String query){
		int ret = 0;
		try {
			if(mConn != null){
				if(mConn.prepareStatement(query).execute()) ret = 1;
			}
		} catch (SQLException e) {
			mLogger.error("[SQL] " + query, e.fillInStackTrace());
		}
		return ret;
	}
	
	public synchronized void close(){
		if(mConn != null){
			try {
				mConn.close();
			} catch (SQLException e) {
				mLogger.error(e.getMessage(), e.fillInStackTrace());
			}
		}
	}

	public void setConfig(Config mConfig) {
		PATH = mConfig.OUTPUT_DB_PATH;
		USER = mConfig.OUTPUT_DB_ID; 
		PW = mConfig.OUTPUT_DB_PW;
		
		String query = null;
		// log 저장을 위한 수단 마련
		// DB 로 저장할 것인가?
		// FILE 로 떨굴것인가?
		
		
		// 콘텐츠 저장을 위한 DB TABLE 을 준비해라!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		try {
			this.open();
			List<CollectRecode> collects = mConfig.getCollects();
			String tableName, colName, colType;
			for(CollectRecode c : collects){
				// TABLE NAME
				tableName = c.getName().toUpperCase(); 
				query = "";
				for(CollectRecode.Column col : c.getColumns()){
					colName = col.getDataName().toUpperCase(); // 컬럼 명
					colType = col.getDataType(); // 컬럼 타입
					query += ", " + colName + " " + colType;
				}
				if(query.length() > 0){
					query = String.format(Q_CREATE_CONTENTS_TABLE, tableName, query);
					this.insert(query);
				}
			}
		} catch (Exception e) {
			mLogger.error("[SQL] " + query, e.fillInStackTrace());
		} finally {
			this.close();
		}
	}

	public synchronized int write(String... values) {
		return 0;
	}
	
	public synchronized int write(Contents contents) {
		String tableName = contents.getName();
		List<KeyValue> data = contents.getData();
		
		String cols = "";
		String values = "";
		boolean isFisrt = true;
		for(KeyValue kv : data){
			if(kv != null){
				if(isFisrt){
					cols += kv.key();
					values += "\"" + kv.value().replace("\"", "\\\"") + "\"";
					isFisrt = false;
				}else{
					cols += "," + kv.key();
					values += ", \"" + kv.value().replace("\"", "\\\"") + "\"";
				}
			}
		}
		return insert(String.format(Q_INSERT_CONTENTS, tableName, cols, values));
	}

	public synchronized int write(List<Contents> aryContents) {
		// TODO Auto-generated method stub
		return 0;
	}
}