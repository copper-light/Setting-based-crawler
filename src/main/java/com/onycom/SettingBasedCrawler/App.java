package com.onycom.SettingBasedCrawler;

import java.io.File;

import com.onycom.crawler.core.*;

/**
 * Hello world!
 */
public class App  
{
    public static void main(String[] args)
    {
//    	Crawler c = new Crawler(1, 0);
//    	c.setConfigFile("./config/config_DC_superidea.json");
//    	c.start();

    	String config_path = null;
    	if(args != null){
    		int len = args.length;
    		if(len >= 2){
    			if(args[0].equalsIgnoreCase("-config")){
    				config_path = args[1];
    			}
    		}else{
    			System.out.println("input parameter : -config [FILE_PATH] [arg0] [arg1] [arg2] [argN]");
    			return;
    		}
    	}
    	
    	File configFile = new File(config_path);
    		
    	if(configFile.exists()){
    		if(config_path != null && !config_path.isEmpty()){
    	    	Crawler c = new Crawler(1, 0);
    	    	//c.setCommendArgs())
    	    	c.setConfigFile(config_path, args);
    	    	c.start();
    		}else{
    			System.out.println("Config file is not exists");
    		}
    	}else{
    		System.out.println("Config file is not exists");
    	}
    }
}