package com.onycom.crawler.parser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.onycom.common.Util;
import com.onycom.crawler.core.Crawler;
import com.onycom.crawler.core.WorkQueue;
import com.onycom.crawler.data.Collect;
import com.onycom.crawler.data.Config;
import com.onycom.crawler.data.Contents;
import com.onycom.crawler.data.URLInfo;

/** 
 * 웹페이지 파싱 인터페이스를 위한 추상 클래스 <p>
 * <b>T</b> : 스크랩해야할 사이트 객체 <p>
 * <b>C</b> : 저장할 콘텐츠 객체
 * */
public abstract class Parser {
	Config mConfig;
	
	/**
	 * http 로 시작하는 패턴. 
	 * (세팅파일에 정규식을 넣기 때문에, 정확한 연산을 위해 정규식을 제외하는 로직 필요하지 않을까 생각됨)
	 * */
	public static final String REGEX_START_HTTP = "^.*(http).*$";
		
	/**
	 * URL을 수집하는 메서드. 설정된 정규식에 의해 불필요한 URL들을 필터링하는 로직 구현<p>
	 * 
	 * @param urlInfo 처리할 웹페이지의 URL 정보
	 * @param document 처리할 HTML 문서 
	 * @return 중복이 제거된 URL 목록
	 * */
	public abstract List<URLInfo> parseURL(URLInfo urlInfo, Document dom);
	
	/**
	 * 설정 적용<p>
	 * 
	 * @param config 설정 정보를 담고 있는 객체
	 */
	public void setConfig(Config config){
		mConfig = config;
	}
	
	public Config getConfig() {
		return mConfig;
	}
	
	/** 
	 * 기본 파싱 절차 
	 * 콘텐츠 파싱 -> 콘텐츠 저장 -> URL 파싱 -> URL 중복처리 <p>
	 * 
	 * @param history 현재까지 수집된 모든 URL 목록 (중복된 URL에 접근하지 않기 위해서 비교하기위해 가져옴)
	 * @param urlInfo 처리할 웹페이지의 URL 정보
	 * @param document 처리할 html 페이지
	 * @return 스크랩할 URL 목록. Q 에 전달됨
	 */
	public List<URLInfo> parse(URLInfo[] history, URLInfo urlInfo, Document document){
		List<URLInfo> urls = null;
		if(document != null){
			Date startTime = new Date();
			List<Contents> contents = parseContents(urlInfo, document);
			//System.out.println("[parseContents expire : ] " + Util.CalcExpiredTime(startTime));
			
			startTime = new Date();
			saveContents(urlInfo , contents);
			//System.out.println("[saveContents expire : ] " + Util.CalcExpiredTime(startTime));
			
			startTime = new Date();
			urls = parseURL(urlInfo, document);
			//System.out.println("[parseURL expire : ] " + Util.CalcExpiredTime(startTime));
			
			startTime = new Date();
			urls = checkDupliate(history, urls);
			System.out.println("[checkDupliate expire : ] " + Util.CalcExpiredTime(startTime));
		}
		return urls;
	}
	
	/**
	 * 콘텐츠를 수집하는 메서드. 설정된 CSS 패턴에 의해서 데이터를 찾고 데이터 객체에 저장하는 로직 구현<p>
	 * 
	 * 콘텐츠를 파싱한 실패 시 NULL 을 반환하지만 이를 처리하는 로직 없음
	 * 해당 로직은 이 메서드를 호출하는 parser 에서 처리해야 할 것
	 * 
	 * @param urlInfo 처리할 웹페이지의 URL 정보
	 * @param document 처리할 HTML 문서 
	 * @return 콘텐츠 데이터 객체 목록
	 * */
	
