package com.onycom.crawler.parser;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebElement;

import com.onycom.common.Util;
import com.onycom.crawler.core.Crawler;
import com.onycom.crawler.data.Action;
import com.onycom.crawler.data.Config;
import com.onycom.crawler.data.Contents;
import com.onycom.crawler.data.Scenario;
import com.onycom.crawler.data.URLInfo;
import com.onycom.crawler.scraper.Scraper;

public class ScenarioDynamicParser extends Parser {
	public static final Logger mLogger = LogManager.getLogger(ScenarioDynamicParser.class);
	static {
		mLogger.setLevel(Level.ALL);
		
	}
	
	int parseCount;
	
	public ScenarioDynamicParser() {
		parseCount = 0;
	}
	
	int call_stack = 0;
	
	@Override
	public List<URLInfo> parse(URLInfo[] history, URLInfo urlInfo, Document document) {
		List<URLInfo> ret = new ArrayList<URLInfo>();
		/*
		 * 재귀 호출이다. 제대로 관리해야함
		 * Q 를 사용하지 않음
		 * event listener 호출에 의한 진행 상황 파악 기능 추가 필요
		 * */
		call_stack++;
//		if(mLogger.isDebugEnabled()){
//			mLogger.debug("Printing stack trace:");
//			StackTraceElement[] elements = Thread.currentThread().getStackTrace();
//			for (int i = 1; i < elements.length; i++) {
//			     StackTraceElement s = elements[i];
//			     mLogger.debug("\tat " + s.getClassName() + "." + s.getMethodName() + "(" + s.getFileName() + ":" + s.getLineNumber() + ")");
//			}
//			mLogger.debug(call_stack);
//		}
//		try {
//			document = Scraper.GetDocument(urlInfo); /* 스크래퍼가 여기 있는게 가장 마음에 안듬 */
//		} catch (Exception e) {
//			e.printStackTrace();
//			mLogger.error(e.getMessage(), e.fillInStackTrace());
//			// 스크래퍼 오류
//		}
		if(document != null){
			try{
				mLogger.debug("[cur action] "+ urlInfo.getAction().getType());
			}catch(Exception e) {}
			if(urlInfo.getAction() != null && urlInfo.getAction().getType().equalsIgnoreCase(Action.TYPE_PARSE_CONTENTS)){
				List<Contents> contents = this.parseContents(urlInfo, document);
				int saveCount = 0;
				if(contents != null){
					saveCount = this.saveContents(urlInfo, contents);
				}
				parseCount++;
				mLogger.info("[parse count] " + parseCount + " @ " + urlInfo.getURL());
				mLogger.info("[save count] " + saveCount + " @ " + urlInfo.getURL());
			}
			
			List<URLInfo> list = parseURL(urlInfo, document);
//			for(URLInfo info : list){
//				try {
//					parse(null, info, null); // 재귀 호출
//				} catch (Exception e) {
//					e.printStackTrace();
//					mLogger.info(e.getMessage(), e.fillInStackTrace());
//				}
//			}
			for(int i = list.size()-1 ; 0 <= i ; i--){
				ret.add(list.get(i));
			}
			mLogger.debug("[list count ] " + list.size());
		}else{
			// 파싱 또는 로드 오류
			mLogger.error("[load fail docment] " + urlInfo.getLoadCheckSelectors());
		}
		call_stack --;
		
		return ret;
	}

	/**
	 * parseAction
	 * */
	@Override
	public List<URLInfo> parseURL(URLInfo urlInfo, Document document) {
		List<URLInfo> ret = new ArrayList<URLInfo>();
		Elements els;
		Scenario scen, newScen;
		Action action;
		String href, url, domain_url, sub_url, target, type, value, selector;
		String[] tmp;
		List<String> aryCheckSelector;
		URLInfo newUrlInfo;
		boolean allow = false;
		if(super.ifLeaf(urlInfo)){
			return ret;
		}
		
		int curDepth = urlInfo.getDepth();
		
		Map<Integer, Scenario> scenarios = getConfig().getScenarios();
		mLogger.debug("[depth]" + curDepth +"/"+scenarios.size());
		if(scenarios != null){
			int depth;
			int len = scenarios.size();
			if(len != 0 && len >= curDepth){
				scen = scenarios.get(curDepth);
				if(scen == null) return ret;
				len = scen.getSize();
				for(int i = 0 ; i < len ; i ++){
					action = scen.getAction(i);
					target = action.getTarget();
					type = action.getType();
					value = action.getValue();
					depth = action.getDepth();
					mLogger.debug("-----> " + type +" @ " + action.getSelector());
					//aryCheckSelector
//					System.out.println("document size " + document.toString().length());
//					try {
//						PrintWriter out = new PrintWriter("html_"+ new Date().getTime()+".html");
//						out.println(curDepth);
//						out.println(selector.getSelector());
//						out.println(document.toString());
//						out.flush();
//						out.close();
//					} catch (FileNotFoundException e1) {
//						e1.printStackTrace();
//					}
					
					selector = action.getSelector();
					if(selector != null){
						if(!selector.equalsIgnoreCase("html") && !type.equalsIgnoreCase(Action.TYPE_PARSE_CONTENTS)){
							els = document.select(action.getSelector());
							for(Element e : els){
//								href = e.attr("href").trim();
//								//if(href.length() == 0) continue;
//								tmp = Util.SplitDomainAndSubURL(urlInfo, href);
//								domain_url = tmp[0];
//								sub_url = tmp[1];
//								url = domain_url + sub_url;	
//								allow = super.isAllow(urlInfo, domain_url, sub_url);
//								if(allow){ }
								newUrlInfo = new URLInfo(urlInfo.getURL()).setDepth(depth);
								newUrlInfo.setHighPriority(true);
								newUrlInfo.setAction(new Action(depth, e.cssSelector(), target, type, value));
								mLogger.debug("-- find selector : " + e.cssSelector());
								newScen = scenarios.get(depth);
								if(newScen != null){
									newUrlInfo.setLoadCheckSelectors(newScen.getLoadCheckSelector());
								}
								ret.add(newUrlInfo);
							}
						}else{
							newUrlInfo = new URLInfo(urlInfo.getURL()).setDepth(depth);
							newUrlInfo.setHighPriority(true);
							newUrlInfo.setAction(new Action(depth, selector, target, type, value));
							newScen = scenarios.get(depth);
							if(newScen != null){
								newUrlInfo.setLoadCheckSelectors(newScen.getLoadCheckSelector());
							}
							ret.add(newUrlInfo);
						}
					}
				}
			}
		}
		
		return ret;
	}
}
