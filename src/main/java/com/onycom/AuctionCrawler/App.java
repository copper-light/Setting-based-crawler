package com.onycom.AuctionCrawler;

import com.onycom.crawler.core.*;

/**
 * Hello world!
 */
public class App 
{
    public static void main(String[] args)
    {
    	Crawler c = new Crawler(1, 0);
    	c.setConfigFile("./config/config_action.json");
    	c.start();
    }
}