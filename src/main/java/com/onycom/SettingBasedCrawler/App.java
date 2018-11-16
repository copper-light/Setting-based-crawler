package com.onycom.SettingBasedCrawler;

import java.io.File;
import java.net.URISyntaxException;

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
    			if(args[0].equalsIgnoreCase("-config_file")){
    				config_path = args[1];
    			}else{
    				System.out.println("input parameter : -config_file [FILE_PATH] -params [arg0] [argN]");
    				return;
    			}
    		}else{
    			System.out.println("input parameter : -config_file [FILE_PATH] -params [arg0] [argN]");
    			return;
    		}
    	}
    	
    	File configFile = new File(config_path);
    	String[][] params = null;
    	if(configFile.exists()){
    		if(config_path != null && !config_path.isEmpty()){
    			Crawler c = new Crawler(1, 0);
    			if(args.length >= 4){
    				if(args[2].equalsIgnoreCase("-params")){
        				params = new String[1][args.length - 3];
        				for(int j = 3 ;j  < args.length ; j++){
        					params[0][j-3] = args[j];
        				}
        			}
    				for(int i = 0 ; i < params.length ; i++){
        				for(int j = 0; j < params[i].length ; j++){
        	    			c.setConfig(config_path, null, params[i]);
        	    	    	c.start();
        				}
        			}
    			}else{
    				params = null;
	    			c.setConfig(config_path, null, null);
	    	    	c.start();
    			}
    			
    		}else{
    			System.err.println("[ERROR] Config file is not exists");
    		}
    	}else{
    		System.err.println("[ERROR] Config file is not exists");
    	}
    	
    }
}