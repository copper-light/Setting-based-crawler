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
import com.onycom.crawler.data.Work;

public class ScenarioStasticParser extends StaticParser{

	@Override
	public List<Work> parseURL(Work work, Document document) {
		List<Work> ret = new ArrayList<Work>();
		Elements els;
		Scenario scen;
		Action action;
		String href, url, domain_url, sub_url;
		String tmp[];
		boolean allow = false;
		
		if(super.ifLeaf(work)){
			return ret;
		}
		
		int curDepth = work.getDepth();
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
					if(els.size() > 0){
						for(Element e : els){
							href = e.attr("href").trim();
							if(href.length() == 0) continue;
							tmp = Util.SplitDomainAndSubURL(work, href);
							domain_url = tmp[0];
							sub_url = tmp[1];
							url = domain_url + sub_url;
							
							if(getConfig().getFilterAllow() != null && getConfig().getFilterAllow().size() > 0 &&
									getConfig().getFilterDisallow() != null  && getConfig().getFilterDisallow().size() > 0){
								allow = super.isAllow(work, domain_url, sub_url);
								if(allow) ret.add(new Work(url).setDepth(depth));
							}else{
								ret.add(new Work(url)
											.setDepth(depth));
							}
						}
					}else{
						work.result().addError(Work.Error.ERR_SCEN_ELEMENT, action.getSelector());
					}
				}
			}
		}
		
		return ret;
	}

	@Override
	public List<Work> checkDupliate(Work[] aryHistory, List<Work> aryNewUrl) {
		return aryNewUrl;
	}
	
}