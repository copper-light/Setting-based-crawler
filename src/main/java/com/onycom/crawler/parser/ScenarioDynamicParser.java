package com.onycom.crawler.parser;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebElement;

import com.onycom.common.Util;
import com.onycom.crawler.data.Action;
import com.onycom.crawler.data.Config;
import com.onycom.crawler.data.Contents;
import com.onycom.crawler.data.Scenario;
import com.onycom.crawler.data.URLInfo;
import com.onycom.crawler.scraper.Scraper;

public class ScenarioDynamicParser extends Parser {
	
	@Override
	public List<URLInfo> parse(URLInfo[] history, URLInfo urlInfo, Document document) {
		/*
		 * 재귀 호출이다. 제대로 관리해야함
		 * Q 를 사용하지 않음
		 * event listener 호출에 의한 진행 상황 파악 기능 추가 필요
		 * */
		
		List<Contents> contents = this.parseContents(urlInfo, document);
		int saveCount = 0;
		if(contents != null){
			saveCount = this.saveContents(urlInfo, contents);
		}
		
		
		
		List<URLInfo> list = parseURL(urlInfo, document);
		System.out.println("Find action : " + list.size());
		Document doc;
		for(URLInfo info : list){
			try {
				doc = Scraper.GetDocument(info); /* 스크래퍼가 여기 있는게 가장 마음에 안듬 */
				if(doc != null){
					this.parse(null, info, doc);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		Scraper.closeDocument(urlInfo);
		return null;
	}

	@Override
	public List<URLInfo> parseURL(URLInfo urlInfo, Document document) {
		List<URLInfo> ret = new ArrayList<URLInfo>();
		Elements els;
		Scenario scen, newScen;
		Action action;
		String href, url, domain_url, sub_url, target, type;
		String[] tmp;
		List<String> aryCheckSelector;
		URLInfo newUrlInfo;
		boolean allow = false;
		if(super.ifLeaf(urlInfo)){
			return ret;
		}
		
		int curDepth = urlInfo.getDepth();
		Map<Integer, Scenario> scenarios = getConfig().getScenarios();
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
					depth = action.getDepth();
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
					els = document.select(action.getSelector());
					for(Element e : els){
						href = e.attr("href").trim();
						if(href.length() == 0) continue;
						tmp = Util.SplitDomainAndSubURL(urlInfo, href);
						domain_url = tmp[0];
						sub_url = tmp[1];
						url = domain_url + sub_url;
						allow = super.isAllow(urlInfo, domain_url, sub_url);
						if(allow){
							newUrlInfo = new URLInfo(urlInfo.getURL()).setDepth(depth);
							newUrlInfo.setAction(new Action(depth, e.cssSelector(), target, type));
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
