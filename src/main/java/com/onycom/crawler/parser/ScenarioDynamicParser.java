package com.onycom.crawler.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebElement;

import com.onycom.crawler.data.Action;
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
		if (document != null) {
			if (urlInfo.getAction() != null
					&& urlInfo.getAction().getType().equalsIgnoreCase(Action.TYPE_PARSE_CONTENTS)) {
				List<Contents> contents = this.parseContents(urlInfo, document);
				int saveCount = 0;
				if (contents != null) {
					saveCount = this.saveContents(urlInfo, contents);
				}
				parseCount++;
				mLogger.info("[parse count] " + parseCount + " @ " + urlInfo.getURL());
				mLogger.info("[save count] " + saveCount + " @ " + urlInfo.getURL());
			}

			if (urlInfo.getParseType() != URLInfo.PARSE_NORMAL) {
				List<URLInfo> list = parseURL(urlInfo, document);
				for (int i = list.size() - 1; 0 <= i; i--) {
					ret.add(list.get(i));
				}
			}
			// mLogger.debug("[list count ] " + list.size());
		} else {
			// 파싱 또는 로드 오류
			// mLogger.error("[load fail docment] " +
			// urlInfo.getLoadCheckSelectors());
		}
		return ret;
	}

	int point_x;
	int point_y;

	/**
	 * parseAction
	 */
	@Override
	public List<URLInfo> parseURL(URLInfo urlInfo, Document document) {
		List<URLInfo> ret = new ArrayList<URLInfo>();
		List<WebElement> wes;
		Scenario scen, newScen;
		Action action;
		String href, url, domain_url, sub_url, target, type, value, selector;
		String[] tmp;
		List<String> aryCheckSelector;
		URLInfo newUrlInfo;
		boolean allow = false;
		if (super.ifLeaf(urlInfo)) {
			return ret;
		}

		Elements els;
		Element el;

		if (urlInfo.getParseType() == URLInfo.PARSE_FIND_ACTION) {
			selector = urlInfo.getAction().getSelector();
			els = document.select(selector);
			action = urlInfo.getAction();
			for (int i = 0; i < els.size(); i++) {
				el = els.get(i);
				newUrlInfo = new URLInfo(urlInfo.getURL());
				newUrlInfo.setDepth(urlInfo.getDepth());
				newUrlInfo.setHighPriority(true);
				newUrlInfo.setAction(
						new Action(action.getTargetDepth(), el.cssSelector(), action.getType(), action.getValue()));
				ret.add(newUrlInfo);
			}
		} else if (urlInfo.getParseType() == URLInfo.PARSE_SCENARIO) {
			Map<Integer, Scenario> scenarios = getConfig().getScenarios();
			int curDepth = urlInfo.getDepth(); /* 현재 뎁스 */
			if (scenarios != null) {
				int len = scenarios.size();
				int target_depth;
				if (len != 0 && len >= curDepth) {
					scen = scenarios.get(curDepth);
					if (scen == null)
						return ret;
					len = scen.getSize();
					for (int i = 0; i < len; i++) {
						action = scen.getAction(i);
						type = action.getType(); /* 액션타입 */
						value = action.getValue(); /* 액션값 */
						target_depth = action.getTargetDepth(); /* 시나리오변경 */
						selector = action.getSelector(); /* selector */
						newUrlInfo = new URLInfo(urlInfo.getURL()).setDepth(target_depth);
						newUrlInfo.setHighPriority(true);
						newUrlInfo.setAction(new Action(target_depth, selector, type, value));
						ret.add(newUrlInfo);
					}
				}
			}
		}

		// mLogger.debug("-----> " + type +" @ " + action.getSelector());
		// //aryCheckSelector
		//// System.out.println("document size " +
		// document.toString().length());
		//// try {
		//// PrintWriter out = new PrintWriter("html_"+ new
		// Date().getTime()+".html");
		//// out.println(curDepth);
		//// out.println(selector.getSelector());
		//// out.println(document.toString());
		//// out.flush();
		//// out.close();
		//// } catch (FileNotFoundException e1) {
		//// e1.printStackTrace();
		//// }
		//
		//
		// selector = action.getSelector();
		// if(selector != null &&
		// !type.equalsIgnoreCase(Action.TYPE_PARSE_CONTENTS)){
		// wes = Scraper.GetEelements(action.getSelector());
		//
		//
		//
		//
		// if(wes != null){
		// //els = document.select(action.getSelector());
		// for(WebElement e : wes){
		// // href = e.attr("href").trim();
		// // //if(href.length() == 0) continue;
		// // tmp = Util.SplitDomainAndSubURL(urlInfo, href);
		// // domain_url = tmp[0];
		// // sub_url = tmp[1];
		// // url = domain_url + sub_url;
		// // allow = super.isAllow(urlInfo, domain_url, sub_url);
		// // if(allow){ }
		// System.out.println(e);
		// newUrlInfo = new URLInfo(urlInfo.getURL()).setDepth(target_depth);
		// newUrlInfo.setHighPriority(true);
		// newUrlInfo.setAction(new Action(target_depth, "", type, value));
		// ret.add(newUrlInfo);
		// }
		// }else{
		// // not found element
		// }
		// }else{
		// newUrlInfo = new URLInfo(urlInfo.getURL()).setDepth(target_depth);
		// newUrlInfo.setHighPriority(true);
		// newUrlInfo.setAction(new Action(target_depth, selector, type,
		// value));
		// ret.add(newUrlInfo);
		// }
		// }
		// }
		// }

		return ret;
	}
}