	/**
	 * 콘텐츠가 동일한 웹페이지들을 필터링 하기 위한 메서드. URL이 동일하지 않더라도 콘텐츠는 동일한 경우가 존재하며, 
	 * 그 외의 다양한 케이스들을 고려한 구현 필요<p>
	 * 가장 로드가 많이 걸리는 메서드기도 함. 크롤링된 웹페이지가 1,000건 이상일 때부터 평균 처리속도가 1초를 넘어가기 시작하며
	 * 최적화가 필요해 보임<p>
	 * 
	 * @param history 현재까지 수집된 URL 목록. Q 에서 전달받음.
	 * @param newList 새로 수집된 URL 목록. 
	 * @return 중복이 제거된 URL 목록
	 */
	public List<URLInfo> checkDupliate(URLInfo[] history, List<URLInfo> newList){
		return newList;
	}
	
	public List<Contents> parseContents(URLInfo urlInfo, Document document){
		String currentURL = urlInfo.getURL();
		String currentDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		String txt, type, attr_name, tag_type, data_type, data_name, value = null;
		String[] regexs;
		boolean isAllow = false;
		Elements els;
		Element e;
		Contents contents = null;
		Collect.Item item;
		Contents[] aryContents = null;
		ArrayList<Contents> ret = null;
		List<Collect> aryCollects = mConfig.getCollects();
		
		int len_i, len_j;
		for(Collect collect : aryCollects){
			System.out.println("title : " + document.title() + " " + urlInfo.getURL());
			aryContents = null;
			contents = null;
			if(collect.getDepth() == urlInfo.getDepth() || urlInfo.getURL().matches(collect.getUrl())){
				len_i = collect.getItems().size();
				// 웹페이지의 contents 파싱 루프
				// 데이터 배열을 먼저 만듬
				for(int i = 0 ; i < len_i ; i++){ // item loof
					item = collect.getItem(i);
					type = item.getType();
					data_type = item.getDataType();
					data_name = item.getDataName();
					attr_name = item.getAttrName();
					tag_type = item.getTagType();
					regexs = item.getAllowRegex();
					
					if(type == "" || type == null || type.contentEquals(Config.COLLECT_ITEMTYPE_TAG)){
						els = document.select(item.getSelector());
						value = null;
						if(els != null && els.size() > 0){
							len_j = els.size();
							
							if (aryContents ==  null){
								aryContents = new Contents[len_j];
								for(int j = 0 ; j < len_j ; j ++){
									aryContents[j] = new Contents(collect.getName(), len_i);
								}
							}
							
							// 추출할 데이터 타입 별 처리
							for(int j = 0 ; j < len_j ; j++){
								e = els.get(j);
								contents = aryContents[j];
								if(contents == null) continue;
								
								if(item.getTagType().contentEquals("attr")){
									value = e.attr(attr_name);
								}else if(item.getTagType().contentEquals("html")){
									value = e.html();
								}else{ // text
									value = e.text();
								}
								
								if(value != null){ // 인코딩 해야할 필요가 있다면?
									//value = Util.EncodingUTF8(value);
									/**
									 * 값을 인코딩하거나 필터링해야할 필요가 있다면 
									 * 여기서 구현 정의
									 * */
									value = Util.Remove4ByteEmoji(value);
									if(regexs != null){
										isAllow = false;
										for(String regex : regexs){
											if(value.matches(regex)){
												isAllow = true;
												break;
											}
										}
										
										if(isAllow){
											contents.add(i, data_name, value);
										}else{
											aryContents[j] = null;
										}
									}else{
										contents.add(i, data_name, value);
									}
								}else{
									// 파싱을 찾을수 없는 상황임
									// 데이터 하나만 삑사리나도 웹페이지의  파싱의 모든걸 중단할 것인가?
									// 는 고려해봐야할 문제임
									System.err.println("[CONTENTS PARSING ERROR1]" + data_name +" "+ item.getSelector());
									return null;
								}
							}
						}else{
							// 파싱을 찾을수 없는 상황임
							// 데이터 하나만 삑사리나도 웹페이지의  파싱의 모든걸 중단할 것인가?
							// 는 고려해봐야할 문제임
							System.err.println("[CONTENTS PARSING ERROR2]" + data_name +" "+ item.getSelector());
							return null;
						}
					}
					
				}
				
				// 크롤러가 가지고 있는 정보를 담기 위한 루프
				if(aryContents != null){
					for(int i = 0 ; i < len_i ; i++){
						item = collect.getItem(i);
						type = item.getType();
						data_type = item.getDataType();
						data_name = item.getDataName();
						len_j = aryContents.length;
						for(int j = 0 ; j < len_j ; j++){
							contents = aryContents[j];
							if(contents == null) continue;
							if(type.contentEquals(Config.COLLECT_ITEMTYPE_URL)){
								contents.add(i, data_name, currentURL);
							}
							else if(type.contentEquals(Config.COLLECT_ITEMTYPE_DATETIME)){
								contents.add(i, data_name, currentDateTime);
							}
						}
					}
				}
			}
			
			if(ret == null) ret = new ArrayList<Contents>();
			if(aryContents != null && aryContents.length > 0){
				ret.addAll(Arrays.asList(aryContents));
			}
		}
		
		return ret;
	}
	
