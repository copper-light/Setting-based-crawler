package com.onycom.crawler.core;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

import org.jsoup.nodes.Document;

import com.onycom.crawler.data.URLInfo;
import com.onycom.crawler.parser.Parser;
import com.onycom.crawler.scraper.Scraper;

/**
 * 워크 쓰레드의 Runnable 구현체. 여기서 parser 를 호출하고 parser 의 결과를 모니터링하는 리스너 호출함 <p>
 * 리스너의 결과 정보 값이 부실하므로 앞으로 추가 구현 해야할 것으로 보임. 크롤링 유지보수를 위한 파싱 에러의 세분화 필요
 * */
class WorkRunnable implements Runnable{
	boolean mIsRunning = false;
	int mId;
	Crawler mCrawler;
	Scraper mScraper;
	Parser mParser;
	WorkQueue mQueue;
	URLInfo mUrlInfo;
	WorkListener<URLInfo> mListener;
	
	public WorkRunnable(int id, WorkQueue queue, Parser parser, URLInfo urlInfo, WorkListener<URLInfo> listener){
		mId = id;
		//mCrawler = crawler;
		mUrlInfo = urlInfo;
		mListener = listener;
		mParser = parser;
		mQueue = queue;
	}
	
	public WorkRunnable setWork(URLInfo urlInfo){
		mUrlInfo = urlInfo;
		return this;
	}
	
	public void run() {
		mIsRunning = true;
		URLInfo info = null;
		Document doc = null;
		List<URLInfo> results = null;
		//mIsRunning = true;
		info = mUrlInfo; // mQueue.pullURL();
		if(mListener.start(info) && info != null){
			try {
				doc = Scraper.GetDocument(info);
				if(doc != null && mParser != null){
					results = mParser.parse(mQueue.getHistory(), info, doc);
				}
			} catch (Exception e){
				e.printStackTrace();
				Crawler.Log('e', e.getMessage(), e.fillInStackTrace());
			}
		}
		// 쓰레드 끝나는걸 확인하는 로직을 재 구성해야합니다. 알겠죠?
		mIsRunning = false;
		if(results != null){
			mListener.done(info, results);
		}else{
			mListener.error(info);
		}
	}
	
	public void stop(){
		//mIsRunning = false;
	}
	
	public boolean isRunning(){
		return mIsRunning;
	}
}
