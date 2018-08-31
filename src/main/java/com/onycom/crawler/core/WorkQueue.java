package com.onycom.crawler.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import com.onycom.crawler.data.URLInfo;
import com.onycom.crawler.writer.DBWriter;

/**
 * 크롤러가 작업할 페이지들의 목록을 관리하는 큐. 멀티쓰레드 작업시 반드시 동기화 필요
 */
public class WorkQueue {

	public static final byte LOCK = 0x0; // 0b0000 읽기 쓰기 불가
	public static final byte WRITE = 0x1; // 0b0001 쓰기만 가능
	public static final byte READ = 0x2; // 0b0010 읽기만 가능
	public static final byte READ_AND_WRINE = 0x3; // 읽기 쓰기 가능

	private byte ACCESS_MODE = READ_AND_WRINE; // 기본은 읽기 쓰기 모드

	/**
	 * 작업 해야할 페이지 목록
	 */
	private Queue<URLInfo> mQueue;

	/**
	 * 방문한 페이지 목록 중복 페이지 방지
	 */
	Set<URLInfo> mHistory;

	
	// test 용 DB 연결
	DBWriter mDB;
	
	
	public WorkQueue() {
		mQueue = new LinkedList<URLInfo>();
		mHistory = new HashSet<URLInfo>();
		mDB = new DBWriter();
	}

	public synchronized void setAccessMode(byte mode) {
		ACCESS_MODE = mode;
	}

	public synchronized int getAccessMode() {
		return ACCESS_MODE;
	}

	public boolean contains(URLInfo urlInfo){
		if ((ACCESS_MODE & WRITE) == WRITE) {
			return mHistory.contains(urlInfo);
		}else{
			return false;
		}
	}
	
	public synchronized boolean offerURL(URLInfo urlInfo) {
		if ((ACCESS_MODE & WRITE) == WRITE) {
			// 중복된 페이지 접근은 하지 않음
			mQueue.offer(urlInfo);
			return true;
//			if (mHistory.contains(urlInfo) == true) {
//				return false;
//			} else {
//				mHistory.add(urlInfo);
//				mQueue.offer(urlInfo);
////				
////				try {
////					mDB.connect().insertHistory(urlInfo.getURL()).close();
////				} catch (Exception e) {
////					e.printStackTrace();
////				} 
//				return true;
//			}
		} else {
			return false;
		}
	}

	public synchronized URLInfo pullURL() {
		if ((ACCESS_MODE & READ) == READ)
			return mQueue.poll();
		else
			return null;

	}

	public synchronized boolean isEmpty() {
		return mQueue.isEmpty();
	}

	public synchronized long getSize() {
		return mQueue.size();
	}

	public long getHistorySize() {
		return mHistory.size();
	}

	public URLInfo[] getHistory(){
		return mHistory.toArray(new URLInfo[mHistory.size()]);
	}
	
	public void clear(){
		mQueue.clear();
	}
}
