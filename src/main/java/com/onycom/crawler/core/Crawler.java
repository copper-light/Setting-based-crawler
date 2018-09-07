package com.onycom.crawler.core;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.select.Elements;

import com.onycom.common.Util;
import com.onycom.crawler.data.Config;
import com.onycom.crawler.data.Robots;
import com.onycom.crawler.data.URLInfo;
import com.onycom.crawler.parser.Parser;
import com.onycom.crawler.parser.RobotsParser;
import com.onycom.crawler.parser.ScenarioDynamicParser;
import com.onycom.crawler.parser.ScenarioStasticParser;
import com.onycom.crawler.parser.StaticParser;
import com.onycom.crawler.scraper.Scraper;
import com.onycom.crawler.writer.CsvWriter;
import com.onycom.crawler.writer.DBWriter;

/**
 * 크롤러의 메인 클래스.
 * */
public class Crawler {
	public static final int STATE_IDLE = 0x0; // 대기중
	public static final int STATE_RUNNING = 0x1; // 동작중
	
	public static final String FILE_NAME_ROBOTS = "/robots.txt";
	public static final String USER_AGENT_NAME = "IM_STUDENT_TEST_FOR_A_STUDY";

	private WorkManager mWorkManager;
	
	static int cnt = 0;
	static long startTime;
	Parser mParser;
	
	public static DBWriter DB;
	public static com.onycom.crawler.writer.Writer Writer;

	public Config mConfig;
	
	public Crawler(int size, long delay, Parser parser){
		mWorkManager = new WorkManager(size);
		mWorkManager.setWorkDelay(delay);
		this.setCrawlerListener(mWMListener);
		parser = mParser;
	}
	
	public Crawler(int size, long delay){
		this(1, 1, null);
	}
	
	public Crawler(Parser parser){
		this(1, 1, parser);
	}
	
	public Crawler(){
		this(1, 1, null);
	}
	
	public void setConfigFile(String filePath){
		String config = (String) Util.GetConfigFile(filePath);
		setConfigJson(config);
	}

	public void setConfigJson(String jsonConfig) {
		if(mConfig == null) mConfig = new Config();
		mConfig.setConfig(jsonConfig);
		
		if(mConfig.CRAWLING_TYPE.contentEquals(Config.CRAWLING_TYPE_SCENARIO_STATIC)){
			setParser(new ScenarioStasticParser());
		}else if(mConfig.CRAWLING_TYPE.contentEquals(Config.CRAWLING_TYPE_SCENARIO_DYNAMIC)){
			setParser(new ScenarioDynamicParser());
		}else if(mConfig.CRAWLING_TYPE.contentEquals(Config.CRAWLING_TYPE_STATIC)){ 
			setParser(new StaticParser());
		}
    	
		if(mParser == null){ 
			System.out.println("[ERROR] Not found parser.");
			return;
		}
		Scraper.SetConfig(mConfig);
		mParser.setConfig(mConfig);
		mWorkManager.setConfig(mConfig);
		if(mConfig.OUTPUT_SAVE_TYPE.contentEquals(Config.SAVE_TYPE_DB)){
			DB = new DBWriter();
			Writer = DB;
		}else{
			DB = new DBWriter();
			Writer = new CsvWriter();
		}
		Writer.setConfig(mConfig);
		
//			if(DB == null) DB = new DBWriter();
//			DB.setConfig(mConfig);
		URLInfo seed = mConfig.getSeedInfo();
		if(seed != null){
			seedUrl(seed);
		}
	}
	
	private Crawler setParser(Parser parser){
		mParser = parser;
		mWorkManager.setParser(parser);
		return this;
	}
	
	public Crawler seedUrl(URLInfo info) {
		if(info == null) return this;
		if(info.getDomainURL() == null) return this;
		
//		String robots_url = info.getDomainURL() + FILE_NAME_ROBOTS;
//		
//		try {
//			URLInfo robots = new URLInfo(robots_url);
//			Document doc = Scraper.GetHttp(robots);
//			new RobotsParser().parse(robots, doc);
//		} catch (KeyManagementException e) {
//			e.printStackTrace();
//		} catch (NoSuchAlgorithmException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		
		if(mWorkManager.addWork(info)){ // 시드 URL을 주면 robots.txt 파일을 열어서 크롤링 권한을 확인
			//notifyWorker();
//			info.getDomainURL()+FILE_NAME_ROBOTS;
//			RobotsParser
		}
		return this; 
	}
	
	public Crawler seedUrl(String url) {
		if(url == null) return this;
		URLInfo info = new URLInfo(url);
		return seedUrl(info); 
	}

	public void start() {
		if(mParser == null){
			System.out.println("[ERROR] Can't start. Config ERR");
			return;
		}
		mWorkManager.start();
	} 
	
	public void setCrawlerListener(WorkManagerListener listener){
		mWorkManager.setManagerListener(listener);
	}

	
	
	WorkManagerListener mWMListener = new WorkManagerListener() {
		
		public void start() {
			System.out.println("============== Start Crawler =============");
			try {
				Writer.open();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Scraper.open();
			startTime = new Date().getTime();
		}
		
		public void progress(long remain, long cnt, long fail, long total) {
			System.out.println("[progress] "+ remain + " remain / " +  cnt + " cnt / " + fail + " fail / " + total + " total");
		}

		public void finish(long suc, long fail, long total) {
			long e = (new Date().getTime()) - startTime ;
			if(Writer != null){
				try {
					Writer.close();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
			Scraper.close();
			
			System.out.println("============== Finish Crawler =============");
			System.out.println("[finish "+ Math.floor(e / 1000) +"s]");
			System.out.println("[finish "+ suc +" suc]");
			System.out.println("[finish "+ fail +" fail]");
			System.out.println("[finish "+ total +" total]");
		}
		
		public void error() {
			
		}

	};
	

	
}
