package com.onycom.crawler.parser;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.onycom.SettingBasedCrawler.Crawler;
import com.onycom.crawler.data.Config;
import com.onycom.crawler.data.Contents;
import com.onycom.crawler.data.Work;

/**
 * 커스텀 파싱 구현체
 * */
public class CustomParser  extends Parser{
	private int MAX_DEPTH = 1; 
	private int CNT = 0;
	
	@Override
	public List<Work> parseURL(Work urlInfo, Document document) {
		List<Work> ret = new ArrayList<Work>();
		int curDepth = urlInfo.getDepth();
		int depth; 
		String parentRoot, parentURL, parentSub;
		Work url;
		String sub_url, tmp;
		int idx;
		
		if(curDepth >= MAX_DEPTH) return ret;
		
		// 페이지에 따라 뎁스를 안내려갈때도 있다는걸 염두 -> 해당 로직 추가 필요 있음
		depth = ++curDepth;
		
		Elements els = document.select("a[href]");
		for(Element e : els){
			parentRoot = urlInfo.getDomainURL();
			parentSub = urlInfo.getSubURL();
			parentURL = urlInfo.getURL();
			//depth = curDepth;
			
			sub_url = e.attr("href");
			//System.out.println("[LINK] "+ sub_url);
			if(sub_url.trim().length() == 0) continue;
			
			
			//e.select("a[ng-if=]")
			
			// 1 순위 : 루트페이지의 하위 URL링크 목록만 가져오기
			// 2 순위 : 무조건 제외할 목록
			// # 제외 - ^(?!#).*$
			// 메일 제외 - ^(?!mailto:).*$
			// // 
			// javscript: 
			// 3 순위 : 허용 목록
			// / 또는 ? 시작하는 것  - ^(/|\\?)(?!(/)).*$
			// http:// 또는 https:// 로 시작하는 것
			// 상대주소 (/없이) 인것들 -> 마지막 / 찾아서 연결해줘야한다.
			if(sub_url.matches("^(http).*$")){
				if(sub_url.indexOf(parentRoot) != -1){
					sub_url = sub_url.substring(parentRoot.length());
				}else{ // 시드 url 과 도메인이 다른 링크는 제외
					continue;
				}
				//System.out.println(">> " + sub_url);
				url = new Work(parentRoot + sub_url).setDepth(depth);
				ret.add(url);
			}
			else if(sub_url.matches("^(?!#).*$")
					&& sub_url.matches("^(?!mailto:).*$")
					&& sub_url.matches("^(?!//).*$")
					&& sub_url.matches("^(?!javascript:).*$")){
				if(sub_url.matches("^(\\?).*$")){
					tmp = parentSub;
					idx = tmp.lastIndexOf("?");
					if(idx != -1){
						sub_url = tmp.substring(0, idx)+sub_url;
					}
				}else if(!sub_url.matches("^(/).*$")){
					tmp = urlInfo.getSubURL();
					idx = tmp.lastIndexOf("/");
					if(idx != -1){
						sub_url = tmp.substring(0, idx+1)+sub_url;
					}
				} 
				url = new Work(parentRoot + sub_url).setDepth(depth);
				ret.add(url);
			}else{
//				try {
//					Crawler.DB.writeFilter(parentURL, new URLInfo(parentRoot + sub_url).getURL());
//				} catch (Exception ex) {
//					ex.printStackTrace();
//				}
			}
		}
		return ret;
	}

	@Override
	public List<Contents> parseContents(Work urlInfo, Document document) {
		int curDepth = urlInfo.getDepth();
		System.out.println("[parse " + ++CNT +"] "+curDepth +"depth - "+urlInfo.getURL()+" - "+ document.title());
		int idx;
		String tmp;
		//FormElement fe = document.getElementById("ee")
		
		
		System.out.println("[CONTENTS] " + document.getElementsByTag("title").text());
		return null;
	}
}
