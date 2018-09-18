package com.onycom.crawler.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.onycom.common.Util;
import com.onycom.crawler.data.Action;
import com.onycom.crawler.data.Scenario;
import com.onycom.crawler.data.URLInfo;

public class ScenarioStasticParser extends StaticParser{

	@Override
	public List<URLInfo> parseURL(URLInfo urlInfo, Document document) {
		List<URLInfo> ret = new ArrayList<URLInfo>();
		Elements els;
		Scenario scen;
		Action action;
		String href, url, domain_url, sub_url;
		String tmp[];
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
					depth = action.getTargetDepth();
					els = document.select(action.getSelector());
					els = els.select("a[href]");
					for(Element e : els){
						href = e.attr("href").trim();
						if(href.length() == 0) continue;
						tmp = Util.SplitDomainAndSubURL(urlInfo, href);
						domain_url = tmp[0];
						sub_url = tmp[1];
						url = domain_url + sub_url;
						
						allow = super.isAllow(urlInfo, domain_url, sub_url);
						if(allow) ret.add(new URLInfo(url).setDepth(depth));
					}
				}
			}
		}
		
		return ret;
	}

	@Override
	public List<URLInfo> checkDupliate(URLInfo[] aryHistory, List<URLInfo> aryNewUrl) {
		return aryNewUrl;
	}
	
}
