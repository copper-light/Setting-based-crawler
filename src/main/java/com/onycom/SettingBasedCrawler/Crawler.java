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
	public String[] mCrawlingArgs;
	public String mConfigPath;
	
	public static String BASE_PATH;
	
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

	public boolean setConfig(String filePath, String[] argsMeta, String[] argsCrawling) {
		if(mConfig == null) {
			mConfig = new Config();
			if(!mConfig.setConfig(filePath, argsMeta, argsCrawling)){
				System.err.println("[ERROR] Config file parsing failed.");
				return false;
			}
		}else{
			if(!mConfig.updateNext()){
				return false;
			}
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
			return false;
		}else{
			mWorkManager.setScraper(mScraper).setParser(mParser);
		}
		//mScraper.setConfig(mConfig);
		mParser.setConfig(mConfig);
		mWorkManager.setConfig(mConfig);
		if(mConfig.OUTPUT_SAVE_TYPE.contentEquals(Config.SAVE_TYPE_DB)){
			if(DB == null){
				Writer = DB = new DBWriter();
			}
		}else{
			if(DB == null) DB = new DBWriter();
			if(Writer == null) Writer = new CsvWriter();
		}
//		if(DB == null) DB = new DBWriter();
//		DB.setConfig(mConfig);
		Writer.setConfig(mConfig);
		Work seed = mConfig.getSeedInfo();
		if(seed != null){
			seedUrl(seed);
		}
		return true;
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
		Work info = new Work(url, mConfig.CHARACTER_SET);
		return seedUrl(info); 
	}

	public void start() {
		if(mParser == null){
			System.err.println("[ERROR] Can't start. Config err.");
			return;
		}
		
		CrawlerLog.SetName(mConfig.CRAWLING_NAME);
		if(mLogger == null){
			mLogger = CrawlerLog.GetInstance(Crawler.class);
		}
		do {
			mWorkManager.start();
		}while(setConfig(null,null,null));
		if(Writer != null){
			//Writer.
			if(Writer.getClass() == DBWriter.class){
				String[] query = mConfig.getPostProcessingQuery();
				for(int i = 0 ; i < query.length ; i++){
					DBWriter dbw = (DBWriter) Writer;
					mLogger.info("============== Post processing =============");
					try {
						dbw.insert(query[i]);
						mLogger.info("processing query "+ i+ " :" + query[i]);
					} catch (Exception e) {
						//e.printStackTrace();
						mLogger.error("err query : " + e.getMessage());
					}
				}
			}
			try {
				Writer.close();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		if(mProcessedCount > 0){
			mLogger.info("============== Finish Crawling =============");
			mLogger.info("[total time] "+ Util.GetElapedTime(mConfig.getStartTime()));
			mLogger.info("[save contents] "+ mTotalSaveCnt);
			mLogger.info("[error work] "+ mErrCnt);
			mLogger.info("[remain work] "+ mRemainWork);
			mLogger.info("[total processed work] " + mProcessedCount);
		}
		if(mScraper != null) mScraper.close();
	} 
	
	public void setCrawlerListener(WorkManagerListener listener){
		mWorkManager.setManagerListener(listener);
	}
	
	long mTotalSaveCnt = 0;
	long mErrCnt = 0;
	long mProcessedCount = 0;
	long mRemainWork = 0;
	long mWorkSaveCnt = 0;
	long mWorkErrCnt = 0;
	static Logger mErrorLogger = null;
	WorkManagerListener mWMListener = new WorkManagerListener() {
		
		public boolean start() {
			boolean ret = false;
			String[] args = mConfig.getCurArguments();
			mWorkSaveCnt = 0;
			mWorkErrCnt = 0;
			if(mProcessedCount == 0){
				mLogger.info("============== Start Crawler =============");
				mLogger.info("CONFIG FILE    : " + mConfig.getConfigFileName());
				if(mConfig.GET_ARGUMENTS_LIST != null && mConfig.GET_ARGUMENTS_LIST.length > 0){
					mLogger.info("ARG LIST COUNT : " + mConfig.GET_ARGUMENTS_LIST.length);
				}
				if(args != null){
					//String[] args = mConfig.GET_ARGUMENTS_LIST[mCo];
					for(int i = 0 ; i < args.length ; i++){
						mLogger.info(String.format("CMD PARAM %02d   : %s", (i), args[i]));
					}
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
			}else{
				if(args != null){
					mLogger.info("============== update params =============");
					//String[] args = mConfig.GET_ARGUMENTS_LIST[mCo];
					for(int i = 0 ; i < args.length ; i++){
						mLogger.info(String.format("CMD PARAM %02d   : %s", (i), args[i]));
					}
					mLogger.info("==========================================");
				}
			}
			
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
			mWorkSaveCnt += work.result().getSaveCount();
			
			if(work.result().getErrorList().size() > 0){
				mLogger.error("[ERR URL] " + work.getURL());
				mErrCnt += work.result().getErrorList().size();
				mWorkErrCnt += work.result().getErrorList().size();
				for(Work.Error err : work.result().getErrorList()){
					mLogger.error("L " + err.toStringTypeAndMsg());
					//mLogger.error("L " + err.);
				}
			}
			
			mLogger.info(String.format("[Progress %d] elaped time : %s, curSave : %d, Totalsave : %d, curErr : %d, totalErr : %d, remain_work : %d, total : %d",
												mProcessedCount,
												Util.GetElapedTime(mConfig.getStartTime()),
												mWorkSaveCnt,
												mTotalSaveCnt,
												mWorkErrCnt,
												mErrCnt,
												workDeque.getSize(),
												workDeque.getHistorySize()));
			
			System.out.println();
			//mLogger.info(String.format());
			/**
			 * 크롤링 횟수 제한 설정이 있다면, 작업 날리기
			 * */
			if(mConfig.CRAWLING_MAX_COUNT != -1 && (mWorkSaveCnt + mWorkErrCnt) >= mConfig.CRAWLING_MAX_COUNT){ 
				workDeque.clear();
			}
		}

		public void finish(WorkDeque workDeque) {
			mRemainWork += workDeque.getSize();
			mScraper.clear();
		}
		
		public void error() {
			
		}

	};
}
