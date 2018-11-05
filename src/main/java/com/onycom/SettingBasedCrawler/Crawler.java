package com.onycom.SettingBasedCrawler;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.onycom.crawler.scraper.JsoupScraper;
import com.onycom.crawler.scraper.SeleniumScraper;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.onycom.common.CrawlerLog;
import com.onycom.common.Util;
import com.onycom.crawler.core.WorkDeque;
import com.onycom.crawler.core.WorkManager;
import com.onycom.crawler.core.WorkManagerListener;
import com.onycom.crawler.data.Config;
import com.onycom.crawler.data.Work;
import com.onycom.crawler.parser.Parser;
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
	Parser mParser;
	Scraper mScraper;
	
	public static DBWriter DB;
	public static com.onycom.crawler.writer.Writer Writer;

	public static Logger mLogger;
	
	public Config mConfig;
	public String[] mArgs;
	
	public Crawler(int size, int delay){
		mWorkManager = new WorkManager(size);
		mWorkManager.setWorkDelay(delay);
		this.setCrawlerListener(mWMListener);
		
//		String LOG_FILE = "./log/log4j.properties";
//		Properties logProp = new Properties();
//		try {
//			logProp.load(new FileInputStream(LOG_FILE));
//			PropertyConfigurator.configure(logProp);
//			System.out.println("Logging enabled");
//		} catch (IOException e) {
//			System.out.println("Logging not enabled");
//		}
//		mLogger = CrawlerLog.GetInstance(Crawler.class);
	}
	
	public Crawler(){
		this(1, 1);
	}
	
	public void setConfigFile(String filePath, String[] args){
		mArgs = args;
		System.out.println("Open config file - " + filePath);
		String config = (String) Util.GetConfigFile(filePath);
		int len = args.length;
		
		String regexParams = "<%[0-9]+%>";
        Pattern pattern = Pattern.compile(regexParams);
        Matcher matcher = pattern.matcher(config);

        int totalParams = 0;
        String value = "";
        Integer cnt = 0;
        HashMap<String, Integer> paramMap = new HashMap<String, Integer>();
        while (matcher.find()){
        	value = matcher.group();
        	cnt = paramMap.get(value);
        	if(cnt != null){
        		cnt ++;
        	}else{
        		totalParams++;
        		cnt = 1;
        	}
        	paramMap.put(matcher.group(), cnt);
        }
        
        if(totalParams != (len - 2)){
        	System.err.println("[ERROR] Mismatching config parameters.");
        	return;
        }
		
		if(args != null && len >= 3){
			for(int i = 2 ; i < len ; i++){
				config = config.replace("<%"+ (i-2) +"%>", args[i]);
			}
		}
		
		setConfigJson(config);
	}

	public void setConfigJson(String jsonConfig) {
		if(mConfig == null) mConfig = new Config();
		if(!mConfig.setConfig(jsonConfig)){
			System.err.println("[ERROR] Config file parsing failed.");
			return;
		}
		if(mConfig.CRAWLING_TYPE.contentEquals(Config.CRAWLING_TYPE_SCENARIO_STATIC)){
			mScraper = new JsoupScraper();
			mParser = new ScenarioStasticParser();
		}else if(mConfig.CRAWLING_TYPE.contentEquals(Config.CRAWLING_TYPE_SCENARIO_DYNAMIC)){
			mScraper = new SeleniumScraper(mConfig);
			mParser = new ScenarioDynamicParser();
		}else if(mConfig.CRAWLING_TYPE.contentEquals(Config.CRAWLING_TYPE_STATIC)){
			mScraper = new JsoupScraper();
			mParser = new StaticParser();
		}
    	
		if(mParser == null || mScraper == null){
			System.err.println("[ERROR] Not found parser.");
			return;
		}else{
			mWorkManager.setScraper(mScraper).setParser(mParser);
		}
		//mScraper.setConfig(mConfig);
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
		Work seed = mConfig.getSeedInfo();
		if(seed != null){
			seedUrl(seed);
		}
	}
	
	public Crawler seedUrl(Work info) {
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
		Work info = new Work(url);
		return seedUrl(info); 
	}

	public void start() {
		if(mParser == null){
			System.err.println("[ERROR] Can't start. Config err.");
			return;
		}
		
		CrawlerLog.SetName(mConfig.CRAWLING_NAME_AND_TIME);
		mLogger = CrawlerLog.GetInstance(Crawler.class);
		mWorkManager.start();
	} 
	
	public void setCrawlerListener(WorkManagerListener listener){
		mWorkManager.setManagerListener(listener);
	}
	
	long mTotalSaveCnt = 0;
	long mErrCnt = 0;
	long mProcessedCount = 0;
	static Logger mErrorLogger = null;
	WorkManagerListener mWMListener = new WorkManagerListener() {
		
		public boolean start() {
			boolean ret = false;
			mLogger.info("============== Start Crawler =============");
			mLogger.info("CONFIG FILE    : " + mArgs[1]);
			for(int i = 2 ; i < mArgs.length ; i++){
				mLogger.info(String.format("CMD PARAM %02d   : %s", (i-2), mArgs[i]));
			}
			mLogger.info("CRAWLER NAME   : " + mConfig.CRAWLING_NAME);
			mLogger.info("CRAWLER TYPE   : " + mConfig.CRAWLING_TYPE);
			mLogger.info("CRAWLER DELAY  : " + mConfig.CRAWLING_DELAY + " sec");
			mLogger.info("IGNORE ROBOTS  : " + mConfig.IGNORE_ROBOTS);
			mLogger.info("LIMIT_COUNT    : " + mConfig.CRAWLING_MAX_COUNT);
			mLogger.info("FILTER COUNT   : " + (mConfig.getFilterAllow().size() 
								     		 + mConfig.getFilterDisallow().size() 
								             + mConfig.getFilterDuplicate().size() 
								             + mConfig.getLeafURL().size()));
			mLogger.info("SCENARIO COUNT : " + mConfig.getScenarios().size());
			mLogger.info("COLLECT COUNT  : " + mConfig.getCollects().size());
			mLogger.info("SAVE TYPE      : " + mConfig.OUTPUT_SAVE_TYPE);
			mLogger.info("SAVE HTML      : " + mConfig.SAVE_HTML);
			mLogger.info("==========================================");
			try {
				ret = Writer.open();
				if(ret){
					ret = mScraper.open();
				}
			} catch (Exception e) {
				ret = false;
				mLogger.error(e.getMessage(), e);
			}

			if(!ret){
				mLogger.info("============== Terminate Crawler =============");
				mLogger.error("Can't start Crawler. Initialization failed.");
			}
			return ret;
		}
		
		public void progress(Work work, WorkDeque workDeque) {
			mProcessedCount++;
			mTotalSaveCnt += work.result().getSaveCount();
			if(work.result().getErrorList().size() > 0){
				mLogger.error("[ERR URL] " + work.getURL());
				mErrCnt += work.result().getErrorList().size();
				for(Work.Error err : work.result().getErrorList()){
					mLogger.error("L " + err.toStringTypeAndMsg());
				}
			}
			
			mLogger.info(String.format("[Progress %d] save : %d, err : %d, remain_work : %d, total : %d", 
												mProcessedCount,
												mTotalSaveCnt,
												mErrCnt,
												workDeque.getSize(),
												workDeque.getHistorySize()));
			/**
			 * 크롤링 횟수 제한 설정이 있다면, 작업 날리기
			 * */
			if(mConfig.CRAWLING_MAX_COUNT != -1 && (mTotalSaveCnt + mErrCnt) >= mConfig.CRAWLING_MAX_COUNT){ 
				workDeque.clear();
			}
		}

		public void finish(WorkDeque workDeque) {
			long e = System.currentTimeMillis() - mConfig.getStartTime() ;
			if(Writer != null){
				try {
					Writer.close();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
			mScraper.close();
			
			e = e / 1000;
			int h=0,m=0,s=0;
			if(e > 0){
				h = (int) e / (3600);
				m = (int) e % 3600;
				m = (m > 0)? m/60 : 0;
				s = (int) e % 60;
			}
			
			mLogger.info("============== Finish Crawler =============");
			mLogger.info("[total time] "+ String.format("%02d:%02d:%02d", h,m, s));
			mLogger.info("[save contents] "+ mTotalSaveCnt);
			mLogger.info("[error work] "+ mErrCnt);
			mLogger.info("[remain work] "+ workDeque.getSize());
			mLogger.info("[total processed work] " + mProcessedCount);
		}
		
		public void error() {
			
		}

	};
}
