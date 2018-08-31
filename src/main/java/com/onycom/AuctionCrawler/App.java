package com.onycom.AuctionCrawler;

import com.onycom.common.Util;
import com.onycom.crawler.core.*;
import com.onycom.crawler.data.URLInfo;
import com.onycom.crawler.parser.CustomParser;
import com.onycom.crawler.parser.Parser;
import com.onycom.crawler.parser.ScenarioStasticParser;
import com.onycom.crawler.parser.StaticParser;

/**
 * Hello world!
 */
public class App 
{
	
	
	
    public static void main(String[] args)
    {
    	Crawler c = new Crawler(1, 0);
    	c.setConfigFile("config_naver_blog.json");
    	c.start();
    }
}