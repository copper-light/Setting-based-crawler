package com.onycom.crawler.core;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.jsoup.nodes.Document;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;

import com.onycom.SettingBasedCrawler.Crawler;
import com.onycom.common.CrawlerLog;
import com.onycom.crawler.data.Work;
import com.onycom.crawler.data.WorkResult;
import com.onycom.crawler.parser.Parser;
import com.onycom.crawler.scraper.Scraper;

/**
 * 워크 쓰레드의 Runnable 구현체. 여기서 parser 를 호출하고 parser 의 결과를 모니터링하는 리스너 호출함 <p>
 * 리스너의 결과 정보 값이 부실하므로 앞으로 추가 구현 해야할 것으로 보임. 크롤링 유지보수를 위한 파싱 에러의 세분화 필요
 * */
class WorkRunnable implements Runnable{
	static Logger mLogger = CrawlerLog.GetInstance(WorkRunnable.class);
	
	boolean mIsRunning = false;
	int mId;
	Crawler mCrawler;
	Scraper mScraper;
	Parser mParser;
	WorkDeque mWorkDeque;
	WorkResultQueue mResultQueue;
	Work mUrlInfo;
	
	public WorkRunnable(int id, WorkDeque deque, Parser parser, Work urlInfo){
		mId = id;
		//mCrawler = crawler;
		mUrlInfo = urlInfo;
		mParser = parser;
		mWorkDeque = deque;
	}
	
	public void setWorkResultQueue(WorkResultQueue queue){
		mResultQueue = queue;
	}
	
	public WorkRunnable setWork(Work urlInfo){
		mUrlInfo = urlInfo;
		return this;
	}
	
	public void run() {
		mIsRunning = true;
		boolean isSuccess = false;
		Work info = null;
		Document doc = null;
		List<Work> results = null;
		//mIsRunning = true;
		info = mUrlInfo; // mQueue.pullURL();
		if(info != null){
			try {
				doc = Scraper.GetDocument(info);
				if(doc != null && mParser != null){
					results = mParser.parse(mWorkDeque.getHistory(), info, doc);
				}
				isSuccess = true;
			} catch (JSONException e) { // javascript 반환 파싱 오류
				mLogger.error(e.getMessage(), e.fillInStackTrace());
			} catch (WebDriverException e) { // javascript + element 못찾을때 오류
				if(e.toString().indexOf("Runtime.evaluate") != -1){
					info.result().addError(Work.Error.ERR_ACTION, "javascript syntax err - " + e.getMessage());
					//mLogger.error("javascript syntax err " + e.getMessage(), e.fillInStackTrace());
				}else{
					//info.result().addError(Work.Error.ERR_SCEN_ELEMENT, "");
					mLogger.error("not found element " + e.getMessage(), e.fillInStackTrace());
				}
				
			} catch (KeyManagementException e) {
				mLogger.error(e.getMessage(), e.fillInStackTrace());
			} catch (NoSuchAlgorithmException e) {
				mLogger.error(e.getMessage(), e.fillInStackTrace());
			} catch (IOException e) {
				mLogger.error(e.getMessage(), e.fillInStackTrace());
			}
		}
		// 쓰레드 끝나는걸 확인하는 로직을 재 구성해야합니다. 알겠죠?
		
//		if(isSuccess){
//			mListener.done(info, results);
//		}else{
//			mListener.error(info);
//		}
		
		mResultQueue.offerResult(new WorkResult(info, results));
		mIsRunning = false;
		synchronized (mResultQueue) {
			mResultQueue.resultNotifyAll();
		}
	}
	
	public void stop(){
		//mIsRunning = false;
	}
	
	public boolean isRunning(){
		return mIsRunning;
	}
}