	/**
	 * 수집된 콘텐츠를 저장하는 메서드. DB, CSV, XML에 따라 저장하는 로직 구현<p>
	 * 
	 * @param urlInfo 처리할 웹페이지의 URL 정보
	 * @param contents 저장할 콘텐츠 배열 
	 * @return 저장된 콘텐츠 개수
	 * */
	public int saveContents(URLInfo urlInfo, List<Contents> aryContents){
		int idx = 0;
		if(aryContents != null){
			for(Contents contents : aryContents){
				if(contents != null){
					idx ++;
					try {
						Crawler.Writer.write(contents);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return idx;
	}
	
	public boolean isAllow(URLInfo curUrlInfo, String targetDomain, String targetSub){
		boolean ret = true;
		List<String> aryFilterAllow = mConfig.getFilterAllow();
		List<String> aryFilterDisallow = mConfig.getFilterDisallow();
		
		String url = targetDomain + targetSub;
		for(String filter : aryFilterAllow){
			if(filter.matches(Parser.REGEX_START_HTTP)){
				if(url.matches(filter)){
					ret = true; break;
				}
			}else{
				if(targetDomain.contentEquals(curUrlInfo.getDomainURL())){
					if(targetSub.matches(filter)){
						ret = true; break;
					}
				}
			}
		}
		
		for(String filter : aryFilterDisallow){
			if(filter.matches(Parser.REGEX_START_HTTP)){
				if(url.matches(filter)){
					ret = false; break;
				}
			}else{
				if(targetDomain.contentEquals(curUrlInfo.getDomainURL())){
					if(targetSub.matches(filter)){
						ret = false; break;
					}
				}
			}
		}
		return ret;
	}
	
	public boolean ifLeaf(URLInfo urlInfo){
		if(mConfig.CRAWLING_MAX_DEPTH != -1){
			if(mConfig.CRAWLING_MAX_DEPTH < urlInfo.getDepth()){
				return true;
			}
		}
		
		List<String> aryLeafURL = mConfig.getLeafURL();
		if(aryLeafURL != null){
			for(String filter : aryLeafURL){
				if(filter.matches(Parser.REGEX_START_HTTP)){
					if(urlInfo.getURL().matches(filter)){
	//					System.err.println(">> leaf - " + urlInfo.getURL());
						//Crawler.DB.insertErr(urlInfo.getURL(), "leaf" );
						return true;
					}
				}else{
					if(urlInfo.getSubURL().matches(filter)){
	//					System.err.println(">> leaf - " + urlInfo.getURL());
						//Crawler.DB.insertErr(urlInfo.getURL(), "leaf" );
						return true;
					}
				}
			}
		}
		return false;
	}
}
