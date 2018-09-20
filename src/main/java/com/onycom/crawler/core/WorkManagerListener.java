package com.onycom.crawler.core;

import java.util.List;

import com.onycom.crawler.data.Work;

/**
 * 쓰레드를 관리하는 크롤러 총괄 매니저의 상황을 모니터링 하기위한 리스너 인터페이스. 
 * */
public interface WorkManagerListener {
	public void start();
	
	public void progress(Work work, long q_size, long history_size);

	public void finish(long suc, long fail, long total);
	
	public void error();
}
