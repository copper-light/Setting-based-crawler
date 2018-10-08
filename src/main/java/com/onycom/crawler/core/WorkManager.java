package com.onycom.crawler.core;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Document;

import com.onycom.crawler.data.Config;
import com.onycom.crawler.data.Robots;
import com.onycom.crawler.data.Work;
import com.onycom.crawler.parser.Parser;
import com.onycom.crawler.parser.RobotsParser;
import com.onycom.crawler.scraper.Scraper;
import com.onycom.crawler.writer.DBWriter;

/**
 * 쓰레드를 관리하는 매니저. 크롤링의 핵심 로직도 담고 있는데, 이를 분리하는 작업이 필요할 것으로 보임
 * */
public class WorkManager {
	private Config mConfig;
	private Thread[] mWorkThread;
	private WorkRunnable[] mWorkRunnable;
	private Parser mParser;
	private Integer mWorkingCount = 0;
	private int mThreadPoolSize = 1;
	private long mWorkDelay = 5000; // ms
	private WorkManagerListener mManagerListener;
	private WorkQueue mQueue;
	
	private long mSuccessCount = 0;
	private long mFailCount = 0;
	
	public boolean mIsFallowRobots = false;
	
	private WorkListener<Work> mWorkListener = new WorkListener<Work>(){
		
		public boolean start(Work data) {
			data.updateState(Work.STATE_WORKING);
			synchronized (mWorkingCount) { mWorkingCount++; }
			//System.out.println("[new] " + data.getURL());
			return true;
		}

		public void done(Work work, List<Work> result) {
			work.updateState(Work.STATE_SUCCESS);
			mSuccessCount++;
			
			if(result != null){
				for(Work i : result){
					if(isAllowURL(i)){
						if(mQueue.offerURL(i)){
							
						}
					}else{
						
					}
				}
			}
			synchronized (mWorkingCount) {
				mWorkingCount--;
			}
			if(mManagerListener!= null)	mManagerListener.progress(work, mQueue.getSize(), mQueue.getHistorySize());
		
			if(mConfig.CRAWLING_MAX_COUNT != -1){
				if( mConfig.CRAWLING_MAX_COUNT <= (mSuccessCount + mFailCount) ){
					mQueue.setAccessMode(WorkQueue.LOCK);
					mQueue.clear();
				}
			}
			if(mQueue.getSize() == 0 && mWorkingCount == 0){
				if(mManagerListener!= null)	mManagerListener.finish(mQueue.getSize(), mFailCount, mQueue.getHistorySize());
			}
			notifyWorker(work.getURL());
		}

		public void error(Work work) {
			try {
				//Crawler.DB.writeErr(data.getURL(), "err" );
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.err.println("[ERR] error worker ");
			work.updateState(Work.STATE_FAIL);
			mFailCount++;
			synchronized (mWorkingCount) {
				mWorkingCount--;
				//System.out.println("[log] work worker : " + mWorkingCount);
			}
			
			if(mConfig.CRAWLING_MAX_COUNT != -1){
				if( mConfig.CRAWLING_MAX_COUNT <= (mSuccessCount + mFailCount) ){
					mQueue.setAccessMode(WorkQueue.LOCK);
					mQueue.clear();
				}
			}
			
			if(mManagerListener!= null)	mManagerListener.progress(work, mQueue.getSize(), mQueue.getHistorySize());
		
			if(mQueue.getSize() == 0 && mWorkingCount == 0){
				if(mManagerListener!= null)	mManagerListener.finish(mQueue.getSize(), mFailCount, mQueue.getHistorySize());
			}
			notifyWorker(work.getURL());
		}
	};
	
	public WorkManager(){
		this(1);
	}
	
	public WorkManager(int size){
		mThreadPoolSize = size;
		mWorkThread = new Thread[mThreadPoolSize];
		mWorkRunnable = new WorkRunnable[mThreadPoolSize];
		mQueue = new WorkQueue();
	}
	
	public void setManagerListener(WorkManagerListener managerListener){
		mManagerListener = managerListener;
	}
	
	public void setWorkDelay(long delay){
		mWorkDelay = delay;
	}
	
	public WorkManager setParser(Parser parser){
		mParser = parser;
		return this;
	}
	
	public boolean addWork(Work info){
		return mQueue.offerURL(info);
	}
	
	public void start(){
		if(mManagerListener!= null)	mManagerListener.start();
		notifyWorker(null);
	}
	
	public boolean isAllowURL(Work info){
		Robots robots = mConfig.getRobots().get(info.getDomainURL());
		if(!mConfig.IGNORE_ROBOTS && robots!= null){
			if(!mQueue.contains(info)){
				boolean ret = robots.isAllow(Crawler.USER_AGENT_NAME, info.getSubURL());
				
				if(!ret){
					try {
						//mDB.open();
//						mDB.insertErr(info.getURL(), "deny" ).close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				return ret;
			}else{
				try {
					//mDB.connect().insertErr(info.getURL(), "dup" ).close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				return false;
			}
		}else{
			return true;
		}
	}
	
//	private int getIdleWorkerCount(){ 
//		return mThreadPoolSize - mWorkingCount;
//	}
	
	/**
	 * 큐에 작업이 있으면, 쓰레드에게 작업 할당하는 역할 수행
	 * */
	public void notifyWorker(String url){
		int length = mWorkThread.length;
		Work info;
		//System.err.println("start notifyWorker() " + url + " / " + mThreadPoolSize);
		for(int i = 0; i < length ; i ++){
			if(mQueue.getSize() > 0){
				info = mQueue.pullURL();
				if(info != null){
					
					// info 의 root URL 의 robot 파싱이 있는지 확인하고 없으면 파싱 시작
					if(!mConfig.IGNORE_ROBOTS && mConfig.getRobots().get(info.getDomainURL()) == null){
						
						// 현재 작업 중인 쓰레드들은 작업하도록 나두고
						// 새로운 작업을 수행은 일시정지하기 위하여 쓰기전용 모드로 변경
						// q 를 읽었을때 null 이면 자동으로 알아서 쓰레드들이 멈출테니까.
						try {
							mQueue.setAccessMode(WorkQueue.WRITE);
							Work robotsURL = new Work(info.getDomainURL() + Crawler.FILE_NAME_ROBOTS);
							Document doc = Scraper.GetDocument(robotsURL);
							new RobotsParser().parse(null, robotsURL, doc);
						}catch (Exception e) { 
							e.printStackTrace();
						} finally {
							mQueue.setAccessMode(WorkQueue.READ_AND_WRINE);
						}
					}
					synchronized (mWorkingCount){
						if(mWorkDelay > 0){
							try {
								Thread.sleep(mWorkDelay);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						if((mThreadPoolSize - mWorkingCount) > 0){
							if(mWorkRunnable[i] != null){
								if(!mWorkRunnable[i].isRunning()){
									mWorkThread[i] = new Thread(mWorkRunnable[i].setWork(info));
									mWorkThread[i].start();
								}
							}else{
								mWorkRunnable[i] = new WorkRunnable(i, mQueue, mParser, info, mWorkListener);
								mWorkThread[i] = new Thread(mWorkRunnable[i]);
								mWorkThread[i].start();
							}
						}
					}
				}
			}
		}
		//System.err.println("end notifyWorker()");
	}
	
	public int upWorkingThread() {
		return ++mWorkingCount;
	}

	public int downWorkingThread() {
		return --mWorkingCount;
	}

	public void setConfig(Config config) {
		mConfig = config;
		mWorkDelay = mConfig.CRAWLING_DELAY;
	}
}
